package com.example.sungmin.remotecontrolm;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class LogInfo implements Serializable{       ///// user information
    int mode;
    byte[] id;
    byte[] pw;
    Sockaddr_in private_addr;
    Sockaddr_in public_addr;

    public LogInfo(){
        mode = RcProtocol.MODE_P2P;
        id = new byte[RcProtocol.SIZE_OF_ID];
        pw = new byte[RcProtocol.SIZE_OF_PW];

    }
    public static int SIZE(){
        return 4+RcProtocol.SIZE_OF_ID+RcProtocol.SIZE_OF_PW+Sockaddr_in.SIZE()*2;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public void setId(byte[] id) {
        if(id.length > RcProtocol.SIZE_OF_ID) {
            return;
        }
        for(int i=0; i<id.length; i++)
            this.id[i] = id[i];
    }
    public void setPw(byte[] pw) {
        if(pw.length > RcProtocol.SIZE_OF_PW) {
            return;
        }
        for(int i=0; i<pw.length; i++)
            this.pw[i] = pw[i];
    }

    public void setPrivate_addr(Sockaddr_in private_addr) {
        this.private_addr = private_addr;
    }

    public void setPublic_addr(Sockaddr_in public_addr) {
        this.public_addr = public_addr;
    }

    public void setPrivateAddr(InetSocketAddress private_addr) {
        this.private_addr = new Sockaddr_in(private_addr);
    }
    public void setPublicAddr(InetSocketAddress public_addr) {
        this.public_addr = new Sockaddr_in(public_addr);
    }

    public int getMode() {
        return mode;
    }

    public void convertEndian(){
        this.mode = Convertor.convertEndian(this.mode);
        this.public_addr.convertEndian();
        this.private_addr.convertEndian();
    }
    public byte[] toBytes(){
        ByteBuffer buffer = ByteBuffer.allocate(SIZE());
        byte[] arr = new byte[SIZE()];

        buffer.put(Convertor.toBytes(mode));
        buffer.put(id);
        buffer.put(pw);
        buffer.put(private_addr.toBytes());
        buffer.put(public_addr.toBytes());
        buffer.flip();

        buffer.get(arr);

        return arr;
    }

    /*public void putToBuffer(ByteBuffer des){

        des.put(ToBytes.toBytes(mode));
        des.put(id);
        des.put(pw);
        private_addr.putToBuffer(des);
        public_addr.putToBuffer(des);

    }*/
}
