package org.esa.s3tbx.dataio;

import org.esa.s3tbx.dataio.s3.Sentinel3ProductReaderPlugIn;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.StopWatch;
import org.esa.snap.core.util.io.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Tonio Fincke
 */
public class SpeedTester_2 {

    private static String directory = "C:\\Users\\tonio\\Desktop\\Produkte\\Sentinel-3-updated";
    private static int number_of_iterations = 15;

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
        final File performancesFile = new File("performances.csv");
        final BufferedWriter writer = new BufferedWriter(new FileWriter(performancesFile));
        for (String productDirName : productDirNames) {
            printTimesRequiredForReadingProduct(productDirName, writer);
        }
//        for (String productDirName : productDirNames) {
//            printTimesRequiredForWritingProduct(productDirName, writer);
//        }
        writer.close();
        FileUtils.deleteTree(new File(directory + File.separator + "test"));
    }

    private static void printTimesRequiredForReadingProduct(String productDirName, BufferedWriter writer) throws IOException {
        writer.write("Reading " + productDirName + "\n");
        writer.write("Run;Required time in s;Pixels per s\n");
        final StopWatch stopWatch = new StopWatch();
        double totalTime = 0;
        double size = 0;
        for(int i = 0; i < number_of_iterations; i++) {
            final File file = new File(directory + File.separator + productDirName + File.separator + "xfdumanifest.xml");
            final ProductReader productReader = new Sentinel3ProductReaderPlugIn().createReaderInstance();
            stopWatch.start();
            final Product product = productReader.readProductNodes(file, null);
            stopWatch.stop();
            double time = ((double)stopWatch.getTimeDiff()) / 1000;
            totalTime += time;
            size = product.getSceneRasterWidth() * product.getSceneRasterHeight();
            productReader.close();
            product.dispose();
            writer.write((i + 1) + ";" + time + ";" + (size / time) + "\n");
        }
        totalTime /= number_of_iterations;
        writer.write("Averaged;" + totalTime + ";" + (size / totalTime) + "\n");
        System.out.println("Average time required for reading of " + productDirName + " = " + totalTime + " s");
        System.out.println("Average time required for reading one pixel of " + productDirName + " = " +
                                   (size / totalTime) + " s");
    }

    private static void printTimesRequiredForWritingProduct(String productDirName, BufferedWriter writer) throws IOException {
        writer.write("Writing " + productDirName + "\n");
        writer.write("Run;Required time in s;Pixels per s\n");
        final File file = new File(directory + File.separator + productDirName + File.separator + "xfdumanifest.xml");
        final ProductReader productReader = new Sentinel3ProductReaderPlugIn().createReaderInstance();
        final Product product = productReader.readProductNodes(file, null);
        final StopWatch stopWatch = new StopWatch();
        final File outputFile = new File(directory + File.separator + "test" + File.separator + productDirName);
        double size = product.getSceneRasterWidth() * product.getSceneRasterHeight();
        double totalTime = 0;
        for(int i = 0; i < number_of_iterations; i++) {
            stopWatch.start();
            ProductIO.writeProduct(product, outputFile, "BEAM-DIMAP", false);
            stopWatch.stop();
            outputFile.delete();
            double time = ((double)stopWatch.getTimeDiff())  / 1000;
            writer.write((i + 1) + ";" + time + ";" + (size / time) + "\n");
            totalTime += time;
        }
        totalTime /= number_of_iterations;
        writer.write("Averaged;" + totalTime + ";" + (size / totalTime) + "\n");
        System.out.println("Average time required for writing of " + productDirName + " = " + totalTime + " s");
        System.out.println("Average time required for writing one pixel of " + productDirName + " = " +
                                   (size / totalTime) + " s");
        product.dispose();
    }

}
