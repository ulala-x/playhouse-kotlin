package org.ulalax.playhouse.communicator

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import org.apache.commons.validator.routines.InetAddressValidator


class IpFinderTest : FunSpec() {

    init {
        test("findLocalIp should be valid") {
            InetAddressValidator.getInstance().isValidInet4Address(IpFinder.findLocalIp()).shouldBeTrue()
        }

        test("findPublicIp should be valid") {
            InetAddressValidator.getInstance().isValidInet4Address(IpFinder.findPublicIp()).shouldBeTrue()
        }
    }
}