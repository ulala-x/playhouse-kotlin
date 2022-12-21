//package org.ulalax.playhouse.base.protocol
//
//import org.ulalax.playhouse.Common.HeaderMsg
//import io.netty.buffer.Unpooled
//import org.assertj.core.api.Assertions.assertThat
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.assertThrows
//
//import org.zeromq.jmq.Frame
//import org.zeromq.jmq.JMQExceptions
//import java.nio.ByteBuffer
//import java.nio.charset.StandardCharsets
//import java.util.*
//
//internal class JzmqProtoPayloadTest {
//
//    val source = "123456578"
//    val data: ByteBuffer = StandardCharsets.UTF_8.encode(source)
//
//    @Test
//    fun close() {
//    }
//
//    @Test
//    fun byteBuf_refCnt_from_frame(){
//        var frame = Frame(data)
//        assertThat(ProtoPayload(frame).buffer(false).refCnt()).isEqualTo(1)
//        assertThat(ProtoPayload(frame).buffer(true).refCnt()).isEqualTo(1)
//    }
//    @Test
//    fun close_payload(){
//        var frame = Frame()
//        val payload= ProtoPayload(frame)
//        payload.close()
//        assertThrows<JMQExceptions.InternalError> {
//            frame.close()
//        }
//    }
//    @Test
//    fun create_from_frame_and_to_byteBuf(){
//        var buffer:ByteBuffer? = null;
//
//        val frame = Frame(data)
//        frame.use {
//            val payload = ProtoPayload(frame)
//            buffer = payload.buffer(true).nioBuffer()
//        }
//
//        for( i in 0 until 1000){
//            Frame("afjkladsjfal")
//        }
//
//        assertThat(StandardCharsets.UTF_8.decode(buffer).toString()).isEqualTo(source)
//    }
//
//
//    @Test
//    fun create_from_byteBuf_and_to_frame() {
//
//        var nioBuffer = ByteBuffer.allocateDirect(64)
//        nioBuffer.put(data)
//        nioBuffer.flip()
//
//        var payload  = ProtoPayload(Unpooled.wrappedBuffer(nioBuffer))
//
//        assertThat(payload.buffer().refCnt()).isEqualTo(1)
//        assertThat(source).isEqualTo(StandardCharsets.UTF_8.decode(payload.frame().buffer()).toString())
//    }
//
//    @Test
//    fun create_from_default_and_to_byteBuf(){
//        val payload = ProtoPayload()
//        val frame = payload.frame()
//        assertThat(frame.buffer().limit()).isEqualTo(1)
//    }
//
//    @Test
//    fun bufferFromFrame(){
//
//        var frame = Frame("1234567890");
//        val payload = ProtoPayload(Unpooled.wrappedBuffer(frame.buffer()))
//        assertThat(payload.buffer().refCnt()).isEqualTo(1)
//    }
//
//    @Test
//    fun check_direct_buffer_validity_in_multi() {
//
//        val atomicList = Collections.synchronizedList(mutableListOf<Frame>())
//
//        val threadList = mutableListOf<Thread>()
//        for (i in 0 until 5000){
//            val thread = Thread(){
//                val frame = Frame(HeaderMsg.newBuilder().setMsgName("test").setServiceId("api").setMsgSeq(i).build().toByteArray())
//                atomicList.add(frame)
//            }
//            threadList.add(thread)
//        }
//
//        threadList.forEach{
//            it.start()
//        }
//
//        threadList.forEach{
//            it.join()
//        }
//
//        assertThat(atomicList.size).isEqualTo(5000)
//
//        atomicList.forEach{
//            it.use {
//                val message = HeaderMsg.parseFrom(it.buffer())
//                assertThat(message.msgName).isEqualTo("test")
//                assertThat(message.msgSeq).isIn(0..5000)
//            }
//        }
//    }
//}