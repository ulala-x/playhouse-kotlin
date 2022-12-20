package com.lifemmo.pl.base.communicator

import com.lifemmo.pl.base.Plbase
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.masterreplica.MasterReplica
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection
import java.util.concurrent.TimeUnit


class LettuceRedisClient(redisIp:String,redisBindPort:Int) : StorageClient {
    private val redisURI:String = "redis://$redisIp:${redisBindPort}/"
    private lateinit var connection: StatefulRedisMasterReplicaConnection<ByteArray, ByteArray>

    private lateinit var asyncCommands: RedisAsyncCommands<ByteArray, ByteArray>
    private val redisKey = "plbase_serverinfos"

    override fun updateServerInfo(serverInfo: ServerInfo) {
        //asyncCommands.expire(redisKey.toByteArray(),60)
        asyncCommands.hset(redisKey.toByteArray(),serverInfo.bindEndpoint.toByteArray(),serverInfo.toByteArray())
    }

    override fun getServerList(endpoint:String): List<ServerInfo> {
        return asyncCommands.hgetall(redisKey.toByteArray()).get(1,TimeUnit.SECONDS)
            .values.map {serverInfo-> ServerInfo.of(Plbase.ServerInfoMsg.parseFrom(serverInfo)) }
            .filter { it.bindEndpoint != endpoint }.toList()
    }

    fun connect() {
        var redisClient:RedisClient = RedisClient.create()
        connection = MasterReplica.connect(redisClient, ByteArrayCodec.INSTANCE, RedisURI.create(redisURI))
        asyncCommands = connection.async()
    }
}