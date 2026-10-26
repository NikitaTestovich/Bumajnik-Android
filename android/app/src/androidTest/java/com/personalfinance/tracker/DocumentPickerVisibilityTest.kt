package com.personalfinance.tracker

import android.content.Intent
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DocumentPickerVisibilityTest {
    @Test fun appCanDiscoverCsvPickersWhenInstalledOnDevice() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        listOf(Intent.ACTION_OPEN_DOCUMENT, Intent.ACTION_CREATE_DOCUMENT).forEach { action ->
            val output = ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
                "cmd package query-activities --brief -a $action -c android.intent.category.OPENABLE -t text/csv",
            )).bufferedReader().use { it.readText() }
            assumeTrue("No system CSV provider installed", output.contains("activities found") && !output.startsWith("0 activities"))
            val intent = Intent(action).addCategory(Intent.CATEGORY_OPENABLE).setType("text/csv")
            assertNotNull("Manifest queries must expose the installed CSV picker for $action", intent.resolveActivity(instrumentation.targetContext.packageManager))
        }
    }
}
