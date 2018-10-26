package com.example.admin.webcardemo;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Handler;
import android.util.Log;

import com.example.admin.webcardemo.utils.Font16;
import com.google.android.things.contrib.driver.ssd1306.BitmapHelper;
import com.google.android.things.contrib.driver.ssd1306.Ssd1306;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;



public class OledControl{

    private static final String TAG = "OledScreenActivity";
    private static final int SCREEN_WIDTH = 128;
    private static final int SCREEN_HEIGHT = 64;
    private  static final  String StartGif = "startMode.gif";

    private Ssd1306 mScreen;

    private Bitmap mBitmap;
    PeripheralManager piomanager;
    private Resources resources;

    private Font16 font;
    private String hostIp;

    private static String pinreset = "BCM26";
    public static Gpio pinReset;



    protected void init(Resources resources, String Ip) {

        this.resources = resources;
        this.hostIp = Ip;
        piomanager = PeripheralManager.getInstance();
        font = new Font16(resources);

        Log.i(TAG, "hostIp: "+ this.hostIp);
        try {
            /* Control reset*/
            pinReset = piomanager.openGpio(pinreset);
            pinReset.setDirection(1);
            pinReset.setValue(false);
            Thread.sleep(1000);
            pinReset.setValue(true);
            Log.i(TAG, "OLED reset:" + pinreset);

            /**Control I2c interface */
            List<String> listI2c = piomanager.getI2cBusList();
            for(int i = 0; i < listI2c.size();i++)
                Log.i(TAG, "listI2C: " + i + " , " + listI2c.get(i));
            mScreen = new Ssd1306(listI2c.get(0));
        } catch (IOException e) {
            Log.e(TAG, "Error while opening screen", e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "OLED screen activity created");
    }
    
    public void Deinit() {
        // remove pending runnable from the handler
        // Close the device.
        try {
            mScreen.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing SSD1306", e);
        } finally {
            mScreen = null;
        }
    }

    public void drawStrings(String str){
        int xOffset = 0;
        int yOffset = 0;
        int koffset = 0;
        String dispStr = "Ip:"+ this.hostIp + str;
        boolean[] strindex = new boolean[dispStr.length()];
        boolean[][][] arr = font.drawString(dispStr, strindex);
        if(mScreen  == null)
            return;
        mScreen.clearPixels();
            for (int k = 0; k < dispStr.length(); k++) {
                for (int i = 0; i < arr[k].length; i++) {
                    for (int j = 0; j < arr[k][i].length; j++) { // %8表示一行显示8个汉字，
                        mScreen.setPixel(i  + xOffset , j + yOffset, arr[k][j][i]);
                    }
                }
                if(strindex[k] == false){
                    xOffset += 8;
                }else{
                    xOffset += 16;
                }
                if(xOffset > (128 - 16)) {
                    xOffset = 0;
                    yOffset += 16;
                }
            }
        try {
            mScreen.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void drawlogoGif(){
        InputStream ins = null;
        try {
            ins = resources.getAssets().open(StartGif);
        }catch (IOException e) {
            e.printStackTrace();
        }

        //ins = this.getAssets().open(StartGif);
        if(ins != null) {
            gifDecoder gifDecoder = new gifDecoder();
            gifDecoder.read(ins, 7723650); //data is a byte array
            final int frameCount = gifDecoder.getFrameCount();
            Bitmap bitmap;
            for (int i = 0; i < frameCount; i++) {
                gifDecoder.advance();
                bitmap = gifDecoder.getNextFrame();
                try {
                    drawBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void drawEjoin(){
        int i;
        int[] id = new int[8];
        id[0] = R.drawable.eye_f;
        id[1] = R.drawable.eye_f_2;
        id[2] = R.drawable.eye_f_3;
        id[3] = R.drawable.eye_f_4;
        id[4] = R.drawable.eye_f_5;
        id[5] = R.drawable.eye_f_6;
        id[6] = R.drawable.eye_f_7;
        id[7] = R.drawable.eye_f_8;
        for(i = 0; i < 15; i+=2){
            Bitmap bmp;
            if(i < 8)
                bmp = BitmapFactory.decodeResource(resources, id[i]);
            else
                bmp = BitmapFactory.decodeResource(resources, id[14 - i]);
            try {
                drawBitmap(bmp);
//                    Thread.currentThread();
//                    Thread.sleep(8, 0);
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }


    private static Bitmap toSmall(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postScale((float) SCREEN_WIDTH/bitmap.getWidth(),(float) SCREEN_HEIGHT/bitmap.getHeight()); //长和宽放大缩小的比例
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
        return resizeBmp;
    }
    /**
     * Draws a BMP in one of three positions.
     */
    public void drawBitmapUsePath(String pathname) throws IOException {
        FileInputStream fis = new FileInputStream(pathname);
        mScreen.clearPixels();

            BitmapFactory.Options bfoOptions = new BitmapFactory.Options();
            bfoOptions.inScaled = false;
            mBitmap = BitmapFactory.decodeStream(fis);

        mBitmap = toSmall(mBitmap);
      //  Log.i(TAG, "resize after width: "+ mBitmap.getWidth() + ", height: " + mBitmap.getHeight());
        BitmapHelper.setBmpData(mScreen, 0, 0, mBitmap, false);
        mScreen.show();
    }

    /**
     * Draws a BMP in one of three positions.
     */
    public void drawBitmap(Bitmap bitmap) throws IOException {;
        mScreen.clearPixels();
        mBitmap = toSmall(bitmap);
      //  Log.i(TAG, "resize after width: "+ mBitmap.getWidth() + ", height: " + mBitmap.getHeight());
        //handle need 30ms
        BitmapHelper.setBmpData(mScreen, 0, 0, mBitmap, false);
        //handle100ms
        mScreen.show();
    }
}
