package com.example.sungmin.remotecontrolm;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ImagePacket {
    private int seq;
    private int flag;       ///// remain fragments : true
    private int size;
    private byte[] data;

    public ImagePacket() {
        seq = -1;
        flag = 0;
        data = new byte[RcProtocol.MTU];
        size = 0;
    }

    public void convertEndian(){            ///// Communication between Java and C
        this.size = Convertor.convertEndian(this.size);
        this.seq = Convertor.convertEndian(this.seq);
        this.flag = Convertor.convertEndian(this.flag);
    }

    public byte[] toBytes(){
        ByteBuffer buffer = ByteBuffer.allocate(SIZE());
        byte[] arr = new byte[SIZE()];
        buffer.put(Convertor.toBytes(seq));
        buffer.put(Convertor.toBytes(flag));
        buffer.put(Convertor.toBytes(size));
        buffer.put(data);

        buffer.flip();

        buffer.get(arr);

        return arr;
    }
    public void setSeq(int seq) {
        this.seq = seq;
    }
    public void setFlag(int n) {
        this.flag = n;
    }
    public void setData(byte[] data) {
       this.data = data;
    }
    public void setSize(int size) {
        this.size = size;
    }



    public static int SIZE(){
        return 3*(Integer.SIZE/8)+RcProtocol.MTU;
    }

    public byte[] getData() {
        return data;
    }

    public int getFlag() {
        return flag;
    }

    public int getSize() {
        return size;
    }

    public int getSeq() {
        return seq;
    }
}
