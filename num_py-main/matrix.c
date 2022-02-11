#include "matrix.h"
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <omp.h>

// Include SSE intrinsics
#if defined(_MSC_VER)
#include <intrin.h>
#elif defined(__GNUC__) && (defined(__x86_64__) || defined(__i386__))
#include <immintrin.h>
#include <x86intrin.h>
#endif

/* Below are some intel intrinsics that might be useful
 * void _mm256_storeu_pd (double * mem_addr, __m256d a)
 * __m256d _mm256_set1_pd (double a)
 * __m256d _mm256_set_pd (double e3, double e2, double e1, double e0)
 * __m256d _mm256_loadu_pd (double const * mem_addr)
 * __m256d _mm256_add_pd (__m256d a, __m256d b)
 * __m256d _mm256_sub_pd (__m256d a, __m256d b)
 * __m256d _mm256_fmadd_pd (__m256d a, __m256d b, __m256d c)
 * __m256d _mm256_mul_pd (__m256d a, __m256d b)
 * __m256d _mm256_cmp_pd (__m256d a, __m256d b, const int imm8)
 * __m256d _mm256_and_pd (__m256d a, __m256d b)
 * __m256d _mm256_max_pd (__m256d a, __m256d b)
*/

/*
 * Generates a random double between `low` and `high`.
 */
double rand_double(double low, double high) {
    double range = (high - low);
    double div = RAND_MAX / range;
    return low + (rand() / div);
}

/*
 * Generates a random matrix with `seed`.
 */
void rand_matrix(matrix *result, unsigned int seed, double low, double high) {
    srand(seed);
    for (int i = 0; i < result->rows; i++) {
        for (int j = 0; j < result->cols; j++) {
            set(result, i, j, rand_double(low, high));
        }
    }
}

/*
 * Allocate space for a matrix struct pointed to by the double pointer mat with
 * `rows` rows and `cols` columns. You should also allocate memory for the data array
 * and initialize all entries to be zeros. Remember to set all fieds of the matrix struct.
 * `parent` should be set to NULL to indicate that this matrix is not a slice.
 * You should return -1 if either `rows` or `cols` or both have invalid values, or if any
 * call to allocate memory in this function fails. If you don't set python error messages here upon
 * failure, then remember to set it in numc.c.
 * Return 0 upon success and non-zero upon failure.
 */
int allocate_matrix(matrix **mat, int rows, int cols) {

    if (rows <= 0 || cols <= 0) {
    	//return -2; This -2 is our error code
    	return -1; //What the spec wants us to return
    }
    *mat = (matrix*) malloc(sizeof(matrix));
    if(!mat) {
        return -1;
    }
    (*mat)->rows = rows;
    (*mat)->cols = cols;
    (*mat)->data = (double **) malloc(rows * sizeof(double *));
    double** data = (*mat)->data;
    if (data == NULL) {
    	free(mat);
    	return -1;
    }
    data[0] = (double *) calloc(rows * cols, sizeof(double));
    double* base = data[0];
    if (base == NULL) {
		    free(data);
        free(mat);
        return -1;
    }
    #pragma omp parallel for if(rows >= 350 && cols >= 350)
    for (int i = 1; i < ((rows/4) * 4); i += 4) {
    	data[i] = base + (i * cols);
      data[i+1] = base + ((i+1) * cols);
      data[i+2] = base + ((i+2) * cols);
      data[i+3] = base + ((i+3) * cols);
    }
    #pragma omp parallel for if(rows >= 350 && cols >= 350)
    for (int i = ((rows/4) * 4); i < rows; i++) {
      data[i] = base + (i * cols);
    }
    (rows == 1 || cols == 1) ? ((*mat)->is_1d = 1) : ((*mat)->is_1d = 0);
    (*mat)->ref_cnt = 1;
    (*mat)->parent = NULL;
    return 0;
}

/*
 * Allocate space for a matrix struct pointed to by `mat` with `rows` rows and `cols` columns.
 * This is equivalent to setting the new matrix to be
 * from[row_offset:row_offset + rows, col_offset:col_offset + cols]
 * If you don't set python error messages here upon failure, then remember to set it in numc.c.
 * Return 0 upon success and non-zero upon failure.
 */
int allocate_matrix_ref(matrix **mat, matrix *from, int row_offset, int col_offset,
                        int rows, int cols) {
    if (from->cols < col_offset) {
        return -1; //Out of bounds
    }
    if (from->rows < row_offset) {
        return -1; //Out of bounds
    }
    *mat = (matrix*) malloc(sizeof(matrix));
    if (!mat) {
        return -1;
    }
    (*mat)->rows = rows;
    (*mat)->cols = cols;
    (*mat)->data = (double **) malloc(rows * sizeof(double*));
    double** data = (*mat)->data;
    if (data == NULL) {
        free(mat);
        return -1;
    }
    double** parent = from->data;
    #pragma omp parallel for if(rows >= 350 && cols >= 350)
    for (int i = 0; i < ((rows/4) * 4); i += 4) {
      data[i] = parent[row_offset + i] + col_offset;
      data[i+1] = parent[row_offset + i + 1] + col_offset;
      data[i+2] = parent[row_offset + i + 2] + col_offset;
      data[i+3] = parent[row_offset + i + 3] + col_offset;
    }
    #pragma omp parallel for if(rows >= 350 && cols >= 350)
    for (int i = ((rows/4) * 4); i < rows; i++) {
      data[i] = parent[row_offset + i] + col_offset;
    }
    (rows == 1 || cols == 1) ? ((*mat)->is_1d = 1) : ((*mat)->is_1d = 0);
    (*mat)->ref_cnt = 1;
    (*mat)->parent = from;
    from->ref_cnt += 1;
    return 0;
}

