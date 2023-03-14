package org.ulalax.playhouse.service.session.network.Stream

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.ulalax.playhouse.Logger
import org.ulalax.playhouse.communicator.message.ClientPacket
import org.ulalax.playhouse.service.session.network.PacketParser
import org.zeromq.SendFlag
import org.zeromq.SocketType
import org.zeromq.ZSocket
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.HashMap

class StreamNetwork(private val sessionPacketListener: SessionPacketListener,private val log: Logger)  {

    var socket: ZSocket = ZSocket(SocketType.STREAM)
    var clients = HashMap<Int,ByteArray>()
    var clientParsers = HashMap<Int, PacketParser>()
    private val sendQueue:Queue<Pair<Int, ClientPacket?>> = ConcurrentLinkedQueue()
    var isShutdown = false;
    var isTcp = true;

    fun bind(bindEndpoint:String){
        if(bindEndpoint.contains("ws://")){
            isTcp = false;
        }
        socket.bind(bindEndpoint)
    }

    private fun newBuffer():ByteBuf{
        return Unpooled.buffer(PacketParser.MAX_PACKET_SIZE)
    }
    private fun receiveLoop(){

        var message = socket.receive(true)

        while(message.size > 0){
            message.use{
                var identity = (message.data(0))
                var sid = IdConvertor.getIdentify(identity)
                val data = message[1].data()
                var dataSize = data.size

                if(dataSize == 0){
                    if(clients.containsKey(sid)){
                        //disconnect
                        sessionPacketListener.onDisconnect(sid);

                        clients.remove(sid)
                        clientParsers.remove(sid)
                    }else{
                        //connect
                        sessionPacketListener.onConnect(sid);
                        clients[sid] = identity
                        clientParsers[sid] = getParsers()
                    }
                }else{
                    //parase
                    val packetParser = clientParsers[sid]!!
                    var packets = packetParser.parse(Unpooled.wrappedBuffer(data))
                    packets.forEach{e->sessionPacketListener.onReceive(sid,e)}
                }
            }
            message = socket.receive(true)
        }

    }

    private fun getParsers(): PacketParser {
        return PacketParser(log)
    }

    private fun sendLoop(){
        val pairPacket  = sendQueue.poll();
        while(pairPacket != null){
            val sid = pairPacket.first
            val packet = pairPacket.second
            val identity = this.clients[sid]

            if(identity == null){
                log.debug("$sid already disconnected",this::class.simpleName)
                continue
            }
            if(packet == null){
                //disconnect packet
                socket.send(identity,SendFlag.SEND_MORE)
                socket.send(ByteArray(0),SendFlag.DONT_WAIT)
                clients.remove(sid)
            }else{
                packet.use {
                    socket.send(identity,SendFlag.SEND_MORE)
                 //   socket.send(packet.toSendData())
                }

            }
        }
    }
    fun start() {
        while (!isShutdown) {
            receiveLoop()
            sendLoop()
            sleep(10)
        }
    }
    fun shutdown() {
        this.isShutdown = true;
    }

    fun send(sid:Int, clientPacket: ClientPacket) {
        this.sendQueue.add(Pair(sid,clientPacket))
    }

    fun disconnect(sid:Int) {
        this.sendQueue.add(Pair(sid,null));
    }


}