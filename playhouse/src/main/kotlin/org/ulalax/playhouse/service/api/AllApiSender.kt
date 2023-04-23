package org.ulalax.playhouse.service.api

import org.ulalax.playhouse.communicator.ClientCommunicator
import org.ulalax.playhouse.communicator.RequestCache
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.protocol.Server.*
import org.ulalax.playhouse.service.*

class AllApiSender (override val serviceId:Short,
                    override val accountId:Long,
                    override val sessionEndpoint:String,
                    override val sid:Int,
                    private val clientCommunicator: ClientCommunicator,
                    private val reqCache: RequestCache,
) : XApiCommonSender(serviceId,clientCommunicator,reqCache),
    ApiSender, ApiBackendSender {

    override fun getFromEndpoint(): String {
        return this.currentHeader?.from ?:""
    }
//    override fun sessionEndpoint(): String {
//        return this.currentHeader?.from ?:""
//    }
//
//    override fun getSid():Int {
//        return this.currentHeader?.sid ?:0
//    }


    override fun authenticate(accountId:Long){
        val message = AuthenticateMsg.newBuilder()
            .setServiceId(serviceId.toInt())
            .setAccountId(accountId).build()

        this.currentHeader?.run {
            sendToBaseSession(from,sid, Packet(message))
        } ?: throw ApiException.NotExistApiHeaderInfoException()

    }

    fun clone(): AllApiSender {
        return AllApiSender(this.serviceId,this.accountId,this.sessionEndpoint,this.sid,this.clientCommunicator,this.reqCache)
    }

}