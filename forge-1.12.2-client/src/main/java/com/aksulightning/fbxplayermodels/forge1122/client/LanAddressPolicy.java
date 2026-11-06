package com.aksulightning.fbxplayermodels.forge1122.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/** Checks resolved peer addresses without DNS lookups or trusting a server-list label. */
public final class LanAddressPolicy {
    public static final int PROTOCOL_VERSION = 1;
    public static final String UNSUPPORTED_MESSAGE =
            "FBX Player Models is inactive here. This release supports single-player and LAN only.";

    private LanAddressPolicy() {}

    public static boolean isLanAddress(SocketAddress peer) {
        if (!(peer instanceof InetSocketAddress)) return false;
        InetSocketAddress socket = (InetSocketAddress) peer;
        if (socket.isUnresolved()) return false;
        InetAddress address = socket.getAddress();
        if (address.isAnyLocalAddress() || address.isMulticastAddress()) return false;
        if (address.isLoopbackAddress() || address.isLinkLocalAddress()) return true;
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int first = bytes[0] & 255;
            int second = bytes[1] & 255;
            return first == 10 || first == 172 && second >= 16 && second <= 31
                    || first == 192 && second == 168;
        }
        // IPv6 unique-local addresses (fc00::/7); deprecated site-local space is excluded.
        return bytes.length == 16 && (bytes[0] & 254) == 252;
    }
}
