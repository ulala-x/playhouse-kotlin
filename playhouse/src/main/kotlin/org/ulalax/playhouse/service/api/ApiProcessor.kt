package org.ulalax.playhouse.service.api

import org.ulalax.playhouse.communicator.message.RoutePacket
import org.apache.commons.lang3.exception.ExceptionUtils
import LOG
import org.ulalax.playhouse.communicator.*
import org.ulalax.playhouse.protocol.Common.*
import org.ulalax.playhouse.protocol.Server.DisconnectNoticeMsg
import org.ulalax.playhouse.service.BaseSystemPanel
import java.util.concurrent.atomic.AtomicReference

class ApiProcessor(
        override val serviceId: Short,
        private val apiOption: ApiOption,
        private val requestCache: RequestCache,
        private val clientCommunicator: ClientCommunicator,
        private val allApiSender: AllApiSender,
        private val systemPanelImpl: BaseSystemPanel
    ) : Processor {


    private val state = AtomicReference(ServerState.DISABLE)
    private val apiReflection = ApiReflection(apiOption.apiPath)

    override fun onStart() {
        state.set(ServerState.RUNNING)
        apiReflection.callInitMethod(systemPanelImpl,allApiSender)
    }

    override fun onReceive(routePacket: RoutePacket) = routePacket.use  {

        val routeHeader = routePacket.routeHeader
        val apiCallBackHandler = apiOption.apiCallBackHandler
        val executorService = apiOption.executorService

        val apiSender = AllApiSender(serviceId,clientCommunicator,requestCache).apply {
            setCurrentPacketHeader(routeHeader)
        }

        try {
            if (routeHeader.isBase) {
                return when (routeHeader.msgId()) {
                    DisconnectNoticeMsg.getDescriptor().index -> {
                        val disconnectNoticeMsg = DisconnectNoticeMsg.parseFrom(routePacket.data())
                        apiCallBackHandler.onDisconnect(disconnectNoticeMsg.accountId, routeHeader.sessionInfo)
                    }

                    else -> {
                        LOG.error("Invalid base Api packet:${routeHeader.msgId()}",this)
                    }
                }
            }
            val packet = routePacket.toPacket()

            executorService.execute {
                try{
                    apiReflection.callMethod(
                        routeHeader,
                        packet,
                        routePacket.isBackend(),
                        apiSender
                    )
                }catch (e:Exception){
                    apiSender.errorReply(routeHeader, BaseErrorCode.SYSTEM_ERROR_VALUE.toShort())
                    LOG.error(ExceptionUtils.getStackTrace(e),this,e)
                }
            }
        }catch (e:Exception){
                apiSender.errorReply(routeHeader, BaseErrorCode.SYSTEM_ERROR_VALUE.toShort())
                LOG.error(ExceptionUtils.getStackTrace(e),this,e)
        }
    }

    override fun onStop() {
        state.set(ServerState.DISABLE)
    }

    override fun getWeightPoint(): Int {
        return 0
    }

    override fun getServerState(): ServerState {
         return state.get()
    }

    override fun getServiceType(): ServiceType {
        return ServiceType.API
    }


    override fun pause() {
        state.set(ServerState.PAUSE)
    }

    override fun resume() {
        state.set(ServerState.RUNNING)
    }

}