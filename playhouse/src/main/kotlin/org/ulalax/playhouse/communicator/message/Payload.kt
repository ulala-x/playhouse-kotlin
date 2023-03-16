package org.ulalax.playhouse.communicator.message

import com.google.protobuf.ByteString
import com.google.protobuf.GeneratedMessageV3
import org.zeromq.ZFrame


interface Payload : AutoCloseable{
    fun frame(): ZFrame
}

class XPayload constructor() : Payload {
    constructor(message: GeneratedMessageV3) : this() {
        this.proto = message
    }
    constructor(message: ByteString): this(){
        this.byteString = message
    }

    constructor(message: ZFrame): this(){
        this.frame = message
    }

    private var frame: ZFrame? = null
    private var byteString: ByteString? = null
    private var proto: GeneratedMessageV3? = null
    override fun close() {
        frame?.apply {
            this.close()
        }

        frame = null
        byteString = null
        proto = null
    }

    override fun frame(): ZFrame {
        if(frame == null){
            proto?.apply {
                frame = ZFrame(proto!!.toByteArray());
            }
            byteString?.apply {
                frame = ZFrame(byteString!!.toByteArray());
            }
        }

        return frame ?: ZFrame(ByteArray(0))
    }

    fun proto(): GeneratedMessageV3? {
        return proto
    }

}
