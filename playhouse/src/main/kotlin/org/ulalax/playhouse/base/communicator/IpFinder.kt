package org.ulalax.playhouse.base.communicator

import org.apache.commons.validator.routines.InetAddressValidator
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL


object IpFinder {
    fun findLocalIp(): String {
        try{
            Socket().use { socket ->
                socket.connect(InetSocketAddress("google.com", 80))
                return socket.localAddress.hostAddress
            }
        }catch (e:Exception){
            val localhost = InetAddress.getLocalHost()
            return localhost.hostAddress
        }
    }

    fun findPublicIp(): String {
        val urlList = listOf("http://checkip.amazonaws.com/","https://ipv4.icanhazip.com/","http://myexternalip.com/raw","http://ipecho.net/plain")
        urlList.forEach { urlString ->
            val checkPublicIp = checkPublicIp(urlString)
            if(InetAddressValidator.getInstance().isValidInet4Address(checkPublicIp)){
                return checkPublicIp
            }
        }
        return findLocalIp()
    }

    private fun checkPublicIp(urlString: String): String {
        val url = URL(urlString)
        BufferedReader(InputStreamReader(url.openStream())).use { br -> return br.readLine() }
    }

}
