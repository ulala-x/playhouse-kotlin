package org.ulalax.playhouse.service.api

import org.ulalax.playhouse.communicator.message.RoutePacket
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.ErrorCode
import org.ulalax.playhouse.protocol.Server.DisconnectNoticeMsg
import org.ulalax.playhouse.communicator.CommunicateClient
import org.ulalax.playhouse.communicator.ServerInfo
import org.ulalax.playhouse.communicator.Service
import org.ulalax.playhouse.communicator.ServiceType
import org.ulalax.playhouse.service.RequestCache
import org.ulalax.playhouse.service.SystemPanelImpl
import java.util.concurrent.atomic.AtomicReference

class ApiService(
    private val serviceId: String,
    private val apiOption: ApiOption,
    private val requestCache: RequestCache,
    private val communicateClient: CommunicateClient,
    private val apiBaseSenderImpl: ApiBaseSenderImpl,
    private val systemPanelImpl: SystemPanelImpl,
    ) : Service {

    private val log = logger()
    private val state = AtomicReference(ServerInfo.ServerState.DISABLE)
    private val apiReflection = ApiReflection(apiOption.apiPath,apiOption.applicationContext)

    override fun onStart() {
        state.set(ServerInfo.ServerState.RUNNING)
        apiReflection.callInitMethod(systemPanelImpl,apiBaseSenderImpl)
    }

    override fun onReceive(routePacket: RoutePacket) = routePacket.use  {

        val routeHeader = routePacket.routeHeader
        val apiCallBackHandler = apiOption.apiCallBackHandler
        val executorService = apiOption.executorService

        val apiSender = ApiSenderImpl(serviceId,communicateClient,requestCache).apply {
            setCurrentPacketHeader(routeHeader)
        }

        try {
            if (routeHeader.isBase) {
                return when (routeHeader.msgName()) {
                    DisconnectNoticeMsg.getDescriptor().name -> {
                        val disconnectNoticeMsg = DisconnectNoticeMsg.parseFrom(routePacket.buffer())
                        apiCallBackHandler.onDisconnect(disconnectNoticeMsg.accountId, routeHeader.sessionInfo)
                    }

                    else -> {
                        log.error("Invalid base Api packet:${routeHeader.msgName()}")
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
                    apiSender.errorReply(routeHeader, ErrorCode.SYSTEM_ERROR)
                    log.error(ExceptionUtils.getStackTrace(e))
                }
            }
        }catch (e:Exception){
                apiSender.errorReply(routeHeader, ErrorCode.SYSTEM_ERROR)
                log.error(ExceptionUtils.getStackTrace(e))
        }
    }

    override fun onStop() {
        state.set(ServerInfo.ServerState.DISABLE)
    }

    override fun weightPoint(): Int {
        return 0
    }

    override fun serverState(): ServerInfo.ServerState {
         return state.get()
    }

    override fun serviceType(): ServiceType {
        return ServiceType.API
    }

    override fun serviceId(): String {
       return serviceId
    }

    override fun pause() {
        state.set(ServerInfo.ServerState.PAUSE)
    }

    override fun resume() {
        state.set(ServerInfo.ServerState.RUNNING)
    }

}