/*
 * This function will be called automatically by Python when a numc matrix loses all of its
 * reference pointers.
 * You need to make sure that you only free `mat->data` if no other existing matrices are also
 * referring this data array.
 * See the spec for more information.
 */
void deallocate_matrix(matrix *mat) {
    if (!mat) {
        return;
    }
    if (mat->ref_cnt == 1) {
        if (mat->parent == NULL) {
        	free(mat->data[0]);
		free(mat->data);
        } else {
            mat->parent->ref_cnt -= 1;
        }
        free(mat);
    }
}

/*
 * Return the double value of the matrix at the given row and column.
 * You may assume `row` and `col` are valid.
 */
double get(matrix *mat, int row, int col) {
    return mat->data[row][col];
}

/*
 * Set the value at the given row and column to val. You may assume `row` and
 * `col` are valid
 */
void set(matrix *mat, int row, int col, double val) {
    mat->data[row][col] = val;
}

/*
 * Set all entries in mat to val
 */
void fill_matrix(matrix *mat, double val) {
    double* base = mat->data[0];
    int row = mat->rows;
    int col = mat->cols;
    int size = row * col;
    #pragma omp parallel for if(row >= 350 && col >= 350)
    for (int i = 0; i < ((size/4) * 4); i += 4) {
      base[i] = val;
      base[i+1] = val;
      base[i+2] = val;
      base[i+3] = val;
    }
    #pragma omp parallel for if(row >= 350 && col >= 350)
    for (int i = ((size/4) * 4); i < size; i++) {
      base[i] = val;
    }
}

/*
 * Store the result of adding mat1 and mat2 to `result`.
 * Return 0 upon success and a nonzero value upon failure.
 */
int add_matrix(matrix *result, matrix *mat1, matrix *mat2) {
    // Here, I'm assuming that we already have result as a valid matrix that's passed in
    double* result_data = result->data[0];
    double* mat1_data = mat1->data[0];
    double* mat2_data = mat2->data[0];
    int row = mat1->rows;
    int col = mat1->cols;
    int size = row * col;
    #pragma omp parallel for if(row >= 350 && col >= 350)
    for (int i = 0; i < ((size)/4 * 4); i += 4) {
      result_data[i] = mat1_data[i] + mat2_data[i];
      result_data[i+1] = mat1_data[i+1] + mat2_data[i+1];
      result_data[i+2] = mat1_data[i+2] + mat2_data[i+2];
      result_data[i+3] = mat1_data[i+3] + mat2_data[i+3];
    }
    #pragma omp parallel for if(row >= 350 && col >= 350)
    for (int i = (size/4 * 4); i < size; i++) {
      result_data[i] = mat1_data[i] + mat2_data[i];
    }
    return 0;
}

/*
 * Store the result of subtracting mat2 from mat1 to `result`.
 * Return 0 upon success and a nonzero value upon failure.
 */
int sub_matrix(matrix *result, matrix *mat1, matrix *mat2) {
    double* result_data = result->data[0];
    double* mat1_data = mat1->data[0];
    double* mat2_data = mat2->data[0];
    int row = mat1->rows;
    int col = mat1->cols;
    int size = row * col;
    #pragma omp parallel for if(row >= 350 && col >= 350)
    for (int i = 0; i < size/4 * 4; i += 4) {
      result_data[i] = mat1_data[i] - mat2_data[i];
      result_data[i+1] = mat1_data[i+1] - mat2_data[i+1];
      result_data[i+2] = mat1_data[i+2] - mat2_data[i+2];
      result_data[i+3] = mat1_data[i+3] - mat2_data[i+3];
    }
    #pragma omp parallel for if(row >= 350 && col >= 350)
    for (int i = size/4 * 4; i < size; i++) {
      result_data[i] = mat1_data[i] - mat2_data[i];
    }
    return 0;
}

int mul_matrix_Small(matrix *result, matrix *mat1, matrix *mat2) {
  int row = mat1->rows;
  int col = mat2->cols;
  int mat1_cols = mat1->cols;
  fill_matrix(result, 0);
  if (row < 350 && col < 350) {
    for (int i = 0; i < row; i++) {
        double* mat1_data = mat1->data[i];
        double* result_data = result->data[i];
        for (int n = 0; n < mat1_cols; n++) {
          double* mat2_data = mat2->data[n];
            for (int j = 0; j < col; j++) {
                    result_data[j] += mat1_data[n] * mat2_data[j];
            }
        }
    }
    return 0;
  } else {
      #pragma omp parallel for if(row >= 350 && col >= 350)
      for (int i = 0; i < row; i++) {
        double* result_data = result->data[i];
        double* mat1_data = mat1->data[i];
        for (int n = 0; n < (mat1_cols/4) * 4; n+=4) {
            for (int j = 0; j < col; j++) {
                    result_data[j] += (mat1_data[n] * mat2->data[n][j]) + (mat1_data[n+1] * mat2->data[n+1][j]) + (mat1_data[n+2] * mat2->data[n+2][j]) + (mat1_data[n+3] * mat2->data[n+3][j]);
            }
        }
	#pragma omp parallel for if(row >= 350 && col >= 350)
        for (int n = (mat1_cols/4) * 4; n < mat1_cols; n++) {
          for (int j = 0; j < col; j++) {
                    result_data[j] += mat1_data[n] * mat2->data[n][j];
            }
        }
    }
    return 0;
  }
}



