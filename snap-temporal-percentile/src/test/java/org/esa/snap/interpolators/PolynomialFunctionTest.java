package org.esa.snap.interpolators;


import org.junit.Test;

import static org.junit.Assert.*;

public class PolynomialFunctionTest {

    @Test
    public void testToString() {
        final PolynomialFunction polynomialFunction = new PolynomialFunction(new double[]{-1.8, 2.6, 0.0067});

        assertEquals("-1.8 + 2.6 x + 0.0067 x^2", polynomialFunction.toString());
    }

    @Test
    public void testToString_zeroCoefficient() {
        final PolynomialFunction polynomialFunction = new PolynomialFunction(new double[]{0.0});

        assertEquals("0", polynomialFunction.toString());
    }

    @Test
    public void testToString_firstCoefficientZero() {
        final PolynomialFunction polynomialFunction = new PolynomialFunction(new double[]{0.0, 1.1});

        assertEquals("1.1 x", polynomialFunction.toString());
    }

    @Test
    public void testToString_intermediateCoefficientZero() {
        final PolynomialFunction polynomialFunction = new PolynomialFunction(new double[]{-2.8, 0.0, -1.1});

        assertEquals("-2.8 - 1.1 x^2", polynomialFunction.toString());
    }

    @Test
    public void testHashCode() {
        final PolynomialFunction polynomialFunction = new PolynomialFunction(new double[]{2.2, 3.3});

        assertEquals(2144601149, polynomialFunction.hashCode());
    }

    @Test
    public void testEquals()  {
        final PolynomialFunction polynomialFunction = new PolynomialFunction(new double[]{1.9, -5.2});
        final PolynomialFunction same = new PolynomialFunction(new double[]{1.9, -5.2});
        final PolynomialFunction different = new PolynomialFunction(new double[]{-1.9, 5.2});

        assertTrue(polynomialFunction.equals(same));
        assertFalse(polynomialFunction.equals(different));
    }
}
