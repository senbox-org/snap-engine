/*
 * $Id: MDArray.java,v 1.1 2007/03/27 12:51:06 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.util.math;

import java.lang.reflect.Array;

//TODO make this public API

/**
 * <p><i><b>IMPORTANT NOTE:</b>
 * This class belongs to a preliminary API.
 * It is not (yet) intended to be used by clients and may change in the future.</i></p>
 */
public final class MDArray {

    /**
     * The memory layout of this array.
     */
    private final Layout _layout;
    /**
     * The Java array which holds the array elements.
     */
    private final Object _javaArray;

    /**
     * Constructs a new MDArray using the layout and multi-dimensional Java array provided by the given MDArray.
     *
     * @param mdArray the MDArray providing the layout and Java array, must not be null
     */
    public MDArray(MDArray mdArray) {
        this(mdArray.getLayout(), mdArray.getJavaArray());
    }

    /**
     * Constructs a new MDArray using the given multi-dimensional Java array. The array layout is created by the method
     * {@link #createLayout} using the supplied array.
     *
     * @param javaArray a regular, multi-dimensional Java array, must not be null
     */
    public MDArray(Object javaArray) {
        this(createLayout(javaArray), javaArray);
    }

    /**
     * Constructs a new MDArray using the given array element type and dimension sizes.
     *
     * @param elementType the array element type, must not be null
     * @param dimSizes    the dimension sizes, length must be greater zero and contain positive sizes only
     */
    public MDArray(Class elementType, int[] dimSizes) {
        this(new Layout(elementType, dimSizes), Array.newInstance(elementType, dimSizes));
    }

    /**
     * Constructs a new MDArray using the given array element type and dimension sizes.
     *
     * @param elementType the array element type, must not be null
     * @param dimSizes    the dimension sizes, length must be greater zero and contain positive sizes only
     */
    public MDArray(Class elementType, int[] dimSizes, Object flatArray) {
        this(elementType, dimSizes);
        fillFromFlatArray(flatArray);
    }

    /**
     * Constructs a new MDArray using the given layout and multi-dimensional Java array.
     *
     * @param layout    the array layout, must not be null
     * @param javaArray a regular, multi-dimensional Java array, must not be null
     */
    private MDArray(Layout layout, Object javaArray) {
        _layout = layout;
        _javaArray = javaArray;
    }

    public static Layout createLayout(Object array) {
        Class at = array.getClass();
        if (!at.isArray()) {
            throw new IllegalArgumentException("'array' is not an array");
        }

        int rank = 0;
        while (at.isArray()) {
            at = at.getComponentType();
            rank++;
        }

        final Class elementType = at;
        final int[] dimSizes = new int[rank];

        Object a = array;
        for (int i = 0; i < rank; i++) {
            dimSizes[i] = Array.getLength(a);
            if (dimSizes[i] == 0) {
                throw new IllegalArgumentException("array is not regular");
            }
            a = Array.get(a, 0);
        }

        return new Layout(elementType, dimSizes);
    }

    public Layout getLayout() {
        return _layout;
    }

    public int getRank() {
        return _layout.getRank();
    }

    public int getDimSize(int dimIndex) {
        return _layout.getDimSize(dimIndex);
    }

    public Object getJavaArray() {
        return _javaArray;
    }

    public static long getElementCount(final int[] dimSizes) {
        long size = dimSizes[0];
        for (int i = 1; i < dimSizes.length; i++) {
            size *= dimSizes[i];
        }
        return size;
    }

    /**
     * Fills the MDArray with the elements provided by the given flat Java array. Except the following conditions, the
     * flat Java array must have the same element type as this MDArray:
     * <p/>
     * <ol> <li>Element type of <code>this</code> is <code>float</code> and of <code>flatArray</code> is
     * <code>double</code></li> <li>Element type of <code>this</code> is <code>double</code> and of
     * <code>flatArray</code> is <code>float</code></li> </ol>
     *
     * @param flatArray the Java array with a flat memory layout, must not be null
     */
    public void fillFromFlatArray(Object flatArray) {
        copyFlatIntoDeep(flatArray, getJavaArray(), getLayout(), null);
    }

    public static void copyFlatIntoDeep(Object flatArray, Object deepArray) {
        copyFlatIntoDeep(flatArray, deepArray, null, null);
    }

