#include "numc.h"
#include <structmember.h>

PyTypeObject Matrix61cType;

/* Helper functions for initalization of matrices and vectors */

/*
 * Return a tuple given rows and cols
 */
PyObject *get_shape(int rows, int cols) {
  if (rows == 1 || cols == 1) {
    return PyTuple_Pack(1, PyLong_FromLong(rows * cols));
  } else {
    return PyTuple_Pack(2, PyLong_FromLong(rows), PyLong_FromLong(cols));
  }
}
/*
 * Matrix(rows, cols, low, high). Fill a matrix random double values
 */
int init_rand(PyObject *self, int rows, int cols, unsigned int seed, double low,
              double high) {
    matrix *new_mat;
    int alloc_failed = allocate_matrix(&new_mat, rows, cols);
    if (alloc_failed) return alloc_failed;
    rand_matrix(new_mat, seed, low, high);
    ((Matrix61c *)self)->mat = new_mat;
    ((Matrix61c *)self)->shape = get_shape(new_mat->rows, new_mat->cols);
    return 0;
}

/*
 * Matrix(rows, cols, val). Fill a matrix of dimension rows * cols with val
 */
int init_fill(PyObject *self, int rows, int cols, double val) {
    matrix *new_mat;
    int alloc_failed = allocate_matrix(&new_mat, rows, cols);
    if (alloc_failed)
        return alloc_failed;
    else {
        fill_matrix(new_mat, val);
        ((Matrix61c *)self)->mat = new_mat;
        ((Matrix61c *)self)->shape = get_shape(new_mat->rows, new_mat->cols);
    }
    return 0;
}

/*
 * Matrix(rows, cols, 1d_list). Fill a matrix with dimension rows * cols with 1d_list values
 */
int init_1d(PyObject *self, int rows, int cols, PyObject *lst) {
    if (rows * cols != PyList_Size(lst)) {
        PyErr_SetString(PyExc_ValueError, "Incorrect number of elements in list");
        return -1;
    }
    matrix *new_mat;
    int alloc_failed = allocate_matrix(&new_mat, rows, cols);
    if (alloc_failed) return alloc_failed;
    int count = 0;
    for (int i = 0; i < rows; i++) {
        for (int j = 0; j < cols; j++) {
            set(new_mat, i, j, PyFloat_AsDouble(PyList_GetItem(lst, count)));
            count++;
        }
    }
    ((Matrix61c *)self)->mat = new_mat;
    ((Matrix61c *)self)->shape = get_shape(new_mat->rows, new_mat->cols);
    return 0;
}

/*
 * Matrix(2d_list). Fill a matrix with dimension len(2d_list) * len(2d_list[0])
 */
int init_2d(PyObject *self, PyObject *lst) {
    int rows = PyList_Size(lst);
    if (rows == 0) {
        PyErr_SetString(PyExc_ValueError,
                        "Cannot initialize numc.Matrix with an empty list");
        return -1;
    }
    int cols;
    if (!PyList_Check(PyList_GetItem(lst, 0))) {
        PyErr_SetString(PyExc_ValueError, "List values not valid");
        return -1;
    } else {
        cols = PyList_Size(PyList_GetItem(lst, 0));
    }
    for (int i = 0; i < rows; i++) {
        if (!PyList_Check(PyList_GetItem(lst, i)) ||
                PyList_Size(PyList_GetItem(lst, i)) != cols) {
            PyErr_SetString(PyExc_ValueError, "List values not valid");
            return -1;
        }
    }
    matrix *new_mat;
    int alloc_failed = allocate_matrix(&new_mat, rows, cols);
    if (alloc_failed) return alloc_failed;
    for (int i = 0; i < rows; i++) {
        for (int j = 0; j < cols; j++) {
            set(new_mat, i, j,
                PyFloat_AsDouble(PyList_GetItem(PyList_GetItem(lst, i), j)));
        }
    }
    ((Matrix61c *)self)->mat = new_mat;
    ((Matrix61c *)self)->shape = get_shape(new_mat->rows, new_mat->cols);
    return 0;
}

/*
 * This deallocation function is called when reference count is 0
 */
void Matrix61c_dealloc(Matrix61c *self) {
    deallocate_matrix(self->mat);
    Py_TYPE(self)->tp_free(self);
}

/* For immutable types all initializations should take place in tp_new */
PyObject *Matrix61c_new(PyTypeObject *type, PyObject *args,
                        PyObject *kwds) {
    /* size of allocated memory is tp_basicsize + nitems*tp_itemsize*/
    Matrix61c *self = (Matrix61c *)type->tp_alloc(type, 0);
    return (PyObject *)self;
}

/*
 * This matrix61c type is mutable, so needs init function. Return 0 on success otherwise -1
 */
