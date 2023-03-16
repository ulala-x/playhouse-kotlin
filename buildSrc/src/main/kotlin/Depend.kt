object Depend {

    val junitJupiter by lazy {"org.junit.jupiter:junit-jupiter:${Versions.junit_jupiter}"}


    val protoBuf by lazy {"com.google.protobuf:protobuf-java:${Versions.protobuf}"}
    val commonMath3 by lazy {"org.apache.commons:commons-math3:${Versions.commons_math3}"}
    val commonLang3 by lazy {"org.apache.commons:commons-lang3:${Versions.commons_lang3}"}
    val commonsValidator by lazy {"commons-validator:commons-validator:${Versions.commons_validator}"}

    val nettyCodecHttp2 by lazy {"io.netty:netty-codec-http2:${Versions.netty}"}
    val nettyCommon by lazy {"io.netty:netty-common:${Versions.netty}"}
    val nettyBuffer by lazy {"io.netty:netty-buffer:${Versions.netty}"}

    val embededRedis by lazy {"it.ozimov:embedded-redis:${Versions.embedded_redis}"}

//    val log4JCore by lazy {"org.apache.logging.log4j:log4j-core:${Versions.log4j2}"}
//    val log4JApi by lazy {"org.apache.logging.log4j:log4j-api:${Versions.log4j2}"}
//    val log4JApiKotlin by lazy {"org.apache.logging.log4j:log4j-api-kotlin:${Versions.log4j2kotlin}"}
//    val log4JWeb by lazy {"org.apache.logging.log4j:log4j-web:${Versions.log4j2}"}

    val lettuce by lazy {"io.lettuce:lettuce-core:${Versions.lettuce}"}


//    val kotlinLoggingJvm by lazy {"io.github.microutils:kotlin-logging-jvm:${Versions.kotlin_logging_jvm}"}
    val kotlinCoroutinesCore by lazy {"org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinx_coroutines_core}"}
}