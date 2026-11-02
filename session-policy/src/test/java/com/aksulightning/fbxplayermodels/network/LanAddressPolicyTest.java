package com.aksulightning.fbxplayermodels.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class LanAddressPolicyTest {
    private static InetSocketAddress peer(String literal) throws Exception {
        return new InetSocketAddress(InetAddress.getByName(literal), 25565);
    }

    @Test
    public void permitsPrivateIpv4RangeBoundaries() throws Exception {
        for (String address : new String[]{"10.0.0.0", "10.255.255.255", "172.16.0.0",
                "172.31.255.255", "192.168.0.0", "192.168.255.255"}) {
            assertTrue(address, LanAddressPolicy.isLanAddress(peer(address)));
        }
    }

    @Test
    public void rejectsPublicNeighborsAndSharedCarrierSpace() throws Exception {
        for (String address : new String[]{"9.255.255.255", "11.0.0.0", "172.15.255.255",
                "172.32.0.0", "192.167.255.255", "192.169.0.0", "100.64.0.1", "8.8.8.8"}) {
            assertFalse(address, LanAddressPolicy.isLanAddress(peer(address)));
        }
    }

    @Test
    public void permitsLoopbackAndLinkLocalPeers() throws Exception {
        for (String address : new String[]{"127.0.0.1", "127.0.0.2", "169.254.1.2", "::1", "fe80::1"}) {
            assertTrue(address, LanAddressPolicy.isLanAddress(peer(address)));
        }
    }

    @Test
    public void permitsUniqueLocalIpv6() throws Exception {
        assertTrue(LanAddressPolicy.isLanAddress(peer("fc00::1")));
        assertTrue(LanAddressPolicy.isLanAddress(peer("fdff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")));
    }

    @Test
    public void rejectsGlobalAndDeprecatedSiteLocalIpv6() throws Exception {
        assertFalse(LanAddressPolicy.isLanAddress(peer("2606:4700:4700::1111")));
        assertFalse(LanAddressPolicy.isLanAddress(peer("fec0::1")));
    }

    @Test
    public void rejectsWildcardAndMulticastDestinations() throws Exception {
        for (String address : new String[]{"0.0.0.0", "::", "224.0.0.1", "239.255.255.250", "ff02::1"}) {
            assertFalse(address, LanAddressPolicy.isLanAddress(peer(address)));
        }
    }

    @Test
    public void doesNotResolveHostnamesOrUnresolvedPrivateLiterals() {
        assertFalse(LanAddressPolicy.isLanAddress(InetSocketAddress.createUnresolved("localhost", 25565)));
        assertFalse(LanAddressPolicy.isLanAddress(InetSocketAddress.createUnresolved("192.168.1.2", 25565)));
        assertFalse(LanAddressPolicy.isLanAddress(InetSocketAddress.createUnresolved("fd00::1", 25565)));
    }

    @Test
    public void rejectsMissingAndNonIpConnections() {
        assertFalse(LanAddressPolicy.isLanAddress(null));
        assertFalse(LanAddressPolicy.isLanAddress(new SocketAddress() {}));
    }
}