int Matrix61c_init(PyObject *self, PyObject *args, PyObject *kwds) {
    /* Generate random matrices */
    if (kwds != NULL) {
        PyObject *rand = PyDict_GetItemString(kwds, "rand");
        if (!rand) {
            PyErr_SetString(PyExc_TypeError, "Invalid arguments");
            return -1;
        }
        if (!PyBool_Check(rand)) {
            PyErr_SetString(PyExc_TypeError, "Invalid arguments");
            return -1;
        }
        if (rand != Py_True) {
            PyErr_SetString(PyExc_TypeError, "Invalid arguments");
            return -1;
        }

        PyObject *low = PyDict_GetItemString(kwds, "low");
        PyObject *high = PyDict_GetItemString(kwds, "high");
        PyObject *seed = PyDict_GetItemString(kwds, "seed");
        double double_low = 0;
        double double_high = 1;
        unsigned int unsigned_seed = 0;

        if (low) {
            if (PyFloat_Check(low)) {
                double_low = PyFloat_AsDouble(low);
            } else if (PyLong_Check(low)) {
                double_low = PyLong_AsLong(low);
            }
        }

        if (high) {
            if (PyFloat_Check(high)) {
                double_high = PyFloat_AsDouble(high);
            } else if (PyLong_Check(high)) {
                double_high = PyLong_AsLong(high);
            }
        }

        if (double_low >= double_high) {
            PyErr_SetString(PyExc_TypeError, "Invalid arguments");
            return -1;
        }

        // Set seed if argument exists
        if (seed) {
            if (PyLong_Check(seed)) {
                unsigned_seed = PyLong_AsUnsignedLong(seed);
            }
        }

        PyObject *rows = NULL;
        PyObject *cols = NULL;
        if (PyArg_UnpackTuple(args, "args", 2, 2, &rows, &cols)) {
            if (rows && cols && PyLong_Check(rows) && PyLong_Check(cols)) {
                return init_rand(self, PyLong_AsLong(rows), PyLong_AsLong(cols), unsigned_seed, double_low,
                                 double_high);
            }
        } else {
            PyErr_SetString(PyExc_TypeError, "Invalid arguments");
            return -1;
        }
    }
    PyObject *arg1 = NULL;
    PyObject *arg2 = NULL;
    PyObject *arg3 = NULL;
    if (PyArg_UnpackTuple(args, "args", 1, 3, &arg1, &arg2, &arg3)) {
        /* arguments are (rows, cols, val) */
        if (arg1 && arg2 && arg3 && PyLong_Check(arg1) && PyLong_Check(arg2) && (PyLong_Check(arg3)
                || PyFloat_Check(arg3))) {
            if (PyLong_Check(arg3)) {
                return init_fill(self, PyLong_AsLong(arg1), PyLong_AsLong(arg2), PyLong_AsLong(arg3));
            } else
                return init_fill(self, PyLong_AsLong(arg1), PyLong_AsLong(arg2), PyFloat_AsDouble(arg3));
        } else if (arg1 && arg2 && arg3 && PyLong_Check(arg1) && PyLong_Check(arg2) && PyList_Check(arg3)) {
            /* Matrix(rows, cols, 1D list) */
            return init_1d(self, PyLong_AsLong(arg1), PyLong_AsLong(arg2), arg3);
        } else if (arg1 && PyList_Check(arg1) && arg2 == NULL && arg3 == NULL) {
            /* Matrix(rows, cols, 1D list) */
            return init_2d(self, arg1);
        } else if (arg1 && arg2 && PyLong_Check(arg1) && PyLong_Check(arg2) && arg3 == NULL) {
            /* Matrix(rows, cols, 1D list) */
            return init_fill(self, PyLong_AsLong(arg1), PyLong_AsLong(arg2), 0);
        } else {
            PyErr_SetString(PyExc_TypeError, "Invalid arguments");
            return -1;
        }
    } else {
        PyErr_SetString(PyExc_TypeError, "Invalid arguments");
        return -1;
    }
}

/*
 * List of lists representations for matrices
 */
PyObject *Matrix61c_to_list(Matrix61c *self) {
    int rows = self->mat->rows;
    int cols = self->mat->cols;
    PyObject *py_lst = NULL;
    if (self->mat->is_1d) {  // If 1D matrix, print as a single list
        py_lst = PyList_New(rows * cols);
        int count = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                PyList_SetItem(py_lst, count, PyFloat_FromDouble(get(self->mat, i, j)));
                count++;
            }
        }
    } else {  // if 2D, print as nested list
        py_lst = PyList_New(rows);
        for (int i = 0; i < rows; i++) {
            PyList_SetItem(py_lst, i, PyList_New(cols));
            PyObject *curr_row = PyList_GetItem(py_lst, i);
            for (int j = 0; j < cols; j++) {
                PyList_SetItem(curr_row, j, PyFloat_FromDouble(get(self->mat, i, j)));
            }
        }
    }
    return py_lst;
}

PyObject *Matrix61c_class_to_list(Matrix61c *self, PyObject *args) {
    PyObject *mat = NULL;
    if (PyArg_UnpackTuple(args, "args", 1, 1, &mat)) {
        if (!PyObject_TypeCheck(mat, &Matrix61cType)) {
            PyErr_SetString(PyExc_TypeError, "Argument must of type numc.Matrix!");
            return NULL;
        }
        Matrix61c* mat61c = (Matrix61c*)mat;
        return Matrix61c_to_list(mat61c);
    } else {
        PyErr_SetString(PyExc_TypeError, "Invalid arguments");
        return NULL;
    }
}

/*
 * Add class methods
 */
PyMethodDef Matrix61c_class_methods[] = {
    {"to_list", (PyCFunction)Matrix61c_class_to_list, METH_VARARGS, "Returns a list representation of numc.Matrix"},
    {NULL, NULL, 0, NULL}
};

/*
 * Matrix61c string representation. For printing purposes.
 */
PyObject *Matrix61c_repr(PyObject *self) {
    PyObject *py_lst = Matrix61c_to_list((Matrix61c *)self);
    return PyObject_Repr(py_lst);
}

/* NUMBER METHODS */

