package org.ulalax.playhouse.service.session.network.Stream

import java.nio.ByteBuffer

object IdConvertor {
    private val idBuffer5 = ByteArray(5);
    private val idBuffer4 = ByteArray(4);

    fun getIdentify(identify:ByteArray):Int{
        return ByteBuffer.wrap(identify,1,4).int
    }
    fun getIdentify(identify: Int):ByteArray {
        val array = ByteBuffer.wrap(idBuffer4).putInt(identify).array();
        System.arraycopy(array,0, idBuffer5,1,4)
        return idBuffer5
    }
}