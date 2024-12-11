package org.esa.snap.oldImpl.tests.unittests.performancetests;

import org.esa.snap.oldImpl.performance.performancetests.AbstractPerformanceTest;
import org.esa.snap.oldImpl.performance.performancetests.PerformanceTestFactory;
import org.esa.snap.oldImpl.performance.performancetests.ReadSingleProductTest;
import org.esa.snap.oldImpl.performance.performancetests.WriteSingleProductTest;
import org.esa.snap.oldImpl.performance.util.MyParameters;
import org.esa.snap.oldImpl.performance.util.Threading;
import org.junit.Test;

import static org.junit.Assert.*;

public class PerformanceTestFactoryTest {

    @Test
    public void createPerformanceTest() {

        MyParameters params1 = new MyParameters("read-single-product","","", "",false, Threading.SINGLE, 12);
        MyParameters params2 = new MyParameters("Read-Single-PRODUCT","","", "",false, Threading.SINGLE, 12);
        MyParameters params3 = new MyParameters("readsingleproduct","","", "",false, Threading.SINGLE, 12);
        MyParameters params4 = new MyParameters("write-single-product-from-reader","","", "",false, Threading.SINGLE, 12);
        MyParameters params5 = new MyParameters("Write-Single-PRODUCT-from-reader","","", "",false, Threading.SINGLE, 12);
        MyParameters params6 = new MyParameters("writesingleproductfromreader","","", "",false, Threading.SINGLE, 12);
        MyParameters params7 = new MyParameters("","","", "",false, Threading.SINGLE, 12);
        MyParameters params8 = new MyParameters("fshhjrtdghdf","","", "",false, Threading.SINGLE, 12);

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