/*
 * Add the second numc.Matrix (Matrix61c) object to the first one. The first operand is
 * self, and the second operand can be obtained by casting `args`.
 */
PyObject *Matrix61c_add(Matrix61c* self, PyObject* args) {
    /* TODO: YOUR CODE HERE */
	if (!PyObject_TypeCheck(args, &Matrix61cType)) {
		PyErr_SetString(PyExc_TypeError, "Argument must of type numc.Matrix!");
		return NULL;
        }
	Matrix61c* b = (Matrix61c*) args;
	if ((b->mat->rows != self->mat->rows) || (b->mat->cols != self->mat->cols)) {
		PyErr_SetString(PyExc_ValueError, "Dimensions do not match for matrix addition!");
       		return NULL;
	}
	Matrix61c *rv = (Matrix61c *) Matrix61c_new(&Matrix61cType, NULL, NULL);
	int allocate_failure = allocate_matrix(&(rv->mat), self->mat->rows, self->mat->cols);
	if (allocate_failure != 0) {
                PyErr_SetString(PyExc_RuntimeError, "Failed to allocate a matrix for addition!");
                return NULL;
        }
    add_matrix(rv->mat, self->mat, b->mat);
	rv->shape = get_shape(rv->mat->rows, rv->mat->cols);
	return ((PyObject*) rv);
}

/*
 * Substract the second numc.Matrix (Matrix61c) object from the first one. The first operand is
 * self, and the second operand can be obtained by casting `args`.
 */
PyObject *Matrix61c_sub(Matrix61c* self, PyObject* args) {
    /* TODO: YOUR CODE HERE */
	if (!PyObject_TypeCheck(args, &Matrix61cType)) {
                PyErr_SetString(PyExc_TypeError, "Argument must of type numc.Matrix!");
                return NULL;
        }
        Matrix61c* b = (Matrix61c*) args;
        if ((b->mat->rows != self->mat->rows) || (b->mat->cols != self->mat->cols)) {
            PyErr_SetString(PyExc_ValueError, "Dimensions do not match for matrix subtraction!");
            return NULL;
        }
        Matrix61c *rv = (Matrix61c *) Matrix61c_new(&Matrix61cType, NULL, NULL);
        int allocate_failure = allocate_matrix(&(rv->mat), self->mat->rows, self->mat->cols);
        if (allocate_failure != 0) {
                PyErr_SetString(PyExc_RuntimeError, "Failed to allocate a matrix for subtraction!");
                return NULL;
        }
        sub_matrix(rv->mat, self->mat, b->mat);
        rv->shape = get_shape(rv->mat->rows, rv->mat->cols);
        return ((PyObject*) rv);
}

/*
 * NOT element-wise multiplication. The first operand is self, and the second operand
 * can be obtained by casting `args`.
 */
PyObject *Matrix61c_multiply(Matrix61c* self, PyObject *args) {
    /* TODO: YOUR CODE HERE */
	if (!PyObject_TypeCheck(args, &Matrix61cType)) {
            PyErr_SetString(PyExc_TypeError, "Argument must of type numc.Matrix!");
            return NULL;
        }
        Matrix61c* b = (Matrix61c*) args;
        if (self->mat->cols != b->mat->rows) {
            PyErr_SetString(PyExc_ValueError, "Dimensions do not match for matrix multiplication!");
            return NULL;
        }
        Matrix61c *rv = (Matrix61c *) Matrix61c_new(&Matrix61cType, NULL, NULL);
        int allocate_failure = allocate_matrix(&(rv->mat), self->mat->rows, b->mat->cols);
        if (allocate_failure != 0) {
                PyErr_SetString(PyExc_RuntimeError, "Failed to allocate a matrix for multiplication!");
                return NULL;
        }
        mul_matrix(rv->mat, self->mat, b->mat);
        rv->shape = get_shape(rv->mat->rows, rv->mat->cols);
        return ((PyObject*) rv);
}

/*
 * Negates the given numc.Matrix.
 */
PyObject *Matrix61c_neg(Matrix61c* self) {
    /* TODO: YOUR CODE HERE */
	Matrix61c *rv = (Matrix61c *) Matrix61c_new(&Matrix61cType, NULL, NULL);
	int allocate_failure = allocate_matrix(&(rv->mat), self->mat->rows, self->mat->cols);
        if (allocate_failure != 0) {
                PyErr_SetString(PyExc_RuntimeError, "Failed to allocate a matrix for negation!");
                return NULL;
        }
    	rv->shape = get_shape(rv->mat->rows, rv->mat->cols);
    neg_matrix(rv->mat, self->mat);
    return ((PyObject*) rv);
}

/*
 * Take the element-wise absolute value of this numc.Matrix.
 */
PyObject *Matrix61c_abs(Matrix61c *self) {
    /* TODO: YOUR CODE HERE */
	Matrix61c *rv = (Matrix61c *) Matrix61c_new(&Matrix61cType, NULL, NULL);
    	int allocate_failure = allocate_matrix(&(rv->mat), self->mat->rows, self->mat->cols);
        if (allocate_failure != 0) {
                PyErr_SetString(PyExc_RuntimeError, "Failed to allocate a matrix for absolute value!");
                return NULL;
        }
    	rv->shape = get_shape(rv->mat->rows, rv->mat->cols);
        abs_matrix(rv->mat, self->mat);
        return ((PyObject*) rv);
}

/*
 * Raise numc.Matrix (Matrix61c) to the `pow`th power. You can ignore the argument `optional`.
 */
