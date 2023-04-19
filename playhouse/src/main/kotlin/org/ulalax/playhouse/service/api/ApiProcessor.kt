package org.ulalax.playhouse.service.api

import LOG
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Scheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.exception.ExceptionUtils
import org.ulalax.playhouse.NamedThreadFactory
import org.ulalax.playhouse.communicator.*
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.protocol.Common.BaseErrorCode
import org.ulalax.playhouse.service.Sender
import org.ulalax.playhouse.service.XSystemPanel
import java.util.concurrent.*
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
    private val cachedThreadPool : ExecutorService = Executors.newCachedThreadPool(NamedThreadFactory("ApiProcessor"))

    override fun onStart()  = runBlocking{
        state.set(ServerState.RUNNING)

        launch {
            apiReflection.callInitMethod(systemPanel,sender)
        }

        Thread({ messageLoop() },"api:message-loop").start()
    }

    private fun messageLoop(){
        val coroutineDispatcher = cachedThreadPool.asCoroutineDispatcher()
        //val commonCoroutineDispatcher = apiOption.commonExecutor.asCoroutineDispatcher()
        val scope = CoroutineScope(coroutineDispatcher)
        while(state.get() != ServerState.DISABLE) {
                var routePacket = msgQueue.poll()
                while (routePacket != null) {
                    routePacket.use {
                        val routeHeader = routePacket.routeHeader

                        try {
                            val accountId = routeHeader.accountId
                            if (accountId != 0.toLong()) {
                                var accountApiProcessor = cache.getIfPresent(accountId)
                                if (accountApiProcessor == null) {
                                    accountApiProcessor = AccountApiProcessor(
                                        serviceId,
                                        requestCache,
                                        clientCommunicator,
                                        apiReflection,
                                        apiOption.apiCallBackHandler,
                                        coroutineDispatcher
                                    )

                                    cache.put(accountId, accountApiProcessor)
                                }
                                scope.launch{
                                    accountApiProcessor.dispatch(routePacket)
                                }

                            } else {

                                val apiSender = AllApiSender(serviceId, clientCommunicator, requestCache).apply {
                                    setCurrentPacketHeader(routeHeader)
                                }

                                scope.launch {
                                    try {
                                        apiReflection.callMethod(
                                            routeHeader,
                                            routePacket.toPacket(),
                                            routePacket.isBackend(),
                                            apiSender
                                        )
                                    } catch (e: Exception) {
                                        apiSender.errorReply(routeHeader, BaseErrorCode.SYSTEM_ERROR_VALUE.toShort())
                                        LOG.error(ExceptionUtils.getStackTrace(e), this, e)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            LOG.error(ExceptionUtils.getStackTrace(e), this, e)
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