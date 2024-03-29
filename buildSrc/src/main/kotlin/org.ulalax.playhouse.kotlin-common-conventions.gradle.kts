import com.google.protobuf.gradle.*


/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    // Apply the java Plugin to add support for Java.
    id("org.jetbrains.kotlin.jvm")
//    java
    `maven-publish`
//    id("io.freefair.lombok")
    id("com.google.protobuf")
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
    mavenLocal()
}


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

sourceSets.main {
    java.srcDirs("src/main/Java", "src/main/Kotlin")
}


java {
    withSourcesJar()
}



dependencies {
    constraints {
        // Define dependency versions as constraints
        implementation("org.apache.commons:commons-text:1.9")
    }

    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
   // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    // https://mvnrepository.com/artifact/com.github.ben-manes.caffeine/caffeine
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.1")
    implementation("org.zeromq:zmqj:4.3.4.1")
// https://mvnrepository.com/artifact/commons-io/commons-io



    implementation(Depend.protoBuf)
    implementation(Depend.protoBufKotlin)
//    implementation(Depend.log4JApiKotlin)
//    implementation(Depend.log4JApi)
//    implementation(Depend.log4JCore)
    implementation(Depend.nettyBuffer)
    implementation(Depend.commonLang3)

    testImplementation("io.kotest:kotest-runner-junit5:5.5.5")
    testImplementation ("io.kotest:kotest-assertions-core:5.5.5")
    testImplementation ("io.kotest:kotest-property:5.5.5")


    // Use JUnit Jupiter for testing.
//    testImplementation(Depend.junitJupiter)
//    // https://mvnrepository.com/artifact/org.assertj/assertj-core
//    testImplementation("org.assertj:assertj-core:3.23.1")
//    // https://mvnrepository.com/artifact/org.mockito.kotlin/mockito-kotlin
//    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    testImplementation("io.mockk:mockk:1.13.5")


//
//    testImplementation("org.mockito:mockito-inline:5.1.1")


}




tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    systemProperty("file.encoding", "utf-8")
    useJUnitPlatform()
    testLogging{
        events("PASSED", "SKIPPED", "FAILED", "STANDARD_OUT", "STANDARD_ERROR")
    }
}
