package com.xephyrka.liora

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test class that executes on a physical Android device or emulator.
 * Used for testing components that require a real Android environment.
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    /** 
     * Verifies that the application's context is correctly resolved to the expected package name.
     */
    @Test
    fun useAppContext() {
        /** The context of the application currently under test. */
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.xephyrka.liora", appContext.packageName)
    }
}
