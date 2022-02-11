/************************************************************************
**
** NAME:        steganography.c
**
** DESCRIPTION: CS61C Fall 2020 Project 1
**
** AUTHOR:      Dan Garcia  -  University of California at Berkeley
**              Copyright (C) Dan Garcia, 2020. All rights reserved.
**				Justin Yokota - Starter Code
**				Xinyu Fu
**
** DATE:        2020-08-23
**
**************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <inttypes.h>
#include "imageloader.h"

int wrapUp (int index, int total_length){
    return (index + total_length) % total_length;
    
}
//Determines what color the cell at the given row/col should be. This should not affect Image, and should allocate space for a new Color.
Color *evaluateOnePixel(Image *image, int row, int col)
{
    uint32_t image_row = image->rows;
    uint32_t image_col = image->cols;
    
    if (row < 0) {
        row = wrapUp (row, image_row);
    }
    else if (row > image_row -1) {
        row = row % image_row;
    }

    if (col < 0) {
        col = wrapUp (col, image_col);
    }
    else if (col > image_col -1) {
        col = col % image_col;
    }
    
    Color ** imageArray = image->image;
    struct Color * color = (Color*) malloc(sizeof(struct Color));
    if (color == NULL) {
        exit(-1);
    }
    
    
    int pos = row * image_row + col;
    color = imageArray [pos];
    return color;
    
    
	//YOUR CODE HERE
}



//Given an image, creates a new image extracting the LSB of the B channel.
Image *steganography(Image *image)
{
    Color** imageArray = image->image;
    uint32_t row = image->rows;
    uint32_t col = image->cols;
    
    struct Image * new_image = (Image*) malloc (sizeof(struct Image));
    if (new_image == NULL){
        exit(-1);
    }
    new_image->cols = col;
    new_image->rows = row;
    new_image->image = (Color **) malloc(col*row* sizeof(Color*));
    if (new_image->image== NULL) {
        free(new_image);
        exit(-1);
    }
    
    int i = 0;
    int total = col* row;
    while (i < total){
        struct Color * new_color = (Color*) malloc(sizeof(struct Color));
        if (new_color == NULL) {
            free(new_image->image);
            free(image);
            exit(-1);
        }
        Color * oldPixel = imageArray[i];
        if (oldPixel->B %2 ==0) {
            new_color->R = 0;
            new_color->G = 0;
            new_color->B = 0;
            
            new_image->image[i] = new_color;
            
        } else{
            new_color->R = 255;
            new_color->G = 255;
            new_color->B = 255;
            
            new_image->image[i] = new_color;
            
        }
        i++;
    }
    return new_image;
	
}

/*
Loads a .ppm from a file, and prints to stdout (e.g. with printf) a new image, 
where each pixel is black if the LSB of the B channel is 0, 
and white if the LSB of the B channel is 1.

argc stores the number of arguments.
argv stores a list of arguments. Here is the expected input:
argv[0] will store the name of the program (this happens automatically).
argv[1] should contain a filename, containing a .ppm.
If the input is not correct, a malloc fails, or any other error occurs, you should exit with code -1.
Otherwise, you should return from main with code 0.
Make sure to free all memory before returning!
*/
int main(int argc, char **argv)
{
    if (argc != 2 ){
        exit(-1);
        
    }
    if (argv == NULL) {
        exit(-1);
        
    }
    if ( argv[0] == NULL) {
        exit(-1);
    }
    if (argv [1] == NULL) {
        exit(-1);
    }
    Image * image = readData(argv[1]);
    Image * new_image = steganography(image);
    writeData(new_image);
    
    freeImage(image);
    
    freeImage(new_image);
    return 0;
    
    
}
