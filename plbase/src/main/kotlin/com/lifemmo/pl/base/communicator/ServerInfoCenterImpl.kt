    package com.lifemmo.pl.base.communicator

import org.apache.logging.log4j.kotlin.logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class ServerInfoCenterImpl : ServerInfoCenter {
    private val log = logger()
    private val serverInfoMap:MutableMap<String,ServerInfo> = ConcurrentHashMap()
    private var serverInfoList:List<ServerInfo> = ArrayList()
    private val offset = AtomicInteger()

    override fun update(serverList: List<ServerInfo>): List<ServerInfo> {
            val updatedMap = HashMap<String,ServerInfo>()
            serverList.forEach { newInfo ->
                newInfo.checkTimeout()

                val oldInfo = serverInfoMap[newInfo.bindEndpoint]
                if(oldInfo==null){
                    serverInfoMap[newInfo.bindEndpoint] = newInfo
                    updatedMap[newInfo.bindEndpoint] = newInfo
                }else{
                    if(oldInfo.update(newInfo)){
                        updatedMap[newInfo.bindEndpoint] = newInfo
                    }
                }
            }
            //만약 리스트에서 아예 빠진 항목이 있다면 해당 정보의 timeout 만 체크한다.
            serverInfoMap.values.forEach{
                if(it.checkTimeout()){
                    updatedMap[it.bindEndpoint] = it
                }
            }
            serverInfoList = serverInfoMap.values.toList().sortedBy{ it.bindEndpoint}

            return updatedMap.values.toList()
    }

    override fun findServer(endpoint: String): ServerInfo {
        val serverInfo = serverInfoMap[endpoint]
        if(serverInfo == null || !serverInfo.isValid()){
            throw CommunicatorException.NotExistServerInfo()
        }
        return serverInfo
    }

    override fun findRoundRobinServer(serviceId:String): ServerInfo {
        val list = serverInfoList
            .filter {
                       it.state.name == ServerInfo.ServerState.RUNNING.name
                        && it.serviceId == serviceId
            }

        if(list.isEmpty()){
            throw CommunicatorException.NotExistServerInfo()
        }

        var next = offset.incrementAndGet()
        if(next < 0){
            next *= next*(-1)
        }

        val index = next % list.size
        return list[index]
    }

    override fun getServerList(): List<ServerInfo> {
        return serverInfoList
    }


}
