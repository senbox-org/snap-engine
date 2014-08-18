package org.esa.beam.dataio;

import org.esa.beam.dataio.s3.Sentinel3ProductReaderPlugIn;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.StopWatch;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author Tonio Fincke
 */
public class SpeedTester_2 {

    private static String directory = "C:\\Users\\tonio\\Desktop\\Produkte\\Sentinel-3-updated";

    public static void main(String[] args) throws IOException {
        String[] productDirNames = new String[] {
                "S3A_OL_1_EFR____20130621T100921_20130621T101417_20140613T170503_0295_001_002______LN2_D_NR____.SEN3",
                "S3A_OL_1_ERR____20130621T100921_20130621T101417_20140613T170503_0295_001_002______LN2_D_NR____.SEN3",
                "S3A_OL_2_LFR____20070425T152940_20070425T153025_20140610T111104_0045_000_000______LN2_D_NR____.SEN3",
                "S3A_OL_2_LRR____20070910T095130_20070910T095442_20140610T110326_0192_000_000______LN2_D_NR____.SEN3",
                "S3A_OL_2_WFR____20070425T152940_20070425T153025_20140610T112151_0045_000_000______MAR_D_NR____.SEN3",
                "S3A_OL_2_WRR____20070910T095130_20070910T095442_20140610T110535_0192_000_000______MAR_D_NR____.SEN3",
                "S3A_SL_1_RBT____20130621T100932_20130621T101146_20140612T070359_0133_001_002______LN1_D_NR____.SEN3",
                "S3A_SL_2_LST____20130621T101013_20130621T101053_20140613T155102_0039_009_022______MAR_O_NR____.SEN3",
                "S3A_SL_2_WST____20130621T101013_20130621T101053_20140613T135758_0039_009_022______MAR_O_NR____.SEN3",
                "S3A_SY_2_SYN____20130621T100932_20130621T101146_20140604T091546_0134_001_002______LN1_D_NC____.SEN3",
                "S3A_SY_2_VG1____20130621T100922_20130621T104922_20140527T011902_GLOBAL____________LN2_D_NR____.SEN3",
                "S3A_SY_2_VGP____20130621T100932_20130621T101146_20140604T091546_0134_001_002______LN1_D_NC____.SEN3"};
        for (String productDirName : productDirNames) {
            printTimeRequiredForReadingProduct(productDirName);
        }
        FileUtils.deleteTree(new File(directory + File.separator + "test"));
    }

    private static void printTimeRequiredForReadingProduct(String productDirName) throws IOException {
        final File file = new File(directory + File.separator + productDirName + File.separator + "xfdumanifest.xml");
        final ProductReader productReader = new Sentinel3ProductReaderPlugIn().createReaderInstance();
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final Product product = productReader.readProductNodes(file, null);
        stopWatch.stop();
        System.out.println("Time required for reading of " + productDirName + " = " + stopWatch.getTimeDiffString());
        final File outputFile = new File(directory + File.separator + "test" + File.separator + productDirName);
        stopWatch.start();
        ProductIO.writeProduct(product, outputFile, "BEAM-DIMAP", false);
        stopWatch.stop();
        System.out.println("Time required for writing of " + productDirName + " = " + stopWatch.getTimeDiffString());
        product.dispose();
    }

}
