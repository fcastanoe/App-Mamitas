plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.chaquo.python")
    id("kotlin-kapt")
}

android {
    namespace = "com.GCPDS.mamitas"
    compileSdk = 35


    defaultConfig {
        applicationId = "com.GCPDS.mamitas"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Configuración de ABI (solo se incluirán estos binarios)
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }
    buildFeatures {
        viewBinding = true
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
    kotlinOptions {
        jvmTarget = "11"
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets")
        }
    }

}
chaquopy {
    defaultConfig {
        // Especifica el ejecutable de Python que usará el build
        buildPython("C:/Users/fcast/AppData/Local/Programs/Python/Python38/python.exe")
        // Agrega el bloque pip para instalar las librerías de Python que usarás en tu script.
        // Ten en cuenta que algunas librerías con componentes nativos pueden requerir configuración adicional.
        pip {
            // Ejemplo de dependencias. Ajusta a lo que requieras y verifica su compatibilidad en Android.
            install("opencv-python")
            install("pytesseract")
            install("matplotlib")
            install("pandas")
            install("numpy")
            install("scipy")
            // Puedes agregar más dependencias necesarias (por ejemplo, ultralytics o kagglehub),
            // pero ten en cuenta que podrían tener limitaciones en Android.
        }
    }
    // Configura dónde están tus fuentes de Python (por defecto Chaquopy usa "src/main/python")
    sourceSets {
        getByName("main") {
            srcDirs(listOf("src/main/python"))
        }
    }
}
dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.coordinatorlayout:coordinatorlayout")
    implementation("com.google.android.material:material")
    implementation("androidx.appcompat:appcompat")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.github.bumptech.glide:glide:4.12.0")
    kapt("com.github.bumptech.glide:compiler:4.12.0")
    implementation("com.rmtheis:tess-two:9.1.0")
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.16.1")
}
