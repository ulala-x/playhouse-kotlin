package org.ulalax.playhouse.communicator

import org.apache.commons.validator.routines.InetAddressValidator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.ulalax.playhouse.communicator.IpFinder


internal class IpFinderTest {

    @Test
    fun findLocalIp() {
        assertThat(InetAddressValidator.getInstance().isValidInet4Address(IpFinder.findLocalIp())).isTrue
    }

    @Test
    fun findPublicIp() {
        assertThat(InetAddressValidator.getInstance().isValidInet4Address(IpFinder.findPublicIp())).isTrue
    }
}