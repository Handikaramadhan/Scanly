import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.artifacts.component.ModuleComponentIdentifier

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
val releaseProperties = Properties()
val releasePropertiesFile = rootProject.file("release-signing.properties")
if (releasePropertiesFile.exists()) releasePropertiesFile.inputStream().use { releaseProperties.load(it) }

android {
    namespace = "com.scanly.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.scanly.app"
        minSdk = 23
        targetSdk = 36
        versionCode = 11
        versionName = "1.7.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true; buildConfig = true }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.maxHeapSize = "1024m"
            it.systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
            it.jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED", "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED", "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED")
        }
    }
    signingConfigs {
        if (releasePropertiesFile.exists()) create("release") {
            fun required(name: String) = releaseProperties.getProperty(name)
                ?: throw GradleException("Lengkapi $name di release-signing.properties")
            storeFile = file(required("storeFile"))
            storePassword = required("storePassword")
            keyAlias = required("keyAlias")
            keyPassword = required("keyPassword")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("androidx.exifinterface:exifinterface:1.4.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("org.apache.pdfbox:pdfbox:3.0.4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    testImplementation("androidx.test:core-ktx:1.7.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// Coordinate inventory only; signing secrets and local SDK paths are never written here.
tasks.register("writeDependencyInventory") {
    val output = layout.buildDirectory.file("reports/runtime-dependencies.txt")
    outputs.file(output)
    outputs.upToDateWhen { false }
    doLast {
        val modules = listOf("debugRuntimeClasspath", "releaseRuntimeClasspath").flatMap { name ->
            configurations.getByName(name).incoming.resolutionResult.allComponents.mapNotNull {
                (it.id as? ModuleComponentIdentifier)?.let { id -> "${id.group}:${id.module}:${id.version}" }
            }
        }.distinct().sorted()
        output.get().asFile.apply { parentFile.mkdirs(); writeText(modules.joinToString("\n", postfix = "\n")) }
    }
}
