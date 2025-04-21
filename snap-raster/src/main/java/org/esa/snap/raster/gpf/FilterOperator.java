/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.raster.gpf;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ConvolutionFilterBand;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.GeneralFilterBand;
import org.esa.snap.core.datamodel.Kernel;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;

/**
 * image filter operator
 */
@OperatorMetadata(alias = "Image-Filter",
        category = "Raster",
        authors = "Norman Fomferra, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2015 by Array Systems Computing Inc.",
        description = "Common Image Processing Filters")
public class FilterOperator extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;

    @Parameter
    private String selectedFilterName = null;

    @Parameter(description = "The kernel file", label = "Kernel File")
    private File userDefinedKernelFile = null;

    private final static Map<String, Filter> filterMap = new HashMap<>();

    private static final String PRODUCT_SUFFIX = "_Flt";

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public FilterOperator() {
        populateFilterMap(LINE_DETECTION_FILTERS);
        populateFilterMap(GRADIENT_DETECTION_FILTERS);
        populateFilterMap(SMOOTHING_FILTERS);
        populateFilterMap(SHARPENING_FILTERS);
        populateFilterMap(LAPLACIAN_FILTERS);
        populateFilterMap(NON_LINEAR_FILTERS);
        populateFilterMap(MORPHOLOGY_FILTERS);
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link Product} annotated with the
     * {@link TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {
        ensureSingleRasterSize(sourceProduct);
        if (userDefinedKernelFile == null && selectedFilterName == null) {
            // we cannot throw exception here because, in this case the GraphBuilder is not working anymore
//            throw new OperatorException("Parameter 'userDefinedKernelFile' or 'selectedFilterName' must be specified");
            // instead we log an error
            SystemUtils.LOG.severe("Parameter 'userDefinedKernelFile' or 'selectedFilterName' must be specified");
        }
        if (sourceBandNames == null || sourceBandNames.length == 0) {
            // we cannot throw exception here because, in this case the GraphBuilder is not working anymore
//            throw new OperatorException("At least one source band must be selected");
            // instead we log an error
            SystemUtils.LOG.severe(String.format("[%s] At least one source band must be selected", getId()));
        }

        Filter selectedFilter = null;
        if (userDefinedKernelFile != null) {
            try {
                selectedFilter = getUserDefinedFilter(userDefinedKernelFile);
            } catch (IOException ioe) {
                // we cannot throw exception here because, in this case the GraphBuilder is not working anymore
//                throw new OperatorException(String.format("Could not load user defined kernel from %s", userDefinedKernelFile), ioe);
                // instead we log an error
                SystemUtils.LOG.log(Level.SEVERE, String.format("Could not load user defined kernel from %s", userDefinedKernelFile), ioe);
            }
        } else {
            selectedFilter = filterMap.get(selectedFilterName);
            if (selectedFilter == null) {
                // we cannot throw exception here because, in this case the GraphBuilder is not working anymore
//                throw new OperatorException(String.format("Specified filter name '%s' is unknown", selectedFilterName));
                // instead we log an error
                SystemUtils.LOG.log(Level.SEVERE, String.format("Specified filter name '%s' is unknown", selectedFilterName));
            }
        }

        targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());
        if (selectedFilter == null) {
            return;
        }
        final Band[] sourceBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames, true);

        try {
            for (Band srcBand : sourceBands) {
                targetProduct.addBand(createFilterBand(selectedFilter, srcBand.getName(), srcBand));
            }
            ProductUtils.copyProductNodes(sourceProduct, targetProduct);
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private static Band createFilterBand(Filter filter, String bandName, Band band) {
        FilterBand filterBand;
        if (filter instanceof KernelFilter) {
            final KernelFilter kernelFilter = (KernelFilter) filter;
            filterBand = new ConvolutionFilterBand(bandName, band, kernelFilter.kernel, 1);
        } else {
            final GeneralFilter generalFilter = (GeneralFilter) filter;
            filterBand = new GeneralFilterBand(bandName, band, generalFilter.operator, getKernel(generalFilter), 1);
        }
        final Band targetBand = new Band(bandName, ProductData.TYPE_FLOAT32, band.getRasterWidth(), band.getRasterHeight());
        targetBand.setUnit(band.getUnit());
        targetBand.setNoDataValueUsed(filterBand.isNoDataValueUsed());
        targetBand.setNoDataValue(filterBand.getNoDataValue());
        targetBand.setValidPixelExpression(band.getValidPixelExpression());
        ProductUtils.copySpectralBandProperties(band, targetBand);
        targetBand.setDescription(String.format("Filter ''%s''", filter));
        targetBand.setSourceImage(filterBand.getSourceImage());
        return targetBand;
    }

    private static Kernel getKernel(GeneralFilter filter) {
        final double[] kernelData = new double[filter.width * filter.height];
        Arrays.fill(kernelData, 1.0);
        return new Kernel(filter.width,
                          filter.height,
                          filter.width / 2,
                          filter.height / 2,
                          1.0,
                          kernelData);
    }

    private static KernelFilter getUserDefinedFilter(File userDefinedKernelFile) throws IOException {
        FileInputStream fis = new FileInputStream(userDefinedKernelFile);
        final float[][] kernelData = readFile(fis, userDefinedKernelFile.getName());
        final int filterWidth = kernelData.length;
        final int filterHeight = kernelData[0].length;
        final double[] data = new double[filterWidth * filterHeight];
        int k = 0;
        for (int r = 0; r < filterHeight; r++) {
            for (int c = 0; c < filterWidth; c++) {
                data[k++] = kernelData[r][c];
            }
        }
        return new KernelFilter("User Defined Filter", new Kernel(filterWidth, filterHeight, data));
    }

    /**
     * Read data from kernel file and save them in a 2D array.
     *
     * @param fileName The kernel file name
     * @return array The 2D array holding kernel data
     */
    public static float[][] readFile(final InputStream stream, final String fileName) {

        // get reader
        final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        // read data from file and save them in 2-D array
        String line;
        StringTokenizer st;
        float[][] array;
        int rowIdx = 0;

        try {
            // get the 1st line
            if ((line = reader.readLine()) == null) {
                throw new OperatorException("Empty file: " + fileName);
            }

            st = new StringTokenizer(line);
            if (st.countTokens() != 2) {
                throw new OperatorException("Incorrect file format: " + fileName);
            }

            final int numRows = Integer.parseInt(st.nextToken());
            final int numCols = Integer.parseInt(st.nextToken());
            array = new float[numRows][numCols];

            // get the rest numRows lines
            while ((line = reader.readLine()) != null) {

                st = new StringTokenizer(line);
                if (st.countTokens() != numCols) {
                    throw new OperatorException("Incorrect file format: " + fileName);
                }

                for (int j = 0; j < numCols; j++) {
                    array[rowIdx][j] = Float.parseFloat(st.nextToken());
                }
                rowIdx++;
            }

            if (numRows != rowIdx) {
                throw new OperatorException("Incorrect number of lines in file: " + fileName);
            }

            reader.close();
            stream.close();

        } catch (IOException e) {
            throw new OperatorException(e);
        }
        return array;
    }

    public static abstract class Filter {

        private final String name;

        public Filter(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public abstract boolean equals(Object obj);
    }

    private static class KernelFilter extends Filter {

        private final Kernel kernel;

        public KernelFilter(String name, Kernel kernel) {
            super(name);
            this.kernel = kernel;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof KernelFilter) {
                final KernelFilter other = (KernelFilter) obj;
                return toString().equals(other.toString()) && kernel.equals(other.kernel);
            }
            return false;
        }
    }

    private static class GeneralFilter extends Filter {

        final int width;
        final int height;
        final GeneralFilterBand.OpType operator;

        public GeneralFilter(String name, int width, int height, GeneralFilterBand.OpType operator) {
            super(name);
            this.width = width;
            this.height = height;
            this.operator = operator;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof GeneralFilter) {
                final GeneralFilter other = (GeneralFilter) obj;
                return toString().equals(other.toString()) && operator == other.operator;
            }
            return false;
        }
    }


    public static final Filter[] LINE_DETECTION_FILTERS = {
            new KernelFilter("Horizontal Edges", new Kernel(3, 3, new double[]{
                    -1, -1, -1,
                    +2, +2, +2,
                    -1, -1, -1
            })),
            new KernelFilter("Vertical Edges", new Kernel(3, 3, new double[]{
                    -1, +2, -1,
                    -1, +2, -1,
                    -1, +2, -1
            })),
            new KernelFilter("Left Diagonal Edges", new Kernel(3, 3, new double[]{
                    +2, -1, -1,
                    -1, +2, -1,
                    -1, -1, +2
            })),
            new KernelFilter("Right Diagonal Edges", new Kernel(3, 3, new double[]{
                    -1, -1, +2,
                    -1, +2, -1,
                    +2, -1, -1
            })),

            new KernelFilter("Compass Edge Detector", new Kernel(3, 3, new double[]{
                    -1, +1, +1,
                    -1, -2, +1,
                    -1, +1, +1,
            })),

            new KernelFilter("Diagonal Compass Edge Detector", new Kernel(3, 3, new double[]{
                    +1, +1, +1,
                    -1, -2, +1,
                    -1, -1, +1,
            })),

            new KernelFilter("Roberts Cross North-West", new Kernel(2, 2, new double[]{
                    +1, 0,
                    0, -1,
            })),

            new KernelFilter("Roberts Cross North-East", new Kernel(2, 2, new double[]{
                    0, +1,
                    -1, 0,
            })),
    };
    public static final Filter[] GRADIENT_DETECTION_FILTERS = {
            new KernelFilter("Sobel North", new Kernel(3, 3, new double[]{
                    -1, -2, -1,
                    +0, +0, +0,
                    +1, +2, +1,
            })),
            new KernelFilter("Sobel South", new Kernel(3, 3, new double[]{
                    +1, +2, +1,
                    +0, +0, +0,
                    -1, -2, -1,
            })),
            new KernelFilter("Sobel West", new Kernel(3, 3, new double[]{
                    -1, 0, +1,
                    -2, 0, +2,
                    -1, 0, +1,
            })),
            new KernelFilter("Sobel East", new Kernel(3, 3, new double[]{
                    +1, 0, -1,
                    +2, 0, -2,
                    +1, 0, -1,
            })),
            new KernelFilter("Sobel North East", new Kernel(3, 3, new double[]{
                    +0, -1, -2,
                    +1, +0, -1,
                    +2, +1, -0,
            })),
    };
    public static final Filter[] SMOOTHING_FILTERS = {
            new KernelFilter("Arithmetic 3x3 Mean", new Kernel(3, 3, 1.0 / 9.0, new double[]{
                    +1, +1, +1,
                    +1, +1, +1,
                    +1, +1, +1,
            })),

            new KernelFilter("Arithmetic 4x4 Mean", new Kernel(4, 4, 1.0 / 16.0, new double[]{
                    +1, +1, +1, +1,
                    +1, +1, +1, +1,
                    +1, +1, +1, +1,
                    +1, +1, +1, +1,
            })),

            new KernelFilter("Arithmetic 5x5 Mean", new Kernel(5, 5, 1.0 / 25.0, new double[]{
                    +1, +1, +1, +1, +1,
                    +1, +1, +1, +1, +1,
                    +1, +1, +1, +1, +1,
                    +1, +1, +1, +1, +1,
                    +1, +1, +1, +1, +1,
            })),

            new KernelFilter("Low-Pass 3x3", new Kernel(3, 3, 1.0 / 16.0, new double[]{
                    +1, +2, +1,
                    +2, +4, +2,
                    +1, +2, +1,
            })),
            new KernelFilter("Low-Pass 5x5", new Kernel(5, 5, 1.0 / 60.0, new double[]{
                    +1, +1, +1, +1, +1,
                    +1, +4, +4, +4, +1,
                    +1, +4, 12, +4, +1,
                    +1, +4, +4, +4, +1,
                    +1, +1, +1, +1, +1,
            })),
    };
    public static final Filter[] SHARPENING_FILTERS = {
            new KernelFilter("High-Pass 3x3 #1", new Kernel(3, 3, new double[]{
                    -1, -1, -1,
                    -1, +9, -1,
                    -1, -1, -1
            })),


            new KernelFilter("High-Pass 3x3 #2", new Kernel(3, 3, new double[]{
                    +0, -1, +0,
                    -1, +5, -1,
                    +0, -1, +0
            })),

            new KernelFilter("High-Pass 5x5", new Kernel(5, 5, new double[]{
                    +0, -1, -1, -1, +0,
                    -1, +2, -4, +2, -1,
                    -1, -4, 13, -4, -1,
                    -1, +2, -4, +2, -1,
                    +0, -1, -1, -1, +0,
            })),

    };
    public static final Filter[] LAPLACIAN_FILTERS = {
            new KernelFilter("Laplace 3x3", new Kernel(3, 3, new double[]{
                    +0, -1, +0,
                    -1, +4, -1,
                    +0, -1, +0,
            })),
            new KernelFilter("Laplace 3x3 (b)", new Kernel(3, 3, new double[]{
                    -1, -1, -1,
                    -1, +8, -1,
                    -1, -1, -1,
            })),
            new KernelFilter("Laplace 5x5", new Kernel(5, 5, new double[]{
                    +1, +1, +1, +1, +1,
                    +1, +1, +1, +1, +1,
                    +1, +1, 24, +1, +1,
                    +1, +1, +1, +1, +1,
                    +1, +1, +1, +1, +1,
            })),
            new KernelFilter("Laplace 5x5 (b)", new Kernel(5, 5, new double[]{
                    -1, -1, -1, -1, -1,
                    -1, -1, -1, -1, -1,
                    -1, -1, 24, -1, -1,
                    -1, -1, -1, -1, -1,
                    -1, -1, -1, -1, -1,
            })),
    };

    public static final Filter[] NON_LINEAR_FILTERS = {
            new GeneralFilter("Minimum 3x3", 3, 3, GeneralFilterBand.OpType.MIN),
            new GeneralFilter("Minimum 5x5", 5, 5, GeneralFilterBand.OpType.MIN),
            new GeneralFilter("Maximum 3x3", 3, 3, GeneralFilterBand.OpType.MAX),
            new GeneralFilter("Maximum 5x5", 5, 5, GeneralFilterBand.OpType.MAX),
            new GeneralFilter("Mean 3x3", 3, 3, GeneralFilterBand.OpType.MEAN),
            new GeneralFilter("Mean 5x5", 5, 5, GeneralFilterBand.OpType.MEAN),
            new GeneralFilter("Median 3x3", 3, 3, GeneralFilterBand.OpType.MEDIAN),
            new GeneralFilter("Median 5x5", 5, 5, GeneralFilterBand.OpType.MEDIAN),
            new GeneralFilter("Standard Deviation 3x3", 3, 3, GeneralFilterBand.OpType.STDDEV),
            new GeneralFilter("Standard Deviation 5x5", 5, 5, GeneralFilterBand.OpType.STDDEV),
            new GeneralFilter("Standard Deviation 7x7", 7, 7, GeneralFilterBand.OpType.STDDEV),
    };

    public static final Filter[] MORPHOLOGY_FILTERS = {
            new GeneralFilter("Erosion 3x3", 3, 3, GeneralFilterBand.OpType.EROSION),
            new GeneralFilter("Erosion 5x5", 5, 5, GeneralFilterBand.OpType.EROSION),
            new GeneralFilter("Erosion 7x7", 7, 7, GeneralFilterBand.OpType.EROSION),
            new GeneralFilter("Dilation 3x3", 3, 3, GeneralFilterBand.OpType.DILATION),
            new GeneralFilter("Dilation 5x5", 5, 5, GeneralFilterBand.OpType.DILATION),
            new GeneralFilter("Dilation 7x7", 7, 7, GeneralFilterBand.OpType.DILATION),
            new GeneralFilter("Opening 3x3", 3, 3, GeneralFilterBand.OpType.OPENING),
            new GeneralFilter("Opening 5x5", 5, 5, GeneralFilterBand.OpType.OPENING),
            new GeneralFilter("Opening 7x7", 7, 7, GeneralFilterBand.OpType.OPENING),
            new GeneralFilter("Closing 3x3", 3, 3, GeneralFilterBand.OpType.CLOSING),
            new GeneralFilter("Closing 5x5", 5, 5, GeneralFilterBand.OpType.CLOSING),
            new GeneralFilter("Closing 7x7", 7, 7, GeneralFilterBand.OpType.CLOSING),
    };

    private static void populateFilterMap(Filter[] filters) {
        for (Filter f : filters) {
            filterMap.put(f.toString(), f);
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(FilterOperator.class);
        }
    }
}
