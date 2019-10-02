package com.example.sungmin.remotecontrolm;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
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

    static public int deviceWidth = 0;
    static public int deviceHeight = 0;

    static public Handler s_Handler = new Handler();

    KbThread kbThread;
    TouchThread touchThread;
    RCTouchListener rcTouchListener;
    GestureDetector detector;

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

            detector = new GestureDetector(this, new RcGestureListener(packetInfo.uId));

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

                        rcTouchListener = new RCTouchListener(detector, packetInfo.uId);
                        imageView.setOnTouchListener(rcTouchListener);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Log.i("thread", "RcActivity thread interrupt");
        }
    }

    class RcGestureListener extends GestureDetector.SimpleOnGestureListener {
        TouchThread touchThread;
        PacketInfo packetInfo;
        ByteBuffer[] buffer;
        ByteBuffer head;
        ByteBuffer data;
        MPoint mp;
        final String TAG = "Gesture";

        RcGestureListener(int uId) {
            head = ByteBuffer.allocateDirect(PacketInfo.SIZE());
            data = ByteBuffer.allocateDirect(Mouse_Point.SIZE());
            buffer = new ByteBuffer[2];
            buffer[0] = head;
            buffer[1] = data;
            packetInfo = new PacketInfo();
            packetInfo.setuId(uId);
            packetInfo.setType(RcProtocol.PACKET_TYPE_SEND_MP);
            packetInfo.convertEndian();
            mp = new MPoint();
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            mp = getPosition(imageView, e);
            Mouse_Point buf1 = new Mouse_Point(mp, RcProtocol.WM_LBUTTONDOWN);
            touchThread = new TouchThread(buffer, packetInfo, buf1);
            touchThread.start();
            Mouse_Point buf2 = new Mouse_Point(mp, RcProtocol.WM_LBUTTONUP);
            touchThread = new TouchThread(buffer, packetInfo, buf2);
            touchThread.start();

            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.i(TAG, "Double Click");

            mp = getPosition(imageView, e);
            Mouse_Point buf = new Mouse_Point(mp, RcProtocol.WM_LBUTTONDBCLK);
            touchThread = new TouchThread(buffer, packetInfo, buf);
            touchThread.start();
            return true;
        }

        MPoint getPosition(View v, MotionEvent event) {
            ImageView imageView = (ImageView) v;
            float[] values = new float[9];
            imageView.getImageMatrix().getValues(values);

            float width = imageView.getDrawable().getIntrinsicWidth() * values[0];
            float height = imageView.getDrawable().getIntrinsicHeight() * values[4];
            float x = event.getX() - values[2];
            float y = event.getY() - values[5];


            MPoint mp = new MPoint(x / width, y / height);

            return mp;
        }
    }

    class TouchThread extends Thread {         ///// send mouse point to control remote desktop
        ByteBuffer[] buffer;
        PacketInfo head;
        Mouse_Point data;

        TouchThread(ByteBuffer[] buffer, PacketInfo head, Mouse_Point data) {
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onKeyDown(int code, KeyEvent event) {
        keyCode = code;

        Log.i("keybd", "code = " + keyCode + " uni = " + event.getUnicodeChar());
        if (keyCode >= RcProtocol.NUMBER[0] && keyCode <= RcProtocol.NUMBER[1]) {
            this.keyCode += RcProtocol.GAP[0];
        } else if (keyCode >= RcProtocol.ALPHABET[0] && keyCode <= RcProtocol.ALPHABET[1]) {
            this.keyCode += RcProtocol.GAP[1];
        } else if (keyCode == 62)  //// spacebar
            this.keyCode = 32;
        else if (keyCode == 67)  //// backspace
            this.keyCode = 8;
        else if (keyCode == 56)  //// '.'
            this.keyCode = 46;
        else if (keyCode == 69)  //// '-'
            this.keyCode = 45;

        kbThread = new KbThread();
        kbThread.start();

        return true;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.bt_keybd) {             ///// keypad will be appeared
            imageView.requestFocus();
            imm.showSoftInput(imageView, 0);
        }
        if (v.getId() == R.id.bt_finish) {
            threadOn = false;
            onImg = false;

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

    class RCTouchListener implements View.OnTouchListener {      ///// support zoom, touch, screen drag mode
        private Matrix matrix;
        private Matrix savedMatrix;
        private float[] values;

        private MPoint mp;
        private int mode;
        private float oldDist;
        private float newDist;
        private MPoint midPoint;
        private MPoint startPoint;
        private float scale;
        private MPoint[] cordinateXY;
        private MPoint preXY;
        private MPoint nextXY;

        private boolean flag_longClick;
        private boolean flag_longLongClick;
        private CheckLongClick pendingCheckLongClick;
        private CheckLongLongClick pendingCheckLongLongClick;

        private ByteBuffer[] buffer;
        private ByteBuffer head;
        private ByteBuffer data;
        private int msg = -1;
        private TouchThread touchThread;
        private PacketInfo packetInfo;


        public RCTouchListener(GestureDetector detector, int uId) {
            head = ByteBuffer.allocateDirect(PacketInfo.SIZE());
            data = ByteBuffer.allocateDirect(Mouse_Point.SIZE());
            buffer = new ByteBuffer[2];
            buffer[0] = head;
            buffer[1] = data;
            packetInfo = new PacketInfo();
            packetInfo.setuId(uId);
            packetInfo.setType(RcProtocol.PACKET_TYPE_SEND_MP);
            packetInfo.convertEndian();

            matrix = new Matrix();
            savedMatrix = new Matrix();
            values = new float[9];
            mp = new MPoint();
            mode = 0;
            oldDist = 0;
            newDist = 0;
            midPoint = new MPoint();
            startPoint = new MPoint();
            cordinateXY = new MPoint[2];
            preXY = new MPoint();
            nextXY = new MPoint();
            cordinateXY[0] = preXY;
            cordinateXY[1] = nextXY;
            scale = 1;

            msg = -1;
            flag_longClick = false;
            flag_longLongClick = false;
            pendingCheckLongClick = new CheckLongClick();
            pendingCheckLongLongClick = new CheckLongLongClick();

        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            try {
                Log.i("event", " = " + event.getAction());
                detector.onTouchEvent(event);                                ///// double click gesture

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mode = RcProtocol.SHIFT;
                    mp = getPosition(v, event);

                    startPoint.x = event.getX();
                    startPoint.y = event.getY();

                    postCheckLongClick();

                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {             ///// touch End
                    cancelLongClick();
                    Log.i("event", "ACTION_UP");
                    if (mode == RcProtocol.DRAG) {                          ////// drag & drop
                        mp = getPosition(v, event);

                        Mouse_Point buf = new Mouse_Point(mp, RcProtocol.WM_LBUTTONUP);
                        touchThread = new TouchThread(buffer, packetInfo, buf);
                        touchThread.start();

                        flag_longClick = false;
                        flag_longLongClick = false;
                    }
                    return true;

                } else if ((event.getAction()
                        & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN) {    ////// multi touch
                    cancelLongClick();
                    preXY.setValue(event.getX(0), event.getY(0));
                    nextXY.setValue(event.getX(1), event.getY(1));
                    oldDist = spacing(cordinateXY);
                    if (oldDist > 5) {
                        midPoint(midPoint, cordinateXY);
                        mode = RcProtocol.ZOOM;
                    }
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_POINTER_UP) {    ////// multi touch end
                    mode = RcProtocol.NULL;
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {          ///// touch & move
                    Log.i("event", "ACTION_MOVE");
                    if (mode == RcProtocol.ZOOM) {                                ///// zoom mode
                        preXY.setValue(event.getX(0), event.getY(0));
                        nextXY.setValue(event.getX(1), event.getY(1));
                        newDist = spacing(cordinateXY);
                        if (newDist > 5) {
                            matrix.set(savedMatrix);
                            scale = newDist / oldDist;
                            matrix.postScale(scale, scale, (float) midPoint.x, (float) midPoint.y);
                            matrix.getValues(values);
                            float height = imageView.getDrawable().getIntrinsicHeight() * values[4];
                            float width = imageView.getDrawable().getIntrinsicWidth() * values[0];
                            if (values[5] <= 0 && values[5] + height >= RcActivity.deviceHeight) {
                                if (!(values[2] <= 0 && values[2] + width >= RcActivity.deviceWidth)) {
                                    scale = 1;
                                    matrix.reset();
                                }
                            } else {
                                scale = 1;
                                matrix.reset();
                            }
                            imageView.setImageMatrix(matrix);
                            savedMatrix.set(matrix);
                        }
                        oldDist = newDist;
                        return true;
                    } else if (mode == RcProtocol.SHIFT) {                 ///// drag mode
                        float dist = event.getY() - (float) startPoint.y;
                        if (Math.abs(dist) > 3) {
                            cancelLongClick();
                            matrix.set(savedMatrix);
                            matrix.postTranslate(0, event.getY() - (float) startPoint.y);
                            matrix.getValues(values);
                            float height = imageView.getDrawable().getIntrinsicHeight() * values[4];

                            if (values[5] <= 0 && values[5] + height >= RcActivity.deviceHeight) {
                                imageView.setImageMatrix(matrix);
                                savedMatrix.set(matrix);
                                mode = RcProtocol.NULL;
                            } else {                                        /////screen out of display vertically -> wheel event
                                matrix.set(savedMatrix);
                                imageView.setImageMatrix(matrix);
                                mp.y = dist;
                                mp.x = 0;
                                Mouse_Point buf = new Mouse_Point(mp, RcProtocol.WM_MOUSEWHEEL);
                                touchThread = new TouchThread(buffer, packetInfo, buf);
                                touchThread.start();
                            }
                            matrix.postTranslate(event.getX() - (float) startPoint.x, 0);
                            matrix.getValues(values);
                            float width = imageView.getDrawable().getIntrinsicWidth() * values[0];

                            if (values[2] <= 0 && values[2] + width >= RcActivity.deviceWidth) {
                                imageView.setImageMatrix(matrix);
                                savedMatrix.set(matrix);
                            } else {                                         /////screen out of display horizontally
                                matrix.set(savedMatrix);
                                imageView.setImageMatrix(matrix);
                            }

                            savedMatrix.set(matrix);
                            startPoint.x = event.getX();
                            startPoint.y = event.getY();
                        }
                        return true;
                    } else {
                        //msg = RcProtocol.WM_MOUSEMOVE;
                        //Mouse_Point buf = new Mouse_Point(mp, msg);
                        //sendData(buf);
                    }
                } else return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        ///// distance between points ///////
        float spacing(MPoint[] mp) {
            float x = 0;
            float y = 0;

            x = (float) mp[0].x - (float) mp[1].x;
            y = (float) mp[0].y - (float) mp[1].y;

            return (float) Math.sqrt(x * x + y * y);
        }

        void midPoint(MPoint point, MPoint[] mp) {
            point.x = ((float) mp[0].x + (float) mp[1].x) / 2.0F;
            point.y = ((float) mp[0].y + (float) mp[1].y) / 2.0F;
        }

        class CheckLongClick implements Runnable {
            @Override
            public void run() {
                if (performLongClick()) {
                    flag_longClick = true;
                    if (pendingCheckLongLongClick == null)
                        pendingCheckLongLongClick = new CheckLongLongClick();
                    RcActivity.s_Handler.postDelayed(pendingCheckLongLongClick, 1000);
                }
            }
        }

        class CheckLongLongClick implements Runnable {
            @Override
            public void run() {
                if (performLongLongClick()) {
                    flag_longLongClick = true;
                }
            }
        }

        ///// start long click //////
        private void postCheckLongClick() {
            flag_longClick = false;
            flag_longLongClick = false;

            if (pendingCheckLongClick == null)
                pendingCheckLongClick = new CheckLongClick();
            RcActivity.s_Handler.postDelayed(pendingCheckLongClick, ViewConfiguration.getLongPressTimeout());
        }

        ///// cancel long click /////
        private void cancelLongClick() {
            RcActivity.s_Handler.removeCallbacks(pendingCheckLongLongClick);
            RcActivity.s_Handler.removeCallbacks(pendingCheckLongClick);
        }

        ///// perform long click //////
        private boolean performLongClick() {
            mode = RcProtocol.DRAG;
            Mouse_Point buf = new Mouse_Point(mp, RcProtocol.WM_LBUTTONDOWN);
            touchThread = new TouchThread(buffer, packetInfo, buf);
            touchThread.start();

            return true;
        }

        ////// start longlong click //////
        private boolean performLongLongClick() {
            mode = RcProtocol.RCLICK;
            Mouse_Point buf = new Mouse_Point(mp, RcProtocol.WM_RBUTTONDOWN);
            touchThread = new TouchThread(buffer, packetInfo, buf);
            touchThread.start();
            buf = new Mouse_Point(mp, RcProtocol.WM_RBUTTONUP);
            touchThread = new TouchThread(buffer, packetInfo, buf);
            touchThread.start();

            mode = RcProtocol.NULL;
            return true;
        }

        ///// transform position ( display -> desktop ) ///////
        MPoint getPosition(View v, MotionEvent event) {
            ImageView imageView = (ImageView) v;
            float[] values = new float[9];
            imageView.getImageMatrix().getValues(values);

            float width = imageView.getDrawable().getIntrinsicWidth() * values[0];
            float height = imageView.getDrawable().getIntrinsicHeight() * values[4];
            float x = event.getX() - values[2];
            float y = event.getY() - values[5];


            MPoint mp = new MPoint(x / width, y / height);

            return mp;
        }

        public void freeMemory() {
            matrix = null;
            savedMatrix = null;
            mp = null;
            midPoint = null;
            startPoint = null;
            preXY = null;
            nextXY = null;
            cordinateXY[0] = null;
            cordinateXY[1] = null;
            values = null;
            pendingCheckLongClick = null;
            pendingCheckLongLongClick = null;
            touchThread = null;
            data = null;
            head = null;
            buffer = null;
            packetInfo = null;
        }
    }

}
