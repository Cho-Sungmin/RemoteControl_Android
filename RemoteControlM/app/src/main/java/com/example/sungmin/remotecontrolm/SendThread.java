package com.example.sungmin.remotecontrolm;

import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class SendThread extends Thread{         ///// send mouse point to control remote desktop
    DatagramChannel ch;
    ByteBuffer[] buffer;
    PacketInfo head;
    Mouse_Point data;

    SendThread(DatagramChannel ch, ByteBuffer[] buffer, PacketInfo head, Mouse_Point data){
        this.ch = ch;
        this.buffer = buffer;
        this.head = head;
        this.data = data;
    }

    @Override
    public void run() {
        synchronized (buffer) {
            buffer[0].put(head.toBytes());
            buffer[0].flip();
            data.convertEndian();
            buffer[1].put(data.toBytes());
            buffer[1].flip();
            try {
                ch.write(buffer);
            } catch (Exception e) {
                e.printStackTrace();
            }
            buffer[0].clear();
            buffer[1].clear();
        }
    }
}