package com.example.admin.webcardemo.handler;

import android.util.Log;

import com.yanzhenjie.andserver.RequestHandler;
import com.yanzhenjie.andserver.util.HttpRequestParser;

import org.apache.httpcore.HttpException;
import org.apache.httpcore.HttpRequest;
import org.apache.httpcore.HttpResponse;
import org.apache.httpcore.entity.StringEntity;
import org.apache.httpcore.protocol.HttpContext;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Map;

public class loginHandler implements RequestHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
        Map<String, String> params = HttpRequestParser.parseParams(request);

        if (!params.containsKey("username") || !params.containsKey("password")) {
            StringEntity stringEntity = new StringEntity("Please enter your account number and password.", "utf-8");

            response.setStatusCode(400);
            response.setEntity(stringEntity);
            return;
        }
        Log.d("loginHandler", "Inter ...");
        String userName = URLDecoder.decode(params.get("username"), "utf-8");
        String password = URLDecoder.decode(params.get("password"), "utf-8");

        if ("123".equals(userName) && "123".equals(password)) {
            StringEntity stringEntity = new StringEntity("Login Succeed", "utf-8");

            response.setStatusCode(200);
            response.setEntity(stringEntity);
        } else {
            StringEntity stringEntity = new StringEntity("Login Failed", "utf-8");

            response.setStatusCode(400);
            response.setEntity(stringEntity);
        }
    }
}

