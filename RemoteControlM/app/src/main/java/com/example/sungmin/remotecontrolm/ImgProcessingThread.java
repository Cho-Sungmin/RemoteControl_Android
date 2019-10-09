package com.example.sungmin.remotecontrolm;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import java.nio.ByteBuffer;

public class ImgProcessingThread extends Thread {
    DataQueue dataQueue;
    ByteBuffer buffer;
    ByteBuffer imgBuffer;
    byte[] data;
    ImagePacket imgFile;
    ImageView imageView;
    int seq = 0;
    int size = 0;
    Bitmap bmp;


    public ImgProcessingThread(DataQueue queue, ImageView imageView) {
        this.dataQueue = queue;
        this.imageView = imageView;

        imgFile = new ImagePacket();

        buffer = ByteBuffer.allocateDirect(ImagePacket.SIZE());
        imgBuffer = ByteBuffer.allocateDirect(RcProtocol.IMAGE_BUFFER_SIZE);
        data = new byte[RcProtocol.IMAGE_BUFFER_SIZE];
    }

    @Override
    public void run() {
        try {
            while (RcActivity.threadOn) {
                if(saveImage())
                    showImage();
            }
            freeMemory();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            freeMemory();
        }
    }
    public boolean saveImage() {
        seq = 0;
        try {
            imgBuffer.clear();
            while (true) {

                if (dataQueue.dequeue(buffer)) {
                    Convertor.byteToImagePacket(buffer, imgFile);
                    imgFile.convertEndian();
                    if (imgFile.getSeq() > seq) {        ///// fragments remain
                        imgBuffer.put(imgFile.getData(), 0, imgFile.getSize());
                        seq = imgFile.getSeq();
                        if (imgFile.getFlag() == 0) {     ///// the last fragment arrived
                            imgBuffer.flip();
                            size = imgBuffer.limit();
                            imgBuffer.get(data, 0, size);
                            break;
                        }
                    }else {
                        return false;
                    }
                }else
                    sleep(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
    public void showImage() {
        try {
            final Bitmap bm = BitmapFactory.decodeByteArray(data, 0, size);
            imageView.post(new Runnable() {

                @Override
                public void run() {

                    if (bm != null) {
                        imageView.setImageBitmap(bm);
                        imageView.invalidate();
                        if(bmp!=null)
                            bmp.recycle();
                        bmp = bm;
                    }
                    RcActivity.deviceWidth = imageView.getWidth();
                    RcActivity.deviceHeight = imageView.getHeight();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void freeMemory() {
        try {
            dataQueue = null;
            imgFile = null;
            //img = null;
            //cache = null;
            buffer = null;
            imgBuffer = null;
            data = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}