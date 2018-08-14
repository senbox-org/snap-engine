package org.esa.snap.dataio.netcdf.nc;


import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class WriterTest
{

    @Ignore
    @Test
    public  void testWriter() throws IOException {
        Product product;
        Product winProduct;
        //File inputFile = new File ("/home/roman/second/FIRE-CCI/ESACCI-LC-L4-LCCS-Map-300m-P1Y-1994-WESTERN_EUROPE_AND_MEDITERRANEAN-v2.0.7.nc");
        //File inputFile = new File ("/home/roman/data/spark_test_dataset/robust_testcase.nc");
        File inputFile = new File ("/home/roman/second/test_case.nc");
        //test fail1
        //File inputFile = new File ("/home/roman/second/netcdfTester/applications/generic_reader/NetCDF/NetCDF-CF/MER_FR__1_test.N1_C2IOP.nc");
        //test fail2
        //File inputFile = new File ("/home/roman/second/netcdfTester/applications/generic_reader/NetCDF/NetCDF-CF/LM32170241982254XXX01.nc");
        //test fail3
        //File inputFile = new File ("/home/roman/second/netcdfTester/applications/generic_reader/NetCDF/NetCDF4-CF/L5043033_03319950627.nc");
        //test fail4
        //File inputFile = new File ("/home/roman/second/netcdfTester/applications/generic_reader/NetCDF/NetCDF4-CF/MER_RR__1PPBCM20100101_213129_000003972085_00358_40993_0096.N1_C2IOP.nc");
        //test fail5
        //File inputFile = new File ("/home/roman/second/netcdfTester/applications/generic_reader/NetCDF/NetCDF-BEAM/MER_FRS_1PNMAP20090808_073618_000001632081_00264_38895_0001.N1_C2IOP.nc");
        //test fail6
        //File inputFile = new File ("/home/roman/second/netcdfTester/applications/generic_reader/NetCDF/NetCDF4-BEAM/MER_FR__1PNUPA20030808_073810_000000982018_00450_07518_6007.N1_C2IOP.nc");
        //test fail7
        //File inputFile = new File ("/home/roman/second/netcdfTester/applications/generic_reader/NetCDF/NetCDF-BEAM/LM11870291976166ESA00.nc");

        product = ProductIO.readProduct(inputFile);

        //assertNotNull("product must be not null", product);



        Map<String, Object> parameterMap = new HashMap<>() ;
        //parameterMap.put("region","1000,1000,1000,1000");
        //Product output= GPF.createProduct("Subset", parameterMap, product);
        ProductIO.writeProduct(product, "/home/roman/second/Def0.nc","NetCDF4-BEAM");

        //ProductIO.writeProduct(product, "/home/roman/second/FIRE-CCI/output_straight.nc","NetCDF4-BEAM");

        //Product output= GPF.createProduct("Subset", parameterMap, product);
        //ProductIO.writeProduct(output, "/home/roman/data/spark_test_dataset/test_output_30.nc","NetCDF4-BEAM");
        //File reopenFile = new File ("/home/roman/data/spark_test_dataset/test_output_30.nc");
        //winProduct = ProductIO.readProduct(reopenFile);
        //Product output2= GPF.createProduct("Subset", parameterMap, winProduct);
        //ProductIO.writeProduct(winProduct, "/home/roman/data/spark_test_dataset/test_output_recalc.nc","NetCDF4-BEAM");
        //ProductIO.writeProduct(output, "/home/roman/data/spark_test_dataset/netcdf3.nc","NetCDF-BEAM");
        //ProductIO.writeProduct(output, "/home/roman/data/spark_test_dataset/netcdf4.nc","NetCDF4-BEAM");

    }
}
