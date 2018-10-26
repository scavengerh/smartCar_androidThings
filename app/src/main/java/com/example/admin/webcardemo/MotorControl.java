package com.example.admin.webcardemo;

import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.Pwm;

import java.io.IOException;
import java.util.List;

public class MotorControl {
    String TAG = MotorControl.class.getSimpleName();
    public static final int CMD_FORWARD = 100;
    public static final int CMD_BACKOFF = 101;
    public static final int CMD_TURNLEGT = 102;
    public static final int CMD_TURNRIGHT = 103;
    public static final int CMD_STOP = 104;

    private static String leftup = "BCM12";
    private static String leftdown = "BCM16";
    private static String rightup = "BCM20";
    private static String rightdown = "BCM21";
    public static Gpio pinLeft1;
    public static Gpio pinLeft2;
    public static Gpio pinRight1;
    public static Gpio pinRight2;
    private static Pwm pwmLevel1;
    private static Pwm pwmLevel2;

    private boolean isRun = false;
    private boolean motorConlFlags = false;
    private Double angle;
    private int oldMotorCmd = -1;
    private double pwm1Value, pwm1AdjustValue;
    private double pwm2Value, pwm2AdjustValue;

    PeripheralManager piomanager;
    private static MotorControl motorControl = null;

    public static MotorControl getInstance() {
        if (motorControl == null) {
            motorControl = new MotorControl();
        }

        return motorControl;
    }

