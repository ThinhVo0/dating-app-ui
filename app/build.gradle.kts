plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.datingapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.datingapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment) // Chỉ giữ một lần
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Glide
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // Google Play Services
    implementation(libs.google.play.services.location)

    // Retrofit và Gson
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3") // Để debug request/response
    implementation ("com.google.android.material:material:1.9.0")
    // Lombok
    implementation(libs.lombok)
    annotationProcessor(libs.lombok)

    // Flexbox
    implementation("com.google.android.flexbox:flexbox:3.0.0")
}