package org.ulalax.playhouse.service.api

import org.ulalax.playhouse.communicator.message.RoutePacket
import org.apache.commons.lang3.exception.ExceptionUtils
import LOG
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Scheduler
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.ulalax.playhouse.communicator.*
import org.ulalax.playhouse.protocol.Common.*
import org.ulalax.playhouse.service.XSystemPanel
import org.ulalax.playhouse.service.Sender
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class ApiProcessor(
        override val serviceId: Short,
        private val apiOption: ApiOption,
        private val requestCache: RequestCache,
        private val clientCommunicator: ClientCommunicator,
        private val sender: Sender,
        private val systemPanel: XSystemPanel
    ) : Processor {

    private val state = AtomicReference(ServerState.DISABLE)
    private val apiReflection = ApiReflection(apiOption.apiPath)
    private val msgQueue = ConcurrentLinkedQueue<RoutePacket>()

    private val cache: Cache<Long, AccountApiProcessor>
    = Caffeine.newBuilder().scheduler(Scheduler.systemScheduler()).expireAfterWrite(5, TimeUnit.MINUTES).build()

    override fun onStart() {
        state.set(ServerState.RUNNING)
        apiReflection.callInitMethod(systemPanel,sender)
        Thread({ messageLoop() },"api:message-loop").start()
    }

    private fun messageLoop() = runBlocking {
        val accountCoroutineDispatcher = apiOption.accountPacketExcutor.asCoroutineDispatcher()
        val commonCoroutineDispatcher = apiOption.commonExcutor.asCoroutineDispatcher()
        while(state.get() != ServerState.DISABLE){
            var routePacket = msgQueue.poll()
            while(routePacket!=null){
                routePacket.use {
                    val routeHeader = routePacket.routeHeader

                    try {
                        val accountId = routeHeader.accountId
                        if( accountId != 0.toLong()){
                            var accountApiProcessor = cache.getIfPresent(accountId)
                            if(accountApiProcessor==null){
                                accountApiProcessor = AccountApiProcessor(serviceId,
                                        requestCache,
                                        clientCommunicator,
                                        apiReflection,
                                        apiOption.apiCallBackHandler,
                                        accountCoroutineDispatcher
                                )

                                cache.put(accountId,accountApiProcessor)
                            }
                            accountApiProcessor.dispatch(routePacket)
                        }else{

                            val apiSender = AllApiSender(serviceId,clientCommunicator,requestCache).apply {
                                setCurrentPacketHeader(routeHeader)
                            }

                            launch(commonCoroutineDispatcher) {
                                try{
                                    apiReflection.callMethod(
                                            routeHeader,
                                            routePacket.toPacket(),
                                            routePacket.isBackend(),
                                            apiSender
                                    )
                                }catch (e:Exception){
                                    apiSender.errorReply(routeHeader, BaseErrorCode.SYSTEM_ERROR_VALUE.toShort())
                                    LOG.error(ExceptionUtils.getStackTrace(e),this,e)
                                }
                            }
                        }
                    }catch (e:Exception){
                        LOG.error(ExceptionUtils.getStackTrace(e),this,e)
                    }
                }
                routePacket = msgQueue.poll()
            }
            Thread.sleep(10)
        }
    }

    override fun onReceive(routePacket: RoutePacket)  {
        this.msgQueue.add(routePacket)
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