//
// Created by admin on 2018/6/29.
//

#ifndef WEBCARDEMO_USBCAMERA_H
#define WEBCARDEMO_USBCAMERA_H

#include <sys/types.h>
#include <string>
#include <pthread.h>
#include <android/log.h>
#include "yuvTobmp.h"
#include <iostream>

#define  LOG_TAG    "UsbCamera"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)


typedef  void (*DataCallback)(char* buff, int length);
typedef struct {
    char* start;
    size_t length;
} buffer_t;
#define  PICTURE_NUM 20
class UsbCamera {
public:
    UsbCamera(std::string name, int width, int height);

    int UsbCameraOpen(void);
    int UsbCameraInit(void);
    int UsbCameraStart(void);
    int UsbCameraCapture(void);
    int UsbCameraFrame(struct timeval timeout);
    int UsbCameraStop(void);
    void UsbCameraFinish(void);
    void UsbCameraClose(void);

    int RegisterUsbCameraCallback(DataCallback callback);

    bool threadExit;
    buffer_t head;
    DataCallback  dataCallback;
    int width;
    int height;
    yuvToBmp *yuvTobmp;
    char savePath[128];
    char listpath[PICTURE_NUM][128];
    int listindex;

private:
    int xioctl(int fd, int request, void* arg);

    pthread_t pthread;
    int fd;
    size_t buffer_count;
    buffer_t* buffers;
    char namepath[128];
};

#endif //WEBCARDEMO_USBCAMERA_H
