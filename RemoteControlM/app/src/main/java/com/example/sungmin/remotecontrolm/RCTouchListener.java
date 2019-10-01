package com.example.sungmin.remotecontrolm;

import android.content.Context;
import android.graphics.Matrix;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;

import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class RCTouchListener implements View.OnTouchListener {      ///// support zoom, touch, screen drag mode
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

    private ImageView imageView;

    private DatagramChannel ch;
    private ByteBuffer[] buffer;
    private ByteBuffer head;
    private ByteBuffer data;
    private PacketInfo packetInfo;
    private int msg = -1;
    private SendThread sendThread;


    public RCTouchListener(DatagramChannel ch, int uId, ImageView target) {
        this.ch = ch;
        imageView = target;
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
        Context context;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        try {
            Log.i("event", " = "+event.getAction());
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mode = RcProtocol.SHIFT;
                mp = getPosition(v, event);

                startPoint.x = event.getX();
                startPoint.y = event.getY();

                postCheckLongClick();

                //detector.onTouchEvent(event);                                ///// double click gesture
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {             ///// touch End
                cancelLongClick();
                Log.i("event", "ACTION_UP");
                if (flag_longClick == false) {
                    if (mode != RcProtocol.ZOOM) {                            ////// left click
                        Mouse_Point buf1 = new Mouse_Point(mp, RcProtocol.WM_LBUTTONDOWN);
                        sendThread = new SendThread(ch, buffer, packetInfo, buf1);
                        sendThread.start();
                        Mouse_Point buf2 = new Mouse_Point(mp, RcProtocol.WM_LBUTTONUP);
                        sendThread = new SendThread(ch, buffer, packetInfo, buf2);
                        sendThread.start();
                    }
                       return true;
                } else {                                                        ///// drag mode
                    if (flag_longLongClick == false)
                        mp = getPosition(v, event);
                    msg = RcProtocol.WM_LBUTTONUP;
                    flag_longClick = false;
                    flag_longLongClick = false;
                }

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
                    float dist = Math.abs(event.getY() - (float) startPoint.y);
                    if (dist > 3) {
                        cancelLongClick();
                        matrix.set(savedMatrix);
                        matrix.postTranslate(0, event.getY() - (float) startPoint.y);
                        matrix.getValues(values);
                        float height = imageView.getDrawable().getIntrinsicHeight() * values[4];

                        if (values[5] <= 0 && values[5] + height >= RcActivity.deviceHeight) {
                            imageView.setImageMatrix(matrix);
                            savedMatrix.set(matrix);
                        } else {                                        /////screen out of display vertically
                            matrix.set(savedMatrix);
                            imageView.setImageMatrix(matrix);
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


            return true;
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
                RcActivity.s_Handler.postDelayed(pendingCheckLongLongClick, 3000);
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
        sendThread = new SendThread(ch, buffer, packetInfo, buf);
        sendThread.start();

        return true;
    }

    ////// start longlong click //////
    private boolean performLongLongClick() {
        mode = RcProtocol.RCLICK;
        Mouse_Point buf = new Mouse_Point(mp, RcProtocol.WM_RBUTTONDOWN);
        sendThread = new SendThread(ch, buffer, packetInfo, buf);
        sendThread.start();
        buf = new Mouse_Point(mp, RcProtocol.WM_RBUTTONUP);
        sendThread = new SendThread(ch, buffer, packetInfo, buf);
        sendThread.start();

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
    public void freeMemory(){
        matrix = null;
        savedMatrix = null;
        mp = null;
        midPoint = null;
        startPoint = null;
        preXY = null;
        nextXY =  null;
        cordinateXY[0] = null;
        cordinateXY[1] = null;
        values = null;
        pendingCheckLongClick = null;
        pendingCheckLongLongClick = null;
        sendThread = null;
        data = null;
        head = null;
        buffer = null;
        packetInfo = null;
    }

    class RcGestureDetector extends GestureDetector.SimpleOnGestureListener {
        Mouse_Point buf;
        MPoint mp;
        View view;
        final String TAG = "Gesture";

        RcGestureDetector(View view){
            this.view = view;
            mp = new MPoint();
            buf = new Mouse_Point(mp, 0);
        }
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.i(TAG, "Double Click");

            mp = getPosition(view, e);
            buf.point = mp;
            buf.msg = RcProtocol.WM_LBUTTONDBCLK;
            //sendData(buf);
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
}
