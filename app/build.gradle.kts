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

    // Release signing is optional: set SNAPKEY_KEYSTORE and SNAPKEY_KEY_PASSWORD to sign.
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
        release {
            isMinifyEnabled = false
            if (keystore != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
