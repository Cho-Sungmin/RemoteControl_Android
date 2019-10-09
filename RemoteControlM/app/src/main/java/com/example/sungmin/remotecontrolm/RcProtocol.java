package com.example.sungmin.remotecontrolm;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class RcProtocol {
    static final int MTU = 576;
    static final String SERVER_IP = "192.168.0.13";
    static final int RELAY_PORT = 3333;
    static final int UDP_PORT = 4444;
    static final int ON_MOUSEHOOK = 9000;

    static final int DISCONNECT = -1;

    static final int IO_TYPE_SEND = 0;
    static final int IO_TYPE_RECV = 1;

    static final int PACKET_TYPE_CON_CUST = 1;
    static final int PACKET_TYPE_CON_HOST = 2;
    static final int PACKET_TYPE_SEND_IMG = 3;
    static final int PACKET_TYPE_SEND_MP = 4;
    static final int PACKET_TYPE_SEND_KB = 5;
    static final int CON_FAILED = 0;
    static final int CON_SUCCESS = 1;
    static final int MODE_P2P = 1;
    static final int MODE_RELAY = 0;
    static final int MAX_USER = 3;

    static final int SIZE_OF_ID = 10;
    static final int SIZE_OF_PW = 10;

    static final int WM_MOUSEWHEEL= 0x020A;
    static final int WM_MOUSEMOVE = 0x0200;
    static final int WM_LBUTTONDOWN = 0x0201;
    static final int WM_LBUTTONUP = 0x0202;
    static final int WM_LBUTTONDBCLK = 0x0203;
    static final int WM_RBUTTONDOWN = 0x0204;
    static final int WM_RBUTTONUP = 0x0205;

    static final int RCLICK = 4;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    static final int SHIFT = 3;
    static final int NULL = 0;

    static final int IMAGE_BUFFER_SIZE = 1024 * 1024;

    ///// to map window's keycode
    static final int[] NUMBER = {7, 28};
    static final int[] ALPHABET = {29, 54};
    static final int[] GAP = {41, 36};
    //static final int[] NUMBER = {48, 57};
    //static final int[] ALPHABET = {65, 90};

    public static String getMyIp() {        ///// get ip address, not loopback
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress() && inetAddress.isSiteLocalAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
        }
        return null;
    }

}

