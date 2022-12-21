package org.ulalax.playhouse.base.service.session

import org.ulalax.playhouse.base.communicator.CommunicateClient
import org.ulalax.playhouse.base.service.BaseSenderImpl
import org.ulalax.playhouse.base.service.RequestCache
import org.ulalax.playhouse.base.service.SessionSender


class SessionSenderImpl(serviceId:String, communicateClient: CommunicateClient, reqCache:RequestCache) :
    BaseSenderImpl(serviceId,communicateClient,reqCache),SessionSender