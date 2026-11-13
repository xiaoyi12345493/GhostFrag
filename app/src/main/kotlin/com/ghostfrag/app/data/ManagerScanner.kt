package com.ghostfrag.app.data

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import java.io.File
import java.util.concurrent.TimeUnit

/** Finds root-manager candidates and orders them for ksud resolution. */
object ManagerScanner {
    const val PREF_KEY = "manager_package"

    /** Built-in KernelSU-family managers, priority order. */
    val BUILTIN = listOf(
        "me.weishu.kernelsu.pr",
        "me.weishu.kernelsu",
        "com.sukisu.ultra.pr",
        "com.sukisu.ultra",
        "com.resukisu.resukisu",
        "org.bakasu.bakasu",
        "com.kowx712.supermanager",
    )

    data class Candidate(
        val packageName: String,
        val label: String,
        val hasKsud: Boolean,
        val builtin: Boolean,
    )

    /** Fallback display names when the package is hidden from label queries. */
    private val BUILTIN_LABELS = mapOf(
        "me.weishu.kernelsu.pr" to "KernelSU (PR)",
        "me.weishu.kernelsu" to "KernelSU",
        "com.sukisu.ultra.pr" to "SukiSU Ultra (PR)",
        "com.sukisu.ultra" to "SukiSU Ultra",
        "com.resukisu.resukisu" to "ReSukiSU",
        "org.bakasu.bakasu" to "BakaSU",
        "com.kowx712.supermanager" to "KowSU",
    )

    /**
     * All non-system apps, compatible first. Relies on QUERY_ALL_PACKAGES;
     * if that permission is ever dropped, filter to BUILTIN here.
     *
     * Package visibility is not uniform across OEM/emulator builds: MuMu on
     * Android 15 omits some installed managers from getInstalledPackages()
     * even though a named getApplicationInfo() succeeds. The built-ins are
     * therefore queried explicitly as a fallback, which keeps KSU visible on
     * those devices without weakening the system/self filters.
     */
    @Suppress("DEPRECATION")
    fun scan(pm: PackageManager, selfPackage: String): List<Candidate> {
        val seen = mutableSetOf<String>()
        val candidates = mutableListOf<Candidate>()

        fun add(packageName: String, ai: ApplicationInfo) {
            if (packageName == selfPackage || packageName in seen) return
            if ((ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0) return
            seen += packageName
            candidates += candidateOf(pm, packageName, ai)
        }

        pm.getInstalledPackages(0).forEach { pi ->
            pi.applicationInfo?.let { add(pi.packageName, it) }
        }
        for (pkg in BUILTIN) {
            if (pkg in seen) continue
            val ai = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull()
            if (ai != null) {
                add(pkg, ai)
                continue
            }
            val fallback = candidateFromPmPath(pkg)
            if (fallback != null) {
                Log.d("GhostFragMgr", "shell fallback: $pkg hasKsud=true")
                seen += pkg
                candidates += fallback
            }
        }
        return candidates.sortedWith(sortComparator())
    }

    /**
     * MuMu/Android 15 can hide an installed manager from every PackageManager
     * list and named lookup, while `/system/bin/pm path` still resolves it.
     * Derive the lib dir from the APK path and probe the known ABI folders.
     */
    private fun candidateFromPmPath(pkg: String): Candidate? {
        val output = runCatching {
            val process = ProcessBuilder("/system/bin/pm", "path", pkg)
                .redirectErrorStream(true)
                .start()
            val text = process.inputStream.bufferedReader().use { it.readText() }
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroy()
                return null
            }
            text
        }.getOrNull() ?: return null
        val apkPath = output.lineSequence()
            .firstOrNull { it.startsWith("package:") }
            ?.removePrefix("package:")?.trim()
            ?: return null
        val appDir = File(apkPath).parentFile ?: return null
        val hasKsud = listOf(
            "lib/x86_64",
            "lib/arm64-v8a",
            "lib/x86",
            "lib/armeabi-v7a",
            "lib",
        ).any { File(appDir, "$it/libksud.so").isFile }
        if (!hasKsud) return null
        return Candidate(
            packageName = pkg,
            label = BUILTIN_LABELS[pkg] ?: pkg,
            hasKsud = true,
            builtin = pkg in BUILTIN,
        )
    }

    private fun candidateOf(
        pm: PackageManager,
        packageName: String,
        ai: ApplicationInfo,
    ): Candidate {
        val dir = ai.nativeLibraryDir
        return Candidate(
            packageName = packageName,
            label = runCatching { ai.loadLabel(pm).toString() }.getOrDefault(packageName),
            hasKsud = dir != null && File(dir, "libksud.so").isFile,
            builtin = packageName in BUILTIN,
        )
    }

    /** Pure sort, extracted so it is testable without a PackageManager. */
    fun sortComparator() = compareByDescending<Candidate> { it.builtin && it.hasKsud }
        .thenByDescending { it.hasKsud }
        .thenBy { it.label.lowercase() }

    /** Chosen package first, then the rest of the built-ins in priority order. */
    fun resolvePackages(chosen: String?): List<String> =
        if (chosen.isNullOrBlank()) BUILTIN
        else listOf(chosen) + BUILTIN.filter { it != chosen }
}
