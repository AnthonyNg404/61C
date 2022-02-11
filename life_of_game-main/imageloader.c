/************************************************************************
**
** NAME:        imageloader.c
**
** DESCRIPTION: CS61C Fall 2020 Project 1
**
** AUTHOR:      Dan Garcia  -  University of California at Berkeley
**              Copyright (C) Dan Garcia, 2020. All rights reserved.
**              Justin Yokota - Starter Code
**				Xinyu Fu
**
**
** DATE:        2020-08-15
**
**************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <inttypes.h>
#include <string.h>
#include "imageloader.h"


//Opens a .ppm P3 image file, and constructs an Image object. 
//You may find the function fscanf useful.
//Make sure that you close the file with fclose before returning.
Image *readData(char *filename) 
{
    FILE *fp = fopen(filename,"r");
    if (!fp) {
        exit(-1);
    }
    char type[20];
    int row, col, scale;
    fscanf(fp, " %s ", type);
    fscanf(fp, " %d %d ", &col, &row);
    fscanf(fp, " %d ", &scale);
    uint8_t r, g, b;
    int i = 0;
    struct Image * whole_image = (Image*) malloc (sizeof(struct Image));
    if (whole_image == NULL) {
        exit(-1);
        
        
    }
    whole_image->rows = row;
    whole_image->cols = col;
    
        
    //struct Color ** imageArray = whole_image->image;
    
    whole_image->image = (Color **) malloc(col*row* sizeof(Color*)); //size of image
    if (whole_image->image == NULL){
        free(whole_image);
        exit(-1);
    }
    
    while(fscanf(fp, " %hhu %hhu %hhu ", &r, &g, &b)!=EOF) {
        struct Color * color = (Color*) malloc(sizeof(struct Color));
        
        if (color == NULL){
            int j = i - 1;
            while (j >= 0) {
                free(whole_image->image[j]);
                j--;
                
            }
    
            
            free(whole_image->image);
            free(whole_image);
            exit(-1);
            
        }
        
        color->R = r;
        color->G = g;
        color->B = b;
        whole_image->image[i] = color;
        i++;
    }
    fclose(fp);
    return whole_image;
    
}



//Given an image, prints to stdout (e.g. with printf) a .ppm P3 file with the image's data.
void writeData(Image *image)
{
    //struct Color ** imageArray = image->image;
    uint32_t col = image->cols;
    uint32_t row = image->rows;
    printf("P3\n");
    printf("%d %d\n", col, row);
    printf("255\n");
    uint32_t total = col*row;
    int i = 0;
    int j = 1;
    while (i< total){
        
        Color * color = image->image[i];
        uint8_t r = color -> R;
        uint8_t g = color -> G;
        uint8_t b = color -> B;
        if (j % col == 0) {
            printf("%3hhu ", r);
            printf("%3hhu ", g);
            printf("%3hhu", b);
            printf("\n");
        } else {
            printf("%3hhu ", r);
            printf("%3hhu ", g);
            printf("%3hhu   ", b);
        }
        j++;
        i++;
    }
    
}


//Frees an image
void freeImage(Image *image)
{
   Color ** imageArray = image->image;
    uint32_t col = image->cols;
    uint32_t row = image->rows;
    uint32_t total = col*row;
    int i = 0;
    while (i< total){
        //Color * color = image->image[i];
        Color * current = imageArray[i];
        free(current);
        i++;
    }
    free(image->image);
    free(image);
    
    
    
	
}
