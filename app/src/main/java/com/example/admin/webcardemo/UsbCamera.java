package com.example.admin.webcardemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Environment;
import android.util.Log;
import android.util.Size;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import com.example.admin.webcardemo.SockService;

public class UsbCamera {
    private String TAG = this.getClass().getSimpleName();
    private static int width;
    private static int height;
    private static int size;
    private boolean isCameraOk = false;
    private static boolean isFrameUpdate = false;

    private static ByteBuffer mDirectBuffer;
    private static Bitmap mBitmap;

    private static SockService sockService;

    public UsbCamera(int width, int height, SockService sockService) {
        this.width = width;
        this.height = height;
        this.size = width * height * 2;
        if (0 == UsbCameraInit(width, height)) {
            isCameraOk = true;
            mDirectBuffer = ByteBuffer.allocateDirect(this.size);
            UsbCameraSetDirectBuffer(mDirectBuffer, this.size);

            UsbCameraRegisterCallback();
        } else {
            isCameraOk = false;
        }

        this.sockService = sockService;
    }

    public int usbCameraStart() {
        if (0 != UsbCameraStart()) {
            Log.i(TAG, "Start usb camera failed");
            return -1;
        }

        return 0;
    }

    public int usbCameraStop() {
        if (0 != UsbCameraStop()) {
            Log.i(TAG, "Stop usb camera failed");
            return -1;
        }

        return 0;
    }

    public int usbCameraClose() {

        if (isCameraOk) {
            UsbCameraClose();
        }

        return 0;
    }

    public void usbCameraUpdate(){
        isFrameUpdate = true;
    }

    public boolean usbCameraUpdateStatus(){
        return isFrameUpdate;
    }
    public Bitmap usbCameraGetFrame(){
        return mBitmap;
    }


    //This function called by Native.
    public static void usbCameraDataHandle() {

        Size sizeyuv = new Size(640, 480);
        YuvImage image = new YuvImage(mDirectBuffer.array(), ImageFormat.YUY2, sizeyuv.getWidth(), sizeyuv.getHeight(), null);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        image.compressToJpeg(new Rect(0, 0, sizeyuv.getWidth(), sizeyuv.getHeight()), 80, outputStream);

        if (isFrameUpdate) {
            mBitmap = BitmapFactory.decodeByteArray(outputStream.toByteArray(), 0, outputStream.size());
            isFrameUpdate = false;
        }

        if (sockService.isClientExist()) {
            sockService.putData(outputStream.toByteArray());
        }

//
//        if(CapImageCount < 100) {
//            String fileName = "image_" + CapImageCount++ + ".jpg";
//            try {
//                // 创建指定路径的文件
//                File file = new File(Environment.getExternalStorageDirectory() + "/DCIM/", fileName);
//                // 如果文件不存在
//                if (file.exists()) {
//                    // 创建新的空文件
//                    //file.delete();
//                }
//                file.createNewFile();
//                // 获取文件的输出流对象
//                FileOutputStream outStream = new FileOutputStream(file);
//                // 获取字符串对象的byte数组并写入文件流
//                outStream.write(outputStream.toByteArray());
//                // 最后关闭文件输出流
//                outStream.close();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
    }

    public native int UsbCameraInit(int width, int height);

    public native int UsbCameraStart();

    public native String UsbCameraGetPictrue();

    public native int UsbCameraStop();

    public native int UsbCameraClose();

    public native void UsbCameraRegisterCallback();

    public native void UsbCameraSetDirectBuffer(Object buffer, int length);
}
