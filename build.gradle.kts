///////////////////////////////////////////////////////////////////////////////
//  GRADLE CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
plugins {
    id("com.diffplug.spotless") version "5.10.2"
    id("org.sonarqube") version "3.1"
    id("org.jetbrains.dokka") version "1.4.32"
}

buildscript {
    val kotlinVersion: String by project
    repositories {
        mavenLocal()
        maven(url = "https://repo.eclipse.org/service/local/repositories/maven_central/content")
        mavenCentral()
        google()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("com.android.tools.build:gradle:4.1.3")
    }
}

///////////////////////////////////////////////////////////////////////////////
//  APP CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
allprojects {
    repositories {
        maven (url = "https://s01.oss.sonatype.org/content/repositories/snapshots" )
        mavenCentral()
        google()
    }
}

///////////////////////////////////////////////////////////////////////////////
//  TASKS CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
tasks {
    spotless {
        kotlin{
            target("**/*.kt")
            ktfmt()
            licenseHeaderFile("${project.rootDir}/LICENSE_HEADER")
        }
        java {
            target("**/src/**/*.java")
            licenseHeaderFile("${project.rootDir}/LICENSE_HEADER")
            importOrder("java", "javax", "org", "com", "")
            removeUnusedImports()
            googleJavaFormat()
        }
    }
    sonarqube {
        properties {
            property("sonar.projectKey", "Famoco_keyple-plugin-se-communication-lib")
            property("sonar.organization", "famoco")
            property("sonar.host.url", "https://sonarcloud.io")
            property("sonar.login", System.getenv("SONAR_LOGIN"))
            System.getenv("BRANCH_NAME")?.let {
                if (it != "main") {
                    property("sonar.branch.name", it)
                }
            }
        }
    }
}
