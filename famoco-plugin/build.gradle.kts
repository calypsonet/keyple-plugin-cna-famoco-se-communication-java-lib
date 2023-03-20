import org.jetbrains.kotlin.konan.properties.suffix
import org.jetbrains.kotlin.util.suffixIfNot

///////////////////////////////////////////////////////////////////////////////
//  GRADLE CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
plugins {
    id("com.android.library")
    id("kotlin-android")
    kotlin("android.extensions")
    id("org.jetbrains.dokka")
    id("com.diffplug.spotless")
}

ext {
    val pomArtifactId by extra("keyple-plugin-se-communication-lib")
    val pomDescription by extra("The Keyple Plugin SE Communication Library is an add-on to allow an application using" +
                " Keyple to interact with Famoco terminals.")
    val pomName by extra("keyple-plugin-se-communication-lib")
    val moduleTitle by extra("Keyple Plugin SE Communication Library")
    val pomLicenceName by extra("https://www.eclipse.org/legal/epl-2.0/")
    val pomLicenceURL by extra("http://www.apache.org/licenses/LICENSE-2.0.txt")
    val pomUrl by extra("https://github.com/Famoco/keyple-plugin-se-communication-lib")
    val pomOrgName by extra("Famoco")
    val pomOrgUrl by extra("https://famoco.com")
    val pomScmUrl by extra("https://github.com/Famoco/keyple-plugin-se-communication-lib")
    val pomScmConnection by extra("scm:git:git://github.com/Famoco/keyple-plugin-se-communication-lib.git")
    val pomScmdeveloperConnection by extra("scm:git:https://github.com/Famoco/keyple-plugin-se-communication-lib.git")
    val pomDevelopersName by extra("Famoco Mobile team")
    val pomDevelopersemail by extra("support@famoco.com")
}

/**
 * Sets version inside the gradle.properties file
 * Usage: ./gradlew setVersion -P version=1.0.0
 */
tasks.register("setVersion") {
    val backupFile = rootProject.file("gradle.properties.bak")
    backupFile.delete()
    val propsFile = rootProject.file("gradle.properties")
    propsFile.renameTo(backupFile)

    var version = rootProject.version as String
    version = version.removeSuffix("-SNAPSHOT")
    propsFile.printWriter().use {
        var versionApplied = false
        backupFile.readLines()
            .forEach { line ->
                if (line.matches(Regex("version\\s*=.*"))) {
                    versionApplied = true
                    it.println("version = $version")
                } else {
                    it.println(line)
                }
            }
        if (!versionApplied) {
            it.println("version = $version")
        }
    }

    println("Setting new version for ${rootProject.name} to $version")
}

tasks.register("setSnapshot") {
    val backupFile = rootProject.file("gradle.properties.bak")
    backupFile.delete()
    val propsFile = rootProject.file("gradle.properties")
    propsFile.renameTo(backupFile)

    var version = rootProject.version as String
    version = version.suffixIfNot("-SNAPSHOT")
    propsFile.printWriter().use {
        var versionApplied = false
        backupFile.readLines()
            .forEach { line ->
                if (line.matches(Regex("version\\s*=.*"))) {
                    versionApplied = true
                    it.println("version = $version")
                } else {
                    it.println(line)
                }
            }
        if (!versionApplied) {
            it.println("version = $version")
        }
    }

    println("Setting new version for ${rootProject.name} to $version")
}

///////////////////////////////////////////////////////////////////////////////
//  APP CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
val kotlinVersion: String by project
val archivesBaseName: String by project
android {
    compileSdkVersion(29)
    buildToolsVersion("30.0.2")

    buildFeatures {
        viewBinding = true
    }

    defaultConfig {
        minSdkVersion(19)
        targetSdkVersion(29)
        versionName(project.version.toString())

        testInstrumentationRunner("android.support.test.runner.AndroidJUnitRunner")
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("release") {
            minifyEnabled(false)
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    val javaSourceLevel: String by project
    val javaTargetLevel: String by project
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(javaSourceLevel)
        targetCompatibility = JavaVersion.toVersion(javaTargetLevel)
    }

    testOptions {
        unitTests.apply {
            isReturnDefaultValues = true // mock Log Android object
            isIncludeAndroidResources = true
        }
    }

    lintOptions {
        isAbortOnError = false
    }

    // generate output aar with a qualified name : with version number
    libraryVariants.all {
        outputs.forEach { output ->
            if (output is com.android.build.gradle.internal.api.BaseVariantOutputImpl) {
                output.outputFileName = "${archivesBaseName}-${project.version}.${output.outputFile.extension}"
            }
        }
    }

    kotlinOptions {
        jvmTarget = javaTargetLevel
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
        getByName("debug").java.srcDirs("src/debug/kotlin")
        getByName("test").java.srcDirs("src/test/kotlin")
        getByName("androidTest").java.srcDirs("src/androidTest/kotlin")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    // famoco libs
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    //keyple
    implementation("org.eclipse.keyple:keyple-common-java-api:2.0.0")
    implementation("org.eclipse.keyple:keyple-plugin-java-api:2.0.0")
    implementation("org.eclipse.keyple:keyple-util-java-lib:2.1.0")

    //android
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.appcompat:appcompat:1.1.0")

    //logging
    implementation("org.slf4j:slf4j-api:1.7.32")
    implementation("com.jakewharton.timber:timber:4.7.1") //Android
    implementation("com.arcao:slf4j-timber:3.1@aar") //SLF4J binding for Timber

    /** Test **/
    testImplementation("androidx.test:core-ktx:1.3.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.9")
    testImplementation("org.robolectric:robolectric:4.3.1")
    // famoco libs
    testImplementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    androidTestImplementation("com.android.support.test:runner:1.0.2")
    androidTestImplementation("com.android.support.test.espresso:espresso-core:3.0.2")
}

///////////////////////////////////////////////////////////////////////////////
//  TASKS CONFIGURATION
///////////////////////////////////////////////////////////////////////////////

tasks {
    dokkaHtml.configure {
        dokkaSourceSets {
            named("main") {
                noAndroidSdkLink.set(false)
                includeNonPublic.set(false)
                includes.from(files("src/main/kdoc/overview.md"))
            }
        }
    }
}
apply(from = "maven.gradle")  // To do last
