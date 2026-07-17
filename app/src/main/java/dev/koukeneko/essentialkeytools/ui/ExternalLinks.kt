package dev.koukeneko.essentialkeytools.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import dev.koukeneko.essentialkeytools.R

const val PRIVACY_POLICY_URL =
    "https://github.com/KoukeNeko/EssentialKeyTools/blob/main/PRIVACY.md"

private const val PLAY_STORE_PACKAGE = "com.android.vending"
private const val PRODUCTION_APPLICATION_ID = "dev.koukeneko.essentialkeytools"
private const val PLAY_STORE_WEB_URL =
    "https://play.google.com/store/apps/details?id=$PRODUCTION_APPLICATION_ID"

/** Opens a public project page in the user's browser without crashing if no handler is installed. */
fun openExternalUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (error: android.content.ActivityNotFoundException) {
        Toast.makeText(context, R.string.external_link_open_failed, Toast.LENGTH_LONG).show()
    }
}

/** Opens the production listing in Google Play, with the web listing as a safe fallback. */
fun openPlayStoreListing(context: Context) {
    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=$PRODUCTION_APPLICATION_ID")
    ).apply {
        setPackage(PLAY_STORE_PACKAGE)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(marketIntent)
    } catch (error: android.content.ActivityNotFoundException) {
        openExternalUrl(context, PLAY_STORE_WEB_URL)
    }
}