int transpose(matrix *original, matrix *trans) {
  int original_row = original->rows;
  int original_col = original->cols;


  #pragma omp parallel for if(original_row >= 100 && original_col >= 100)
  for (int i = 0; i < original_row * original_col/4*4; i+=4){
    trans->data[i % original_col][i/original_col] = original->data[i/original_col][i % original_col];
    trans->data[(i+1) % original_col][(i+1)/original_col] = original->data[(i+1)/original_col][(i + 1) % original_col];
    trans->data[(i+2) % original_col][(i +2)/original_col] = original->data[(i + 2)/original_col][(i +2)% original_col];
    trans->data[(i+3) % original_col][(i + 3)/original_col] = original->data[(i +3)/original_col][(i +3)% original_col];

  }

  for (int i =original_row * original_col/4*4;i <original_row * original_col; i++) {
    trans->data[i % original_col][i/original_col] = original->data[i/original_col][i % original_col];
  }
  return 0;

}

/*
 * Store the result of multiplying mat1 and mat2 to `result`.
 * Return 0 upon success and a nonzero value upon failure.
 * Remember that matrix multiplication is not the same as multiplying individual elements.
 */
int mul_matrix(matrix *result, matrix *mat1, matrix *mat2) {
    /* TODO: YOUR CODE HERE */
    // This is our actual, naive implementation of matmul
    /* TODO: YOUR CODE HERE */
// This is our actual, naive implementation of matmul
    if (mat1->rows < 300 && mat2->rows<300) {
      mul_matrix_Small(result,mat1,mat2 );
    }
    matrix * mat2_trans;
    int mat2_trans_fail = allocate_matrix(&mat2_trans, mat2->cols, mat2->rows);
    if (mat2_trans_fail !=0) {
      return -3;
    }
    int transpose_fail = transpose(mat2, mat2_trans);
    if (transpose_fail !=0) {
      return -1;
    }
    double** mat1_data = mat1->data;
    double** mat2_trans_data = mat2_trans->data;



    fill_matrix(result, 0);
    #pragma omp parallel for if(mat1->rows >= 500 && mat1->cols >= 500)
    for (int i = 0; i < mat1->rows; i++) {
      double* result_line_pointer = result->data[i];

      __m256d line_result[mat2_trans->rows];
      for (int x = 0; x< mat2_trans->rows; x++) {
        line_result[x] = _mm256_set1_pd(0);
      }
      for (int n = 0; n < mat1->cols/16*16; n+=16) {
        for (int j = 0; j < mat2_trans->rows; j++) {
            line_result[j] += _mm256_mul_pd(_mm256_loadu_pd(mat1_data[i]+n), _mm256_loadu_pd(mat2_trans_data[j]+n));
            line_result[j] += _mm256_mul_pd(_mm256_loadu_pd(mat1_data[i]+n+4),_mm256_loadu_pd(mat2_trans_data[j]+n +4));
            line_result[j] += _mm256_mul_pd(_mm256_loadu_pd(mat1_data[i]+n+8), _mm256_loadu_pd(mat2_trans_data[j]+n +8));
            line_result[j] += _mm256_mul_pd(_mm256_loadu_pd(mat1_data[i]+n+12), _mm256_loadu_pd(mat2_trans_data[j]+n +12));
        }
      }
        for (int x = 0; x < mat2_trans->rows/4*4; x+=4){
          double A1[4] = {0, 0, 0, 0};
          double A2[4] = {0, 0, 0, 0};
          double A3[4] = {0, 0, 0, 0};
          double A4[4] = {0, 0, 0, 0};
          _mm256_storeu_pd(A1, line_result[x]);
          _mm256_storeu_pd(A2, line_result[x+1]);
          _mm256_storeu_pd(A3, line_result[x+2]);
          _mm256_storeu_pd(A4, line_result[x+3]);

          result_line_pointer[x] = A1[0] + A1[1] + A1[2] + A1[3];
          result_line_pointer[x+1] = A2[0] + A2[1] + A2[2] + A2[3];
          result_line_pointer[x+2] = A3[0] + A3[1] + A3[2] + A3[3];
          result_line_pointer[x+3] = A4[0] + A4[1] + A4[2] + A4[3];
        }
        for (int y = mat2_trans->rows/4*4; y< mat2_trans->rows; y++) {
            double A[4] = {0, 0, 0, 0};
            _mm256_storeu_pd(A, line_result[y]);
            result_line_pointer[y] = A[0] + A[1] + A[2] + A[3];
        }
        for (int n = mat1->cols/16*16; n< mat1->cols; n++){
          for (int j = 0; j < mat2_trans->rows; j++) {
            result->data[i][j]+= mat1->data[i][n]*mat2_trans->data[j][n];
          }
        }
    }



    return 0;
}








// int transpose(matrix *original, matrix *trans) {
//   int original_row = original->rows;
//   int original_col = original->cols;


//   #pragma omp parallel for if(original_row >= 100 && original_col >= 100)
//   for (int i = 0; i < original_row * original_col/4*4; i+=4){
//     trans->data[i % original_col][i/original_col] = original->data[i/original_col][i % original_col];
//     trans->data[(i+1) % original_col][(i+1)/original_col] = original->data[(i+1)/original_col][(i + 1) % original_col];
//     trans->data[(i+2) % original_col][(i +2)/original_col] = original->data[(i + 2)/original_col][(i +2)% original_col];
//     trans->data[(i+3) % original_col][(i + 3)/original_col] = original->data[(i +3)/original_col][(i +3)% original_col];

//   }

