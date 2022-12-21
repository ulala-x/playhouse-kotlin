//package org.ulalax.playhouse.protocol
//
//import com.google.protobuf.ByteString
//import com.google.protobuf.GeneratedMessageV3
//
//class ReplyPacketImpl(
//                      private val msgName:String,
//                      private val message: ByteString,
//                      private val errorCode:Int,
//): ReplyPacket {
//    constructor(message: GeneratedMessageV3,errorCode: Int = 0):this(message.descriptorForType.name,message.toByteString(),errorCode)
//
//    override fun errorCode(): Int {
//        return errorCode
//    }
//
//    override fun msgName(): String {
//        return msgName
//    }
//
//    override fun message(): ByteString {
//        return message
//    }
//
//    override fun isSuccess(): Boolean {
//        return errorCode == 0
//    }
//}