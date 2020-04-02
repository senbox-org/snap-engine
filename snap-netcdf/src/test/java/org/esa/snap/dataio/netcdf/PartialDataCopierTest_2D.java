package org.esa.snap.dataio.netcdf;

import org.junit.Before;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class PartialDataCopierTest_2D {

    private Array source;
    private Array chunk;

    @Before
    public void setUp() {

        int[] sourceData = {
                0, 1, 2, 3, 4,
                5, 6, 7, 8, 9,
                10, 11, 12, 13, 14,
                15, 16, 17, 18, 19
        };
        source = Array.factory(DataType.INT, new int[]{sourceData.length}, sourceData).reshape(new int[]{4, 5});

        int[] chunkData = {
                40, 40, 40,
                40, 40, 40
        };
        chunk = Array.factory(DataType.INT, new int[]{chunkData.length}, chunkData).reshape(new int[]{2, 3});
    }

    @Test
    public void read2DArraySection_case_inside_center() throws InvalidRangeException {
        final int[] from = {1, 1};

        PartialDataCopier.copy(from, source, chunk);

        assertThat((int[]) chunk.copyTo1DJavaArray(), equalTo(new int[]{
                6, 7, 8,
                11, 12, 13
        }));
    }

    @Test
    public void read2DArraySection_case_inside_UpperLeft() throws InvalidRangeException {
        final int[] from = {0, 0};

        PartialDataCopier.copy(from, source, chunk);

        assertThat((int[]) chunk.copyTo1DJavaArray(), equalTo(new int[]{
                0, 1, 2,
                5, 6, 7
        }));
    }

    @Test
    public void read2DArraySection_case_inside_UpperCenter() throws InvalidRangeException {
        final int[] from = {0, 1};

        PartialDataCopier.copy(from, source, chunk);

        assertThat((int[]) chunk.copyTo1DJavaArray(), equalTo(new int[]{
                1, 2, 3,
                6, 7, 8
        }));
    }

    @Test
    public void read2DArraySection_case_inside_UpperRight() throws InvalidRangeException {
        final int[] from = {0, 2};

        PartialDataCopier.copy(from, source, chunk);

        assertThat((int[]) chunk.copyTo1DJavaArray(), equalTo(new int[]{
                2, 3, 4,
                7, 8, 9
        }));
    }

    @Test
    public void read2DArraySection_case_inside_CenterRight() throws InvalidRangeException {
        final int[] from = {1, 2};

        PartialDataCopier.copy(from, source, chunk);

        assertThat((int[]) chunk.copyTo1DJavaArray(), equalTo(new int[]{
                7, 8, 9,
                12, 13, 14
        }));
    }

    @Test
    public void read2DArraySection_case_inside_LowerRight() throws InvalidRangeException {
        final int[] from = {2, 2};

        PartialDataCopier.copy(from, source, chunk);

        assertThat((int[]) chunk.copyTo1DJavaArray(), equalTo(new int[]{
                12, 13, 14,
                17, 18, 19
        }));
    }

    @Test
    public void read2DArraySection_case_inside_LowerCenter() throws InvalidRangeException {
        final int[] from = {2, 1};

        PartialDataCopier.copy(from, source, chunk);

        assertThat((int[]) chunk.copyTo1DJavaArray(), equalTo(new int[]{
                11, 12, 13,
                16, 17, 18
        }));
    }

    @Test
    public void read2DArraySection_case_inside_LowerLeft() throws InvalidRangeException {
        final int[] from = {2, 0};

        PartialDataCopier.copy(from, source, chunk);

        assertThat((int[]) chunk.copyTo1DJavaArray(), equalTo(new int[]{
                10, 11, 12,
                15, 16, 17
        }));
    }

    @Test
    public void read2DArraySection_case_inside_CenterLeft() throws InvalidRangeException {
        final int[] from = {1, 0};

        PartialDataCopier.copy(from, source, chunk);

        assertThat((int[]) chunk.copyTo1DJavaArray(), equalTo(new int[]{
                5, 6, 7,
                10, 11, 12
        }));
    }

    @Test
    public void read2DArraySection_case_outside_UpperLeft() throws InvalidRangeException {
        final int[] from = {-1, -1};

        PartialDataCopier.copy(from, source, chunk);

        assertThat((int[]) chunk.copyTo1DJavaArray(), equalTo(new int[]{
                40, 40, 40,
                40, 0, 1
        }));
    }

    @Test
    public void read2DArraySection_case_outside_UpperCenter() throws InvalidRangeException {
        final int[] from = {-1, 1};

        PartialDataCopier.copy(from, source, chunk);

        assertThat((int[]) chunk.copyTo1DJavaArray(), equalTo(new int[]{
                40, 40, 40,
                1, 2, 3
        }));
    }

    @Test
    public void read2DArraySection_case_outside_UpperRight() throws InvalidRangeException {
        final int[] from = {-1, 3};

        PartialDataCopier.copy(from, source, chunk);

        assertThat((int[]) chunk.copyTo1DJavaArray(), equalTo(new int[]{
                40, 40, 40,
                3, 4, 40
        }));
    }

    @Test
    public void read2DArraySection_case_outside_CenterRight() throws InvalidRangeException {
        final int[] from = {1, 3};

        PartialDataCopier.copy(from, source, chunk);

        assertThat((int[]) chunk.copyTo1DJavaArray(), equalTo(new int[]{
                8, 9, 40,
                13, 14, 40
        }));
    }

    @Test
    public void read2DArraySection_case_outside_LowerRight() throws InvalidRangeException {
        final int[] from = {3, 3};

        PartialDataCopier.copy(from, source, chunk);

        assertThat((int[]) chunk.copyTo1DJavaArray(), equalTo(new int[]{
                18, 19, 40,
                40, 40, 40
        }));
    }

    @Test
    public void read2DArraySection_case_outside_LowerCenter() throws InvalidRangeException {
        final int[] from = {3, 1};

        PartialDataCopier.copy(from, source, chunk);

        assertThat((int[]) chunk.copyTo1DJavaArray(), equalTo(new int[]{
                16, 17, 18,
                40, 40, 40
        }));
    }

    @Test
    public void read2DArraySection_case_outside_LowerLeft() throws InvalidRangeException {
        final int[] from = {3, -1};

        PartialDataCopier.copy(from, source, chunk);

        assertThat((int[]) chunk.copyTo1DJavaArray(), equalTo(new int[]{
                40, 15, 16,
                40, 40, 40
        }));
    }

    @Test
    public void read2DArraySection_case_outside_CenterLeft() throws InvalidRangeException {
        final int[] from = {1, -1};

        PartialDataCopier.copy(from, source, chunk);

        assertThat((int[]) chunk.copyTo1DJavaArray(), equalTo(new int[]{
                40, 5, 6,
                40, 10, 11
        }));
    }
}