package org.ulalax.playhouse.service.api

import org.springframework.context.ApplicationContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ApiOption {
    var executorService: ExecutorService = Executors.newFixedThreadPool(200)
    lateinit var apiPath:String
    lateinit var applicationContext: ApplicationContext
    lateinit var apiCallBackHandler: ApiCallBack

}
