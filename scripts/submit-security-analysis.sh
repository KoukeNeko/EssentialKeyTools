#!/usr/bin/env bash

set -u -o pipefail

APK_PATH="${1:-}"
if [ -z "$APK_PATH" ] || [ ! -f "$APK_PATH" ]; then
  echo "Usage: $0 <signed-apk>" >&2
  exit 2
fi

OUTPUT_FILE="${GITHUB_OUTPUT:-/dev/null}"
SUMMARY_FILE="${GITHUB_STEP_SUMMARY:-/dev/null}"
WORK_DIR="$(mktemp -d "${RUNNER_TEMP:-/tmp}/essential-key-tools-security.XXXXXX")"
trap 'rm -rf "$WORK_DIR"' EXIT

SHA256="$(sha256sum "$APK_PATH" | awk '{print $1}')"
VT_STATUS="skipped"
VT_REPORT_URL=""
KOODOUS_STATUS="skipped"
KOODOUS_REPORT_URL=""

warn() {
  echo "::warning::$1"
}

request() {
  local output_path="$1"
  shift
  local status
  if ! status="$(curl --silent --show-error --location \
    --connect-timeout 15 --max-time 180 \
    --output "$output_path" --write-out '%{http_code}' "$@")"; then
    status="000"
  fi
  printf '%s' "$status"
}

submit_to_virustotal() {
  if [ -z "${VT_API_KEY:-}" ]; then
    warn "VT_API_KEY is not configured; skipping VirusTotal submission."
    return
  fi

  local details_body="$WORK_DIR/virustotal-details.json"
  local status
  status="$(request "$details_body" \
    --header "x-apikey: $VT_API_KEY" \
    "https://www.virustotal.com/api/v3/files/$SHA256")"

  if [ "$status" = "200" ]; then
    VT_STATUS="existing"
    VT_REPORT_URL="https://www.virustotal.com/gui/file/$SHA256"
    return
  fi
  if [ "$status" != "404" ]; then
    VT_STATUS="failed"
    warn "VirusTotal lookup failed with HTTP $status; continuing the release."
    return
  fi

  local upload_url_body="$WORK_DIR/virustotal-upload-url.json"
  status="$(request "$upload_url_body" \
    --header "x-apikey: $VT_API_KEY" \
    "https://www.virustotal.com/api/v3/files/upload_url")"
  if [ "$status" != "200" ]; then
    VT_STATUS="failed"
    warn "VirusTotal upload URL request failed with HTTP $status; continuing the release."
    return
  fi

  local upload_url
  if ! upload_url="$(jq --raw-output --exit-status '.data | select(type == "string" and length > 0)' "$upload_url_body")"; then
    VT_STATUS="failed"
    warn "VirusTotal returned an invalid upload URL; continuing the release."
    return
  fi
  case "$upload_url" in
    https://www.virustotal.com/*|https://*.virustotal.com/*) ;;
    *)
      VT_STATUS="failed"
      warn "VirusTotal returned an unexpected upload host; continuing the release."
      return
      ;;
  esac

  local upload_body="$WORK_DIR/virustotal-upload.json"
  status="$(request "$upload_body" \
    --request POST \
    --header "x-apikey: $VT_API_KEY" \
    --form "file=@$APK_PATH" \
    "$upload_url")"
  if [[ "$status" =~ ^2[0-9][0-9]$ ]]; then
    VT_STATUS="submitted"
    VT_REPORT_URL="https://www.virustotal.com/gui/file/$SHA256"
  else
    VT_STATUS="failed"
    warn "VirusTotal upload failed with HTTP $status; continuing the release."
  fi
}

submit_to_koodous() {
  if [ -z "${KOODOUS_TOKEN:-}" ]; then
    warn "KOODOUS_TOKEN is not configured; skipping Koodous submission."
    return
  fi

  local details_body="$WORK_DIR/koodous-details.json"
  local status
  status="$(request "$details_body" \
    --header "Authorization: Token $KOODOUS_TOKEN" \
    "https://developer.koodous.com/apks/$SHA256/")"

  if [ "$status" = "200" ]; then
    KOODOUS_STATUS="existing"
    KOODOUS_REPORT_URL="https://koodous.com/apks/$SHA256"
    return
  fi
  if [ "$status" != "404" ]; then
    KOODOUS_STATUS="failed"
    warn "Koodous lookup failed with HTTP $status; continuing the release."
    return
  fi

  local upload_url_body="$WORK_DIR/koodous-upload-url.json"
  status="$(request "$upload_url_body" \
    --header "Authorization: Token $KOODOUS_TOKEN" \
    "https://developer.koodous.com/apks/$SHA256/get_upload_url/")"
  if [ "$status" != "200" ]; then
    KOODOUS_STATUS="failed"
    warn "Koodous upload URL request failed with HTTP $status; continuing the release."
    return
  fi

  local upload_url
  if ! upload_url="$(jq --raw-output --exit-status '.upload_url | select(type == "string" and length > 0)' "$upload_url_body")"; then
    KOODOUS_STATUS="failed"
    warn "Koodous returned an invalid upload URL; continuing the release."
    return
  fi
  case "$upload_url" in
    https://warehouse.koodous.com/warehouse/upload/*) ;;
    *)
      KOODOUS_STATUS="failed"
      warn "Koodous returned an unexpected upload host; continuing the release."
      return
      ;;
  esac

  local upload_body="$WORK_DIR/koodous-upload.json"
  status="$(request "$upload_body" \
    --request POST \
    --form "file=@$APK_PATH" \
    "$upload_url")"
  if [[ "$status" =~ ^2[0-9][0-9]$ ]]; then
    KOODOUS_STATUS="submitted"
    KOODOUS_REPORT_URL="https://koodous.com/apks/$SHA256"
  else
    KOODOUS_STATUS="failed"
    warn "Koodous upload failed with HTTP $status; continuing the release."
  fi
}

submit_to_virustotal
submit_to_koodous

{
  echo "sha256=$SHA256"
  echo "virustotal_status=$VT_STATUS"
  echo "virustotal_report_url=$VT_REPORT_URL"
  echo "koodous_status=$KOODOUS_STATUS"
  echo "koodous_report_url=$KOODOUS_REPORT_URL"
} >> "$OUTPUT_FILE"

{
  echo "### APK security analysis"
  echo
  echo "SHA-256: \`$SHA256\`"
  echo
  if [ -n "$VT_REPORT_URL" ]; then
    echo "- VirusTotal: [$VT_STATUS]($VT_REPORT_URL)"
  else
    echo "- VirusTotal: $VT_STATUS"
  fi
  if [ -n "$KOODOUS_REPORT_URL" ]; then
    echo "- Koodous: [$KOODOUS_STATUS]($KOODOUS_REPORT_URL)"
  else
    echo "- Koodous: $KOODOUS_STATUS"
  fi
} >> "$SUMMARY_FILE"
