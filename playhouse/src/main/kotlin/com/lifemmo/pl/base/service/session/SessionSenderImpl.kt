package com.lifemmo.pl.base.service.session

import com.lifemmo.pl.base.communicator.CommunicateClient
import com.lifemmo.pl.base.service.BaseSenderImpl
import com.lifemmo.pl.base.service.RequestCache
import com.lifemmo.pl.base.service.SessionSender


class SessionSenderImpl(serviceId:String, communicateClient: CommunicateClient, reqCache:RequestCache) :
    BaseSenderImpl(serviceId,communicateClient,reqCache),SessionSender