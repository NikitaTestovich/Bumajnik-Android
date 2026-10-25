package com.personalfinance.tracker.data.csv

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

/** Document picker contracts with an explicit OPENABLE category for Android 11+ providers. */
class CreateCsvDocument : ActivityResultContract<String, Uri?>() {
    override fun createIntent(context: Context, input: String): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("text/csv")
            .putExtra(Intent.EXTRA_TITLE, input)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent.takeIf { resultCode == Activity.RESULT_OK }?.data
}

class OpenCsvDocument : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*")
            .putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/csv", "text/comma-separated-values"))

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent.takeIf { resultCode == Activity.RESULT_OK }?.data
}