    private static void copyFlatIntoDeep(Object flatArray,
                                         Object deepArray,
                                         Layout deepArrayLayout,
                                         ArrayCopy arrayCopy) {
        if (!flatArray.getClass().isArray()) {
            throw new IllegalArgumentException("flatArray is not an array");
        }
        if (deepArrayLayout == null) {
            deepArrayLayout = createLayout(deepArray);
        }
        if (arrayCopy == null) {
            arrayCopy = getArrayCopy(flatArray.getClass().getComponentType(),
                                     deepArrayLayout.getElementType());
        }
        copyFlatIntoDeepRecursive(flatArray,
                                  new int[1],
                                  deepArray,
                                  deepArrayLayout.getDimSizes(),
                                  0,
                                  arrayCopy);
    }

    private static void copyFlatIntoDeepRecursive(Object flatArray,
                                                  int[] offset,
                                                  Object deepArray,
                                                  int[] dimSizes,
                                                  int dimIndex,
                                                  ArrayCopy arrayCopy) {
        final int rank = dimSizes.length;
        final int size = dimSizes[dimIndex];
        if (dimIndex == rank - 1) {
            arrayCopy.copyArray(flatArray,
                                offset[0],
                                deepArray,
                                0,
                                size);
            offset[0] += size;
        } else if (dimIndex < rank - 1) {
            final Object[] array = (Object[]) deepArray;
            for (int i = 0; i < size; i++) {
                copyFlatIntoDeepRecursive(flatArray,
                                          offset,
                                          array[i],
                                          dimSizes,
                                          dimIndex + 1,
                                          arrayCopy);
            }
        } else {
            throw new IllegalArgumentException("dim >= rank");
        }
    }

    private static ArrayCopy getArrayCopy(final Class srcType, final Class destType) {
        ArrayCopy arrayCopy = SYSTEM_FAC;
        if (srcType.equals(float.class)) {
            if (destType.equals(double.class)) {
                arrayCopy = FLOAT_TO_DOUBLE_FAC;
            }
        } else if (srcType.equals(double.class)) {
            if (destType.equals(float.class)) {
                arrayCopy = DOUBLE_TO_FLOAT_FAC;
            }
        }
        return arrayCopy;
    }

    /**
     * Provides information about the layout of a multi-dimensional array.
     */
    public static class Layout {

        private final Class _elementType;
        private final int[] _dimSizes;

        /**
         * Constructs a new descriptor.
         *
         * @param elementType the element type
         * @param dimSizes    the array sizes for each array dimension
         */
        public Layout(Class elementType, int[] dimSizes) {
            final long elementCount = MDArray.getElementCount(dimSizes);
            if (elementCount < 0) {
                throw new IllegalArgumentException("invalid dimSizes: elementCount < 0");
            }
            if (elementCount > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("invalid dimSizes: elementCount > " + Integer.MAX_VALUE);
            }
            _elementType = elementType;
            _dimSizes = dimSizes;
        }

        public Class getElementType() {
            return _elementType;
        }

        public int getRank() {
            return _dimSizes.length;
        }

        public int[] getDimSizes() {
            return _dimSizes;
        }

        public int getDimSize(int dimIndex) {
            return _dimSizes[dimIndex];
        }
    }

    public interface ArrayCopy {

        /**
         * Copies an array from the specified source array, beginning at the specified position, to the specified
         * position of the destination array.
         *
         * @param src     the source array.
         * @param srcPos  starting position in the source array.
         * @param dest    the destination array.
         * @param destPos starting position in the destination data.
         * @param length  the number of array elements to be copied.
         * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
         * @throws ArrayStoreException       if an element in the <code>src</code> array could not be stored into the
         *                                   <code>dest</code> array because of a type mismatch.
         * @throws NullPointerException      if either <code>src</code> or <code>dest</code> is <code>null</code>.
         */
        void copyArray(Object src, int srcPos, Object dest, int destPos, final int length);
    }

    private final static ArrayCopy SYSTEM_FAC = new ArrayCopy() {
        public void copyArray(Object src, int srcPos, Object dest, int destPos, int length) {
            System.arraycopy(src, srcPos, dest, destPos, length);
        }
    };

    private final static ArrayCopy FLOAT_TO_DOUBLE_FAC = new ArrayCopy() {
        public void copyArray(Object src, int srcPos, Object dest, int destPos, int length) {
            float[] fSrc = (float[]) src;
            double[] dDest = (double[]) dest;
            for (int i = 0; i < length; i++) {
                dDest[destPos + i] = fSrc[srcPos + i];
            }
        }
    };

    private final static ArrayCopy DOUBLE_TO_FLOAT_FAC = new ArrayCopy() {
        public void copyArray(Object src, int srcPos, Object dest, int destPos, int length) {
            double[] dSrc = (double[]) src;
            float[] fDest = (float[]) dest;
            for (int i = 0; i < length; i++) {
                fDest[destPos + i] = (float) dSrc[srcPos + i];
            }
        }
    };
}
