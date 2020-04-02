package org.esa.snap.dataio.netcdf;

import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PartialDataCopierTest_1D {

    @Test
    public void copyIntFromCenter() throws InvalidRangeException {
        final Array source = createArray(new int[]{10}, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
        final Array target = createArray(new int[]{3}, new int[]{0, 0, 0});

        final int[] offset = {3};
        PartialDataCopier.copy(offset, source, target);

        assertThat(target.getStorage(), is(equalTo(new int[]{3, 4, 5})));
    }

    @Test
    public void copyLeftBorder() throws InvalidRangeException {
        final Array source = createArray(new int[]{10}, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
        final Array target = createArray(new int[]{3}, new int[]{-1, -1, -1});

        final int[] offset = {0};
        PartialDataCopier.copy(offset, source, target);

        assertThat(target.getStorage(), is(equalTo(new int[]{0, 1, 2})));
    }


    @Test
    public void copyLeftOutsideBorder() throws InvalidRangeException {
        final Array source = createArray(new int[]{10}, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
        final Array target = createArray(new int[]{3}, new int[]{-1, -1, -1});

        final int[] offset = {-1};
        PartialDataCopier.copy(offset, source, target);

        assertThat(target.getStorage(), is(equalTo(new int[]{-1, 0, 1})));
    }

    @Test
    public void copyRightBorder() throws InvalidRangeException {
        final Array source = createArray(new int[]{10}, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
        final Array target = createArray(new int[]{3}, new int[]{0, 0, 0});

        final int[] offset = {7};
        PartialDataCopier.copy(offset, source, target);

        assertThat(target.getStorage(), is(equalTo(new int[]{7, 8, 9})));
    }


    @Test
    public void copyRightOutsideBorder() throws InvalidRangeException {
        final Array source = createArray(new int[]{10}, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
        final Array target = createArray(new int[]{3}, new int[]{0, 0, 0});

        final int[] offset = {8};
        PartialDataCopier.copy(offset, source, target);

        assertThat(target.getStorage(), is(equalTo(new int[]{8, 9, 0})));
    }

    private Array createArray(int[] shape, int[] storage) {
        return Array.factory(DataType.INT, new int[]{storage.length}, storage).reshape(shape);
    }
}