package dev.koukeneko.essentialkeytools.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import dev.koukeneko.essentialkeytools.R

const val PRIVACY_POLICY_URL =
    "https://github.com/KoukeNeko/EssentialKeyTools/blob/main/PRIVACY.md"

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
