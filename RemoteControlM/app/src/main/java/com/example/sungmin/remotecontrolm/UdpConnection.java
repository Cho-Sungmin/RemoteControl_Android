package com.example.sungmin.remotecontrolm;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;



public class UdpConnection extends AppCompatActivity {
    private LogInfo logInfo;
    private PacketInfo packInfo;
    Bundle param;

    static InetSocketAddress myAddress;
    InetSocketAddress serverAddress;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        Intent intent = getIntent();
        String id = intent.getStringExtra("id");
        String pw = intent.getStringExtra("pw");
        int mode = -1;
        mode = intent.getIntExtra("mode", 1);

        serverAddress = new InetSocketAddress("192.168.0.13", RcProtocol.RELAY_PORT);


        logInfo = new LogInfo();
        logInfo.setId(id.getBytes());
        logInfo.setPw(pw.getBytes());
        logInfo.setMode(mode);

        new Thread() {
            @Override
            public void run() {
                try {
                    String address = RcProtocol.getMyIp();

                    myAddress = new InetSocketAddress(address, RcProtocol.UDP_PORT);
                    logInfo.setPrivateAddr(myAddress);
                    logInfo.setPublicAddr(myAddress);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (connection() == true) {
                    Intent intent = new Intent(getApplicationContext(), RcActivity.class);
                    param = new Bundle();
                    if(logInfo.getMode()==RcProtocol.MODE_RELAY){
                        Sockaddr_in addr = new Sockaddr_in(serverAddress);
                        logInfo.setPublic_addr(addr);
                    }
                    param.putSerializable("userInfo", logInfo);
                    param.putSerializable("packetInfo", packInfo);
                    intent.putExtras(param);
                    startActivity(intent);

                    finish();
                }
            }
        }.start();
    }

    public Boolean connection() {
        try {

            DatagramChannel ch = DatagramChannel.open(StandardProtocolFamily.INET);
            ch.bind(myAddress);
            ch.connect(serverAddress);

            ByteBuffer head = ByteBuffer.allocate(PacketInfo.SIZE());
            ByteBuffer data = ByteBuffer.allocate(LogInfo.SIZE());
            ByteBuffer[] packet = {head, data};
            packInfo = new PacketInfo();

            packInfo.setType(RcProtocol.PACKET_TYPE_CON_HOST);
            packInfo.convertEndian();
            logInfo.convertEndian();
            head.put(packInfo.toBytes());
            data.put(logInfo.toBytes());
            head.flip();
            data.flip();
            ch.write(packet);
            head.clear();
            data.clear();

            long size = ch.read(packet);
            head.flip();
            data.flip();
            packInfo.byteToPacketInfo(head);
            packInfo.convertEndian();

            if (packInfo.type == RcProtocol.CON_SUCCESS && size > 0) {
                logInfo = Convertor.byteToLogInfo(data);
                logInfo.convertEndian();
                head.clear();
                data.clear();
                ch.close();
                return true;
            } else {
                System.out.println("server connection failed");
                ch.close();
                freeMemory();
                finish();
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        freeMemory();
    }

    void freeMemory(){
        try {
            serverAddress = null;
            myAddress = null;
            logInfo = null;
            packInfo = null;
            param = null;
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

