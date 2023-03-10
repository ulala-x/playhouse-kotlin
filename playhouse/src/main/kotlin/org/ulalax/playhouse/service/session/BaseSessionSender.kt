package org.ulalax.playhouse.service.session

import org.ulalax.playhouse.communicator.CommunicateClient
import org.ulalax.playhouse.communicator.RequestCache
import org.ulalax.playhouse.service.BaseSenderImpl
import org.ulalax.playhouse.service.SessionSender


class SessionSenderImpl(serviceId:String, ICommunicateClient: CommunicateClient, reqCache: RequestCache) :
    BaseSenderImpl(serviceId,ICommunicateClient,reqCache), SessionSender