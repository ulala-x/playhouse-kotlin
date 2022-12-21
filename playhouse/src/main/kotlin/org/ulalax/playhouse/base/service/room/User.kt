package org.ulalax.playhouse.base.service.room

interface User {
    val userSender:UserSender
    fun onCreate()
    fun onDestroy()
}