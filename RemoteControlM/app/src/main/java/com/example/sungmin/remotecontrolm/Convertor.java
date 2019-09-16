package com.example.sungmin.remotecontrolm;

import java.nio.ByteBuffer;

////////////////////////////////////////////////////////////////////////////////////////
////////// supports transformation of data type, endian and toByte func  ///////////////
/////////////////////////////////////////////////////////////////////////////////////////

public class Convertor {

    public static byte[] reverse(byte[] arr) {
        int size = arr.length;
        byte[] tmp = new byte[size];
        for (int i = 0; i < size; i++) {
            tmp[i] = arr[size - 1 - i];
        }
        return tmp;
    }

    public static int byteToInt(byte[] arr) {
        return (arr[0] & 0xff) << 24 | (arr[1] & 0xff) << 16 |
                (arr[2] & 0xff) << 8 | (arr[3] & 0xff);
    }

    public static int convertEndian(int num) {
        byte[] tmp;
        tmp = toBytes(num);

        return byteToInt(reverse(tmp));
    }
    public static double convertEndian(double num) {
        byte[] tmp;
        tmp = toBytes(num);

        return byteToDouble(reverse(tmp));
    }
    public static short convertEndian(short num) {
        byte[] tmp;
        tmp = toBytes(num);

        return byteToShort(reverse(tmp));
    }

    public static long convertEndian(long num) {
        byte[] tmp;
        tmp = toBytes(num);

        return byteToLong(reverse(tmp));
    }

    private static double byteToDouble(byte[] arr) {
        ByteBuffer tmp = ByteBuffer.allocate(arr.length);

        tmp.put(arr);
        tmp.flip();

        return  tmp.getDouble();
    }

    private static long byteToLong(byte[] arr) {
        return (long) ((arr[0]) << 56 | (arr[1]) << 48
                | (arr[2]) << 40 | (arr[3]) << 32
                | (arr[4]) << 16 | (arr[5]) << 8
                | (arr[6]));
    }

    private static short byteToShort(byte[] arr) {
        return (short) ((arr[0]) << 8 | (arr[1]));
    }

    public static LogInfo byteToLogInfo(ByteBuffer buffer) {
            LogInfo logInfo = new LogInfo();

            byte[] tmp = new byte[RcProtocol.SIZE_OF_ID];

            logInfo.setMode(buffer.getInt(0));
            buffer.position(4);
            buffer.get(tmp, 0, tmp.length);
            logInfo.setId(tmp);
            buffer.get(tmp, 0, tmp.length);
            logInfo.setPw(tmp);
            tmp = new byte[Sockaddr_in.SIZE()];
            buffer.get(tmp, 0, tmp.length);
            logInfo.setPrivate_addr(Convertor.byteToSockaddr_in(tmp));
            buffer.get(tmp, 0, tmp.length);
            logInfo.setPublic_addr(Convertor.byteToSockaddr_in(tmp));


        return logInfo;
    }

    public static Sockaddr_in byteToSockaddr_in(byte[] arr) {
        Sockaddr_in sockaddr_in = new Sockaddr_in();

        ByteBuffer buffer = ByteBuffer.allocate(arr.length);
        buffer.put(arr);
        buffer.flip();

        sockaddr_in.setSin_family(buffer.getShort(0));
        sockaddr_in.setSin_port(buffer.getShort(2));
        sockaddr_in.setSin_addr(buffer.getInt(4));
        buffer.position(8);
        buffer.get(sockaddr_in.getSin_zero(), 0, 8);

        return sockaddr_in;
    }

    public static ImagePacket byteToImagePacket(ByteBuffer buffer) {
        ImagePacket imgPack = new ImagePacket();

        imgPack.setSeq(buffer.getInt(0));
        imgPack.setFlag(buffer.getInt(4));
        imgPack.setSize(buffer.getInt(8));
        buffer.position(12);
        buffer.get(imgPack.getData(), 0, RcProtocol.MTU);

        buffer.clear();

        return imgPack;
    }


    public static byte[] toBytes(int n) {
        int size = Integer.SIZE / 8;
        int t = size * 8;
        byte[] arr = new byte[size];

        for (int i = 0; i < size; i++) {
            t -= 8;
            arr[i] = (byte) (n >>> t);
        }

        return arr;
    }

    public static byte toByte(Boolean n) {
        byte tORf = 0;

        if (n == true)
            tORf = 1;
        else
            tORf = 0;

        return tORf;
    }

    public static byte[] toBytes(double n) {
       ByteBuffer tmp = ByteBuffer.allocate(Double.SIZE/8);
       byte[] arr = new byte[Double.SIZE/8];
       tmp.putDouble(n);
       tmp.flip();
       tmp.get(arr);
       return arr;
    }

    public static byte[] toBytes(short n) {
        int size = Short.SIZE / 8;
        int t = size * 8;
        byte[] arr = new byte[size];

        for (int i = 0; i < size; i++) {
            t -= 8;
            arr[i] = (byte) (n >>> t);
        }
        return arr;
    }

    public static byte[] toBytes(long n) {
        int size = Long.SIZE / 8;
        int t = size * 8;
        byte[] arr = new byte[size];


        for (int i = 0; i < size; i++) {
            t -= 8;
            arr[i] = (byte) (n >>> t);
        }

        return arr;
    }
}
