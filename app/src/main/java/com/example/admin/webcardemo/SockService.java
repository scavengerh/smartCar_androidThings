package com.example.admin.webcardemo;


import android.util.Log;

import java.awt.font.TextAttribute;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class SockService extends Thread {
    private String TAG = this.getClass().getSimpleName();
    private ServerSocket server;
    private Socket clientSocket;
    private InputStream ins = null;
    private OutputStream outs = null;
    private boolean isClientExist = false;

    private boolean isRun = false;
    private MotorControl motorControl;

    private LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue();
    private LinkedBlockingQueue<byte[]> queueStr = new LinkedBlockingQueue();

    public SockService() {
        motorControl = MotorControl.getInstance();
    }

    public void putData(byte[] buffer) {
        try {
            queue.put(buffer);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void putStrData(byte[] buffer){
        try {
            queueStr.put(buffer);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void set(boolean run) {
        isRun = run;
    }

    public boolean isClientExist() {
        return isClientExist;
    }

    public void run() {
        super.run();

        isRun = true;
        try {
            server = new ServerSocket(8080);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (isRun) {
            try {
                if (isClientExist == false) {
                    if (clientSocket != null) {
                        clientSocket.close();
                    }
                    clientSocket = server.accept(); //阻塞等待从设备连接
                    ins = clientSocket.getInputStream();
                    outs = clientSocket.getOutputStream();
                    isClientExist = true;
                    new Thread(new SendRunable()).start();
                    new Thread(new RecvRunable()).start();
                    Log.i(TAG, "server: client " + clientSocket.getInetAddress().getHostAddress() + " connected");
                } else {
                    Thread.sleep(200);
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }
    }

    public class SendRunable implements Runnable {

        @Override
        public void run() {
            byte[] endPrompt = new byte[4];
            endPrompt[0] = 0x11;
            endPrompt[1] = 0x22;
            while (isClientExist) {
                try {
                    if (queue.size() > 0) {
                        //发送图像数据，数据为jpg图像流
                        endPrompt[2] = 0x33;
                        endPrompt[3] = 0x55;
                        byte[] data = queue.poll();
                        outs.write(data);
                        outs.write(endPrompt);
                        outs.flush();
                    }else if(queueStr.size() > 0){
                        //发送图像数据，数据为jpg图像流
                        endPrompt[2] = 0x44;
                        endPrompt[3] = 0x66;
                        byte[] data = queueStr.poll();
                        outs.write(data);
                        outs.write(endPrompt);
                        outs.flush();
                    }else {
                        Thread.sleep(5);
                        continue;
                    }
                } catch (IOException e) {
                    isClientExist = false;
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class RecvRunable implements Runnable {

        @Override
        public void run() {
            byte[] recvBuff = new byte[1024];
            int readLen = 0;
            while (isClientExist) {
                try {
                    readLen = ins.read(recvBuff);
                    if (readLen == 3) {
                        switch (recvBuff[0]) {
                            case 0x10:
                                motorControl.motorContrlAngle((double) recvBuff[1] * 10, true);
                                break;
                            case 0x20:
                                motorControl.controlSevroMotorCmd(1, recvBuff[2]);
                                break;
                            case 0x30:
                                motorControl.motorContrlAngle((double) recvBuff[1] * 10, false);
                                motorControl.controlSevroMotorCmd(3,0);
                                break;
                            default:
                                break;
                        }
                    }
                   // Log.i(TAG, "Server： receive data length:" + readLen + ",data: " + recvBuff[0] + ", " + recvBuff[1] + ", " + recvBuff[2]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
