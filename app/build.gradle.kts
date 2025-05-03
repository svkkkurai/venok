plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.sakkkurai.musicapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sakkkurai.musicapp"
        minSdk = 30
        targetSdk = 35
        versionCode = 312
        versionName = "3.1"
    }


    buildTypes {
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {

    implementation(libs.room.runtime);
    annotationProcessor(libs.room.compiler)
    implementation(libs.viewpager2);
    implementation(libs.media)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.mp3agic)
    implementation(libs.gson)
//    implementation("me.zhanghai.android.fastscroll:library:1.3.0")
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
