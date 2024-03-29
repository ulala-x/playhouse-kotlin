/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("org.ulalax.playhouse.kotlin-library-conventions")
}

group =  Versions.groupId
version = Versions.version


publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = Versions.groupId
            artifactId = "clientConnector"
            version = Versions.version
            from(components["java"])
        }
    }
}

dependencies {
    api(project(":common"))
    implementation("io.netty:netty-all:4.1.90.Final")
}
repositories {
    mavenCentral()
}
