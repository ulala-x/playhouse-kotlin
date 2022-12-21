package org.ulalax.playhouse.service.session

import org.ulalax.playhouse.communicator.CommunicateClient
import org.ulalax.playhouse.service.BaseSenderImpl
import org.ulalax.playhouse.service.RequestCache
import org.ulalax.playhouse.service.SessionSender


class SessionSenderImpl(serviceId:String, communicateClient: CommunicateClient, reqCache: RequestCache) :
    BaseSenderImpl(serviceId,communicateClient,reqCache), SessionSender