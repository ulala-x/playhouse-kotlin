import com.google.protobuf.gradle.proto
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("org.ulalax.playhouse.kotlin-library-conventions")
}

group =  Versions.groupId
version = Versions.version


java {
    withSourcesJar()
    withJavadocJar()
}


publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = Versions.groupId
            artifactId = "protocol"
            version = Versions.version
            from(components["java"])
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${Versions.protocVersion}"
    }
}

sourceSets {
    main {
        proto {
            srcDir ("./proto")
        }
        java {
            srcDirs(
                "build/generated/source/proto/main/java"
            )
        }
    }
}
//
//java{
//    withSourcesJar()
//}

//tasks {
//    val sourcesJar by creating(Jar::class) {
//        archiveClassifier.set("sources")
//        from(kotlin.sourceSets.getByName("main").kotlin.srcDirs)
//    }
//
//    artifacts {
//        archives(sourcesJar)
//    }
//}


dependencies {
    // https://mvnrepository.com/artifact/it.ozimov/embedded-redis
    //implementation("org.zeromq:jmq:1.0")
//    api(project(":zmq"))

}
