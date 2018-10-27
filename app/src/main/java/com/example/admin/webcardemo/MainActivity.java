package com.example.admin.webcardemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.util.Size;

import com.example.admin.webcardemo.classifier.Recognition;
import com.example.admin.webcardemo.classifier.TensorFlowImageClassifier;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.media.CamcorderProfile.get;

//https://muyangmin.github.io/glide-docs-cn/javadocs/470/index.html
import com.bumptech.glide.gifdecoder.*;

// http://www.w3school.com.cn/tags/tag_br.asp   HTML语言开发
public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String MODEL_ChineseFILE = "labelsChinese.txt";
    private static final String MODEL_indexFILE = "labels.txt";
    private static final int TF_INPUT_IMAGE_WIDTH = 224;
    private static final int TF_INPUT_IMAGE_HEIGHT = 224;

    public String HostIp;

    private MotorControl motorControl;
    private OledControl oledControl;
    private TensorFlowImageClassifier mTensorFlowClassifier;
    private Handler TrigCanera;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private  boolean usbCameraisOK = false;
    private Map<String, String > engToCh;
    private static SockService sockService;
    private UsbCamera usbCamera;

    static {
        System.loadLibrary("native-lib");
    }

    private String intToIp(int i) {
        return (i & 0xFF ) + "." +
                ((i >> 8 ) & 0xFF) + "." +
                ((i >> 16 ) & 0xFF) + "." +
                ( i >> 24 & 0xFF) ;
    }

    /**
     * 获取Wifi IP地址
     * */
    private String getWifiIp(){
        String ip;
        WifiManager wm=(WifiManager)getSystemService(Context.WIFI_SERVICE);
        //检查Wifi状态
        if(!wm.isWifiEnabled())
            wm.setWifiEnabled(true);
        WifiInfo wi= wm.getConnectionInfo();
        //获取32位整型IP地址
        int ipAdd = wi.getIpAddress();
        //把整型地址转换成“*.*.*.*”地址
        ip=intToIp(ipAdd);
        return ip;
    }

    /**
     * 获取以太网ip地址
     * @return
     */
    private String getNetworkIp() {
        String hostIp = null;
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia = null;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        continue;// skip ipv6
                    }
                    String ip = ia.getHostAddress();
                    if (!"127.0.0.1".equals(ip)) {
                        hostIp = ia.getHostAddress();
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            Log.i("yao", "SocketException");
            e.printStackTrace();
        }

        if(hostIp == null){
            return getWifiIp();
        }else
            return hostIp;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate start...");
        HostIp = getNetworkIp();
        Log.i(TAG, "Get IP address: " + HostIp);

        /**
         * Start Oled screen for display current status.
         * */
        oledControl = new OledControl();
        oledControl.init(getResources());
        oledControl.drawlogoGif();

        motorControl = MotorControl.getInstance();
        motorControl.Init();

        oledControl.drawStrings("启动电机成功。");


        /**
         * Start Socket service
         * */
        sockService = new SockService();
        sockService.start();
        oledControl.drawStrings("启动网络服务成功。");

        /**
         * Load camera driver
         * */
        if(false == usbCameraisOK){
            //Start Camera
            usbCamera = new UsbCamera(640, 480, sockService);
            if(usbCamera.usbCameraStart() == 0){
                usbCameraisOK = true;
                oledControl.drawStrings("启动摄像头成功。");
            }
        }

        createTabelMap();
        initBackgroundThread();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        motorControl.close();
        oledControl.Deinit();

        if(usbCameraisOK){
            usbCamera.usbCameraStop();
            usbCamera.usbCameraClose();
        }

        try {
            if (mBackgroundThread != null) mBackgroundThread.quit();
        } catch (Throwable t) {
            // close quietly
        }
        mBackgroundThread = null;
        mBackgroundHandler = null;

        try {
            if (mTensorFlowClassifier != null) mTensorFlowClassifier.destroyClassifier();
        } catch (Throwable t) {
            // close quietly
        }

    }

    /**
     * Initialize main loop for display status and gif picture.
     * */
    private void initBackgroundThread() {
        mBackgroundThread = new HandlerThread("BackgroundThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        mBackgroundHandler.post(mInitializeOnBackground);
    }

    /**
     * Create table for check result map to chinese.
     * */
    private void createTabelMap(){
        engToCh = new HashMap<>();
        InputStream ins = null;
        InputStream insc = null;
        try {
            ins = this.getAssets().open(MODEL_indexFILE);
            insc = this.getAssets().open(MODEL_ChineseFILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedReader reader = null;
        BufferedReader readerc = null;
        try {
            String engString = null;
            String chineString = null;

            reader = new BufferedReader(new InputStreamReader(ins));
            readerc = new BufferedReader(new InputStreamReader(insc));
            while ((engString = reader.readLine()) !=null){
                chineString = readerc.readLine();
                engToCh.put(engString, chineString);
            }
            reader.close();
            readerc.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static Bitmap toSmall(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postScale((float) TF_INPUT_IMAGE_WIDTH/bitmap.getWidth(),(float) TF_INPUT_IMAGE_HEIGHT/bitmap.getHeight()); //长和宽放大缩小的比例
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
        return resizeBmp;
    }

    private Runnable mInitializeOnBackground = new Runnable() {
        @Override
        public void run() {
            try {
                mTensorFlowClassifier = new com.example.admin.webcardemo.classifier.TensorFlowImageClassifier(com.example.admin.webcardemo.MainActivity.this,
                        TF_INPUT_IMAGE_WIDTH, TF_INPUT_IMAGE_HEIGHT);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot initialize TFLite Classifier", e);
            }
            Log.i(TAG, "install tensorflow");

            TrigCanera = new Handler();
            TrigCanera.postDelayed(TensorflowRun, 2000);
            Log.i(TAG, "Start tensorflowrun thread");
        }
    };


    private  Runnable TensorflowRun =new Runnable() {
        private boolean cameraFrameIsOk = false;

        @Override
        public void run() {
            //如果摄像头没有启动，再次启动摄像头。
            if(false == usbCameraisOK){
                //Start Camera
                usbCamera = new UsbCamera(640, 480, sockService);
                if(usbCamera.usbCameraStart() == 0){
                    usbCameraisOK = true;
                    oledControl.drawStrings("启动摄像头成功。");
                }
            }else{
                //如果有客户端手机连接，执行抓取图像，并识别
                if(sockService.isClientExist()){
                    if(false == cameraFrameIsOk){
                        usbCamera.usbCameraUpdate();
                        cameraFrameIsOk = true;
                    }else{
                        if(usbCamera.usbCameraUpdateStatus() == false){
                            Bitmap tfBitmap = toSmall(usbCamera.usbCameraGetFrame());
                            final Collection<Recognition> results = mTensorFlowClassifier.doRecognize(tfBitmap);  //TUDO
                            Iterator<Recognition> it = results.iterator();
                            Recognition first = it.hasNext() ? it.next() : null;
                            //oledControl.drawStrings("图像识别结果： " + first.getTitle() + "（" + engToCh.get(first.getTitle()) + ")" + "(" + first.getConfidence() * 100.0f + ")");
                            String checkStr = first.getTitle() + "（" + engToCh.get(first.getTitle()) + ")" + "(" + first.getConfidence() * 100.0f + ")";
//                            oledControl.drawStrings(checkStr);
                            try {
                                sockService.putStrData(checkStr.getBytes("UTF8"));
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            cameraFrameIsOk = false;
                        }
                    }
                    oledControl.drawEjoin();
                }else{
                    oledControl.drawStrings("等待手机连接:  "+ getNetworkIp());
                }
            }
            TrigCanera.postDelayed(TensorflowRun, 3000);
        }
    };

//    private void toJpeg(byte[] data){
//        Size size = new Size(640, 480);
//        YuvImage image = new YuvImage(data, ImageFormat.YUY2, size.getWidth(), size.getWidth(), null);
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        image.compressToJpeg(new Rect(0,0, size.getWidth(), size.getHeight()),80,outputStream);
//    }

}