//   for (int i =original_row * original_col/4*4;i <original_row * original_col; i++) {
//     trans->data[i % original_col][i/original_col] = original->data[i/original_col][i % original_col];
//   }
//   return 0;

// }

// /*
//  * Store the result of multiplying mat1 and mat2 to `result`.
//  * Return 0 upon success and a nonzero value upon failure.
//  * Remember that matrix multiplication is not the same as multiplying individual elements.
//  */

// int mul_matrix(matrix *result, matrix *mat1, matrix *mat2) {
//   int row = mat1->rows;
//   int col = mat2->cols;
//   int mat1_cols = mat1->cols;
//   fill_matrix(result, 0);
//   if (row < 300 && col < 300) {
//     for (int i = 0; i < row; i++) {
//         double* mat1_data = mat1->data[i];
//         double* result_data = result->data[i];
//         for (int n = 0; n < mat1_cols; n++) {
//           double* mat2_data = mat2->data[n];
//             for (int j = 0; j < col; j++) {
//                     result_data[j] += mat1_data[n] * mat2_data[j];
//       __m256d line_result[mat2_trans->rows];
//       for (int x = 0; x< mat2_trans->rows; x++) {
//         line_result[x] = _mm256_set1_pd(0);
//       }
//       for (int n = 0; n < mat1->cols/16*16; n+=16) {
//         for (int j = 0; j < mat2_trans->rows; j++) {
//             line_result[j] += _mm256_mul_pd(_mm256_loadu_pd(mat1_data[i]+n), _mm256_loadu_pd(mat2_trans_data[j]+n));
//             line_result[j] += _mm256_mul_pd(_mm256_loadu_pd(mat1_data[i]+n+4),_mm256_loadu_pd(mat2_trans_data[j]+n +4));
//             line_result[j] += _mm256_mul_pd(_mm256_loadu_pd(mat1_data[i]+n+8), _mm256_loadu_pd(mat2_trans_data[j]+n +8));
//             line_result[j] += _mm256_mul_pd(_mm256_loadu_pd(mat1_data[i]+n+12), _mm256_loadu_pd(mat2_trans_data[j]+n +12));
//         }
//       }
//         for (int x = 0; x < mat2_trans->rows/4*4; x+=4){
//           double A1[4] = {0, 0, 0, 0};
//           double A2[4] = {0, 0, 0, 0};
//           double A3[4] = {0, 0, 0, 0};
//           double A4[4] = {0, 0, 0, 0};
//           _mm256_storeu_pd(A1, line_result[x]);
//           _mm256_storeu_pd(A2, line_result[x+1]);
//           _mm256_storeu_pd(A3, line_result[x+2]);
//           _mm256_storeu_pd(A4, line_result[x+3]);

//           result_line_pointer[x] = A1[0] + A1[1] + A1[2] + A1[3];
//           result_line_pointer[x+1] = A2[0] + A2[1] + A2[2] + A2[3];
//           result_line_pointer[x+2] = A3[0] + A3[1] + A3[2] + A3[3];
//           result_line_pointer[x+3] = A4[0] + A4[1] + A4[2] + A4[3];
//         }
//         for (int y = mat2_trans->rows/4*4; y< mat2_trans->rows; y++) {
//             double A[4] = {0, 0, 0, 0};
//             _mm256_storeu_pd(A, line_result[y]);
//             result_line_pointer[y] = A[0] + A[1] + A[2] + A[3];
//         }
//         for (int n = mat1->cols/16*16; n< mat1->cols; n++){
//           for (int j = 0; j < mat2_trans->rows; j++) {
//             result->data[i][j]+= mat1->data[i][n]*mat2_trans->data[j][n];
//           }
//         }
//     }


    // #pragma omp parallel for
    // for (int i = 0; i < mat1->rows; i++) {
    //     #pragma omp parallel for
    //     __m256d c0 = {0, 0, 0, 0};
    //     for (int j = 0; j < mat2->cols; j++) {
    //         result->data[i][j] = 0;
    //             for (int n = 0; n < mat1->cols; n+=4) {
    //                 result->data[i][j] += mat1->data[i][n] * mat2->data[n][j];
    //             }
    //     }
    // }
    // //Loop reordering:
//   fill_matrix(result, 0);
//     #pragma omp parallel for if(mat1->rows >= 1000 && mat1->cols >= 1000)
//     for (int i = 0; i < mat1->rows; i++) {
//         #pragma omp parallel for if(mat1->rows >= 1000 && mat1->cols >= 1000)
//         for (int n = 0; n < (mat1->cols/4) * 4; n+=4) {
//             for (int j = 0; j < mat2->cols; j++) {
//                     result->data[i][j] += (mat1->data[i][n] * mat2->data[n][j]) + (mat1->data[i][n+1] * mat2->data[n+1][j]) + (mat1->data[i][n+2] * mat2->data[n+2][j]) + (mat1->data[i][n+3] * mat2->data[n+3][j]);
//             }
//         }
//     }
//     return 0;
//   } else {
//       #pragma omp parallel for if(row >= 300 && col >= 300)
//       for (int i = 0; i < row; i++) {
//         double* result_data = result->data[i];
//         double* mat1_data = mat1->data[i];
//         for (int n = 0; n < (mat1_cols/4) * 4; n+=4) {
//             for (int j = 0; j < col; j++) {
//                     result_data[j] += (mat1_data[n] * mat2->data[n][j]) + (mat1_data[n+1] * mat2->data[n+1][j]) + (mat1_data[n+2] * mat2->data[n+2][j]) + (mat1_data[n+3] * mat2->data[n+3][j]);
//             }
//         }
//         for (int n = (mat1_cols/4) * 4; n < mat1_cols; n++) {
//           for (int j = 0; j < col; j++) {
//                     result_data[j] += mat1_data[n] * mat2->data[n][j];
//             }
//         }
//     }
//     return 0;
//   }
// }


