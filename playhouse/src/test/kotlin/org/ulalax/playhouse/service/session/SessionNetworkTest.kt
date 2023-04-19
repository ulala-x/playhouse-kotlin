package org.ulalax.playhouse.service.session

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.netty.channel.Channel
import LOG
import io.netty.buffer.Unpooled
import org.ulalax.playhouse.client.ApiPacketListener
import org.ulalax.playhouse.client.Connector
import org.ulalax.playhouse.client.network.message.Packet
import org.ulalax.playhouse.communicator.ConstOption
import org.ulalax.playhouse.communicator.IpFinder
import org.ulalax.playhouse.communicator.message.ClientPacket
import org.ulalax.playhouse.communicator.message.FramePayload
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.protocol.Test.TestMsg
import org.ulalax.playhouse.service.session.network.netty.SessionNetwork
import org.ulalax.playhouse.service.session.network.netty.SessionListener
import org.zeromq.ZFrame

class SessionNetworkTest : FunSpec(){


    companion object{
        var resultValue = ""
    }

    class ServerListener: SessionListener {

        var useWebSocket:Boolean = false
        var buffer = Unpooled.buffer(ConstOption.MAX_PACKET_SIZE)

        override fun onConnect(channel: Channel) {
            resultValue = "onConnect"
        }

        override fun onReceive(channel: Channel, clientPacket: ClientPacket) = clientPacket.use {
            LOG.info("server received : ${clientPacket.msgId()}",this)

            val testMsg = TestMsg.parseFrom(clientPacket.data())
            resultValue = testMsg.testMsg
            if(testMsg.testMsg == "request"){
                buffer.clear()
                RoutePacket.writeClientPacketBytes(clientPacket,buffer)
                clientPacket.payload = FramePayload(ZFrame(buffer.array(),buffer.arrayOffset()+buffer.readerIndex(),buffer.readableBytes()))
                channel.writeAndFlush(clientPacket)
            }
        }

        override fun onDisconnect(channel: Channel) {
            resultValue = "onDisconnect"
        }
    }

    val data_4096 = "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequat massa quis enim. Donec pede justo, fringilla vel, aliquet nec, vulputate eget, arcu. In enim justo, rhoncus ut, imperdiet a, venenatis vitae, justo. Nullam dictum felis eu pede mollis pretium. Integer tincidunt. Cras dapibus. Vivamus elementum semper nisi. Aenean vulputate eleifend tellus. Aenean leo ligula, porttitor eu, consequat vitae, eleifend ac, enim. Aliquam lorem ante, dapibus in, viverra quis, feugiat a, tellus. Phasellus viverra nulla ut metus varius laoreet. Quisque rutrum. Aenean imperdiet. Etiam ultricies nisi vel augue. Curabitur ullamcorper ultricies nisi. Nam eget dui. Etiam rhoncus. Maecenas tempus, tellus eget condimentum rhoncus, sem quam semper libero, sit amet adipiscing sem neque sed ipsum. Nam quam nunc, blandit vel, luctus pulvinar, hendrerit id, lorem. Maecenas nec odio et ante tincidunt tempus. Donec vitae sapien ut libero venenatis faucibus. Nullam quis ante. Etiam sit amet orci eget eros faucibus tincidunt. Duis leo. Sed fringilla mauris sit amet nibh. Donec sodales sagittis magna. Sed consequat, leo eget bibendum sodales, augue velit cursus nunc, quis gravida magna mi a libero. Fusce vulputate eleifend sapien. Vestibulum purus quam, scelerisque ut, mollis sed, nonummy id, metus. Nullam accumsan lorem in dui. Cras ultricies mi eu turpis hendrerit fringilla. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; In ac dui quis mi consectetuer lacinia. Nam pretium turpis et arcu. Duis arcu tortor, suscipit eget, imperdiet nec, imperdiet iaculis, ipsum. Sed aliquam ultrices mauris. Integer ante arcu, accumsan a, consectetuer eget, posuere ut, mauris. Praesent adipiscing. Phasellus ullamcorper ipsum rutrum nunc. Nunc nonummy metus. Vestibulum volutpat pretium libero. Cras id dui. Aenean ut eros et nisl sagittis vestibulum. Nullam nulla eros, ultricies sit amet, nonummy id, imperdiet feugiat, pede. Sed lectus. Donec mollis hendrerit risus. Phasellus nec sem in justo pellentesque facilisis. Etiam imperdiet imperdiet orci. Nunc nec neque. Phasellus leo dolor, tempus non, auctor et, hendrerit quis, nisi. Curabitur ligula sapien, tincidunt non, euismod vitae, posuere imperdiet, leo. Maecenas malesuada. Praesent congue erat at massa. Sed cursus turpis vitae tortor. Donec posuere vulputate arcu. Phasellus accumsan cursus velit. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Sed aliquam, nisi quis porttitor congue, elit erat euismod orci, ac placerat dolor lectus quis orci. Phasellus consectetuer vestibulum elit. Aenean tellus metus, bibendum sed, posuere ac, mattis non, nunc. Vestibulum fringilla pede sit amet augue. In turpis. Pellentesque posuere. Praesent turpis. Aenean posuere, tortor sed cursus feugiat, nunc augue blandit nunc, eu sollicitudin urna dolor sagittis lacus. Donec elit libero, sodales nec, volutpat a, suscipit non, turpis. Nullam sagittis. Suspendisse pulvinar, augue ac venenatis condimentum, sem libero volutpat nibh, nec pellentesque velit pede quis nunc. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Fusce id purus. Ut varius tincidunt libero. Phasellus dolor. Maecenas vestibulum mollis diam. Pellentesque ut neque. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. In dui magna, posuere eget, vestibulum et, tempor auctor, justo. In ac felis quis tortor malesuada pretium. Pellentesque auctor neque nec urna. Proin sapien ipsum, porta a, auctor quis, euismod ut, mi. Aenean viverra rhoncus pede. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Ut non enim eleifend felis pretium feugiat. Vivamus quis mi. Phasellus a est. Phasellus magna. In hac habitasse platea dictumst. Curabitur at lacus ac velit ornare lobortis. Cura"


