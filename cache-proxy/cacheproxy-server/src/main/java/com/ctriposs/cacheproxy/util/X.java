package com.ctriposs.cacheproxy.util;

import java.net.*;
import java.util.Enumeration;
import java.util.List;

/**
 * @author:yjfei
 * @date: 2/26/2015.
 */
public class X {

    final static public String getIp() {
        String ip = null;
        try {
            Enumeration<NetworkInterface> er = NetworkInterface.getNetworkInterfaces();
            while (er.hasMoreElements()) {
                NetworkInterface ni = er.nextElement();
                if (ni.getName().startsWith("eth") || ni.getName().startsWith("bond") ) {
                    List<InterfaceAddress> list = ni.getInterfaceAddresses();
                    for (InterfaceAddress interfaceAddress : list) {
                        InetAddress address = interfaceAddress.getAddress();
                        if (address instanceof Inet4Address) {
                            ip = address.getHostAddress();
                            break;
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        if (ip == null) {
            try {
                ip = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        return ip;
    }

    public static boolean isEmpty(final CharSequence s) {
        if (s == null) {
            return true;
        }
        return s.length() == 0;
    }

    public static boolean isBlank(final CharSequence s) {
        if (s == null) {
            return true;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
