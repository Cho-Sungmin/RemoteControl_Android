package com.example.sungmin.remotecontrolm;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class RcActivity extends Activity implements Button.OnClickListener {
    RecvThread recvThread;
    ImgProcessingThread imgThread;
    ImageView imageView;
    DatagramChannel ch;
    InetSocketAddress clntAddr;
    public DataQueue dataQueue;
    LogInfo logInfo;
    PacketInfo packetInfo;
    public static boolean onImg = false;
    public static boolean threadOn = false;
    File cache;          ///// image file displaying the activity will be stored in cache directory

    ///// for keypad hooking /////
    InputMethodManager imm;
    Button exitButton;
    Button keybdButton;
    int keyCode;

    ByteBuffer[] buf;
    ByteBuffer head;
    ByteBuffer data;

    static public int deviceWidth=0;
    static public int deviceHeight=0;

    static public Handler m_Handler = new Handler();

    KbThread kbThread;
    RCTouchListener rcTouchListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_running);
        try {
            Intent intent = getIntent();
            Bundle bundle = intent.getExtras();
            logInfo = (LogInfo) bundle.get("userInfo");
            packetInfo = (PacketInfo) bundle.get("packetInfo");

            //***********button init*********************//
            exitButton = findViewById(R.id.bt_finish);
            exitButton.setOnClickListener(this);
            keybdButton = findViewById(R.id.bt_keybd);
            keybdButton.setOnClickListener(this);
            imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            //***********button init end*********************//

            dataQueue = new DataQueue();
            threadOn = true;

            imageView = (ImageView) findViewById(R.id.imageView1);
            imageView.setFocusableInTouchMode(true);        ///// for keypad

            clntAddr = new InetSocketAddress(logInfo.public_addr.getIP(), logInfo.public_addr.getSin_port());
            keyCode = 0;

            packetInfo.setType(RcProtocol.PACKET_TYPE_SEND_KB);
            packetInfo.convertEndian();

            head = ByteBuffer.allocateDirect(PacketInfo.SIZE());
            data = ByteBuffer.allocateDirect(Integer.SIZE);
            buf = new ByteBuffer[2];
            buf[0] = head;
            buf[1] = data;

            new Thread() {
                @Override
                public void run() {

                    try {
                        ch = DatagramChannel.open(StandardProtocolFamily.INET);
                        ch.bind(UdpConnection.myAddress);

                        ch.connect(clntAddr);
                        ch.write(ByteBuffer.allocate(1));

                        recvThread = new RecvThread(ch, dataQueue);
                        recvThread.start();

                        cache = getCacheDir();

                        imgThread = new ImgProcessingThread(dataQueue, imageView, cache);
                        imgThread.start();

                        rcTouchListener = new RCTouchListener(ch, packetInfo.uId, imageView);
                        imageView.setOnTouchListener(rcTouchListener);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }.start();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            Log.i("thread","RcActivity thread interrupt");
        }
    }

    class KbThread extends Thread {
        @Override
        public void run() {         ///// send keycode to control remote desktop

            head.put(packetInfo.toBytes());
            data.putInt(Convertor.convertEndian(keyCode));
            head.flip();
            data.flip();

            try {
                ch.write(buf);
                head.clear();
                data.clear();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    @Override
    public boolean onKeyDown(int code, KeyEvent event) {
        keyCode = code;

        Log.i("keybd", "code = "+ keyCode + " uni = " + event.getUnicodeChar());
        if(keyCode >= RcProtocol.NUMBER[0] && keyCode <=RcProtocol.NUMBER[1]){
            this.keyCode += RcProtocol.GAP[0];
        }
        else if(keyCode >= RcProtocol.ALPHABET[0] && keyCode <= RcProtocol.ALPHABET[1]){
            this.keyCode += RcProtocol.GAP[1];
        }
        else if(keyCode == 62)
            this.keyCode = 32;
        else if(keyCode == 67)
            this.keyCode = 8;

        kbThread = new KbThread();
        kbThread.start();

        return true;
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.bt_keybd){             ///// keypad will be appeared
            imageView.requestFocus();
            imm.showSoftInput(imageView, 0);
        }
        if (v.getId() == R.id.bt_finish) {
            recvThread.interrupt();
            //sendThread.interrupt();
            imgThread.interrupt();

            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        freeMemory();
    }

    void freeMemory() {
        threadOn = false;
        imgThread = null;
        recvThread = null;
        dataQueue = null;
        logInfo = null;
        clntAddr = null;
        onImg = false;
        kbThread = null;
        rcTouchListener.freeMemory();
        rcTouchListener = null;
    }
}
