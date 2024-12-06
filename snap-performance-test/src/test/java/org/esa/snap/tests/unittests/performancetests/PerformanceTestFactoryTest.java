package org.esa.snap.tests.unittests.performancetests;

import org.esa.snap.performance.performancetests.AbstractPerformanceTest;
import org.esa.snap.performance.performancetests.PerformanceTestFactory;
import org.esa.snap.performance.performancetests.ReadSingleProductTest;
import org.esa.snap.performance.performancetests.WriteSingleProductTest;
import org.esa.snap.performance.util.Parameters;
import org.esa.snap.performance.util.Threading;
import org.junit.Test;

import static org.junit.Assert.*;

public class PerformanceTestFactoryTest {

    @Test
    public void createPerformanceTest() {

        Parameters params1 = new Parameters("read-single-product","","", "",false, Threading.SINGLE, 12);
        Parameters params2 = new Parameters("Read-Single-PRODUCT","","", "",false, Threading.SINGLE, 12);
        Parameters params3 = new Parameters("readsingleproduct","","", "",false, Threading.SINGLE, 12);
        Parameters params4 = new Parameters("write-single-product-from-reader","","", "",false, Threading.SINGLE, 12);
        Parameters params5 = new Parameters("Write-Single-PRODUCT-from-reader","","", "",false, Threading.SINGLE, 12);
        Parameters params6 = new Parameters("writesingleproductfromreader","","", "",false, Threading.SINGLE, 12);
        Parameters params7 = new Parameters("","","", "",false, Threading.SINGLE, 12);
        Parameters params8 = new Parameters("fshhjrtdghdf","","", "",false, Threading.SINGLE, 12);

        AbstractPerformanceTest test1 = PerformanceTestFactory.createPerformanceTest(params1);
        AbstractPerformanceTest test2 = PerformanceTestFactory.createPerformanceTest(params2);
        assertTrue(test1 instanceof ReadSingleProductTest);
        assertTrue(test2 instanceof ReadSingleProductTest);

        assertThrows( IllegalArgumentException.class, () -> { PerformanceTestFactory.createPerformanceTest(params3);});

        AbstractPerformanceTest test4 = PerformanceTestFactory.createPerformanceTest(params4);
        AbstractPerformanceTest test5 = PerformanceTestFactory.createPerformanceTest(params5);
        assertTrue(test4 instanceof WriteSingleProductTest);
        assertTrue(test5 instanceof WriteSingleProductTest);
        assertThrows( IllegalArgumentException.class, () -> { PerformanceTestFactory.createPerformanceTest(params6);});

        assertThrows( IllegalArgumentException.class, () -> { PerformanceTestFactory.createPerformanceTest(params7);});
        assertThrows(IllegalArgumentException.class, () -> { PerformanceTestFactory.createPerformanceTest(params8);});
    }
}