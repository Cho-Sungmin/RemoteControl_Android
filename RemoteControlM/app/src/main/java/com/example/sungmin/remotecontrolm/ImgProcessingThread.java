package com.example.sungmin.remotecontrolm;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class ImgProcessingThread extends Thread {
    DataQueue dataQueue;
    FileOutputStream writer;
    ImagePacket[] imgFile;
    ImageView imageView;
    File img;
    File cache;
    String fileName;
    int seq = 0;

    public ImgProcessingThread(DataQueue queue, ImageView imageView, File path) {
        this.dataQueue = queue;
        this.imageView = imageView;
        this.cache = path;
        fileName = "img.jpg";
        imgFile = new ImagePacket[1];
    }

    @Override
    public void run() {
        try {
            while (RcActivity.threadOn) {
                saveImage();
                if (RcActivity.onImg == false) {
                    showImage();
                }else
                    sleep(1);       ///// switch to another thread
            }
            freeMemory();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            freeMemory();
        }
    }

    public void saveImage() {
        RcActivity.onImg = true;
        seq = 0;
        try {
            img = new File(cache, fileName);
            writer = new FileOutputStream(img);
            while (true) {
                boolean enter = dataQueue.dequeue(imgFile);
                imgFile[0].convertEndian();
                if (enter) {
                    if (imgFile[0].getSeq() > seq) {        ///// fragments remain
                        writer.write(imgFile[0].getData(), 0, imgFile[0].getSize());
                        seq = imgFile[0].getSeq();

                        if (imgFile[0].getFlag() == 0)      ///// the last fragment arrived
                            break;
                    } else
                        break;
                } else {
                    sleep(1);
                }
            }
            writer.close();
            RcActivity.onImg = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showImage() {
        try {
            final String path = cache + "/img.jpg";
            final Bitmap bm = BitmapFactory.decodeFile(path);

            //int deviceWidth = imageView.getWidth();
            //int deviceHeight = imageView.getHeight();
            // BitmapFactory.Options bmpOps = new BitmapFactory.Options();
            // bmpOps.inJustDecodeBounds = true;
            //BitmapFactory.decodeFile(path, bmpOps);
            // deviceWidth = bmpOps.outWidth;
            // deviceHeight = bmpOps.outHeight;

            //int scaleFactor = 1;
            // bmpOps.inJustDecodeBounds = false;
            // bmpOps.inSampleSize = scaleFactor;

            imageView.post(new Runnable() {

                @Override
                public void run() {

                    if (bm != null) {
                        imageView.setImageBitmap(bm);
                    }
                    RcActivity.deviceWidth = imageView.getWidth();
                    RcActivity.deviceHeight = imageView.getHeight();
                    RcActivity.onImg = false;
                }
            });
            sleep(1);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}