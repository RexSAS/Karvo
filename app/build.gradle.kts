import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "com.karvo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.karvo"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        // -PversionNameOverride=1.0.9 builds a private test APK that sees the latest release as newer.
        versionName = (findProperty("versionNameOverride") as String?) ?: "3.0"
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                fun prop(key: String): String =
                    keystoreProps.getProperty(key)?.takeIf { it.isNotBlank() }
                        ?: throw GradleException("keystore.properties is missing '$key'")
                val path = prop("storeFile")
                val resolved = if (path.startsWith("~/")) {
                    file(System.getProperty("user.home") + path.removePrefix("~"))
                } else {
                    file(path)
                }
                if (!resolved.exists()) {
                    throw GradleException("keystore.properties: storeFile '$path' does not exist")
                }
                storeFile = resolved
                storePassword = prop("storePassword")
                keyAlias = prop("keyAlias")
                keyPassword = prop("keyPassword")
            }
        }
    }

    // github: sideloaded APK with the in-app updater. play: Play updates the app, so no updater, install or internet permission.
    flavorDimensions += "store"
    productFlavors {
        create("github") {
            dimension = "store"
            buildConfigField("boolean", "UPDATER", "true")
        }
        create("play") {
            dimension = "store"
            buildConfigField("boolean", "UPDATER", "false")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePropsFile.exists()) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
    // MigrationTestHelper (Robolectric) reads the exported Room schemas from the debug assets; release builds don't ship them.
    sourceSets["debug"].assets.srcDir("$projectDir/schemas")
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.extensions)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.testing)
    testImplementation("org.json:json:20240303")
}
