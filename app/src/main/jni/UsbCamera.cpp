//
// Created by admin on 2018/6/29.
//

#include "UsbCamera.h"
#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>

#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <asm/types.h>
#include <linux/videodev2.h>

#include <sys/time.h>
#include <sys/types.h>
#include <unistd.h>



#define LOGIN LOGI("%s inter", __func__);

UsbCamera::UsbCamera(std::string name, int width, int height) {
    LOGIN
    memset(namepath, 0, sizeof(this->namepath));
    sprintf(namepath, "/dev/%s", name.c_str());
    this->width = width;
    this->height = height;
    this->buffer_count = 0;
    this->buffers = NULL;
    this->head.length = 0;
    this->head.start  =NULL;
    this->threadExit = false;
    this->yuvTobmp  = new yuvToBmp(width, height);

    for(int i = 0; i < PICTURE_NUM; i++)
        memset(this->listpath[i], 0, 128);
    this->listindex = 0;

}

int UsbCamera::xioctl(int fd, int request, void *arg) {
    for (int i = 0; i < 100; i++) {
        int r = ioctl(fd, request, arg);
        if (r != -1 || errno != EINTR) return r;
    }
    return -1;
}

int UsbCamera::UsbCameraOpen(void) {
    LOGIN
    if(access(namepath, F_OK) !=0){
        LOGI("/dev/video0 is not axist!!!");
        return -1;
    }

    fd  = open(namepath, O_RDWR | O_NONBLOCK, 0);
    if(fd == -1){
        LOGE("open %s failed", namepath);
        return -1;
    }
    LOGI("open %s Successed, fd = %d", namepath, fd);
    return 0;
}

int UsbCamera::UsbCameraInit() {
    LOGIN
    struct v4l2_capability cap;
    if (xioctl( fd, VIDIOC_QUERYCAP, &cap) == -1)  LOGE("VIDIOC_QUERYCAP");

    if (!(cap.capabilities & V4L2_CAP_VIDEO_CAPTURE))  LOGE("no capture");
    if (!(cap.capabilities & V4L2_CAP_STREAMING))  LOGE("no streaming");

    struct v4l2_cropcap cropcap;
    memset(&cropcap, 0, sizeof cropcap);
    cropcap.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    if (xioctl( fd, VIDIOC_CROPCAP, &cropcap) == 0) {
        struct v4l2_crop crop;
        crop.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
        crop.c = cropcap.defrect;
        if (xioctl( fd, VIDIOC_S_CROP, &crop) == -1) {
            // cropping not supported
        }
    }

    struct v4l2_format format;

    memset(&format, 0, sizeof(struct v4l2_format));
    format.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    ioctl(fd, VIDIOC_G_FMT, &format);
    uint32_t  pixformat = format.fmt.pix.pixelformat;
    char* pfmt = (char*)&pixformat;
    LOGI("VIDIOC_G_FMT: width:%d height:%d\n", format.fmt.pix.width, format.fmt.pix.height);
    LOGI("VIDIOC_G_FMT:pixelformat: %c %c %c %c, field: %d\n", *pfmt++, *pfmt++, *pfmt++, *pfmt, format.fmt.pix.field);


    memset(&format, 0, sizeof(struct v4l2_format));
    format.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    ioctl(fd, VIDIOC_G_FMT, &format);
    LOGI("VIDIOC_G_FMT: width:%d height:%d\n", format.fmt.pix.width, format.fmt.pix.height);
    LOGI("VIDIOC_G_FMT:pixelformat: %c %c %c %c, field: %d\n", *pfmt++, *pfmt++, *pfmt++, *pfmt, format.fmt.pix.field);

    memset(&format, 0, sizeof format);
    format.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    format.fmt.pix.width =  width;
    format.fmt.pix.height =  height;
    format.fmt.pix.pixelformat =  V4L2_PIX_FMT_YUYV;  //V4L2_PIX_FMT_MJPEG
    format.fmt.pix.field =  V4L2_FIELD_NONE;
    if (xioctl( fd, VIDIOC_S_FMT, &format) == -1)  LOGE("VIDIOC_S_FMT");


//    struct v4l2_queryctrl  Setting;
//    memset(&Setting, 0, sizeof(Setting));
//    Setting.id = V4L2_CID_GAIN;
//    if (xioctl( fd, VIDIOC_QUERYCTRL, &Setting) == -1)  LOGE("VIDIOC_QUERYCTRL");
//     LOGI("VIDIOC_QUERYCTRL: minnum:%d, maxnum:%d, step:%d", Setting.minimum, Setting.maximum, Setting.step );
//

    struct v4l2_control ctrl;
    memset(&ctrl, 0, sizeof(ctrl));
    ctrl.id = V4L2_CID_EXPOSURE_AUTO;
    if (xioctl( fd, VIDIOC_G_CTRL, &ctrl) == -1)  LOGE("VIDIOC_G_CTRL");
     LOGI("VIDIOC_G_CTRL: %d", ctrl.value);


    struct v4l2_requestbuffers req;
    memset(&req, 0, sizeof req);
    req.count = 4;
    req.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    req.memory = V4L2_MEMORY_MMAP;
    if (xioctl( fd, VIDIOC_REQBUFS, &req) == -1)  LOGE("VIDIOC_REQBUFS");
     buffer_count = req.count;
     LOGI("mmap buffer count: %d", buffer_count);
     buffers = (buffer_t*)calloc(req.count, sizeof (buffer_t));

    size_t buf_max = 0;
    for (size_t i = 0; i <  buffer_count; i++) {
        struct v4l2_buffer buf;
        memset(&buf, 0, sizeof buf);
        buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
        buf.memory = V4L2_MEMORY_MMAP;
        buf.index = i;
        if (xioctl( fd, VIDIOC_QUERYBUF, &buf) == -1)
             LOGE("VIDIOC_QUERYBUF");
        if (buf.length > buf_max) buf_max = buf.length;
        // LOGI("mmap buffer index: %d", i);
         buffers[i].length = buf.length;
         buffers[i].start =  (char*)mmap(NULL, buf.length, PROT_READ | PROT_WRITE, MAP_SHARED,
                      fd, buf.m.offset);
        if ( buffers[i].start == MAP_FAILED)  LOGE("mmap");
        //LOGI("mmap buffer length: %d", buf.length);
    }
    LOGI("mmap buffer end.");
     head.start = (char * )malloc(buf_max);
    return 0;
}

