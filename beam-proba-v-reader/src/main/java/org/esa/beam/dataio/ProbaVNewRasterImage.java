package org.esa.beam.dataio;

import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.RasterDataNodeOpImage;
import org.esa.beam.jai.ResolutionLevel;

import java.awt.*;
import java.io.IOException;

/**
 * Proba-V raster data node implementation
 *
 * @author olafd
 */
public class ProbaVNewRasterImage extends RasterDataNodeOpImage {

    private final int dataset_id;
    private final int dataspace_id;

    public ProbaVNewRasterImage(Band outputBand, ResolutionLevel level, int dataset_id, int dataspace_id) {
        super(outputBand, level);
        this.dataset_id = dataset_id;

        this.dataspace_id = dataspace_id;
    }


    @Override
    protected void computeProductData(ProductData outputData, Rectangle region) throws IOException {

        // dataspaces: https://www.hdfgroup.org/HDF5/doc/UG/UG_frame12Dataspaces.html

        try {

            final long[] offset = {region.x, region.y};
            final long[] count = {region.width, region.height};

            final int memspace_id = H5.H5Screate_simple(count.length, // Number of dimensions of dataspace.
                                                        count,        // An array of the size of each dimension.
                                                        null);       // An array of the maximum size of each dimension. // todo: why null?

            H5.H5Sselect_hyperslab(dataspace_id,                   // Identifier of dataspace selection to modify
                                   HDF5Constants.H5S_SELECT_SET,   // Operation to perform on current selection.
                                   offset,                         // Offset of start of hyperslab
                                   null,                           // Hyperslab stride.   todo: why null?
                                   count,                          // Number of blocks included in hyperslab.
                                   null);                          // Size of block in hyperslab.  todo: why null?

            byte[][] outdata = new byte[region.width ][ region.height];
            if (dataset_id >= 0) {
                H5.H5Dread(dataset_id,                    // Identifier of the dataset read from.
                           HDF5Constants.H5T_NATIVE_UINT8,  // Identifier of the memory datatype.   todo: set the right one
                           memspace_id,                   //  Identifier of the memory dataspace.
                           dataspace_id,                  // Identifier of the dataset's dataspace in the file.
                           HDF5Constants.H5P_DEFAULT,     // Identifier of a transfer property list for this I/O operation.
//                        outputData.getElems());        // Buffer to store data read from the file.
                           outdata);        // Buffer to store data read from the file.
                System.out.println();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
