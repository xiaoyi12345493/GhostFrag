package com.ghostfrag.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DirtyFragCompanionTest {
    @Test
    fun `parses gki kmi from uname release`() {
        val kernel = DirtyFragCompanion.parseKernel(
            "6.6.118-android15-8-g2e6b9c3812c5-ab15114928-4k",
        )

        assertEquals(6, kernel?.major)
        assertEquals(6, kernel?.minor)
        assertEquals(15, kernel?.kmi)
        assertTrue(DirtyFragCompanion.supportsKernel(kernel!!))
    }

    @Test
    fun `falls back to vendor release when uname has no kmi`() {
        val kernel = DirtyFragCompanion.parseKernel(
            release = "5.15.100-g1234567890ab",
            vendorRelease = "14",
            systemRelease = "13",
        )

        assertEquals(5, kernel?.major)
        assertEquals(15, kernel?.minor)
        assertEquals(14, kernel?.kmi)
        assertTrue(DirtyFragCompanion.supportsKernel(kernel!!))
    }

    @Test
    fun `accepts kernel lines with a single prebuilt module`() {
        for (release in listOf(
            "6.1.145-android14-11-g74d1702dab4d-ab14669069",
            "6.6.118-android15-8-g21be90ecfb5e-ab15480137-4k",
            "6.12.38-android16-5-g1d46253471dd-ab15048002-4k",
            "6.18.1-android17-1-g000000000000-ab00000000",
        )) {
            val kernel = DirtyFragCompanion.parseKernel(release)
            assertTrue(release, DirtyFragCompanion.supportsKernel(kernel!!))
        }
    }

    @Test
    fun `rejects kernels without a prebuilt module`() {
        val kernel = DirtyFragCompanion.parseKernel("4.14.180-g1234567890ab")
        assertFalse(DirtyFragCompanion.supportsKernel(kernel!!))
    }

    @Test
    fun `rejects a release without a numeric kernel prefix`() {
        assertNull(DirtyFragCompanion.parseKernel("not-a-kernel-release"))
    }
}
