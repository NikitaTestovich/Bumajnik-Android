package com.personalfinance.tracker

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.update.ApkIdentityReader
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApkIdentityReaderTest {
    @Test fun installedPackageAndItsArchiveHaveTheSameIdentity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val reader = ApkIdentityReader(context)
        assertEquals(reader.installed(), reader.archive(File(context.packageCodePath)))
        val invalid = File.createTempFile("not-an-apk", ".apk", context.cacheDir)
        try {
            invalid.writeText("invalid")
            assertTrue(runCatching { reader.archive(invalid) }.isFailure)
        } finally { invalid.delete() }
    }
}