// int mul_matrix(matrix *result, matrix *mat1, matrix *mat2) {
//     // This is our actual, naive implementation of matmul

// // This is our actual, naive implementation of matmul
//     matrix * mat2_trans;
//     int mat2_trans_fail = allocate_matrix(&mat2_trans, mat2->cols, mat2->rows);
//     if (mat2_trans_fail !=0) {
//       return -3;
//     }
//     int transpose_fail = transpose(mat2, mat2_trans);
//     if (transpose_fail !=0) {
//       return -1;
//     }
//     double** mat1_data = mat1->data;
//     double** mat2_trans_data = mat2_trans->data;



//     fill_matrix(result, 0);
//     #pragma omp parallel for if(mat1->rows >= 100 && mat1->cols >= 100)
//     for (int i = 0; i < mat1->rows; i++) {
//       double* result_line_pointer = result->data[i];

//       __m256d line_result[mat2_trans->rows];
//       for (int x = 0; x< mat2_trans->rows; x++) {
//         line_result[x] = _mm256_set1_pd(0);
//       }
//       for (int n = 0; n < mat1->cols/16*16; n+=16) {
//         for (int j = 0; j < mat2_trans->rows; j++) {
//             line_result[j] += _mm256_mul_pd(_mm256_loadu_pd(mat1_data[i]+n), _mm256_loadu_pd(mat2_trans_data[j]+n));
//             line_result[j] += _mm256_mul_pd(_mm256_loadu_pd(mat1_data[i]+n+4),_mm256_loadu_pd(mat2_trans_data[j]+n +4));
//             line_result[j] += _mm256_mul_pd(_mm256_loadu_pd(mat1_data[i]+n+8), _mm256_loadu_pd(mat2_trans_data[j]+n +8));
//             line_result[j] += _mm256_mul_pd(_mm256_loadu_pd(mat1_data[i]+n+12), _mm256_loadu_pd(mat2_trans_data[j]+n +12));
//         }
//       }
//         for (int x = 0; x < mat2_trans->rows/4*4; x+=4) {
//           double A1[4] = {0, 0, 0, 0};
//           double A2[4] = {0, 0, 0, 0};
//           double A3[4] = {0, 0, 0, 0};
//           double A4[4] = {0, 0, 0, 0};
//           _mm256_storeu_pd(A1, line_result[x]);
//           _mm256_storeu_pd(A2, line_result[x+1]);
//           _mm256_storeu_pd(A3, line_result[x+2]);
//           _mm256_storeu_pd(A4, line_result[x+3]);

//           result_line_pointer[x] = A1[0] + A1[1] + A1[2] + A1[3];
//           result_line_pointer[x+1] = A2[0] + A2[1] + A2[2] + A2[3];
//           result_line_pointer[x+2] = A3[0] + A3[1] + A3[2] + A3[3];
//           result_line_pointer[x+3] = A4[0] + A4[1] + A4[2] + A4[3];
//         }
//         for (int y = mat2_trans->rows/4*4; y< mat2_trans->rows; y++) {
//             double A[4] = {0, 0, 0, 0};
//             _mm256_storeu_pd(A, line_result[y]);
//             result_line_pointer[y] = A[0] + A[1] + A[2] + A[3];
//         }
//         for (int n = mat1->cols/16*16; n< mat1->cols; n++){
//           for (int j = 0; j < mat2_trans->rows; j++) {
//             result->data[i][j]+= mat1->data[i][n]*mat2_trans->data[n][j];
//           }
//         }
//     }


//     // #pragma omp parallel for
//     // for (int i = 0; i < mat1->rows; i++) {
//     //     #pragma omp parallel for
//     //     __m256d c0 = {0, 0, 0, 0};
//     //     for (int j = 0; j < mat2->cols; j++) {
//     //         result->data[i][j] = 0;
//     //             for (int n = 0; n < mat1->cols; n+=4) {
//     //                 result->data[i][j] += mat1->data[i][n] * mat2->data[n][j];
//     //             }
//     //     }
//     // }
//     // //Loop reordering:
//   fill_matrix(result, 0);
//     #pragma omp parallel for if(mat1->rows >= 1000 && mat1->cols >= 1000)
//     for (int i = 0; i < mat1->rows; i++) {
//         #pragma omp parallel for if(mat1->rows >= 1000 && mat1->cols >= 1000)
//         for (int n = 0; n < (mat1->cols/4) * 4; n+=4) {
//             for (int j = 0; j < mat2->cols; j++) {
//                     result->data[i][j] += (mat1->data[i][n] * mat2->data[n][j]) + (mat1->data[i][n+1] * mat2->data[n+1][j]) + (mat1->data[i][n+2] * mat2->data[n+2][j]) + (mat1->data[i][n+3] * mat2->data[n+3][j]);
//             }
//         }
//         #pragma omp parallel for if(mat1->rows >= 1000 && mat1->cols >= 1000)
//         for (int n = (mat1->cols/4) * 4; n < mat1->cols; n++) {
//           for (int j = 0; j < mat2->cols; j++) {
//                     result->data[i][j] += mat1->data[i][n] * mat2->data[n][j];
//             }
//         }
//     }
//     Loop reordering part 2:
//     fill_matrix(result, 0);
//     #pragma omp parallel for if(mat1->rows >= 1000 && mat1->cols >= 1000)
//     for (int i = 0; i < mat1->rows; i++) {
//         for (int n = 0; n < mat1->cols; n++) {
//             #pragma omp parallel for if(mat1->rows >= 1000 && mat1->cols >= 1000)
//             for (int j = 0; j < (mat2->cols/4) * 4; j += 4) {
//                     result->data[i][j] += mat1->data[i][n] * mat2->data[n][j];
//                     result->data[i][j+1] += mat1->data[i][n] * mat2->data[n][j+1];
//                     result->data[i][j+2] += mat1->data[i][n] * mat2->data[n][j+2];
//                     result->data[i][j+3] += mat1->data[i][n] * mat2->data[n][j+3];
//             }
//             #pragma omp parallel for if(mat1->rows >= 1000 && mat1->cols >= 1000)
//             for (int j = (mat2->cols/4) * 4; j < mat2->cols; j++) {
//                     result->data[i][j] += mat1->data[i][n] * mat2->data[n][j];
//             }
//         }
//     }
//     return 0;
//   }

