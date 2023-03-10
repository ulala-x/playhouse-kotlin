package org.ulalax.playhouse.communicator

import com.google.protobuf.InvalidProtocolBufferException
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.ByteArrayCodec
import org.ulalax.playhouse.protocol.Server
import java.nio.charset.StandardCharsets

class RedisCache(private val redisClient: RedisClient) {

    private val redisConnection: StatefulRedisConnection<ByteArray, ByteArray> =
            redisClient.connect(ByteArrayCodec())

    private val redisCommands: RedisCommands<ByteArray, ByteArray> = redisConnection.sync()

    private val redisKey = "playhouse_serverinfos".toByteArray(StandardCharsets.UTF_8)

    fun updateServerInfo(serverInfo: BaseServerInfo) {
        val key = serverInfo.bindEndpoint().toByteArray(StandardCharsets.UTF_8)
        val value = serverInfo.toByteArray()
        redisCommands.hset(redisKey, key, value)
    }

    fun getServerList(endpoint: String): List<BaseServerInfo> {
        val hashEntries = redisCommands.hgetall(redisKey)
        return hashEntries.mapNotNull { (key, value) ->
            try {
                val serverInfoMsg = Server.ServerInfoMsg.parseFrom(value)
                val serverInfo = BaseServerInfo.of(serverInfoMsg)
                if (serverInfo.bindEndpoint != endpoint) {
                    serverInfo
                } else {
                    null
                }
            } catch (e: InvalidProtocolBufferException) {
                null
            }
        }
    }

    fun close() {
        redisConnection.close()
        redisClient.shutdown()
    }
}

class RedisClient(redisIp: String, redisBindPort: Int) : StorageClient {

    private val redisURI = "redis://$redisIp:$redisBindPort"

    private val redisClient = RedisClient.create(redisURI)

    private val cache = RedisCache(redisClient)

    fun connect() {
        // connect to Redis cache
    }

    override fun updateServerInfo(serverInfo: BaseServerInfo) {
        cache.updateServerInfo(serverInfo)
    }

    override fun getServerList(endpoint: String): List<BaseServerInfo> {
        return cache.getServerList(endpoint)
    }

    fun close() {
        cache.close()
    }
}

