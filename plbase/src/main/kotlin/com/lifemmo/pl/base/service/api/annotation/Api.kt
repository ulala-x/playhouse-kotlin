package com.lifemmo.pl.base.service.api.annotation


@Target(AnnotationTarget.CLASS)
annotation class Api()

@Target(AnnotationTarget.FUNCTION)
annotation class Init()

@Target(AnnotationTarget.FUNCTION)
annotation class ApiHandler(val msgName:String)


@Target(AnnotationTarget.FUNCTION)
annotation class ApiBackendHandler(val msgName:String)

