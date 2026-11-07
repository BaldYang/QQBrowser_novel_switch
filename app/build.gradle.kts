import java.nio.charset.Charset

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.QQBrowsernovelswitch"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.QQBrowsernovelswitch"
        minSdk = 24
        targetSdk = 37
        versionCode = 3
        versionName = "2.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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

    buildFeatures {

        aidl = true

        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.fragment)
    implementation(libs.preference)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.recyclerview)

    testImplementation(libs.junit)
    testImplementation("org.json:json:20260814")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.firebase.analytics)

    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
}

val sanitizeAidlSources = tasks.register("sanitizeAidlSources") {
    group = "build"
    description = "Convert generated AIDL sources to UTF-8 to avoid unmappable character errors"
    doLast {
        val outputDir = layout.buildDirectory.dir("generated/aidl_source_output_dir").get().asFile
        if (!outputDir.isDirectory) {
            return@doLast
        }
        outputDir.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".java") }
            .forEach { file ->
                val bytes = file.readBytes()
                if (String(bytes, Charsets.UTF_8).contains('\uFFFD')) {
                    val gbk = Charset.forName("GBK")
                    file.writeBytes(String(bytes, gbk).toByteArray(Charsets.UTF_8))
                    logger.lifecycle("sanitized AIDL source: ${file.relativeTo(outputDir)}")
                }
            }
    }
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn(sanitizeAidlSources)
}

sanitizeAidlSources.configure {
    mustRunAfter(tasks.matching { it.name.endsWith("Aidl") })
}
