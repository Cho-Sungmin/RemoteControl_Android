package com.example.sungmin.remotecontrolm;

import java.nio.ByteBuffer;

public class DataQueue {
    //private ImagePacket[] queue;
    private ByteBuffer[] queue;
    private int front;
    private int rear;
    public static int QSIZE = 300;

    public DataQueue() {
        //queue = new ImagePacket[QSIZE];
        queue = new ByteBuffer[QSIZE];
        for(int i=0; i<QSIZE; i++)
        {
            //queue[i] = new ImagePacket();
            queue[i] = ByteBuffer.allocateDirect(ImagePacket.SIZE());
        }
        front = rear = 0;
    }

    boolean enqueue(/*ImagePacket*/ ByteBuffer data) {
        synchronized (this) {
            if (!isFull()) {
                rear = (rear + 1) % QSIZE;
                //queue[rear] = value;
                //queue[rear].imgcpy(value);
                queue[rear].put(data);
                queue[rear].flip();
                return true;
            } else
                return false;
        }
    }

    boolean dequeue(/*ImagePacket*/ ByteBuffer data) {
        synchronized (this) {
            if (!isEmpty()) {
                front = (front + 1) % QSIZE;
                data.put(queue[front]);
                queue[front].clear();
                data.flip();
                return true;
            } else
                return false;
        }
    }

    boolean isFull() {
        if (front == (rear + 1) % QSIZE)
            return true;
        else
            return false;
    }

    boolean isEmpty() {
        if (front == rear)
            return true;
        else
            return false;
    }

    /*ImagePacket peek() {
        if (isEmpty() != true)
            return queue[front + 1];
        else
            return queue[0];
    }*/

    public void freeMemory()
    {
        for(int i=0; i<QSIZE; i++)
        {
            queue[i] = null;
        }
        queue = null;
    }


}
