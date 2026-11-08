import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.bundling.Zip

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.personalfinance.tracker"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.personalfinance.tracker"
        minSdk = 26
        targetSdk = 37
        versionCode = 7
        versionName = "1.22"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release credentials are supplied outside the repository; never fall back to the debug key.
    signingConfigs {
        create("release") {
            storeFile = providers.environmentVariable("FINANCE_RELEASE_KEYSTORE").orNull?.let { file(it) }
            storePassword = providers.environmentVariable("FINANCE_RELEASE_STORE_PASSWORD").orNull
            keyAlias = providers.environmentVariable("FINANCE_RELEASE_KEY_ALIAS").orNull
            keyPassword = providers.environmentVariable("FINANCE_RELEASE_KEY_PASSWORD").orNull
        }
    }
    buildTypes {
        getByName("release") { signingConfig = signingConfigs.getByName("release") }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

val verifyReleaseSigning = tasks.register("verifyReleaseSigning") {
    doLast {
        val variables = listOf("FINANCE_RELEASE_KEYSTORE", "FINANCE_RELEASE_STORE_PASSWORD", "FINANCE_RELEASE_KEY_ALIAS", "FINANCE_RELEASE_KEY_PASSWORD")
        check(variables.all { !System.getenv(it).isNullOrBlank() }) {
            "Release signing requires all FINANCE_RELEASE_* environment variables."
        }
        check(file(System.getenv("FINANCE_RELEASE_KEYSTORE")).isFile) { "Release keystore file is missing." }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(verifyReleaseSigning) }

dependencies {
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso)
    testImplementation(libs.junit)
}

tasks.register<Zip>("archiveDebugApk") {
    group = "build"
    description = "Builds the debug APK and archives it in the project root."
    dependsOn(tasks.named("assembleDebug"))
    archiveFileName.set("AppAndroidBudjets.zip")
    destinationDirectory.set(rootProject.layout.projectDirectory.dir("../"))
    from(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk")) {
        into("AppAndroidBudjets")
    }
}