    init {

        val api:Short = 2
        val session:Short = 1

       test("client and session communicate ") {

            arrayOf(true, false).forEach { useWebSocket ->
                val serverListener = ServerListener()
                serverListener.useWebSocket = useWebSocket
                val sessionNetwork = SessionNetwork(SessionOption().apply { this.useWebSocket = useWebSocket }, serverListener)

                val port = IpFinder.findFreePort()
                val server = Thread {
                    sessionNetwork.bind(port)
                    sessionNetwork.await()
                    LOG.info ( "server down" ,this)
                }
                server.start()
                Thread.sleep(100)

                val connector = Connector(0, useWebSocket,
                { serviceId: Short, packet: Packet ->
                    // ApiPacketListener의 onReceive 함수를 구현하는 람다식
                    LOG.info("onApiReceive - serviceId:$serviceId, packet:${packet.msgId}",this)

                },{ serviceId: Short, stageIndex: Int, packet: Packet ->
                        LOG.info("onStageReceive - serviceId:$serviceId, stageIndex:$stageIndex packet:${packet.msgId}",this)
                })

                connector.connect("127.0.0.1", port)

                Thread.sleep(100)
                resultValue shouldBe "onConnect"

                connector.sendApi(api, Packet(TestMsg.newBuilder().setTestMsg("test").build()))

                Thread.sleep(200)
                resultValue shouldBe "test"

                var replyPacket = connector.requestApi(api, Packet(TestMsg.newBuilder().setTestMsg("request").build()))
                LOG.info("message payload size: ${replyPacket.data().limit()},${replyPacket.msgId}",this)
                TestMsg.parseFrom(replyPacket.data()).testMsg shouldBe "request"

                replyPacket = connector.requestApi(api, Packet(TestMsg.newBuilder().setTestMsg("request").build()))
                TestMsg.parseFrom(replyPacket.data()).testMsg shouldBe "request"

                connector.disconnect()
                Thread.sleep(100)
                resultValue shouldBe "onDisconnect"
                sessionNetwork.shutdown()
            }
        }
    }
}