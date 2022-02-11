/************************************************************************
**
** NAME:        gameoflife.c
**
** DESCRIPTION: CS61C Fall 2020 Project 1
**
** AUTHOR:      Justin Yokota - Starter Code
**				YOUR NAME HERE
**
**
** DATE:        2020-08-23
**
**************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <inttypes.h>
#include "imageloader.h"

//Determines what color the cell at the given row/col should be. This function allocates space for a new Color.
//Note that you will need to read the eight neighbors of the cell in question. The grid "wraps", so we treat the top row as adjacent to the bottom row
//and the left column as adjacent to the right column.
// row and col start from zero index.


//Determines what color the cell at the given row/col should be. This should not affect Image, and should allocate space for a new Color.
int wrap( int index, uint32_t total_length)
{
   
    if (index < 0) {
        index = (index + total_length) % total_length;
    }
    else if (index > total_length -1) {
        index = index % total_length;
    }
    return index;
 
}

void set_bit (uint8_t * color, int pos, int v){
    *color = (*color & (~ (1<<pos)))| (v<<pos);
}

Color *evaluateOneCell(Image *image, int row, int col, uint32_t rule)
{
    Color** imageArray = image->image;
    uint32_t image_col = image->cols;
    uint32_t image_row = image->rows;
  
    
    struct Color * new_pixel = (Color*) malloc(sizeof(struct Color));
       if (new_pixel == NULL) {
           return NULL;
          
       }
    uint8_t data[9][3];
    for (int i = row - 1; i <= row +1; i ++) {
        for (int j = col - 1; j<= col +1; j++){
            int index = (i - row + 1 ) * 3 + (j - col + 1);
            int wrap_row = wrap(i, image_row);
            int wrap_col = wrap(j, image_col);
            struct Color * pixel = imageArray [wrap_row*image_col + wrap_col];

            data[index][0] = pixel->R;
            data[index][1] = pixel->G;
            data[index][2] = pixel->B;
            
            
            

        }
      
       
    }

    int current_state_pos = 4;
    for (int color_index = 0; color_index <=2 ; color_index++){
        for (int pos = 0; pos < 8; pos++) {
            int num_live = 0;
            int current_state = 0;
            
            for (int m = 0; m < 9; m ++){  //iterate through 9 cells
                if (m == current_state_pos) {
                    current_state +=(data[m][color_index]>>pos)%2;
                    
                } else{
                    num_live += (data[m][color_index]>>pos)%2;
    
                }
            }
            if (current_state == 1){  //current is alive
                uint32_t alive_rule = rule >> 9;
                if ((alive_rule>> num_live) % 2 == 1){ //next state is alive
                    set_bit( &(data[current_state_pos][color_index]), pos, 1);
                    
                } else{
                    set_bit(&data[current_state_pos][color_index], pos, 0);
                }
                
            } else{  //current is dead
                if ((rule>>num_live) %2 == 1){ //next state is alive
                    set_bit(&data[current_state_pos][color_index], pos,1);
                
                } else{
                     set_bit(& data[current_state_pos][color_index],pos,0);
                }
            }
        }
    }
    new_pixel->R = data[current_state_pos][0];
    new_pixel->G = data[current_state_pos][1];
    new_pixel->B = data[current_state_pos][2];
    
    return new_pixel;
}

//The main body of Life; given an image and a rule, computes one iteration of the Game of Life.
//You should be able to copy most of this from steganography.c
Image *life(Image *image, uint32_t rule)
{
    //Color** imageArray = image->image;
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
    for (int i = 0; i< row; i++){
        for (int j = 0; j < col; j++){
            struct Color * new_pixel = evaluateOneCell(image, i, j, rule);
            if (new_pixel == NULL){
                int k = i*col + j - 1;
                while (k >= 0){
                    free(new_image->image[k]);
                    k--;
                }
                free(new_image->image);
                free(new_image);
                exit(-1);
                
            }
            new_image->image[i*col + j] = new_pixel;
        }
    }
    return new_image;
    
    
	
}

/*
Loads a .ppm from a file, computes the next iteration of the game of life, then prints to stdout the new image.

argc stores the number of arguments.
argv stores a list of arguments. Here is the expected input:
argv[0] will store the name of the program (this happens automatically).
argv[1] should contain a filename, containing a .ppm.
argv[2] should contain a hexadecimal number (such as 0x1808). Note that this will be a string.
You may find the function strtol useful for this conversion.
If the input is not correct, a malloc fails, or any other error occurs, you should exit with code -1.
Otherwise, you should return from main with code 0.
Make sure to free all memory before returning!

You may find it useful to copy the code from steganography.c, to start.
*/
int main(int argc, char **argv)
{
    if (argc != 3){
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
    if (argv [2] == NULL) {
    exit(-1);
    }
    
    
    Image * image = readData(argv[1]);
    uint32_t rule = strtol(argv[2], NULL, 16);
    Image * new_image = life(image,rule);
    writeData(new_image);
    
    freeImage(image);
    
    freeImage(new_image);
    return 0;
    
}