PyObject *Matrix61c_pow(Matrix61c *self, PyObject *pow, PyObject *optional) {
    /* TODO: YOUR CODE HERE */
	if (!PyObject_TypeCheck(pow, &PyLong_Type)) {
        	PyErr_SetString(PyExc_TypeError, "Power must be of type int!");
        	return NULL;
        }
        if (PyLong_AsLong(pow) < 0) {
            PyErr_SetString(PyExc_ValueError, "Power cannot be negative!");
            return NULL;
        }
	if (self->mat->rows != self->mat->cols) {
		PyErr_SetString(PyExc_ValueError, "Need a square matrix for power function!");
        	return NULL;
	}
        Matrix61c *rv = (Matrix61c *) Matrix61c_new(&Matrix61cType, NULL, NULL);
        int allocate_failure = allocate_matrix(&(rv->mat), self->mat->rows, self->mat->cols);
        if (allocate_failure != 0) {
                PyErr_SetString(PyExc_RuntimeError, "Failed to allocate a matrix for power!");
                return NULL;
        }
        int failure = pow_matrix(rv->mat, self->mat, PyLong_AsLong(pow));
	if (failure != 0) {
        	PyErr_SetString(PyExc_RuntimeError, "Non-zero value returned for matrix to the power!");
        	return NULL;
        }
        rv->shape = get_shape(rv->mat->rows, rv->mat->cols);
        return ((PyObject*) rv);
}

/*
 * Create a PyNumberMethods struct for overloading operators with all the number methods you have
 * define. You might find this link helpful: https://docs.python.org/3.6/c-api/typeobj.html
 */
PyNumberMethods Matrix61c_as_number = {
    /* TODO: YOUR CODE HERE */
	.nb_add = (binaryfunc) Matrix61c_add,
	.nb_subtract = (binaryfunc) Matrix61c_sub,
	.nb_multiply = (binaryfunc) Matrix61c_multiply,
	.nb_negative = (unaryfunc) Matrix61c_neg,
	.nb_absolute = (unaryfunc) Matrix61c_abs,
	.nb_power = (ternaryfunc) Matrix61c_pow
};


/* INSTANCE METHODS */

/*
 * Given a numc.Matrix self, parse `args` to (int) row, (int) col, and (double/int) val.
 * Return None in Python (this is different from returning null).
 */
PyObject *Matrix61c_set_value(Matrix61c *self, PyObject* args) {
    /* TODO: YOUR CODE HERE */

	if (PyTuple_Size(args) != 3) {
		PyErr_SetString(PyExc_TypeError, "Needs three arguments!");
                return NULL;
	}
	PyObject* row_obj = PyTuple_GetItem(args, 0);
	PyObject* col_obj = PyTuple_GetItem(args, 1);
	PyObject* val_obj = PyTuple_GetItem(args, 2);
	if (!PyLong_Check(row_obj)) {
                PyErr_SetString(PyExc_TypeError, "Row value needs to be an integer!");
                return NULL;
        }
	if (!PyLong_Check(col_obj)) {
                PyErr_SetString(PyExc_TypeError, "Column value needs to be an integer!");
                return NULL;
        }
	if (!PyFloat_Check(val_obj) && !PyLong_Check(val_obj)) {
                PyErr_SetString(PyExc_TypeError, "Value must be an integer or a double!");
                return NULL;
        }
	int row = PyLong_AsLong(row_obj);
	int col = PyLong_AsLong(col_obj);
	double val = PyFloat_AsDouble(val_obj);

	if ((row < 0) || (row >= self->mat->rows) || (col < 0) || (col >= self->mat->cols)) {
                PyErr_SetString(PyExc_IndexError, "Row or column value is out of range!");
                return NULL;
        }
	set(self->mat, row, col, val);
	Py_RETURN_NONE;
}

/*
 * Given a numc.Matrix `self`, parse `args` to (int) row and (int) col.
 * Return the value at the `row`th row and `col`th column, which is a Python
 * float/int.
 */
PyObject *Matrix61c_get_value(Matrix61c *self, PyObject* args) {
    /* TODO: YOUR CODE HERE */
	if (PyTuple_Size(args) != 2) {
                PyErr_SetString(PyExc_TypeError, "Needs two arguments!");
                return NULL;
        }
	PyObject* row_obj = PyTuple_GetItem(args, 0);
        PyObject* col_obj = PyTuple_GetItem(args, 1);
	if (!PyLong_Check(row_obj)) {
                PyErr_SetString(PyExc_TypeError, "Row value needs to be an integer!");
                return NULL;
        }
        if (!PyLong_Check(col_obj)) {
                PyErr_SetString(PyExc_TypeError, "Column value needs to be an integer!");
                return NULL;
        }
	int row = PyLong_AsLong(row_obj);
    int col = PyLong_AsLong(col_obj);
    if ((row < 0) || (row >= self->mat->rows) || (col < 0) || (col >= self->mat->cols)) {
        PyErr_SetString(PyExc_IndexError, "Row or column values are out of range!");
        return NULL;
    }
    return PyFloat_FromDouble(get(self->mat, row, col));
}

/*
 * Create an array of PyMethodDef structs to hold the instance methods.
 * Name the python function corresponding to Matrix61c_get_value as "get" and Matrix61c_set_value
 * as "set"
 * You might find this link helpful: https://docs.python.org/3.6/c-api/structures.html
 */
