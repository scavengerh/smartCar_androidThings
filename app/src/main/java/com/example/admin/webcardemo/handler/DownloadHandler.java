package com.example.admin.webcardemo.handler;

import android.util.Log;

import com.yanzhenjie.andserver.RequestHandler;
import com.yanzhenjie.andserver.RequestMethod;
import com.yanzhenjie.andserver.annotation.RequestMapping;

import org.apache.httpcore.HttpException;
import org.apache.httpcore.HttpRequest;
import org.apache.httpcore.HttpResponse;
import org.apache.httpcore.entity.StringEntity;
import org.apache.httpcore.protocol.HttpContext;

import java.io.IOException;


public class DownloadHandler implements RequestHandler {
    String TAG = DownloadHandler.class.getSimpleName();

    @RequestMapping(method = {RequestMethod.GET})
    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
        Log.i(TAG, "Downloadhandler inter...");
        StringEntity stringEntity = new StringEntity("Please enter your account number and password.", "utf-8");

        //Content-Disposition: attachment; filename=aaa.zip --告诉浏览器以下载方式打开资源
   //     String attachment = "attachment;filename=" + UsbCameraGetPictrue();
        response.setHeader("Content-Disposition", "attachment;filename=AndServer.txt");
        response.setStatusCode(200);
        response.setEntity(stringEntity);
    }
}
