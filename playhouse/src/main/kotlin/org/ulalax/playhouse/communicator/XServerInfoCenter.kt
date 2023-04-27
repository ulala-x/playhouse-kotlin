package org.ulalax.playhouse.communicator

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class XServerInfoCenter : ServerInfoCenter {

    private val serverInfoMap:MutableMap<String, XServerInfo> = ConcurrentHashMap()
    private var serverInfoList:List<XServerInfo> = ArrayList()
    private val offset = AtomicInteger()

    override fun update(serverList: List<XServerInfo>): List<XServerInfo> {
            val updatedMap = HashMap<String, XServerInfo>()
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

    override fun findServer(endpoint: String): XServerInfo {
        val serverInfo = serverInfoMap[endpoint]
        if(serverInfo == null || !serverInfo.isValid()){
            throw CommunicatorException.NotExistServerInfo("Cannot find serverInfo information that matches the requested endpoint:$endpoint")
        }
        return serverInfo
    }

    override fun findRoundRobinServer(serviceId:Short): XServerInfo {
        val list = serverInfoList
            .filter {
                       it.state.name == ServerState.RUNNING.name
                        && it.serviceId == serviceId
            }

        if(list.isEmpty()){
            throw CommunicatorException.NotExistServerInfo("Cannot find serverInfo information that matches the requested serviceId:$serviceId")
        }

        var next = offset.incrementAndGet()

        if(next < 0){
            offset.set(0)
            next = 0
        }

        val index = next % list.size
        return list[index]
    }

    override fun getServerList(): List<XServerInfo> {
        return serverInfoList
    }

    override fun findServerByAccountId(serviceId: Short, accountId: Long): XServerInfo {

        val list = serverInfoList
                .filter {
                    it.state.name == ServerState.RUNNING.name
                            && it.serviceId == serviceId
                }

        if(list.isEmpty()){
            throw CommunicatorException.NotExistServerInfo("Cannot find serverInfo information that matches the requested serviceId:$serviceId")
        }
        val index:Int = (accountId % list.size).toInt()
        return list[index]

    }

    override fun findServerType(serviceId: Short) : ServiceType {
        val list = serverInfoList
                .filter { it.serviceId == serviceId }

        if(list.isEmpty()){
            throw CommunicatorException.NotExistServerInfo("Cannot find serverInfo information that matches the requested serviceId:$serviceId")
        }
        return list.first().serviceType
    }
}
