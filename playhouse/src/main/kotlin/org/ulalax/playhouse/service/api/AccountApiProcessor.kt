package org.ulalax.playhouse.service.api

import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.apache.commons.lang3.exception.ExceptionUtils
import org.ulalax.playhouse.communicator.ClientCommunicator
import org.ulalax.playhouse.communicator.RequestCache
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.protocol.Common
import org.ulalax.playhouse.protocol.Server
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class AccountApiProcessor(
        private val serviceId: Short,
        private val requestCache: RequestCache,
        private val clientCommunicator: ClientCommunicator,
        private val apiReflection:ApiReflection,
        private val apiCallBack: ApiCallBack) {

    private val msgQueue = ConcurrentLinkedQueue<RoutePacket>()
    private var isUsing = AtomicBoolean(false)

    suspend fun dispatch(routePacket: RoutePacket) = coroutineScope {
        msgQueue.add(routePacket)
        if(isUsing.compareAndSet(false,true)){
            while(isUsing.get()){
                val item = msgQueue.poll()
                if(item!=null) {
                    val routeHeader = item.routeHeader
                    if (routeHeader.isBase) {
                        when (routeHeader.msgId()) {
                            Server.DisconnectNoticeMsg.getDescriptor().index -> {
                                val disconnectNoticeMsg = Server.DisconnectNoticeMsg.parseFrom(routePacket.data())
                                apiCallBack.onDisconnect(disconnectNoticeMsg.accountId)
                            }
                            else -> {
                                LOG.error("Invalid base Api packet:${routeHeader.msgId()}",this)
                            }
                        }
                    }

                    val apiSender = AllApiSender(serviceId,routeHeader.accountId,"",1,clientCommunicator,requestCache).apply {
                        setCurrentPacketHeader(routeHeader)
                    }

                    try {
                        item.use {
                            apiReflection.callMethod(routeHeader,item.toPacket(),routeHeader.isBase,apiSender)
                        }
                    } catch (e: Exception) {
                        apiSender.errorReply(routePacket.routeHeader, Common.BaseErrorCode.UNCHECKED_CONTENTS_ERROR_VALUE.toShort())
                        LOG.error(ExceptionUtils.getStackTrace(e),this,e)
                    }
                }else{
                    isUsing.set(false)
                }
            }
        }
    }
}