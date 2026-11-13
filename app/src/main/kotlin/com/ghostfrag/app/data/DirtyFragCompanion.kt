package com.ghostfrag.app.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import com.ghostfrag.app.domain.model.DirtyFragAvailability

/**
 * Detects and launches the optional DirtyFrag companion APK.
 *
 * DirtyFrag keeps its original `df.root` package: its eight prebuilt LKMs embed
 * `/data/user_de/0/df.root/...` paths. GhostFrag therefore never links or
 * repackages DirtyFrag; it only checks whether the companion can run on this
 * kernel and hands the run over through an explicit Intent.
 */
class DirtyFragCompanion(private val context: Context) {
    fun availability(selectedManager: String? = null): DirtyFragAvailability {
        val release = System.getProperty("os.version").orEmpty()
        val kernel = parseKernel(
            release = release,
            vendorRelease = systemProperty(VENDOR_RELEASE).orEmpty(),
            systemRelease = systemProperty(SYSTEM_RELEASE).orEmpty(),
        )
        val manager = resolveManager(selectedManager)
        return DirtyFragAvailability(
            installed = isInstalled(),
            kernelSupported = kernel != null && supportsKernel(kernel),
            managerPackage = manager?.packageName,
            managerReady = manager?.hasKsud == true,
            kernelRelease = release,
        )
    }

    /** Sends the companion its auto-run flag and the manager GhostFrag resolved. */
    fun launch(managerPackage: String?): Boolean {
        val intent = Intent().apply {
            setClassName(PACKAGE, ACTIVITY)
            putExtra(EXTRA_AUTORUN, true)
            if (!managerPackage.isNullOrBlank()) putExtra(EXTRA_SU_MANAGER, managerPackage)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    private fun isInstalled(): Boolean = runCatching {
        context.packageManager.getPackageInfo(PACKAGE, 0)
    }.isSuccess

    private fun resolveManager(selectedManager: String?): ManagerScanner.Candidate? {
        val pm = context.packageManager
        val candidates = runCatching {
            ManagerScanner.scan(pm, context.packageName)
        }.getOrDefault(emptyList())
        if (!selectedManager.isNullOrBlank()) {
            /* The selected package may be hidden from a direct getApplicationInfo
             * on some emulators; the scanner already has the shell fallback. */
            return candidates.firstOrNull { it.packageName == selectedManager }
        }
        return candidates.firstOrNull { it.hasKsud }
    }

    @SuppressLint("PrivateApi")
    private fun systemProperty(key: String): String? = runCatching {
        val cls = Class.forName("android.os.SystemProperties")
        val get = cls.getMethod("get", String::class.java, String::class.java)
        get.invoke(null, key, "") as? String
    }.getOrNull()?.takeIf { it.isNotBlank() }

    companion object {
        const val PACKAGE = "df.root"
        const val ACTIVITY = "df.root.MainActivity"
        const val EXTRA_AUTORUN = "df.autorun"
        const val EXTRA_SU_MANAGER = "df.su_manager"

        private const val VENDOR_RELEASE = "ro.vendor.build.version.release"
        private const val SYSTEM_RELEASE = "ro.build.version.release"

        private val VersionPrefix = Regex("""^\s*(\d+)\.(\d+)""")
        private val AndroidKmi = Regex("""android(\d+)""")
        private val LeadingInt = Regex("""^\s*(\d+)""")

        /**
         * Kernel major/minor plus the GKI level. Mirrors DirtyFrag's
         * `read_device_versions`/`select_ko_image`: the kernel pair is the hard
         * filter, and 5.10/5.15 carry two modules so the KMI decides.
         */
        internal data class DirtyFragKernel(val major: Int, val minor: Int, val kmi: Int?)

        internal fun parseKernel(
            release: String,
            vendorRelease: String = "",
            systemRelease: String = "",
        ): DirtyFragKernel? {
            val match = VersionPrefix.find(release) ?: return null
            val major = match.groupValues[1].toIntOrNull() ?: return null
            val minor = match.groupValues[2].toIntOrNull() ?: return null
            val kmi = AndroidKmi.find(release)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it > 0 }
                ?: firstPositiveInt(vendorRelease)
                ?: firstPositiveInt(systemRelease)
            return DirtyFragKernel(major, minor, kmi)
        }

        /** The eight prebuilt `dfroot-*.ko` kernel pairs. */
        internal fun supportsKernel(kernel: DirtyFragKernel): Boolean =
            when (kernel.major to kernel.minor) {
                5 to 10, 5 to 15 -> true
                6 to 1, 6 to 6, 6 to 12, 6 to 18 -> true
                else -> false
            }

        private fun firstPositiveInt(value: String): Int? =
            LeadingInt.find(value)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it > 0 }
    }
}
