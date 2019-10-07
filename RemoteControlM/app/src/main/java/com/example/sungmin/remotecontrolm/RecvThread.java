package com.example.sungmin.remotecontrolm;


import android.os.SystemClock;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class RecvThread extends Thread {
    DataQueue dataQueue;
    DatagramChannel ch;
    PacketInfo packInfo;
    ByteBuffer[] packet;
    ByteBuffer head;
    ByteBuffer data;
    ImagePacket imgPacket;

    public RecvThread(DatagramChannel lParam, DataQueue queue) {
        this.dataQueue = queue;
        this.head = ByteBuffer.allocateDirect(PacketInfo.SIZE());
        this.data = ByteBuffer.allocateDirect(ImagePacket.SIZE());
        this.packet = new ByteBuffer[2];
        this.packet[0] = head;
        this.packet[1] = data;
        this.ch = lParam;
        this.imgPacket = new ImagePacket();
    }

    @Override
    public void run() {
        try {
            while (RcActivity.threadOn) {
                ch.read(packet);
                data.flip();
                //Convertor.byteToImagePacket(data, imgPacket);
                //dataQueue.enqueue(imgPacket);
                dataQueue.enqueue(data);
                data.clear();
                head.clear();
            }
            ///// announce disconnection /////
            Mouse_Point mp = new Mouse_Point(RcProtocol.DISCONNECT);
            mp.convertEndian();
            ByteBuffer dummy = ByteBuffer.allocate(Mouse_Point.SIZE()+PacketInfo.SIZE());
            packInfo = new PacketInfo();
            packInfo.byteToPacketInfo(head);
            packInfo.setType(Convertor.convertEndian(RcProtocol.PACKET_TYPE_SEND_MP));
            dummy.put(packInfo.toBytes());
            dummy.put(mp.toBytes());
            dummy.flip();
            ch.write(dummy);
            freeMemory();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            freeMemory();
        }
    }

    void freeMemory(){
        try {
            head = null;
            data = null;
            packet = null;
            imgPacket = null;
            dataQueue = null;
            if(ch.isConnected())
                ch.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