// int mul_matrix(matrix *result, matrix *mat1, matrix *mat2) {
//     int row = mat1->rows;
//     int col = mat2->cols;
//     int mat1_cols = mat1->cols;
//     double** mat1_data = mat1->data;
//     double** mat2_data = mat2->data;
//     fill_matrix(result, 0);

//     if (row < 500 && col < 500) {
//       #pragma omp parallel for if(row >= 200 && col >= 200)
//       for (int i = 0; i < row; i++) {
//         double* result_data = result->data[i];
//         double* mat1_data = mat1->data[i];
//         for (int n = 0; n < (mat1_cols/4) * 4; n+=4) {
//             for (int j = 0; j < col; j++) {
//                     result_data[j] += (mat1_data[n] * mat2->data[n][j]) + (mat1_data[n+1] * mat2->data[n+1][j]) + (mat1_data[n+2] * mat2->data[n+2][j]) + (mat1_data[n+3] * mat2->data[n+3][j]);
//             }
//         }
//         for (int n = (mat1_cols/4) * 4; n < mat1_cols; n++) {
//           for (int j = 0; j < col; j++) {
//                     result_data[j] += mat1_data[n] * mat2->data[n][j];
//             }
//         }
//     }
//     }
//     else {
//     #pragma omp parallel for if(row >= 500 && col >= 500)
//     for (int i = 0; i < row; i++) {
//       __m256d line_result[col];
//       double* result_data = result->data[i];
//       for (int x = 0; x < col; x++) {
//         line_result[x] = _mm256_set1_pd(0);
//       }
//         for (int n = 0; n < mat1_cols/16*16; n += 16) {
//             for (int j = 0; j < col; j++) {
//                 __m256d four_elem1 = _mm256_set_pd(mat2_data[n+3][j], mat2_data[n+2][j], mat2_data[n+1][j], mat2_data[n][j]);
//                 __m256d four_elem2 = _mm256_set_pd(mat2_data[n+7][j], mat2_data[n+6][j], mat2_data[n+5][j], mat2_data[n+4][j]);
//                 __m256d four_elem3 = _mm256_set_pd(mat2_data[n+11][j], mat2_data[n+10][j], mat2_data[n+9][j], mat2_data[n+8][j]);
//                 __m256d four_elem4 = _mm256_set_pd(mat2_data[n+15][j], mat2_data[n+14][j], mat2_data[n+13][j], mat2_data[n+12][j]);
//                 line_result[j] += _mm256_mul_pd(_mm256_loadu_pd(mat1_data[i]+n), four_elem1);
//                 line_result[j] += _mm256_mul_pd(_mm256_loadu_pd(mat1_data[i]+n+4), four_elem2);
//                 line_result[j] += _mm256_mul_pd(_mm256_loadu_pd(mat1_data[i]+n+8), four_elem3);
//                 line_result[j] += _mm256_mul_pd(_mm256_loadu_pd(mat1_data[i]+n+12), four_elem4);
//             }
//         }
//         for (int x = 0; x < col/4*4; x+=4) {
//           double A1[4] = {0, 0, 0, 0};
//           double A2[4] = {0, 0, 0, 0};
//           double A3[4] = {0, 0, 0, 0};
//           double A4[4] = {0, 0, 0, 0};
//           _mm256_storeu_pd(A1, line_result[x]);
//           _mm256_storeu_pd(A2, line_result[x+1]);
//           _mm256_storeu_pd(A3, line_result[x+2]);
//           _mm256_storeu_pd(A4, line_result[x+3]);