int UsbCamera::UsbCameraStart(void) {
    LOGIN
    for (size_t i = 0; i < buffer_count; i++) {
        struct v4l2_buffer buf;
        memset(&buf, 0, sizeof buf);
        buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
        buf.memory = V4L2_MEMORY_MMAP;
        buf.index = i;
        if (xioctl(fd, VIDIOC_QBUF, &buf) == -1)  LOGE("VIDIOC_QBUF");
    }

    enum v4l2_buf_type type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    if (xioctl(fd, VIDIOC_STREAMON, &type) == -1)
         LOGE("VIDIOC_STREAMON");
    return 0;
}


int UsbCamera::UsbCameraCapture(void) {
    struct v4l2_buffer buf;
    memset(&buf, 0, sizeof buf);
    buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    buf.memory = V4L2_MEMORY_MMAP;
    if (xioctl( fd, VIDIOC_DQBUF, &buf) == -1) return false;
    memcpy( head.start,  buffers[buf.index].start, buf.bytesused);
     head.length = buf.bytesused;
    if (xioctl( fd, VIDIOC_QBUF, &buf) == -1) return false;
    return false;
}

int UsbCamera::UsbCameraFrame(struct timeval timeout) {
    fd_set fds;
    FD_ZERO(&fds);
    FD_SET( fd, &fds);
    int r = select( fd + 1, &fds, 0, 0, &timeout);
    if (r == -1)  LOGE("select");
    if (r == 0) return false;
    return UsbCameraCapture();
}

static void* threadLoop(void * arg) {
    UsbCamera* camera = (UsbCamera*)arg;
    camera->threadExit = true;
    struct timeval timeout;

    static int frameCount = 0;
    while(camera->threadExit){
        //Wait for get frame.
        timeout.tv_sec = 3;
        timeout.tv_usec = 0;
        camera->UsbCameraFrame(timeout);

        //send data to java.
        if(++frameCount%3 == 0)
            camera->dataCallback(camera->head.start, camera->width* camera->height*2);
        usleep(5000);
    }

    return NULL;
}

int UsbCamera::RegisterUsbCameraCallback(DataCallback callback) {
    this->dataCallback = callback;

    pthread_create(&pthread, NULL, threadLoop, this);

    return 0;
}


int UsbCamera::UsbCameraStop(void) {
    enum v4l2_buf_type type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    if (xioctl(fd, VIDIOC_STREAMOFF, &type) == -1)
        LOGE("VIDIOC_STREAMOFF");
    return 0;
}

void UsbCamera::UsbCameraFinish(void) {
    for (size_t i = 0; i <  buffer_count; i++) {
        munmap( buffers[i].start,  buffers[i].length);
    }
    free( buffers);
    buffer_count = 0;
    buffers = NULL;
    free( head.start);
    head.length = 0;
    head.start = NULL;
}

void UsbCamera::UsbCameraClose(void) {
    if (close( fd) == -1)  LOGE("close");

    if(pthread){
        threadExit = false;
        pthread_join(pthread, NULL);
        pthread_exit(&pthread);
    }
    delete(yuvTobmp);
}