PyMethodDef Matrix61c_methods[] = {
    /* TODO: YOUR CODE HERE */
	{"get", (PyCFunction)&Matrix61c_get_value, METH_VARARGS, "This is the get function which retrieves a value from a matrix given a row and column value."},
	{"set", (PyCFunction)&Matrix61c_set_value, METH_VARARGS, "This is the set function which sets the value at a given row and column to a given value."},
    {NULL, NULL, 0, NULL}
};

/* INDEXING */

/*
 * Given a numc.Matrix `self`, index into it with `key`. Return the indexed result.
 */
PyObject *Matrix61c_subscript(Matrix61c* self, PyObject* key) {
    /* TODO: YOUR CODE HERE */
	//Checks if self is a 1D matrix and key is not an integer or a slice.
    int long_check = PyObject_TypeCheck(key, &PyLong_Type);
    int slice_check = PyObject_TypeCheck(key, &PySlice_Type);
   	int tuple_check = PyObject_TypeCheck(key, &PyTuple_Type);
    int is_1d = self->mat->is_1d;
	if (is_1d && (!long_check && !slice_check)) {
        	PyErr_SetString(PyExc_TypeError, "Must use an integer or a slice for indexing into a 1D matrix!");
        	return NULL;
	}
	//Checks if self is a 2D matrix and key is not an integer, a slice, or a tuple of length 2 (but we haven't checked the contents of the tuple yet).
	if ((is_1d != 1) && (!long_check && !slice_check && !(tuple_check && (PyTuple_Size(key) == 2)))) {
        	PyErr_SetString(PyExc_TypeError, "Must use an integer, a slice, or a length-2 tuple of slices and/or integers for indexing into a 2D matrix!");
        	return NULL;
    	}
    if (long_check) {
        int index1 = PyLong_AsLong(key);
        if (index1 < 0) {
            PyErr_SetString(PyExc_ValueError, "Negative indexing is NOT allowed!");
            return NULL;
        }
        if (is_1d){
            if ((index1 >= self->mat->rows) && (index1 >= self->mat->cols)) {
                PyErr_SetString(PyExc_IndexError, "Index is out of bounds!");
                return NULL;
            }
        } else {
            if (index1 >= self->mat->rows) {
                PyErr_SetString(PyExc_IndexError, "Index is out of bounds!");
                return NULL;
            }
        }
        if (is_1d) {
            if (self->mat->rows > self->mat->cols) { //n * 1 matrix
                return PyFloat_FromDouble(self->mat->data[index1][0]);
            } else {     //1 * n matrix
                return PyFloat_FromDouble(self->mat->data[0][index1]);
            }
        } else {
            Matrix61c *rv = (Matrix61c *) Matrix61c_new (&Matrix61cType, NULL, NULL);
            int allocate_failure = allocate_matrix_ref(&(rv->mat), self->mat, index1, 0, 1, self->mat->cols);
            if (allocate_failure != 0) {
            PyErr_SetString(PyExc_RuntimeError, "Failed to allocate a matrix!");
            return NULL;
        }
        rv->shape = get_shape(rv->mat->rows, rv->mat->cols);
        return ((PyObject*) rv);
        }
    }
    if (slice_check) {
        Py_ssize_t start, length1, stop, step, slicelength1;
        if (is_1d) {
            if (self->mat->cols > self->mat->rows) {
                length1 = ((Py_ssize_t) self->mat->cols);
            } else {
                length1 = ((Py_ssize_t) self->mat->rows);
            }
        } else {
            length1 = ((Py_ssize_t) self->mat->rows);
        }
        PySlice_GetIndicesEx(key, length1, &start, &stop, &step, &slicelength1);
        if (step != 1) {
            PyErr_SetString(PyExc_ValueError, "Slice must have a step size of 1!");
            return NULL;
        }
        if (slicelength1 < 1) {
            PyErr_SetString(PyExc_ValueError, "Slice shouldn't be less than 1!");
            return NULL;
        }
        if (is_1d) {
            if (slicelength1 == 1) { // get a single value
                if (self->mat->rows > self->mat->cols) { //n * 1 matrix
                    return PyFloat_FromDouble(self->mat->data[start][0]); // not sure about this
                } else {   // 1 * n matrix
                    return PyFloat_FromDouble(self->mat->data[0][start]);
                }
            } else { // get a slice of 1d matrix
                Matrix61c *rv = (Matrix61c *) Matrix61c_new (&Matrix61cType, NULL, NULL);
                if (self->mat->rows > self->mat->cols) { //n * 1 matrix
                    int allocate_failure = allocate_matrix_ref(&(rv->mat), self->mat, start, 0, slicelength1, 1); //not sure whether will return a slice of matrix
                    if (allocate_failure != 0) {
                        PyErr_SetString(PyExc_RuntimeError, "Failed to allocate a matrix!");
                        return NULL;
                    }
                    rv->shape = get_shape(rv->mat->rows, rv->mat->cols);
                    return ((PyObject*) rv);
                } else {  //1 * n matrix
                    int allocate_failure = allocate_matrix_ref(&(rv->mat), self->mat, 0, start, 1, slicelength1);
                    if (allocate_failure != 0) {
                        PyErr_SetString(PyExc_RuntimeError, "Failed to allocate a matrix!");
                        return NULL;
                    }
                    rv->shape = get_shape(rv->mat->rows, rv->mat->cols);
                    return ((PyObject*) rv);
                }
            }
        } else { //if it's 2d
            Matrix61c *rv = (Matrix61c *) Matrix61c_new (&Matrix61cType, NULL, NULL);
            int allocate_failure = allocate_matrix_ref(&(rv->mat), self->mat, start, 0, slicelength1, self->mat->cols);
            if (allocate_failure != 0) {
                PyErr_SetString(PyExc_RuntimeError, "Failed to allocate a matrix!");
                return NULL;
            }
            rv->shape = get_shape(rv->mat->rows, rv->mat->cols);
            return ((PyObject*) rv);
        }
    }
    if (tuple_check) {
        PyObject* tuple_item1 = PyTuple_GetItem (key, 0);
        PyObject* tuple_item2 = PyTuple_GetItem (key, 1);
        int long_check1 = PyLong_Check(tuple_item1);
        int slice_check1 = PySlice_Check(tuple_item1);
        int long_check2 = PyLong_Check(tuple_item2);
        int slice_check2 = PySlice_Check(tuple_item2);
        if ((long_check1 && PyLong_AsLong(tuple_item1) < 0) || (long_check2 && PyLong_AsLong(tuple_item2) < 0)) {
            PyErr_SetString(PyExc_ValueError, "Negative indexing is NOT allowed!");
            return NULL;
        }
        if (long_check1 && long_check2) {  // it should be a 2d matrix, a tuple of ints give a double
		        int long1 = PyLong_AsLong(tuple_item1);   ///NOT sure whether we need to cast to int
		        int long2 = PyLong_AsLong(tuple_item2);
            if (long1 >= self->mat->rows || long2 >= self->mat->cols) {
                PyErr_SetString(PyExc_IndexError, "Index is out of bounds!");
                return NULL;
            } else {
		            return PyFloat_FromDouble(self->mat->data[long1][long2]);
            }
        }
        if (long_check1 && slice_check2) {
          	int long1 = PyLong_AsLong(tuple_item1);
            if (long1 >= self->mat->rows) {
                PyErr_SetString(PyExc_IndexError, "Index is out of bounds!");
                return NULL;
            }
            Py_ssize_t start, length1, stop, step, slicelength1;
            length1 = self->mat->cols;
            PySlice_GetIndicesEx(tuple_item2, length1, &start, &stop, &step, &slicelength1);
            if (step != 1) {
                PyErr_SetString(PyExc_ValueError, "Slice must have a step size of 1!");
                return NULL;
            }
            if (slicelength1 < 1) {
                PyErr_SetString(PyExc_ValueError, "Slice shouldn't be less than 1!");
                return NULL;
            }
            if (slicelength1 == 1) {
                return PyFloat_FromDouble(self->mat->data[long1][start]);
            }
            Matrix61c *rv = (Matrix61c *) Matrix61c_new (&Matrix61cType, NULL, NULL); //USE PyLong_AsLong(tuple_item1) AS INT
            int allocate_failure  = allocate_matrix_ref(&(rv->mat), self->mat, long1, start, 1, slicelength1);
            if (allocate_failure != 0) {
                PyErr_SetString(PyExc_RuntimeError, "Failed to allocate a matrix!");
                return NULL;
            }
            rv->shape = get_shape(rv->mat->rows, rv->mat->cols);
            return ((PyObject*) rv);
        }
        if (slice_check1 && long_check2) {
            int long2 = PyLong_AsLong(tuple_item2);
            if (long2 >= self->mat->cols) {   // USE JUST tuple_item2 or PyLong_AsLong(tuple_item2) ??
              PyErr_SetString(PyExc_IndexError, "Index is out of bounds!");
              return NULL;
            }
            Py_ssize_t start, length1, stop, step, slicelength1;
            length1 = self->mat->rows;
            PySlice_GetIndicesEx(tuple_item1, length1, &start, &stop, &step, &slicelength1);
            if (step != 1) {
              PyErr_SetString(PyExc_ValueError, "Slice must have a step size of 1!");
              return NULL;
            }
            if (slicelength1 < 1) {
              PyErr_SetString(PyExc_ValueError, "Slice shouldn't be less than 1!");
              return NULL;
            }
            if (slicelength1 == 1) {
              return PyFloat_FromDouble(self->mat->data[start][long2]);
            }
            Matrix61c *rv = (Matrix61c *) Matrix61c_new (&Matrix61cType, NULL, NULL);
            int allocate_failure  = allocate_matrix_ref(&(rv->mat), self->mat, start, long2, slicelength1, 1); //1d matrix with slicelength * 1;
            if (allocate_failure != 0) {
              PyErr_SetString(PyExc_RuntimeError, "Failed to allocate a matrix!");
              return NULL;
            }
            rv->shape = get_shape(rv->mat->rows, rv->mat->cols);
            return ((PyObject*) rv);
        }
        if (slice_check1 && slice_check2) {
          Py_ssize_t start1, length1, stop1, step1, slicelength1;
          Py_ssize_t start2, length2, stop2, step2, slicelength2;
          length1 = self->mat->rows;
          length2 = self->mat->cols;
          PySlice_GetIndicesEx(tuple_item1, length1, &start1, &stop1, &step1, &slicelength1);
          PySlice_GetIndicesEx(tuple_item2, length2, &start2, &stop2, &step2, &slicelength2);
          if (step1 != 1 || step2 != 1) {
              PyErr_SetString(PyExc_ValueError, "Slice must have a step size of 1!");
              return NULL;
          }
          if (slicelength1 < 1 || slicelength2 < 1) {
              PyErr_SetString(PyExc_ValueError, "Slice shouldn't be less than 1!");
              return NULL;
          }
          if (slicelength1 == 1 && slicelength2 == 1) {
            return PyFloat_FromDouble(self->mat->data[start1][start2]);
          }
          Matrix61c *rv = (Matrix61c *) Matrix61c_new (&Matrix61cType, NULL, NULL);
          int allocate_failure  = allocate_matrix_ref(&(rv->mat), self->mat, start1, start2, slicelength1, slicelength2);
          if (allocate_failure != 0) {
            PyErr_SetString(PyExc_RuntimeError, "Failed to allocate a matrix!");
            return NULL;
          }
          rv->shape = get_shape(rv->mat->rows, rv->mat->cols);
          return ((PyObject*) rv);
        }
    }
    Py_RETURN_NONE;
}
/*
 * Given a numc.Matrix `self`, index into it with `key`, and set the indexed result to `v`.
 */
