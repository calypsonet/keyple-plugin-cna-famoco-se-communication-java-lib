///////////////////////////////////////////////////////////////////////////////
//  GRADLE CONFIGURATION
///////////////////////////////////////////////////////////////////////////////

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("com.android.application")
  id("com.diffplug.spotless")
}

///////////////////////////////////////////////////////////////////////////////
//  APP CONFIGURATION
///////////////////////////////////////////////////////////////////////////////

dependencies {
  // Famoco Keyple plugin
  implementation(project(path = ":plugin"))
  // Keyple (APIs of the Keyple 2.x generation, used by the code of this example)
  implementation("org.calypsonet.terminal:calypsonet-terminal-reader-java-api:1.0.+") {
    isChanging = true
  }
  implementation("org.calypsonet.terminal:calypsonet-terminal-calypso-java-api:1.2.+") {
    isChanging = true
  }
  implementation("org.eclipse.keyple:keyple-plugin-android-nfc-java-lib:2.0.1")
  implementation("org.eclipse.keyple:keyple-common-java-api:2.0.+") { isChanging = true }
  implementation("org.eclipse.keyple:keyple-service-java-lib:2.1.0")
  implementation("org.eclipse.keyple:keyple-service-resource-java-lib:2.0.2")
  implementation("org.eclipse.keyple:keyple-card-calypso-java-lib:2.2.1")
  implementation("org.eclipse.keyple:keyple-util-java-lib:2.+") { isChanging = true }
  // Android components
  implementation("androidx.appcompat:appcompat:1.7.1")
  implementation("com.google.android.material:material:1.13.0")
  implementation("androidx.constraintlayout:constraintlayout:2.2.1")
  implementation("androidx.activity:activity-ktx:1.10.1")
  implementation("androidx.fragment:fragment-ktx:1.8.9")
  implementation("androidx.core:core-ktx:1.16.0")
  // Coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
  // Logging libraries used in the project:
  // - SLF4J API provides a common logging interface for the app and third-party libraries (e.g.,
  //   Keyple).
  // - slf4j-timber bridges SLF4J calls to Timber for Android logging.
  // - Timber is used as the primary Android logging framework, offering lightweight and flexible
  //   logging.
  implementation("org.slf4j:slf4j-api:1.7.36")
  implementation("com.arcao:slf4j-timber:3.1@aar")
  implementation("com.jakewharton.timber:timber:5.0.1")
  // Tests
  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test:runner:1.7.0")
  androidTestImplementation("androidx.test.ext:junit:1.3.0")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}

///////////////////////////////////////////////////////////////////////////////
//  STANDARD CONFIGURATION FOR ANDROID APPLICATION KOTLIN-BASED PROJECTS
///////////////////////////////////////////////////////////////////////////////

val javaSourceLevel: String by project
val javaTargetLevel: String by project

android {
  namespace = project.findProperty("androidAppNamespace") as String
  compileSdk = (project.findProperty("androidCompileSdk") as String).toInt()
  defaultConfig {
    applicationId = project.findProperty("androidAppId") as String
    minSdk = (project.findProperty("androidMinSdk") as String).toInt()
    targetSdk = (project.findProperty("androidCompileSdk") as String).toInt()
    versionCode = (project.findProperty("androidAppVersionCode") as String).toInt()
    versionName = project.version.toString()
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }
  buildFeatures { viewBinding = true }
  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.toVersion(javaSourceLevel)
    targetCompatibility = JavaVersion.toVersion(javaTargetLevel)
  }
  kotlin { compilerOptions { jvmTarget.set(JvmTarget.fromTarget(javaTargetLevel)) } }
  packaging {
    // Exclude 'META-INF/NOTICE.md' to resolve the conflict that occurs when multiple dependencies
    // include this file
    resources.excludes.add("META-INF/NOTICE.md")
  }
  lint { abortOnError = false }
}

tasks {
  spotless {
    kotlin {
      target("src/**/*.kt")
      licenseHeaderFile("${project.rootDir}/LICENSE_HEADER")
      ktfmt()
    }
    kotlinGradle {
      target("**/*.kts")
      ktfmt()
    }
  }
}
