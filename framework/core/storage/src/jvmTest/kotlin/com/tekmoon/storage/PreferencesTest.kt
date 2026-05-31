package com.tekmoon.storage

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.io.path.deleteRecursively
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * End-to-end exercise of [Preferences] through the JVM factory + real DataStore file in a
 * per-test temp directory. Verifies the typed get/put round-trip for every primitive, the
 * defaulting behavior when a key is absent, [Preferences.remove] / [Preferences.clear] /
 * [Preferences.contains], and the reactive Flow observation contract.
 */
class PreferencesTest {

    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
    private lateinit var tempDir: java.io.File
    private lateinit var prefs: Preferences

    @BeforeTest
    fun setUp() {
        tempDir = Files.createTempDirectory("tekmoon-prefs-test").toFile()
        // Random store name per test so DataStore's singleton-per-file invariant isn't tripped
        // when @Test methods run sequentially within the same JVM.
        prefs = createPreferences(name = "store-${System.nanoTime()}", baseDir = tempDir)
    }

    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
    @AfterTest
    fun tearDown() {
        tempDir.toPath().deleteRecursively()
    }

    // ---- defaults ----------------------------------------------------------

    @Test fun string_default_when_missing() = runTest {
        assertEquals("fallback", prefs.getString("absent", default = "fallback").first())
        assertEquals(null, prefs.getString("absent").first())
    }

    @Test fun int_default_when_missing() = runTest {
        assertEquals(42, prefs.getInt("absent", default = 42).first())
        assertEquals(0, prefs.getInt("absent").first())
    }

    @Test fun boolean_default_when_missing() = runTest {
        assertEquals(true, prefs.getBoolean("absent", default = true).first())
        assertEquals(false, prefs.getBoolean("absent").first())
    }

    // ---- round-trip per type ----------------------------------------------

    @Test fun string_round_trip() = runTest {
        prefs.putString("k", "hello")
        assertEquals("hello", prefs.getString("k").first())
    }

    @Test fun int_round_trip() = runTest {
        prefs.putInt("k", 7)
        assertEquals(7, prefs.getInt("k").first())
    }

    @Test fun long_round_trip() = runTest {
        prefs.putLong("k", 9_876_543_210L)
        assertEquals(9_876_543_210L, prefs.getLong("k").first())
    }

    @Test fun boolean_round_trip() = runTest {
        prefs.putBoolean("k", true)
        assertEquals(true, prefs.getBoolean("k").first())
    }

    @Test fun float_round_trip() = runTest {
        prefs.putFloat("k", 1.5f)
        assertEquals(1.5f, prefs.getFloat("k").first())
    }

    @Test fun double_round_trip() = runTest {
        prefs.putDouble("k", 3.14159)
        assertEquals(3.14159, prefs.getDouble("k").first())
    }

    @Test fun string_set_round_trip() = runTest {
        prefs.putStringSet("k", setOf("a", "b", "c"))
        assertEquals(setOf("a", "b", "c"), prefs.getStringSet("k").first())
    }

    // ---- type contract: one type per key -----------------------------------

    @Test fun reusing_a_key_with_a_different_type_overwrites_the_slot() = runTest {
        // DataStore Preferences keys are name-based, not (name, type). The second put
        // replaces the first; reading with the original type throws ClassCastException.
        // The framework contract: pick one type per key for its lifetime.
        prefs.putString("k", "text")
        prefs.putInt("k", 99)
        assertEquals(99, prefs.getInt("k").first())  // last writer wins
    }

    // ---- remove / clear / contains ----------------------------------------

    @Test fun remove_drops_the_value() = runTest {
        prefs.putString("k", "x")
        assertEquals("x", prefs.getString("k").first())
        prefs.remove("k")
        assertEquals(null, prefs.getString("k").first())
    }

    @Test fun remove_drops_the_slot_regardless_of_stored_type() = runTest {
        prefs.putString("k", "x")
        prefs.putInt("k", 1)  // overwrites — slot now holds Int
        prefs.remove("k")
        assertEquals(null, prefs.getString("k").first())  // default
        assertEquals(0, prefs.getInt("k").first())  // default
    }

    @Test fun clear_empties_the_store() = runTest {
        prefs.putString("a", "x")
        prefs.putInt("b", 1)
        prefs.clear()
        assertEquals(emptySet(), prefs.keys.first())
    }

    @Test fun contains_returns_true_after_put() = runTest {
        assertFalse(prefs.contains("k"))
        prefs.putString("k", "x")
        assertTrue(prefs.contains("k"))
    }

    @Test fun keys_flow_reflects_writes() = runTest {
        assertEquals(emptySet(), prefs.keys.first())
        prefs.putString("a", "x")
        prefs.putInt("b", 1)
        assertEquals(setOf("a", "b"), prefs.keys.first())
    }
}
