package org.esa.snap.dataio.netcdf;

import com.bc.ceres.core.runtime.internal.IOHelper;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfNetCdfReaderPlugIn;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import org.junit.Test;
import ucar.nc2.NetcdfFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

/**
 * @author Norman Fomferra
 */
public class S3L2PPTest {
    @Test
    public void testSYN() throws Exception {
        String path = System.getProperty("s3l2pp.synpath");
        if (path == null) {
            return;
        }

        final Collection<Path> ncFiles = IOHelper.listFiles(Path.of(path), p -> p.getFileName().toString().endsWith(".nc"));
        if(ncFiles == null){
            return;
        }

        for (Path ncFile : ncFiles) {
            //ncdump(ncFile);

            try {
                final ProductReader reader = new CfNetCdfReaderPlugIn().createReaderInstance();
                final Product product = reader.readProductNodes(ncFile.toString(), null);
                Band[] bands = product.getBands();
                System.out.println("product = " + product.getName());
                for (int i = 0; i < bands.length; i++) {
                    Band band = bands[i];
                    System.out.println("bands[" + i + "] = " + band.getName());
                }
            } catch (IOException e) {
                System.err.println("error: " + e.getMessage());
            }
        }
    }

    private static void ncdump(Path ncFile) throws IOException {
        NetcdfFile netcdfFile = NetcdfFileOpener.open("file:" + ncFile.toString());
        System.out.println(netcdfFile);
        netcdfFile.close();
    }
}
