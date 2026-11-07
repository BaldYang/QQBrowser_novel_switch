plugins {
    alias(libs.plugins.android.application) apply false
}

buildscript {
    repositories {

        google()
    }
    dependencies {

        classpath("com.google.gms:google-services:4.4.3")
    }
}

allprojects {
    repositories {

    }
}
