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

    fun updateServerInfo(serverInfo: XServerInfo) {
        val key = serverInfo.bindEndpoint().toByteArray(StandardCharsets.UTF_8)
        val value = serverInfo.toByteArray()
        redisCommands.hset(redisKey, key, value)
    }

    fun getServerList(endpoint: String): List<XServerInfo> {
        val hashEntries = redisCommands.hgetall(redisKey)
        return hashEntries.mapNotNull { (key, value) ->
            try {
                val serverInfoMsg = Server.ServerInfoMsg.parseFrom(value)
                val serverInfo = XServerInfo.of(serverInfoMsg)
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

class RedisCacheClient(val redisIp: String,val redisBindPort: Int) : StorageClient {

    private lateinit var redisClient:RedisClient
    private lateinit var redisConnection: StatefulRedisConnection<ByteArray, ByteArray>
    private lateinit var redisCommands: RedisCommands<ByteArray, ByteArray>
    private val redisKey = ConstOption.REDIS_CACHE_KEY.toByteArray(StandardCharsets.UTF_8)

    fun connect() {

        val redisURI = "redis://$redisIp:$redisBindPort"
        redisClient = RedisClient.create(redisURI)
        redisConnection = redisClient.connect(ByteArrayCodec())
        redisCommands =  redisConnection.sync()

        redisClient.connect()
    }

    override fun updateServerInfo(serverInfo: XServerInfo) {
        val key = serverInfo.bindEndpoint().toByteArray(StandardCharsets.UTF_8)
        val value = serverInfo.toByteArray()
        redisCommands.hset(redisKey, key, value)
    }

    override fun getServerList(endpoint: String): List<XServerInfo> {
        val hashEntries = redisCommands.hgetall(redisKey)
        return hashEntries.mapNotNull { (key, value) ->
            try {
                val serverInfoMsg = Server.ServerInfoMsg.parseFrom(value)
                val serverInfo = XServerInfo.of(serverInfoMsg)
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

