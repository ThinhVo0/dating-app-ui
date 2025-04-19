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
    packaging {
        packaging {
            resources {
                excludes += "META-INF/license.txt"
                excludes += "META-INF/spring.schemas"
                excludes += "META-INF/spring.tooling"
                excludes += "META-INF/spring.handlers"
                excludes += "META-INF/*.txt"
                excludes += "META-INF/spring.*"
            }
        }
    }
    buildFeatures {
        viewBinding = true
    }

}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
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
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")

    // Lombok
    implementation(libs.lombok)
    annotationProcessor(libs.lombok)

    // Flexbox
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    // WebSocket
    implementation("org.springframework:spring-websocket:5.3.23")
    implementation("org.springframework:spring-messaging:5.3.23")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.5")
    implementation("org.webjars:stomp-websocket:2.3.4")
    implementation("org.webjars:sockjs-client:1.5.1")

    implementation ("org.glassfish.tyrus:tyrus-client:1.17")
    implementation ("org.glassfish.tyrus:tyrus-container-grizzly-client:1.17")
    implementation ("javax.websocket:javax.websocket-api:1.1")

    implementation ("javax.xml.stream:stax-api:1.0-2")

}