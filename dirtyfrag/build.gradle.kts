plugins {
    id("com.android.application")
}

/*
 * DirtyFrag companion APK.
 *
 * It intentionally keeps the original `df.root` applicationId: the eight
 * prebuilt LKMs embed `/data/user_de/0/df.root/...` paths, so repackaging this
 * module under the GhostLock package would break the module/bootstrap handoff.
 * GhostLock treats this APK as an optional companion and launches it through an
 * explicit Intent.
 */

val dirtyFragKeystore = file("keystore.jks")

android {
    namespace = "df.root"
    ndkVersion = "30.0.16248370"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "df.root"
        minSdk = 31
        targetSdk = 37

        versionCode = 35
        versionName = "1.18"

        ndk {
            // arm64 carries the real payload; x86/x86_64 are UI-only stubs for
            // emulators such as MuMu.
            abiFilters += listOf("arm64-v8a", "x86", "x86_64")
        }
    }

    signingConfigs {
        if (dirtyFragKeystore.isFile) {
            create("dirtyfrag") {
                storeFile = dirtyFragKeystore
                storePassword = "dirtyfrag"
                keyAlias = "dirtyfrag"
                keyPassword = "dirtyfrag"
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = if (dirtyFragKeystore.isFile) {
                signingConfigs.getByName("dirtyfrag")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        release {
            signingConfig = if (dirtyFragKeystore.isFile) {
                signingConfigs.getByName("dirtyfrag")
            } else {
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    lint {
        checkReleaseBuilds = false
    }

    buildFeatures {
        viewBinding = false
        buildConfig = true
    }

    /*
     * The native payloads are prebuilt into src/main/jniLibs for every ABI.
     * Keeping externalNativeBuild out of the Gradle graph is intentional:
     * libcxx.S is AArch64 shellcode, so an x86 CMake pass would fail, while the
     * x86/x86_64 APK variants carry UI-only stubs.
     */
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}
