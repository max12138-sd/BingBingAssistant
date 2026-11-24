plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.bingbing.assistant"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bingbing.assistant"
        minSdk = 26          // 规格书 §2：Android 8.0+
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // nova5 Pro（麒麟 980）为 arm64；保留 32 位兼容。减少 ABI 可控住 APK ≤ 60MB 预算（§10）。
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
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
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true   // 传统 View 体系（§2 禁用 Compose），ViewBinding 减少 findViewById 错误
    }
    packaging {
        resources {
            excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1")
        }
    }
}

ksp {
    // Room schema 导出：升级实体时 diff 历史结构，便于维护（保留迁移空间）
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    // —— UI（传统 View + XML，§2 锁定）——
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // —— 异步 ——
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // —— 后台提醒（§7.5：WorkManager 每日检查，无网络）——
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // —— 数据库 ——
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // —— 视觉（§2 锁定：华为无 GMS，OCR 走 RapidOCR + ONNX，禁 ML Kit）——
    implementation("org.opencv:opencv:4.9.0")
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.16.3")

    // —— 测试 ——
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    // JVM 单测使用真实 org.json 实现（android.jar 里的 org.json 是 stub）
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
