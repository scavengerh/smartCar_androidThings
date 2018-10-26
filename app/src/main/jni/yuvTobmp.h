//
// Created by admin on 2018/6/29.
//

#ifndef WEBCARDEMO_YUVTOBMP_H
#define WEBCARDEMO_YUVTOBMP_H

#include <cstdio>

typedef struct                       /**** BMP file header structure ****/
{
    unsigned short bftype;
    unsigned int   bfSize;           /* Size of file */
    unsigned short bfReserved1;      /* Reserved */
    unsigned short bfReserved2;      /* ... */
    unsigned int   bfOffBits;        /* Offset to bitmap data */

} BITMAPFILEHEADER;

typedef struct                       /**** BMP file info structure ****/
{
    unsigned int   biSize;           /* Size of info header */
    int            biWidth;          /* Width of image */
    int            biHeight;         /* Height of image */
    unsigned short biPlanes;         /* Number of color planes */
    unsigned short biBitCount;       /* Number of bits per pixel */
    unsigned int   biCompression;    /* Type of compression to use */
    unsigned int   biSizeImage;      /* Size of image data */
    int            biXPelsPerMeter;  /* X pixels per meter */
    int            biYPelsPerMeter;  /* Y pixels per meter */
    unsigned int   biClrUsed;        /* Number of colors used */
    unsigned int   biClrImportant;   /* Number of important colors */
} BITMAPINFOHEADER;


class yuvToBmp{

public:
    yuvToBmp(int image_width, int image_height);
    ~yuvToBmp();
    int yuv422pToBmp(char* filename, unsigned char* data, int image_width, int image_height);

private:
    int yuv_to_rgb_pixel(int y, int u, int v);
    void SaveBmp(const char *filename, char *data,int width,int height);
    char* rgb  = NULL;
};


#endif //WEBCARDEMO_YUVTOBMP_H