//           result_data[x] = A1[0] + A1[1] + A1[2] + A1[3];
//           result_data[x+1] = A2[0] + A2[1] + A2[2] + A2[3];
//           result_data[x+2] = A3[0] + A3[1] + A3[2] + A3[3];
//           result_data[x+3] = A4[0] + A4[1] + A4[2] + A4[3];
//         }
//         for (int y = col/4*4; y < col; y++) {
//             double A[4] = {0, 0, 0, 0};
//             _mm256_storeu_pd(A, line_result[y]);
//             result_data[y] = A[0] + A[1] + A[2] + A[3];
//         }
//         for (int n = mat1_cols/16*16; n < mat1_cols; n++) {
//           for (int j = 0; j < mat2->cols; j++) {
//             result_data[j] += mat1_data[i][n] * mat2_data[n][j];
//           }
//         }
//     }
//   }
//     // #pragma omp parallel for
//     // for (int i = 0; i < mat1->rows; i++) {
//     //     #pragma omp parallel for
//     //     __m256d c0 = {0, 0, 0, 0};
//     //     for (int j = 0; j < mat2->cols; j++) {
//     //         result->data[i][j] = 0;
//     //             for (int n = 0; n < mat1->cols; n+=4) {
//     //                 result->data[i][j] += mat1->data[i][n] * mat2->data[n][j];
//     //             }
//     //     }
//     // }
//     // //Loop reordering:
//   fill_matrix(result, 0);
//     #pragma omp parallel for if(mat1->rows >= 1000 && mat1->cols >= 1000)
//     for (int i = 0; i < mat1->rows; i++) {
//         #pragma omp parallel for if(mat1->rows >= 1000 && mat1->cols >= 1000)
//         for (int n = 0; n < (mat1->cols/4) * 4; n+=4) {
//             for (int j = 0; j < mat2->cols; j++) {
//                     result->data[i][j] += (mat1->data[i][n] * mat2->data[n][j]) + (mat1->data[i][n+1] * mat2->data[n+1][j]) + (mat1->data[i][n+2] * mat2->data[n+2][j]) + (mat1->data[i][n+3] * mat2->data[n+3][j]);
//             }
//         }
//         #pragma omp parallel for if(mat1->rows >= 1000 && mat1->cols >= 1000)
//         for (int n = (mat1->cols/4) * 4; n < mat1->cols; n++) {
//           for (int j = 0; j < mat2->cols; j++) {
//                     result->data[i][j] += mat1->data[i][n] * mat2->data[n][j];
//             }
//         }
//     }
//     Loop reordering part 2:
//     fill_matrix(result, 0);
//     #pragma omp parallel for if(mat1->rows >= 1000 && mat1->cols >= 1000)
//     for (int i = 0; i < mat1->rows; i++) {
//         for (int n = 0; n < mat1->cols; n++) {
//             #pragma omp parallel for if(mat1->rows >= 1000 && mat1->cols >= 1000)
//             for (int j = 0; j < (mat2->cols/4) * 4; j += 4) {
//                     result->data[i][j] += mat1->data[i][n] * mat2->data[n][j];
//                     result->data[i][j+1] += mat1->data[i][n] * mat2->data[n][j+1];
//                     result->data[i][j+2] += mat1->data[i][n] * mat2->data[n][j+2];
//                     result->data[i][j+3] += mat1->data[i][n] * mat2->data[n][j+3];
//             }
//             #pragma omp parallel for if(mat1->rows >= 1000 && mat1->cols >= 1000)
//             for (int j = (mat2->cols/4) * 4; j < mat2->cols; j++) {
//                     result->data[i][j] += mat1->data[i][n] * mat2->data[n][j];
//             }
//         }
//     }
//     return 0;
// }

// int pow_matrixsimd_(int n, matrix *mat1, matrix *mat2, matrix *product) {
//   double* data1 = mat1->data[0];
//   double* data2 = mat2->data[0];
//   double* result = product->data[0];
//   for (int i = 0; i < n; i += 16) {
//     for(int j = 0; j < n; j++) {
//       __m256d c[4];
//       for (int x = 0; x < 4; x++) {
//         c[x] = _mm256_load_pd(result + i + x*4 + j*n);
//         for (int k = 0; k < n; k++) {
//           __m256d b = _mm256_broadcast_sd(data2+k+j*n);
//             for (int x = 0; x < 4; x++) {
//               c[x] = _mm256_add_pd(c[x],_mm256_mul_pd(_mm256_load_pd(data1+n*k+x*4+i), b));
//             }
//             for (int x = 0; x < 4; x++) {
//               _mm256_store_pd(result+i+x*4+j*n, c[x]);
//             }
//           }
//         }
//       }
//     }
//     return 0;
//   }
// 
// int pow_matrix1(matrix *result, matrix *mat, int pow) {
//   if (pow < 0) {
//       return -3;
//   }
//   if (mat->rows != mat->cols) {
//       return -2;
//   }
//   if (pow == 0) {
//     #pragma omp parallel for if (mat->rows >= 1000 && mat->cols >= 1000)
//     for (int i = 0; i < result->rows; i++) {
//       for (int j = 0; j < result->cols; j++) {
//           if (i == j) {
//                       result->data[i][j] = 1;
//                 }
//                 else {
//                       result->data[i][j] = 0;
//                 }
//             }
//       }
//     return 0;
//   }
//
//
//   if (pow == 1) {
//     double* base = result->data[0];
//     double* template = mat->data[0];
//     #pragma omp parallel for if (mat->rows >= 1000 && mat->cols >= 1000)
//     for (int i = 0; i < ((mat->rows * mat->cols)/4 * 4); i += 4){
//       base[i] = template[i];
//       base[i+1] = template[i+1];
//       base[i+2] = template[i+2];
//       base[i+3] = template[i+3];
//     }
//     #pragma omp parallel for if (mat->rows >= 1000 && mat->cols >= 1000)
//     for (int i = ((mat->rows * mat->cols)/4 * 4); i < (mat->rows * mat->cols); i++){
//       base[i] = template[i];
//
//     }
//     return 0;
//
//   } else {
//     int row = mat->rows;
//     int col = mat->cols;
//     result2 = (matrix**) malloc(sizeof(matrix));
//     matrix *y;
//     int allocate_failed = allocate_matrix(&y, row, col);
//     if (allocate_failed != 0) {
//             return -3;
//     }
//     #pragma omp parallel for if (mat->rows >= 1000 && mat->cols >= 1000)
//     for (int i = 0; i < row; i++) {
//       for (int j = 0; j < col; j++) {
//           if (i == j) {
//             y->data[i][j] = 1;
//           }
//           else {
//             y->data[i][j] = 0;
//           }
//        }
//       }
//     result2 = result * z
//     Chage pointer so result->data = result2->data
//     result2->data = result->data (old data that we dob't care about that can be changed)
//
//
//     Outside if statement:
//     pow = pow >> 1;
//     result2 = z * z
//     Change pointer to z = result2;
//     result2->data would be old value of z
//     END:
//     result would give us the data we want
//     // while (pow > 1) {
//     //   if (pow &1) {
//     //     mul_matrix(result2, result, mat);
//     //   } else {
//     //     pow = pow >>1;
//     //     return 0;
//     //
//     //
//     //   }
//     //
//
//       // if (pow %2 ==0) {
//       //   mul_matrix(result2, mat, mat);
//       //   pow = pow >>>1;
//       // } else {
//       //   mul_matrix(result3, result2, y);
//       //   mul_matrix(result4, result2,result2);
//       //   pow = pow-1;
//       //   pow = pow>>>1;
//       //
//           }
//     }
//   }
//   return 0;
//
//
//
// }
//


