package org.esa.snap.remote.products.repository;

/**
 * The data type of the pixels for a downloaded product.
 *
 * Created by jcoravu on 9/9/2019.
 */
public enum PixelType {

    UINT8(1, "Unsigned byte"),
    INT8(2, "Signed byte"),
    UINT16(3, "Unsigned short"),
    INT16(4, "Signed short"),
    UINT32(5, "Unsigned integer"),
    INT32(6, "Signed integer"),
    FLOAT32(7, "Float"),
    FLOAT64(8, "Double");

    private final int value;
    private final String name;

    PixelType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public String getName() { return this.name; }

    public Integer getValue() { return this.value; }
}
