package com.example.admin.webcardemo.handler;

import android.util.Log;

import com.example.admin.webcardemo.MainActivity;
import com.example.admin.webcardemo.MotorControl;
import com.yanzhenjie.andserver.RequestHandler;
import com.yanzhenjie.andserver.RequestMethod;
import com.yanzhenjie.andserver.SimpleRequestHandler;
import com.yanzhenjie.andserver.annotation.RequestMapping;
import com.yanzhenjie.andserver.util.HttpRequestParser;

import org.apache.httpcore.HttpException;
import org.apache.httpcore.HttpRequest;
import org.apache.httpcore.HttpResponse;
import org.apache.httpcore.entity.StringEntity;
import org.apache.httpcore.protocol.HttpContext;

import java.io.IOException;
import java.util.Map;

public class ControlHandler implements RequestHandler {

    private final String TAG = ControlHandler.class.getSimpleName();
    @RequestMapping(method = {RequestMethod.GET})
    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
        Map<String, String> params = HttpRequestParser.parseParams(request);
        MotorControl motorControl = MotorControl.getInstance();
        //Log.i(TAG, "params : "+ params.toString());
     //   Log.i(TAG,"contaimkey: " + params.get("control"));
        String cmd = params.get("control");
        if(cmd.equals("up")){
            motorControl.controlCarMotorCmd(MotorControl.CMD_FORWARD);
        }else if(cmd.equals("left")){
            motorControl.controlCarMotorCmd(MotorControl.CMD_TURNLEGT);
        }else if(cmd.equals("right")){
            motorControl.controlCarMotorCmd(MotorControl.CMD_TURNRIGHT);
        }else if(cmd.equals("down")){
            motorControl.controlCarMotorCmd(MotorControl.CMD_BACKOFF);
        }else if(cmd.equals("stop")){
            motorControl.controlCarMotorCmd(MotorControl.CMD_STOP);
        }

      //  String utrl = "0.1;url=http://" + MainActivity.getHostIP() + ":8080/control.html";
      //  response.setHeader("refresh", utrl);
        response.setHeader("refresh", "https://...");
    }
}
