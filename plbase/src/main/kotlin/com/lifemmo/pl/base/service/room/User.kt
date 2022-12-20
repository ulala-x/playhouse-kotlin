package com.lifemmo.pl.base.service.room

interface User {
    val userSender:UserSender
    fun onCreate()
    fun onDestroy()
}