int Matrix61c_set_subscript(Matrix61c* self, PyObject *key, PyObject *v) {
  //First we figure out what the resulting slice or value of the self indexed with key is
  PyObject* sliced_mat = Matrix61c_subscript(self, key);
  if (!sliced_mat) {
    PyErr_SetString(PyExc_IndexError, "Memory cannot be accessed!");
    return -1;
  }
  Matrix61c* sliced_matrix  = (Matrix61c*) sliced_mat;
  int long_check_matrix = PyLong_Check(sliced_matrix);
  int float_check_matrix = PyFloat_Check(sliced_matrix);
  int long_check_v = PyLong_Check(v);
  int float_check_v = PyFloat_Check(v);
  double value;
  if (long_check_v) {
    value = PyLong_AsDouble(v);
  }
  if (float_check_v) {
    value = PyFloat_AsDouble(v);
  }
  if (long_check_matrix || float_check_matrix) { // Resulting slice is 1 by 1
    if(!long_check_v && !float_check_v) {
      PyErr_SetString(PyExc_TypeError, "Value should be a float or integer.");
      return -1;
    }
    int long_check_key = PyLong_Check(key);
    int slice_check_key = PySlice_Check(key);
    int tuple_check_key = PyTuple_Check(key);
    ///// WHAT IF KEY IS A FLOAT??
    if (long_check_key) { // so original self matrix is 1d because it returns just value
	    int key_as_long = PyLong_AsLong(key);
      if (self->mat->rows > self->mat->cols) { //n*1 matrix
        self->mat->data[key_as_long][0] = value; //Changed value to be a double.
        return 0;
      } else { //1*n matrix
          self->mat->data[0][key_as_long] = value; //Changed value to be a double.
          return 0;
	       }
    }
    if (slice_check_key) {  // slice_length is 1 and original matrix is 1d
      Py_ssize_t length, start, stop, step, slicelength;
      if (self->mat->rows > self->mat->cols) { //n * 1 matrix
        length = self->mat->rows;
        PySlice_GetIndicesEx(key, length, &start, &stop, &step, &slicelength);
        self->mat->data[start][0] = value;
        return 0;
      } else { // 1*n matrix
        length = self->mat->cols;
        PySlice_GetIndicesEx(key, length, &start, &stop, &step, &slicelength);
        self->mat->data[0][start] = value;
        return 0;
      }
    }
    if (tuple_check_key) {
      PyObject* tuple_item1 = PyTuple_GetItem (key, 0);
      PyObject* tuple_item2 = PyTuple_GetItem (key, 1);
      int long_check1 = PyLong_Check(tuple_item1);
      int slice_check1 = PySlice_Check(tuple_item1);
      int long_check2 = PyLong_Check(tuple_item2);
      int slice_check2 = PySlice_Check(tuple_item2);
      if (long_check1 && long_check2) {
        long long1 = PyLong_AsLong(tuple_item1);
        long long2 = PyLong_AsLong(tuple_item2);
        self->mat->data[long1][long2] = value;
        return 0;
      }
      if (long_check1 && slice_check2) {
        Py_ssize_t start, length, stop, step, slicelength;
        length = self->mat->cols;
        PySlice_GetIndicesEx(tuple_item2, length, &start, &stop, &step, &slicelength);
        self->mat->data[PyLong_AsLong(tuple_item1)][start] = value;
        return 0;
      }
      if (slice_check1 && long_check2) {
        Py_ssize_t start, length, stop, step, slicelength;
        length = self->mat->rows;
        PySlice_GetIndicesEx(tuple_item1, length, &start, &stop, &step, &slicelength);
        self->mat->data[start][PyLong_AsLong(tuple_item2)] = value;
        return 0;
      }
      if (slice_check1 && slice_check2) {
        Py_ssize_t start1, length1, stop1, step1, slicelength1;
        Py_ssize_t start2, length2, stop2, step2, slicelength2;
        length1 = self->mat->rows;
        length2 = self->mat->cols;
        PySlice_GetIndicesEx(tuple_item1, length1, &start1, &stop1, &step1, &slicelength1);
        PySlice_GetIndicesEx(tuple_item2, length2, &start2, &stop2, &step2, &slicelength2);
        self->mat->data[start1][start2] = value;
        return 0;
      }
    }
  } else {  // Resulting slice is not 1 by 1
    if (!PyList_Check(v)) {
      PyErr_SetString(PyExc_TypeError, "Value must be a list!");
      return -1;
    }
    if (sliced_matrix->mat->rows == 1) {  //Resulting slice is 1D 1*n
      if (sliced_matrix->mat->cols != PyList_Size(v)) {
      PyErr_SetString(PyExc_ValueError, "List of values has the wrong length!");
      return -1;
      }
      for (int i = 0; i < sliced_matrix->mat->cols; i++) {
        PyObject* item = PyList_GetItem(v, i);
        int long_check_item = PyLong_Check(item);
        int float_check_item = PyFloat_Check(item);
        if (!long_check_item && !float_check_item) {
          PyErr_SetString(PyExc_ValueError, "List of values has the wrong value type!");
          return -1;
        }
        if (long_check_item) {
          sliced_matrix->mat->data[0][i] = PyLong_AsDouble(item);
        } else {
          sliced_matrix->mat->data[0][i] = PyFloat_AsDouble(item);
        }
      }
      return 0;
    }
    if (sliced_matrix->mat->cols == 1) {  //Resulting slice is 1D  n*1
        if (sliced_matrix->mat->rows != PyList_Size(v)) {
          PyErr_SetString(PyExc_ValueError, "List of values has the wrong length!");
          return -1;
        }
      for (int i = 0; i < sliced_matrix->mat->rows; i++) {
          PyObject* item = PyList_GetItem(v, i);
          int long_check_item = PyLong_Check(item);
          int float_check_item = PyFloat_Check(item);
          if (!long_check_item && !float_check_item) {
          PyErr_SetString(PyExc_ValueError, "List of values has the wrong value type!");
          return -1;
          }
          if (long_check_item) {
            sliced_matrix->mat->data[i][0] = PyLong_AsDouble(item);
          } else {
            sliced_matrix->mat->data[i][0] = PyFloat_AsDouble(item);
          }
      }
      return 0;
    } else {  //Resulting slice is 2D
      int sliced_matrix_row = sliced_matrix->mat->rows;
      int slice_matrix_col = sliced_matrix->mat->cols;
      if (sliced_matrix_row != PyList_Size(v)) {
        PyErr_SetString(PyExc_ValueError, "List of values has the wrong length!");
        return -1;
      }
        for (int i = 0; i < PyList_Size(v); i++) {
        PyObject* each_sublist = PyList_GetItem(v, i);
        Py_ssize_t sub_list_len = PyList_Size(each_sublist);
        if (sub_list_len != slice_matrix_col) {
          PyErr_SetString(PyExc_ValueError, "Sublist of values has the wrong length!"); // chech each sublist has same length as cols
          return -1;
        }
        for (Py_ssize_t j = 0; j < sub_list_len; j++) {
        PyObject* each_element = PyList_GetItem(each_sublist, j);
        int element_is_long = PyLong_Check(each_element);
        int element_is_float = PyFloat_Check(each_element);
          if(!element_is_long && !element_is_float) {
            PyErr_SetString(PyExc_ValueError, "List of values has the wrong value type!");
            return -1;
          }
        }
      }
      for (int i = 0; i < sliced_matrix->mat->rows; i++) {
        for (int j = 0; j < sliced_matrix->mat->cols; j++) {
          PyObject* item = PyList_GetItem(PyList_GetItem(v, i), j);
          if (PyLong_Check(item)) {
            sliced_matrix->mat->data[i][j] = PyLong_AsDouble(item);
          } else {
            sliced_matrix->mat->data[i][j] = PyFloat_AsDouble(item);
          }
        }
      }
      return 0;
    }
  }
  return -1;
}

