plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.sonarqube") version "5.1.0.4882"
    id("jacoco")
    //id("com.google.dagger.hilt.android") version "2.51"
    kotlin("kapt")
    kotlin("plugin.serialization") version "1.9.22"
}
sonar {
    properties {
        property("sonar.projectKey", "SE2-Projekt-Wizard2025_Client_WizardSE2")
        property("sonar.organization", "se2-projekt-wizard2025")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.java.coveragePlugin", "jacoco")
        property("sonar.login", System.getenv("SONAR_TOKEN"))
        property("sonar.androidVariant", "debug")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "${project.projectDir}/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"
        )
        property("sonar.coverage.exclusions", "**/screen/**, **/sections/**, **/model/**, **/theme/**, **/MainActivity.kt, **/response/**")
    }
}
android {
    namespace = "at.klu.client_wizardse2"
    compileSdk = 35

    defaultConfig {
        applicationId = "at.klu.client_wizardse2"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            all {
                // it.useJUnitPlatform()
                it.finalizedBy(tasks.named("jacocoTestReport"))
            }
        }
    }
}
tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Generates code coverage report for the test task."
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        xml.outputLocation.set(file("${project.projectDir}/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"))
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/ui/presentation/screen/**",
        "**/ui/presentation/sections/**",
        "**/ui/theme/**",
        "**/ui/presentation/MainActivity*",
        "**/model/**"
    )


    val debugTree =
        fileTree("${project.layout.buildDirectory.get().asFile}/tmp/kotlin-classes/debug") {
            exclude(fileFilter)
        }

    val javaDebugTree =
        fileTree("${project.layout.buildDirectory.get().asFile}/intermediates/javac/debug") {
            exclude(fileFilter)
        }

    val mainSrc = listOf(
        "${project.projectDir}/src/main/java",
        "${project.projectDir}/src/main/kotlin"
    )

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree, javaDebugTree))
    executionData.setFrom(fileTree(project.layout.buildDirectory.get().asFile) {
        include("jacoco/testDebugUnitTest.exec")
        include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
    })
}
dependencies {

    //implementation(libs.androidx.hilt.navigation.compose)
    //implementation(libs.hilt.android)
    //kapt(libs.hilt.android.compiler)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.krossbow.stomp.core)
    implementation(libs.krossbow.websocket.okhttp)
    implementation(libs.krossbow.websocket.ktor)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.gson)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.reflect)
    testImplementation(libs.robolectric)
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.byte.buddy)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}