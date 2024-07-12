plugins {
    // Penggunaan Plugin
    alias(libs.plugins.android.application) // Android Application
    alias(libs.plugins.jetbrains.kotlin.android) // Kotlin
    alias(libs.plugins.google.gms.google.services) // Google Services
}

android {
    // Menetapkan Namespace dan versi compile SDK
    namespace = "com.example.tubesrpll"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.tubesrpll" // ApplicationID
        minSdk = 24 // Minimum SDK
        targetSdk = 34 // Target SDK version
        versionCode = 1 // versi aplikasi
        versionName = "Silent Echo 1.0" // nama versi aplikasi

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    aaptOptions {
        noCompress += "tflite"
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs("assets")
        }
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.5.0") // Dependency Activity KTX
    implementation("com.squareup.picasso:picasso:2.71828") // Dependency Picasso

    //Dependency dan annotation processor Glide
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")

    //Dependency Firebas UI storage
    implementation("com.firebaseui:firebase-ui-storage:8.0.0")

    //Dependency Tensorflow lite
    implementation("org.tensorflow:tensorflow-lite:2.11.0")

    // Dependency AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Dependency Firebase
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    // Dependency Image Picker 2.1
    implementation("com.github.dhaval2404:imagepicker:2.1")

    //Dependency Firebase ML-KIT
    implementation("com.google.mlkit:object-detection-custom:17.0.1")
    implementation ("com.google.mlkit:linkfirebase:17.0.0")

    //Dependency Testing (Default)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