    public int Init() {
        piomanager = PeripheralManager.getInstance();



        List<String> piolist = piomanager.getGpioList();
        List<String> pwdlist = piomanager.getPwmList();
        for (int i = 0; i < pwdlist.size(); i++)
            Log.i(TAG, "pwm list:" + i + ", " + pwdlist.get(i));

        try {
            pinLeft1 = piomanager.openGpio(leftup);
            pinLeft2 = piomanager.openGpio(leftdown);
            pinRight1 = piomanager.openGpio(rightup);
            pinRight2 = piomanager.openGpio(rightdown);
            pwmLevel1 = piomanager.openPwm("PWM0");
            pwmLevel2 = piomanager.openPwm("PWM1");
        } catch (IOException e) {
            e.printStackTrace();
        }

        pwm1Value = 0;
        pwm2Value = 0;
        controlSevroMotorCmd(1, 0);
        controlSevroMotorCmd(2, 0);
        try {
            pinLeft1.setDirection(1);
            pinLeft1.setValue(false);
            pinLeft2.setDirection(1);
            pinLeft2.setValue(false);
            pinRight1.setDirection(1);
            pinRight1.setValue(false);
            pinRight2.setDirection(1);
            pinRight2.setValue(false);

            pwmLevel1.setEnabled(false);
            pwmLevel2.setEnabled(false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        isRun = true;
        new Thread(new MotorRun()).start();
        return 0;
    }

    public void controlCarMotorCmd(int cmd) {
        if(oldMotorCmd != cmd) {
            switch (cmd) {
                case CMD_FORWARD:
                    try {
                        pinLeft1.setValue(false);
                        pinLeft2.setValue(true);
                        pinRight1.setValue(false);
                        pinRight2.setValue(true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case CMD_BACKOFF:
                    try {
                        pinLeft1.setValue(true);
                        pinLeft2.setValue(false);
                        pinRight1.setValue(true);
                        pinRight2.setValue(false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case CMD_TURNLEGT:
                    try {
                        pinLeft1.setValue(true);
                        pinLeft2.setValue(false);
                        pinRight1.setValue(false);
                        pinRight2.setValue(true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case CMD_TURNRIGHT:
                    try {
                        pinLeft1.setValue(false);
                        pinLeft2.setValue(true);
                        pinRight1.setValue(true);
                        pinRight2.setValue(false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case CMD_STOP:
                    try {
                        pinLeft1.setValue(false);
                        pinLeft2.setValue(false);
                        pinRight1.setValue(false);
                        pinRight2.setValue(false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }
        oldMotorCmd = cmd;
    }

    /**
     *  2.舵机的控制需要MCU产生一个20ms的脉冲信号，以0.5ms到2.5ms的高电平来控制舵机的角度
     *  3.数据:
     *
     *             0.5ms-------------0度；   2.5% 
     *             1.0ms------------45度；   5.0% 
     *             1.5ms------------90度；        7.5%
     *             2.0ms-----------135度；  10.0%
     *             2.5ms-----------180度；  12.5%
     */


    public void controlSevroMotorCmd(int index, double pwm) {
        Log.i(TAG,"PWM: " + index + ", pwm perriod: "+ pwm);
        double pwm1,pwm2;
        pwm1 = pwm + pwm1Value;
        pwm2 = pwm + pwm2Value;
        switch (index) {
            case 1:
                try {
                    if(pwm1 > 90)
                        pwm1 = 90;
                    if(pwm1 < 0)
                        pwm1 = 0;
                    pwm1AdjustValue = pwm1;
                    pwm1 = 2.5 + pwm1*0.1;
                    pwmLevel1.setPwmDutyCycle(pwm1);
                    pwmLevel1.setPwmFrequencyHz(50);
                    pwmLevel1.setEnabled(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 2:
                try {
                    if(pwm2 > 90)
                        pwm2 = 90;
                    if(pwm2 < 0)
                        pwm2 = 0;
                    pwm2AdjustValue = pwm2;
                    pwm2 = 2.5 + pwm2*0.1;
                    pwmLevel2.setPwmDutyCycle(pwm2);
                    pwmLevel2.setPwmFrequencyHz(50);
                    pwmLevel2.setEnabled(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 3:
                pwm1Value = pwm1AdjustValue;
                pwm2Value = pwm2AdjustValue;
                try {
                    pwmLevel1.setEnabled(false);
                    pwmLevel2.setEnabled(false);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                break;
            default:
                break;
        }

    }

    void close() {
        try {
            pinLeft1.close();
            pinLeft2.close();
            pinRight1.close();
            pinRight2.close();
            pwmLevel1.close();
            pwmLevel2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void motorContrlAngle(double angle, boolean enable){
        if(enable){
            /*
            *   angle 是顺时针0 - 360度
            * */
            this.angle = angle;
            motorConlFlags = true;
            Log.i(TAG, "motorContrlAngle: get angle: "+ this.angle);
        }else {
            Log.i(TAG, "motorContrlAngle: Stop motor: ");
            motorConlFlags = false;
        }
    }

    private class MotorRun implements Runnable{
        private int motorIndex;
        private int servoIndex;
        private int count;
        private int stepCount = 100;
        @Override
        public void run() {
            motorIndex = 0;
            servoIndex = 0;
            count = 0;
            while (isRun){
                if(motorConlFlags){
                    if(angle >=270 && angle < 360){
                        motorIndex = (int) Math.abs(360 - angle)/10;
                        if(++count > motorIndex){
                            controlCarMotorCmd(CMD_FORWARD);
                        }else{
                            controlCarMotorCmd(CMD_TURNLEGT);
                        }
                    }

                    if(angle >= 90 && angle < 180){
                        motorIndex = (int) Math.abs(180 - angle)/10;
                        if(++count > motorIndex){
                            controlCarMotorCmd(CMD_BACKOFF);
                        }else{
                            controlCarMotorCmd(CMD_TURNLEGT);
                        }
                    }

                    if(angle >= 0 && angle < 90){
                        motorIndex = (int) Math.abs(angle)/10;
                        if(++count > motorIndex){
                            controlCarMotorCmd(CMD_FORWARD);
                        }else{
                            controlCarMotorCmd(CMD_TURNRIGHT);
                        }
                    }

                    if(angle >=180 && angle < 270){
                        motorIndex = (int) Math.abs(angle-180)/10;
                        if(++count > motorIndex){
                            controlCarMotorCmd(CMD_BACKOFF);
                        }else{
                            controlCarMotorCmd(CMD_TURNRIGHT);
                        }
                    }
                }else{
                    controlCarMotorCmd(CMD_STOP);
                    count = 0;
                }

                try {
                    Thread.sleep(stepCount);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
