//
// Created by admin on 2018/6/29.
//

#include "yuvTobmp.h"
#include <stdio.h>
#include <cstdlib>
#include <android/log.h>

int yuvToBmp::yuv_to_rgb_pixel(int y, int u, int v) {
    unsigned int pixel24 = 0;
    unsigned char *pixel = (unsigned char *)&pixel24;
    int r, g, b;
    static long int ruv, guv, buv;

    r = 1.164*(y-16) + 1.159*(v-128);
    g = 1.164*(y-16) - 0.380*(u-128) - 0.813*(v-128);
    b = 1.164*(y-16) + 2.018*(u-128);

    r = r > 255 ? 255 : r;
    g = g > 255 ? 255 : g;
    b = b > 255 ? 255 : b;

    r = r < 0 ? 0 : r;
    g = g < 0 ? 0 : g;
    b = b < 0 ? 0 : b;

    pixel[0] = r;
    pixel[1] = g;
    pixel[2] = b;
    return pixel24;

    return 0;
}

void yuvToBmp::SaveBmp(const char *filename, char *data, int width, int height) {
    BITMAPFILEHEADER bf;
    BITMAPINFOHEADER bi;
    bf.bftype = 0x4d42;
    bf.bfReserved1 = 0;
    bf.bfReserved2 = 0;
    /*其中这两项为何减去2，是因为结构体对齐问题，sizeof(BITMAPFILEHEADER)并不是14，而是16*/
    bf.bfSize = sizeof(BITMAPFILEHEADER) + sizeof(BITMAPINFOHEADER)+width*height*3-2;
    bf.bfOffBits = sizeof(BITMAPFILEHEADER) + sizeof(BITMAPINFOHEADER)-2;//0x36

    bi.biSize = sizeof(BITMAPINFOHEADER);
    bi.biWidth = width;
    bi.biHeight = -height;//height
    bi.biPlanes = 1;
    bi.biBitCount = 24;
    bi.biCompression = 0;
    bi.biSizeImage = width*height*3;
    bi.biXPelsPerMeter = 5000;
    bi.biYPelsPerMeter = 5000;
    bi.biClrUsed = 0;
    bi.biClrImportant = 0;

    FILE *file = fopen(filename, "wb");
    if (!file)
    {
        printf("file open failed\n");
        return;
    }
    /*这里不直接写fwrite(&bf,sizeof(bf),1,file);原因，
      因为结构体对齐原因，其实第一个bf.bftype并不是占用两个字节，而是4个字节，sizeof
      就是16了，显然不对，bmp固定文件头为14个字节。故按如下方式处理。
    */
    fwrite(&bf.bftype,2,1,file);
    fwrite((&bf.bftype)+2,12,1,file);
    fwrite(&bi,sizeof(bi),1, file);
    fwrite(data,width*height*3,1,file);
    fclose(file);
}

int
yuvToBmp::yuv422pToBmp(char *filename, unsigned char *data, int image_width, int image_height) {
    unsigned int i, j, out;

    unsigned char* py, *pu, *pv;
//    __android_log_print(ANDROID_LOG_INFO, "yuvToBmp", "image_width: %d, image_height:%d", image_width, image_height);
    int y0, u, y1, v;
    unsigned int pixel24;
    unsigned char *pixel = (unsigned char *)&pixel24;
    unsigned int size = image_width*image_height*2;

#if 0
    py = data;
    pu = py + image_height * image_width;
    pv = pu + (image_height * image_width)/2;
#else  //yuyv打包格式
    py = data;
    pu = data + 1;
    pv = data + 3;
#endif
    out = 0;
    for(i = 0; i < image_height; i++){
        for(j = 0; j < image_width/2; j++){
            /*特殊说明，因bmp文件是以bgr顺序排列而不是rgb，故此u，v颠倒*/
            y0 = *py;
            v = *pu;
            py+=2;
            y1 = *py;
            u = *pv;

            pixel24 = yuv_to_rgb_pixel(y0, u, v);
            rgb[out+0] = pixel[0];
            rgb[out+1] = pixel[1];
            rgb[out+2] = pixel[2];

            pixel24 = yuv_to_rgb_pixel(y1, u, v);
            rgb[out+3] = pixel[0];
            rgb[out+4] = pixel[1];
            rgb[out+5] = pixel[2];

            py += 2;
            pu += 4;
            pv += 4;
            out += 6;
        }
    }
    SaveBmp(filename, rgb, image_width, image_height);
    return 0;

    return 0;
}

yuvToBmp::yuvToBmp(int image_width, int image_height) {
    rgb = (char*)malloc(image_width*image_height*3);
    if(rgb == NULL){
         __android_log_print(ANDROID_LOG_INFO, "yuvToBmp", "malloc translate rgb space failed");
    }
}

yuvToBmp::~yuvToBmp() {
    free(rgb);
}
