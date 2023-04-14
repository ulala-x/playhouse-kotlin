//package org.ulalax.playhouse.service.play
//
//import junit.framework.TestCase.assertEquals
//import org.junit.jupiter.api.Test
//import org.mockito.kotlin.argumentCaptor
//
//import org.mockito.kotlin.mock
//import org.mockito.kotlin.verify
//
//
//open class ApiClient {
//    fun getServerInfo(serverId: String){
//    }
//}
//
//class ServerInfoCenter(private val apiClient: ApiClient) {
//    fun getServerInfo(serverId: String) {
//        apiClient.getServerInfo(serverId)
//    }
//}
//
//class ServerInfoCenterTest {
//
//    @Test
//    fun testGetServerInfo() {
//        // Mocking
//        val mockApiClient:ApiClient = mock()
//        val serverInfoCenter = ServerInfoCenter(mockApiClient)
//
//        val captor = argumentCaptor<String>()
//
////        Mockito.`when`(mockApiClient.getServerInfo(captor.capture())).thenReturn(captor.capture())
//
//        // Test
//        serverInfoCenter.getServerInfo("server-001")
//
//        // Capture
//
//        // Verify
//        verify(mockApiClient).getServerInfo(captor.capture())
//
//        // Assert
//        val serverId = captor.firstValue
//        assertEquals("server-001", serverId)
//    }
//}