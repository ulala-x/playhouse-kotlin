package org.ulalax.playhouse.service.session

import org.ulalax.playhouse.communicator.ClientCommunicator
import org.ulalax.playhouse.communicator.RequestCache
import org.ulalax.playhouse.service.BaseSender
import org.ulalax.playhouse.service.SessionSender


class XSessionSender(serviceId:String, clientCommunicator: ClientCommunicator, reqCache: RequestCache) :
    BaseSender(serviceId,clientCommunicator,reqCache), SessionSender