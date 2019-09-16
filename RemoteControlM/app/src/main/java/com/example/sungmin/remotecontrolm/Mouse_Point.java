package com.example.sungmin.remotecontrolm;

import android.content.Intent;

import java.nio.ByteBuffer;

class MPoint{
    double x;
    double y;

    MPoint(){
        this.x = 0;
        this.y = 0;
    }
    MPoint(MPoint p){
        this.x = p.x;
        this.y = p.y;
    }
    MPoint(double x, double y){
        this.x = x;
        this.y = y;
    }

    public void setValue(double x, double y){
        this.x = x;
        this.y = y;
    }

    public static int SIZE(){
        return 2*(Double.SIZE/8);
    }

    public byte[] toBytes(){
        ByteBuffer buffer = ByteBuffer.allocate(SIZE());

        buffer.putDouble(this.x);
        buffer.putDouble(this.y);
        buffer.flip();

        byte[] arr = new byte[SIZE()];

        buffer.get(arr);

        return arr;
    }

    public void convertEndian(){

        this.x = Convertor.convertEndian(this.x);
        this.y = Convertor.convertEndian(this.y);
    }

}
public class Mouse_Point {
    MPoint point;
    int msg;

    public Mouse_Point(int msg){
        this.point = new MPoint();
        this.msg = msg;
    }
    public Mouse_Point(MPoint point, int msg){
        this.point = new MPoint(point);
        this.msg = msg;
    }
    public static int SIZE(){
        return MPoint.SIZE()+ (Integer.SIZE/8);
    }

    public byte[] toBytes(){
        ByteBuffer buffer = ByteBuffer.allocate(SIZE());

        buffer.put(this.point.toBytes());
        buffer.putInt(this.msg);
        buffer.flip();

        byte[] arr = new byte[SIZE()];

        buffer.get(arr);

        return arr;
    }

    public void convertEndian(){

        this.point.convertEndian();
        this.msg = Convertor.convertEndian(this.msg);
    }
}
