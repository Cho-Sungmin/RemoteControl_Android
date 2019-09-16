package com.example.sungmin.remotecontrolm;

public class DataQueue {
    private ImagePacket[] queue;
    private int front;
    private int rear;
    public static int QSIZE = 500;

    public DataQueue() {
        queue = new ImagePacket[QSIZE];
        front = rear = 0;
    }

    boolean enqueue(ImagePacket value) {
        if (!isFull()) {
            synchronized (this) {
                rear = (rear + 1) % QSIZE;
                queue[rear] = value;
            }

            return true;
        } else
            return false;
    }

    boolean dequeue(ImagePacket[] data) {
        if (!isEmpty()) {
            synchronized (this) {
                front = (front + 1) % QSIZE;
            }
            data[0] = queue[front];

            return true;
        } else
            return false;
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

    ImagePacket peek() {
        if (isEmpty() != true)
            return queue[front + 1];
        else
            return queue[0];
    }

}