/*
 * Store the result of raising mat to the (pow)th power to `result`.
 * Return 0 upon success and a nonzero value upon failure.
 * Remember that pow is defined with matrix multiplication, not element-wise multiplication.
 */
int pow_matrix(matrix *result, matrix *mat, int pow) {
    int row = mat->rows;
    int col = mat->cols;
    int size = row * col;
    if (pow == 1) {
        double* base = result->data[0];
        double* template = mat->data[0];
        #pragma omp parallel for if (mat->rows >= 350 && mat->cols >= 350)
        for (int i = 0; i < ((mat->rows * mat->cols)/4 * 4); i += 4) {
          base[i] = template[i];
          base[i+1] = template[i+1];
          base[i+2] = template[i+2];
          base[i+3] = template[i+3];
        }
        #pragma omp parallel for if (row >= 350 && col >= 350)
        for (int i = (size/4 * 4); i < size; i++){
          base[i] = template[i];
        }
        return 0;
    }
    if (pow == 2) {
        mul_matrix(result, mat, mat);
        return 0;
    }
    //Creates identity matrix in result
    double** result_data = result->data;
    #pragma omp parallel for if (row >= 350 && col >= 350)
    for (int i = 0; i < row; i++) {
      for (int j = 0; j < col; j++) {
          if (i == j) {
            result_data[i][j] = 1;
          }
          else {
            result_data[i][j] = 0;
          }
      }
    }
    //We utilized this link to gain a better visualization of repeated squaring for this part: https://www.geeksforgeeks.org/write-an-iterative-olog-y-function-for-powx-y/
    if (pow > 2) {
    matrix *temp;
    int allocate_failed = allocate_matrix(&temp, row, col);
    if (allocate_failed != 0) {
            return -3;
    }
    matrix *z;
    int allocate_failed1 = allocate_matrix(&z, row, col);
    if (allocate_failed1 != 0) {
            return -3;
    }
    mul_matrix(z, mat, result);
    while (pow > 0) {
      if (pow & 1) {
        mul_matrix(temp, result, z);
        double** middle = result->data;
        result->data = temp->data;
        temp->data = middle;
      }
      pow = pow >> 1;
      mul_matrix(temp, z, z);
      double** placeholder = z->data;
      z->data = temp->data;
      temp->data = placeholder;
    }
    deallocate_matrix(temp);
    deallocate_matrix(z);
  }
    return 0;
}

/*
 * Store the result of element-wise negating mat's entries to `result`.
 * Return 0 upon success and a nonzero value upon failure.
 */
int neg_matrix(matrix *result, matrix *mat) {
    double* base = mat->data[0];
    double* new_base = result->data[0];
    int row = mat->rows;
    int col = mat->cols;
    int size = row * col;
    #pragma omp parallel for if(row >= 350 && col >= 350)
    for (int i = 0; i < size/4 * 4; i += 4) {
    	  new_base[i] = - base[i];
        new_base[i+1] = - base[i+1];
        new_base[i+2] = - base[i+2];
        new_base[i+3] = - base[i+3];
    }
    #pragma omp parallel for if(row >= 350 && col >= 350)
    for (int i = size/4 * 4; i < size; i++) {
     new_base[i] = - base[i];
    }
    return 0;
}

/*
 * Store the result of taking the absolute value element-wise to `result`.
 * Return 0 upon success and a nonzero value upon failure.
 */
int abs_matrix(matrix *result, matrix *mat) {
    double* base = mat->data[0];
    double* new_base = result->data[0];
    int row = mat->rows;
    int col = mat->cols;
    int size = row * col;
    #pragma omp parallel for if(row >= 350 && col >= 350)
    for (int i = 0; i < size/4 * 4; i += 4) {
        (base[i] < 0) ? (new_base[i] = -(base[i])) : (new_base[i] = base[i]);
        (base[i+1] < 0) ? (new_base[i+1] = -(base[i+1])) : (new_base[i+1] = base[i+1]);
        (base[i+2] < 0) ? (new_base[i+2] = -(base[i+2])) : (new_base[i+2] = base[i+2]);
        (base[i+3] < 0) ? (new_base[i+3] = -(base[i+3])) : (new_base[i+3] = base[i+3]);
    }
    #pragma omp parallel for if(row >= 350 && col >= 350)
    for (int i = size/4 * 4; i < size; i++) {
        (base[i] < 0) ? (new_base[i] = -(base[i])) : (new_base[i] = base[i]);
    }
   	return 0;
}
