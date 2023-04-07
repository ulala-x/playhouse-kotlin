package org.ulalax.playhouse.service.api.annotation


@Target(AnnotationTarget.CLASS)
annotation class Api()

@Target(AnnotationTarget.FUNCTION)
annotation class Init()

@Target(AnnotationTarget.FUNCTION)
annotation class ApiHandler(val msgId:Int)


@Target(AnnotationTarget.FUNCTION)
annotation class ApiBackendHandler(val msgId:Int)

