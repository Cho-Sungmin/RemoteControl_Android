package com.example.sungmin.remotecontrolm;

import java.io.Serializable;
import java.nio.ByteBuffer;

public class PacketInfo implements Serializable {           ///// packet header
    int uId;
    int type;

    public PacketInfo() {
        uId = -1;
        type = 0;
    }
    public PacketInfo(int uId, int type) {
        this.uId = -1;
        type = 0;
    }

    static public int SIZE() {
        return (Integer.SIZE + Integer.SIZE) / 8;
    }

    public void convertEndian() {
        this.uId = Convertor.convertEndian(this.uId);
        this.type = Convertor.convertEndian(this.type);
    }
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(SIZE());
        byte[] arr = new byte[SIZE()];

        buffer.put(Convertor.toBytes(uId));
        buffer.put(Convertor.toBytes(type));

        buffer.flip();
        buffer.get(arr);

        return arr;
    }
    public void byteToPacketInfo (ByteBuffer buffer){
        this.uId = buffer.getInt(0);
        this.type = buffer.getInt(4);
    }
    /* public void putToBuffer(ByteBuffer des){
         des.put(ToBytes.toBytes(uId));
         des.put(ToBytes.toBytes(type));
     }*/
    public int getType() {
        return type;
    }

    public int getuId() {
        return uId;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setuId(int uId) {
        this.uId = uId;
    }


}
