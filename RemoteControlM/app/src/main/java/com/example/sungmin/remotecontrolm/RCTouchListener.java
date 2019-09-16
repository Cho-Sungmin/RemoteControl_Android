package com.example.sungmin.remotecontrolm;

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
    Matrix m_Matrix;
    Matrix m_SavedMatrix;
    float[] m_Values;

    MPoint m_Mp;
    int m_Mode;
    float m_OldDist;
    float m_NewDist;
    MPoint m_MidPoint;
    MPoint m_StartPoint;
    float m_Scale;
    MPoint[] m_XY;
    MPoint m_stXY;
    MPoint m_ndXY;

    boolean m_isLongClick;
    boolean m_isLongLongClick;
    CheckLongClick m_PendingCheckLongClick;
    CheckLongLongClick m_PendingCheckLongLongClick;

    ImageView m_ImageView;

    DatagramChannel m_ch;
    ByteBuffer[] m_buffer;
    ByteBuffer m_head;
    ByteBuffer m_data;
    PacketInfo m_PacketInfo;
    int m_Msg = -1;
    SendThread m_SendThread;

    public RCTouchListener(DatagramChannel ch, int uId, ImageView target) {
        m_ch = ch;
        m_ImageView = target;
        m_head = ByteBuffer.allocateDirect(PacketInfo.SIZE());
        m_data = ByteBuffer.allocateDirect(Mouse_Point.SIZE());
        m_buffer = new ByteBuffer[2];
        m_buffer[0] = m_head;
        m_buffer[1] = m_data;
        m_PacketInfo = new PacketInfo();
        m_PacketInfo.setuId(uId);
        m_PacketInfo.setType(RcProtocol.PACKET_TYPE_SEND_MP);
        m_PacketInfo.convertEndian();

        m_Matrix = new Matrix();
        m_SavedMatrix = new Matrix();
        m_Values = new float[9];
        m_Mp = new MPoint();
        m_Mode = 0;
        m_OldDist = 0;
        m_NewDist = 0;
        m_MidPoint = new MPoint();
        m_StartPoint = new MPoint();
        m_XY = new MPoint[2];
        m_stXY = new MPoint();
        m_ndXY = new MPoint();
        m_XY[0] = m_stXY;
        m_XY[1] = m_ndXY;
        m_Scale = 1;

        m_Msg = -1;
        m_isLongClick = false;
        m_isLongLongClick = false;
        m_PendingCheckLongClick = new CheckLongClick();
        m_PendingCheckLongLongClick = new CheckLongLongClick();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        try {
            Log.i("event", " = "+event.getAction());
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                m_Mode = RcProtocol.SHIFT;
                m_Mp = getPosition(v, event);

                m_StartPoint.x = event.getX();
                m_StartPoint.y = event.getY();

                postCheckLongClick();

                //m_detector.onTouchEvent(event);                                ///// double click gesture
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {             ///// touch End
                cancelLongClick();
                Log.i("event", "ACTION_UP");
                if (m_isLongClick == false) {
                    if (m_Mode != RcProtocol.ZOOM) {                            ////// left click
                        Mouse_Point buf1 = new Mouse_Point(m_Mp, RcProtocol.WM_LBUTTONDOWN);
                        m_SendThread = new SendThread(m_ch, m_buffer, m_PacketInfo, buf1);
                        m_SendThread.start();
                        Mouse_Point buf2 = new Mouse_Point(m_Mp, RcProtocol.WM_LBUTTONUP);
                        m_SendThread = new SendThread(m_ch, m_buffer, m_PacketInfo, buf2);
                        m_SendThread.start();
                    }
                       return true;
                } else {                                                        ///// drag mode
                    if (m_isLongLongClick == false)
                        m_Mp = getPosition(v, event);
                    m_Msg = RcProtocol.WM_LBUTTONUP;
                    m_isLongClick = false;
                    m_isLongLongClick = false;
                }

            } else if ((event.getAction()
                    & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN) {    ////// multi touch
                cancelLongClick();
                m_stXY.setValue(event.getX(0), event.getY(0));
                m_ndXY.setValue(event.getX(1), event.getY(1));
                m_OldDist = spacing(m_XY);
                if (m_OldDist > 5) {
                    midPoint(m_MidPoint, m_XY);
                    m_Mode = RcProtocol.ZOOM;
                }
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_POINTER_UP) {    ////// multi touch end
                m_Mode = RcProtocol.NULL;
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {          ///// touch & move
                Log.i("event", "ACTION_MOVE");
                if (m_Mode == RcProtocol.ZOOM) {                                ///// zoom mode
                    m_stXY.setValue(event.getX(0), event.getY(0));
                    m_ndXY.setValue(event.getX(1), event.getY(1));
                    m_NewDist = spacing(m_XY);
                    if (m_NewDist > 5) {
                        m_Matrix.set(m_SavedMatrix);
                        m_Scale = m_NewDist / m_OldDist;
                        m_Matrix.postScale(m_Scale, m_Scale, (float) m_MidPoint.x, (float) m_MidPoint.y);
                        m_Matrix.getValues(m_Values);
                        float height = m_ImageView.getDrawable().getIntrinsicHeight() * m_Values[4];
                        float width = m_ImageView.getDrawable().getIntrinsicWidth() * m_Values[0];
                        if (m_Values[5] <= 0 && m_Values[5] + height >= RcActivity.deviceHeight) {
                            if (!(m_Values[2] <= 0 && m_Values[2] + width >= RcActivity.deviceWidth)) {
                                m_Scale = 1;
                                m_Matrix.reset();
                            }
                        } else {
                            m_Scale = 1;
                            m_Matrix.reset();
                        }
                        m_ImageView.setImageMatrix(m_Matrix);
                        m_SavedMatrix.set(m_Matrix);
                    }
                    m_OldDist = m_NewDist;
                    return true;
                } else if (m_Mode == RcProtocol.SHIFT) {                 ///// drag mode
                    float dist = Math.abs(event.getY() - (float) m_StartPoint.y);
                    if (dist > 3) {
                        cancelLongClick();
                        m_Matrix.set(m_SavedMatrix);
                        m_Matrix.postTranslate(0, event.getY() - (float) m_StartPoint.y);
                        m_Matrix.getValues(m_Values);
                        float height = m_ImageView.getDrawable().getIntrinsicHeight() * m_Values[4];

                        if (m_Values[5] <= 0 && m_Values[5] + height >= RcActivity.deviceHeight) {
                            m_ImageView.setImageMatrix(m_Matrix);
                            m_SavedMatrix.set(m_Matrix);
                        } else {                                        /////screen out of display vertically
                            m_Matrix.set(m_SavedMatrix);
                            m_ImageView.setImageMatrix(m_Matrix);
                        }
                        m_Matrix.postTranslate(event.getX() - (float) m_StartPoint.x, 0);
                        m_Matrix.getValues(m_Values);
                        float width = m_ImageView.getDrawable().getIntrinsicWidth() * m_Values[0];

                        if (m_Values[2] <= 0 && m_Values[2] + width >= RcActivity.deviceWidth) {
                            m_ImageView.setImageMatrix(m_Matrix);
                            m_SavedMatrix.set(m_Matrix);
                        } else {                                         /////screen out of display horizontally
                            m_Matrix.set(m_SavedMatrix);
                            m_ImageView.setImageMatrix(m_Matrix);
                        }

                        m_SavedMatrix.set(m_Matrix);
                        m_StartPoint.x = event.getX();
                        m_StartPoint.y = event.getY();
                    }
                    return true;
                } else {
                    //m_Msg = RcProtocol.WM_MOUSEMOVE;
                    //Mouse_Point buf = new Mouse_Point(m_Mp, m_Msg);
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
                m_isLongClick = true;
                if (m_PendingCheckLongLongClick == null)
                    m_PendingCheckLongLongClick = new CheckLongLongClick();
                RcActivity.m_Handler.postDelayed(m_PendingCheckLongLongClick, 3000);
            }
        }
    }

    class CheckLongLongClick implements Runnable {
        @Override
        public void run() {
            if (performLongLongClick()) {
                m_isLongLongClick = true;
            }
        }
    }

    ///// start long click //////
    private void postCheckLongClick() {
        m_isLongClick = false;
        m_isLongLongClick = false;

        if (m_PendingCheckLongClick == null)
            m_PendingCheckLongClick = new CheckLongClick();
        RcActivity.m_Handler.postDelayed(m_PendingCheckLongClick, ViewConfiguration.getLongPressTimeout());
    }

    ///// cancel long click /////
    private void cancelLongClick() {
        RcActivity.m_Handler.removeCallbacks(m_PendingCheckLongLongClick);
        RcActivity.m_Handler.removeCallbacks(m_PendingCheckLongClick);
    }

    ///// perform long click //////
    private boolean performLongClick() {
        m_Mode = RcProtocol.DRAG;
        Mouse_Point buf = new Mouse_Point(m_Mp, RcProtocol.WM_LBUTTONDOWN);
        m_SendThread = new SendThread(m_ch, m_buffer, m_PacketInfo, buf);
        m_SendThread.start();

        return true;
    }

    ////// start longlong click //////
    private boolean performLongLongClick() {
        m_Mode = RcProtocol.RCLICK;
        Mouse_Point buf = new Mouse_Point(m_Mp, RcProtocol.WM_RBUTTONDOWN);
        m_SendThread = new SendThread(m_ch, m_buffer, m_PacketInfo, buf);
        m_SendThread.start();
        buf = new Mouse_Point(m_Mp, RcProtocol.WM_RBUTTONUP);
        m_SendThread = new SendThread(m_ch, m_buffer, m_PacketInfo, buf);
        m_SendThread.start();

        m_Mode = RcProtocol.NULL;
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
        m_Matrix = null;
        m_SavedMatrix = null;
        m_Mp = null;
        m_MidPoint = null;
        m_StartPoint = null;
        m_stXY = null;
        m_ndXY =  null;
        m_XY[0] = null;
        m_XY[1] = null;
        m_Values = null;
        m_PendingCheckLongClick = null;
        m_PendingCheckLongLongClick = null;
        m_SendThread = null;
        m_data = null;
        m_head = null;
        m_buffer = null;
        m_PacketInfo = null;
    }

    class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
        Mouse_Point buf;
        MPoint mp;
        View view;
        final String TAG = "Gesture";

        MyGestureDetector(View view){
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
