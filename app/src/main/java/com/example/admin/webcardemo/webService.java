package com.example.admin.webcardemo;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.admin.webcardemo.handler.ControlHandler;
import com.example.admin.webcardemo.handler.DownloadHandler;
import com.example.admin.webcardemo.handler.loginHandler;

import com.example.admin.webcardemo.utils.NetUtil;
import com.yanzhenjie.andserver.AndServer;
import com.yanzhenjie.andserver.RequestHandler;
import com.yanzhenjie.andserver.Server;
import com.yanzhenjie.andserver.filter.HttpCacheFilter;
import com.yanzhenjie.andserver.util.HttpRequestParser;
import com.yanzhenjie.andserver.website.AssetsWebsite;

import org.apache.httpcore.HttpException;
import org.apache.httpcore.HttpRequest;
import org.apache.httpcore.HttpResponse;
import org.apache.httpcore.entity.StringEntity;
import org.apache.httpcore.protocol.HttpContext;
import org.apache.httpcore.util.NetUtils;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.URLDecoder;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;

public class webService extends Service{
    private static final String TAG = webService.class.getSimpleName();
    private Server mServer;

    public void onCreate(){
        // More usage documentation: http://yanzhenjie.github.io/AndServer
        super.onCreate();
        Log.i(TAG,"Create Web Server");
        mServer = AndServer.serverBuilder()
                .inetAddress(NetUtil.getLocalIPAddress()) // Bind IP address.
                .port(8080)
                .timeout(10, TimeUnit.SECONDS)
                .website(new AssetsWebsite(getAssets(), "web"))
                .registerHandler("/login", new loginHandler())
                .registerHandler("/control", new ControlHandler())
                .registerHandler("/download", new DownloadHandler())
                .filter(new HttpCacheFilter())
                .listener(mListener)
                .build();

        mServer.startup();

        Log.i(TAG, "website is runing: " + mServer.isRunning() );
    }

    /* Server listener.
    */
    private Server.ServerListener mListener = new Server.ServerListener() {
        @Override
        public void onStarted() {
            String hostAddress = mServer.getInetAddress().getHostAddress();
            Log.i(TAG, "onStart, hostAddress: " + hostAddress);
        }

        @Override
        public void onStopped() {
            Log.i(TAG, "onStop." );
        }

        @Override
        public void onError(Exception e) {
            Log.i(TAG, "onError." );
        }
    };
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
