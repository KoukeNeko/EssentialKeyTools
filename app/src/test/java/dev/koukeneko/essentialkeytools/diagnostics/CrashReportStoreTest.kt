package dev.koukeneko.essentialkeytools.diagnostics

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CrashReportStoreTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun storeIn(fileName: String = "last_crash_report.txt") =
        CrashReportStore(File(temporaryFolder.root, fileName))

    @Test
    fun readsBackWhatItWrote() {
        val store = storeIn()

        assertTrue(store.write("crash text"))

        assertEquals("crash text", store.read())
    }

    @Test
    fun readsNullWhenNothingHasCrashed() {
        assertNull(storeIn().read())
    }

    @Test
    fun keepsOnlyTheMostRecentReport() {
        val store = storeIn()

        store.write("first")
        store.write("second")

        assertEquals("second", store.read())
    }

    @Test
    fun clearsAndReportsSuccessEvenWithNothingToClear() {
        val store = storeIn()
        store.write("crash text")

        assertTrue(store.clear())
        assertNull(store.read())
        assertTrue("clearing twice is not a failure", store.clear())
    }

    @Test
    fun reportsFailureInsteadOfThrowingWhenStorageIsUnusable() {
        val unwritable = CrashReportStore(File(temporaryFolder.root, "missing-dir/report.txt"))

        assertFalse(unwritable.write("crash text"))
        assertNull(unwritable.read())
    }
}
