package org.ulalax.playhouse.service.api

import org.springframework.context.ApplicationContext
import org.ulalax.playhouse.service.play.ThreadPoolController
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ApiOption {
    var commonExcutor: ExecutorService = Executors.newFixedThreadPool(ThreadPoolController.workerSize)
    var accountPacketExcutor: ExecutorService = Executors.newFixedThreadPool(ThreadPoolController.workerSize)
    lateinit var apiPath:String
    lateinit var apiCallBackHandler: ApiCallBack

}
