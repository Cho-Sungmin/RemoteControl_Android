package com.example.sungmin.remotecontrolm;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.ContactsContract;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;

public class ImgProcessingThread extends Thread {
    DataQueue dataQueue;
    FileOutputStream writer;
    ByteBuffer buffer;
    ImagePacket imgFile;
    ImageView imageView;
    File img;
    File cache;
    String fileName;
    int seq = 0;
    String path;
    Bitmap bmp;


    public ImgProcessingThread(DataQueue queue, ImageView imageView, File path) {
        this.dataQueue = queue;
        this.imageView = imageView;
        this.cache = path;
        fileName = "img.jpg";
        imgFile = new ImagePacket();
        img = new File(cache, fileName);
        img.deleteOnExit();
        this.path = cache + "/img.jpg";
        buffer = ByteBuffer.allocateDirect(ImagePacket.SIZE());
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
            writer = new FileOutputStream(img);
            while (true) {
                if (dataQueue.dequeue(buffer)) {
                    Convertor.byteToImagePacket(buffer, imgFile);
                    imgFile.convertEndian();
                    if (imgFile.getSeq() > seq) {        ///// fragments remain
                        writer.write(imgFile.getData(), 0, imgFile.getSize());
                        seq = imgFile.getSeq();
                        if (imgFile.getFlag() == 0)      ///// the last fragment arrived
                            break;
                    }else {
                        writer.close();
                        return false;
                    }
                }else
                    sleep(1);
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public void showImage() {
        try {
            final Bitmap bm = BitmapFactory.decodeFile(path);

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
            writer = null;
            imgFile = null;
            img = null;
            cache = null;
            buffer = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}