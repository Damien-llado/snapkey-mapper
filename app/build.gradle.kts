plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.damienllado.snapkeymapper"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.damienllado.snapkeymapper"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    // "full" sets the log permission up by itself (wireless debugging on this phone, needs the internet permission
    // for local sockets only); "lite" has no internet permission and you grant the permission by hand.
    flavorDimensions += "edition"
    productFlavors {
        create("full") { dimension = "edition"; ndk { abiFilters += "arm64-v8a" } }
        create("lite") { dimension = "edition"; applicationIdSuffix = ""; versionNameSuffix = "-lite" }
    }

    // Release builds must be signed: set SNAPKEY_KEYSTORE and SNAPKEY_KEY_PASSWORD (debug builds do not need them).
    val keystore = System.getenv("SNAPKEY_KEYSTORE")
    if (keystore != null) {
        signingConfigs {
            create("release") {
                storeFile = file(keystore)
                storePassword = System.getenv("SNAPKEY_KEY_PASSWORD")
                keyAlias = "snapkey"
                keyPassword = System.getenv("SNAPKEY_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        // the emulator test of the setup needs the x86_64 TLS library; released phones are arm64
        debug { ndk { abiFilters += "x86_64" } }
        release {
            isMinifyEnabled = false
            if (keystore != null) signingConfig = signingConfigs.getByName("release")
            else if (gradle.startParameter.taskNames.any { it.contains("Release") }) throw GradleException("SNAPKEY_KEYSTORE is not set: refusing to build an unsigned release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // In-app ADB client (GPL-3.0-or-later OR Apache-2.0) and its TLS 1.3 provider and certificate generator
    "fullImplementation"("com.github.MuntashirAkon:libadb-android:3.1.1")
    "fullImplementation"("org.conscrypt:conscrypt-android:2.7.0")
    "fullImplementation"("com.github.MuntashirAkon:sun-security-android:1.1")
}
