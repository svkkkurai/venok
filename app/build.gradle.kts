plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.sakkkurai.musicapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sakkkurai.musicapp"
        minSdk = 30
        //noinspection EditedTargetSdkVersion
        targetSdk = 35
        versionCode = 28
        versionName = "2.28"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation("androidx.viewpager2:viewpager2:1.1.0");
    implementation("androidx.media:media:1.7.0")
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-session:1.2.0")
    implementation("com.mpatric:mp3agic:0.9.1")
    implementation("com.google.code.gson:gson:2.10.1")
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
