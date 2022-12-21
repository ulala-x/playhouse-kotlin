package com.lifemmo.pl.base.communicator;

interface CommunicateServer {

    fun bind(listener: CommunicateListener):Unit
    fun communicate():Unit
    fun getClient():CommunicateClient
}
