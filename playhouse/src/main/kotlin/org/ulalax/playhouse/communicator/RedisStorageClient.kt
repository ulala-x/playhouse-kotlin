package org.ulalax.playhouse.communicator

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.masterreplica.MasterReplica
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection
import org.ulalax.playhouse.XBitConverter
import org.ulalax.playhouse.protocol.Server
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit


class RedisStorageClient(redisIp:String, redisBindPort:Int) : StorageClient {
    private val redisURI:String = "redis://$redisIp:${redisBindPort}/"
    private lateinit var connection: StatefulRedisMasterReplicaConnection<ByteArray, ByteArray>

    private lateinit var asyncCommands: RedisAsyncCommands<ByteArray, ByteArray>
    private val redisServerInfoKey = "playhouse_serverinfos"
    private val redisNodeIdKey = "playhouse_nodeId"
    private val redisNodeSequenceKey = "playhouse_nodeId_Seq"

    override fun updateServerInfo(serverInfo: XServerInfo) {
        asyncCommands.hset(redisServerInfoKey.toByteArray(),serverInfo.bindEndpoint.toByteArray(),serverInfo.toByteArray())
    }

    override fun getServerList(endpoint:String): List<XServerInfo> {
        return asyncCommands.hgetall(redisServerInfoKey.toByteArray()).get(1,TimeUnit.SECONDS)
            .values.map {serverInfo-> XServerInfo.of(Server.ServerInfoMsg.parseFrom(serverInfo)) }
            .filter { it.bindEndpoint != endpoint }.filter { !it.timeOver() }.toList()
    }

    override fun getNodeId(bindEndpoint: String): Int {
        val key = bindEndpoint.toByteArray()
        val nodeIdBytes = asyncCommands.hget(redisNodeIdKey.toByteArray(), key).get(1, TimeUnit.SECONDS)
        return if (nodeIdBytes != null) {
            XBitConverter.byteArrayToInt(nodeIdBytes,0,nodeIdBytes.size)
        } else {
            var nodeId = asyncCommands.incr(redisNodeSequenceKey.toByteArray()).get(1, TimeUnit.SECONDS).toInt()
            if(nodeId > 4095){
                throw IllegalArgumentException("nodeId value exceeds maximum value")
            }
            asyncCommands.hset(redisNodeIdKey.toByteArray(), key, ByteBuffer.allocate(Int.SIZE_BYTES).putInt(nodeId).array())
            nodeId
        }
    }


    fun connect() {
        val redisClient:RedisClient = RedisClient.create()
        connection = MasterReplica.connect(redisClient, ByteArrayCodec.INSTANCE, RedisURI.create(redisURI))
        asyncCommands = connection.async()
    }

    fun close(){
        connection.close()
    }


}