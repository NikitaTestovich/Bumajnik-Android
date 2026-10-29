package com.personalfinance.tracker

import android.os.Build
import android.os.Process
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personalfinance.tracker.data.preferences.HomeLayoutStore
import com.personalfinance.tracker.ui.home.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Run seed, force-stop the target package, then verify in a separate instrumentation invocation. */
@RunWith(AndroidJUnit4::class)
class HomeProcessRestartTest {
    @Test fun savedWidgetLayoutSurvivesNewProcess() {
        val phase = InstrumentationRegistry.getArguments().getString("homeRestartPhase")
        assumeTrue(phase == "seed" || phase == "verify")
        check(Build.HARDWARE in listOf("ranchu", "goldfish"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val marker = context.getSharedPreferences("synthetic-home-restart", 0)
        val store = HomeLayoutStore(context)
        val expected = moveHomeBlock(normalizeHomeLayout(emptyList()), HomeBlock.EXPENSES, 0)
            .map { it.copy(visible = it.id == HomeBlock.EXPENSES) }
        if (phase == "seed") {
            // Stop an actual activity so Android flushes pending apply() writes as in a normal exit.
            ActivityScenario.launch(MainActivity::class.java).use {
                store.save(expected)
                assertEquals(expected, store.layout.value)
            }
            assertTrue(marker.edit().putInt("seedPid", Process.myPid()).commit())
        } else {
            try {
                val seedPid = marker.getInt("seedPid", -1)
                assertTrue("Explicit seed phase is required", seedPid > 0)
                assertNotEquals("Verification must run in a new process", seedPid, Process.myPid())
                assertEquals(expected, store.layout.value)
            } finally {
                ActivityScenario.launch(MainActivity::class.java).use { store.reset() }
                marker.edit().clear().commit()
            }
        }
    }
}