PyMappingMethods Matrix61c_mapping = {
    NULL,
    (binaryfunc) Matrix61c_subscript,
    (objobjargproc) Matrix61c_set_subscript,
};

/* INSTANCE ATTRIBUTES*/
PyMemberDef Matrix61c_members[] = {
    {
        "shape", T_OBJECT_EX, offsetof(Matrix61c, shape), 0,
        "(rows, cols)"
    },
    {NULL}  /* Sentinel */
};

PyTypeObject Matrix61cType = {
    PyVarObject_HEAD_INIT(NULL, 0)
    .tp_name = "numc.Matrix",
    .tp_basicsize = sizeof(Matrix61c),
    .tp_dealloc = (destructor)Matrix61c_dealloc,
    .tp_repr = (reprfunc)Matrix61c_repr,
    .tp_as_number = &Matrix61c_as_number,
    .tp_flags = Py_TPFLAGS_DEFAULT |
    Py_TPFLAGS_BASETYPE,
    .tp_doc = "numc.Matrix objects",
    .tp_methods = Matrix61c_methods,
    .tp_members = Matrix61c_members,
    .tp_as_mapping = &Matrix61c_mapping,
    .tp_init = (initproc)Matrix61c_init,
    .tp_new = Matrix61c_new
};


struct PyModuleDef numcmodule = {
    PyModuleDef_HEAD_INIT,
    "numc",
    "Numc matrix operations",
    -1,
    Matrix61c_class_methods
};

/* Initialize the numc module */
PyMODINIT_FUNC PyInit_numc(void) {
    PyObject* m;

    if (PyType_Ready(&Matrix61cType) < 0)
        return NULL;

    m = PyModule_Create(&numcmodule);
    if (m == NULL)
        return NULL;

    Py_INCREF(&Matrix61cType);
    PyModule_AddObject(m, "Matrix", (PyObject *)&Matrix61cType);
    printf("CS61C Fall 2020 Project 4: numc imported!\n");
    fflush(stdout);
    return m;
}
