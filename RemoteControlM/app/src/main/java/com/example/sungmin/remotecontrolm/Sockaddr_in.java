package com.example.sungmin.remotecontrolm;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;


public class Sockaddr_in implements Serializable{       ///// to map window's sockaddr structure
    short sin_family;
    short sin_port;
    int sin_addr;
    byte[] sin_zero;//8

    public Sockaddr_in() {
        sin_zero = new byte[8];
    }

    public Sockaddr_in(InetSocketAddress addr) {
        sin_zero = new byte[8];
        sin_port = (short) addr.getPort();
        sin_addr = inet_addr(addr.getAddress());
        sin_family = 2;

    }

    public static int SIZE() {
        return 16;
    }

    public String getIP() {
        String result = "";
        int num = sin_addr;

        byte[] tmp = Convertor.toBytes(num);
        byte[] arr = new byte[4];
        arr[1] = 0x00;
        arr[2] = 0x00;
        arr[0] = 0x00;

        for (int i = 0; i < 4; i++) {
            arr[3] = tmp[i];
            result += Integer.toString(Convertor.byteToInt(arr));
            if (i != 3)
                result += ".";
        }
        return result;
    }

    public int inet_addr(InetAddress inetAddress) {
        int i;
        int n = 0, result = 0;

        String[] addr = inetAddress.getHostAddress().split("\\.");

        for (i = 3; i >= 0; i--) {
            n = Integer.parseInt(addr[3 - i]);
            result |= n << (i * 8);
        }

        return result;
    }

    public void convertEndian() {
        this.sin_family = Convertor.convertEndian(sin_family);
    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(SIZE());
        byte[] arr = new byte[SIZE()];

        buffer.put(Convertor.toBytes(sin_family));
        buffer.put(Convertor.toBytes(sin_port));
        buffer.put(Convertor.toBytes(sin_addr));
        buffer.put(sin_zero);

        buffer.flip();

        buffer.get(arr);

        return arr;
    }

    public void setSin_port(short sin_port) {
        this.sin_port = sin_port;
    }

    public void setSin_family(short sin_family) {
        this.sin_family = sin_family;
    }

    public void setSin_addr(int sin_addr) {
        this.sin_addr = sin_addr;
    }

    public void setSin_zero(byte[] sin_zero) {
        this.sin_zero = sin_zero;
    }

    public byte[] getSin_zero() {
        return sin_zero;
    }

    public int getSin_port() {
        return ((int) sin_port) & 0xffff;
    }

    public int getSin_addr() {
        return sin_addr;
    }
    /* public void putToBuffer(ByteBuffer des){
        des.put(ToBytes.toBytes(sin_family));
        des.put(ToBytes.toBytes(sin_port));
        des.put(ToBytes.toBytes(sin_addr));
        des.put(sin_zero);
    }*/
}
