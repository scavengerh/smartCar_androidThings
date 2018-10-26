//
// Created by admin on 2018/6/29.
//

#include <jni.h>
#include <string>
#include <android/log.h>
#include "UsbCamera.h"


UsbCamera* camera;

JavaVM* jvm = NULL;
jobject mjobject =NULL;
unsigned char* pbuffer = NULL;

void datahandler(char* buff, int length) {

        memcpy(pbuffer, buff, length);

        JNIEnv *jnienv = NULL;
        jvm->AttachCurrentThread(&jnienv, NULL);

        //1,获取到java class
        jclass mclass = jnienv->GetObjectClass(mjobject);

        //2，获取java方法的ID
        jmethodID jmethodID1 = jnienv->GetStaticMethodID(mclass, "usbCameraDataHandle", "()V");

        // jnienv->CallVoidMethod(mclass, jmethodID1);
        jnienv->CallStaticVoidMethod(mclass, jmethodID1);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_admin_webcardemo_UsbCamera_UsbCameraInit(JNIEnv *env, jobject instance,jint width, jint height) {

    camera = new UsbCamera("video0", width, height);
    if(camera->UsbCameraOpen() < 0){
        return -1;
    }

    if(camera->UsbCameraInit() < 0){
        return -1;
    }

    env->GetJavaVM(&jvm);
    mjobject = env->NewGlobalRef(instance);

    return 0;
}extern "C"
JNIEXPORT jint JNICALL
Java_com_example_admin_webcardemo_UsbCamera_UsbCameraStart(JNIEnv *env, jobject instance) {

    if(camera->UsbCameraStart() < 0){
        return  -1;
    }

    return 0;
}extern "C"
JNIEXPORT jint JNICALL
Java_com_example_admin_webcardemo_UsbCamera_UsbCameraStop(JNIEnv *env, jobject instance) {

    if(camera->UsbCameraStop()< 0){
        return  -1;
    }

    camera->UsbCameraFinish();
    return 0;
}extern "C"
JNIEXPORT jint JNICALL
Java_com_example_admin_webcardemo_UsbCamera_UsbCameraClose(JNIEnv *env, jobject instance) {

    camera->UsbCameraClose();

    delete(camera);
    return 0;
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_admin_webcardemo_UsbCamera_UsbCameraRegisterCallback(JNIEnv *env,
                                                                         jobject instance) {
    // TODO
    camera->RegisterUsbCameraCallback(datahandler);
  //  LOGI("%s register callback end", __func__);

}extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_admin_webcardemo_UsbCamera_UsbCameraGetPictrue(JNIEnv *env, jobject instance) {

    // TODO

    char strbuf[128]  ={0};
    if(camera->listindex != 0){
        strcpy(strbuf, camera->listpath[(camera->listindex) - 1]);
    }else{
        strcpy(strbuf, camera->listpath[PICTURE_NUM - 1]);
    }

    return env->NewStringUTF(strbuf);
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_admin_webcardemo_UsbCamera_UsbCameraSetDirectBuffer(JNIEnv *env, jobject instance,
                                                                     jobject buffer, jint length) {

    // TODO
    pbuffer = (unsigned char*)env->GetDirectBufferAddress(buffer);
    if(pbuffer){
        LOGI("HAL, set direct buffer successed");
    }

}
