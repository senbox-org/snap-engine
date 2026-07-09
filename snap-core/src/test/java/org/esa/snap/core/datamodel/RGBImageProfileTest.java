/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.datamodel;

import com.bc.ceres.core.runtime.ConfigurationElement;
import org.esa.snap.core.util.math.Range;
import org.junit.Test;

import java.util.Arrays;
import java.util.Properties;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RGBImageProfileTest {

    @Test
    public void testDefaults() {
        final RGBImageProfile profile = new RGBImageProfile("X");

        assertEquals("X", profile.getName());
        assertFalse(profile.isInternal());
        assertEquals("", profile.getRedExpression());
        assertEquals("", profile.getGreenExpression());
        assertEquals("", profile.getBlueExpression());
        assertEquals("", profile.getAlphaExpression());
        assertNotNull(profile.getRgbExpressions());
        assertEquals(3, profile.getRgbExpressions().length);
        assertNotNull(profile.getRgbaExpressions());
        assertEquals(4, profile.getRgbaExpressions().length);
        assertNull(profile.getPattern());

        final Range redMinMax = profile.getRedMinMax();
        assertEquals(Double.NaN, redMinMax.getMin(), 1e-8);
        assertEquals(Double.NaN, redMinMax.getMax(), 1e-8);

        final Range greenMinMax = profile.getGreenMinMax();
        assertEquals(Double.NaN, greenMinMax.getMin(), 1e-8);
        assertEquals(Double.NaN, greenMinMax.getMax(), 1e-8);

        final Range blueMinMax = profile.getBlueMinMax();
        assertEquals(Double.NaN, blueMinMax.getMin(), 1e-8);
        assertEquals(Double.NaN, blueMinMax.getMax(), 1e-8);
    }

    @Test
    public void testEqualsAndHashCode() {
        final RGBImageProfile profile1 = new RGBImageProfile("X", new String[]{"A", "B", "C"}, new String[]{
                "prod_type",
                "prod_name",
                "prod_desc"
        });
        final RGBImageProfile profile2 = new RGBImageProfile("X", new String[]{"A", "B", "C"}, new String[]{
                "prod_type",
                "prod_name",
                "prod_desc"
        });
        final RGBImageProfile profile3 = new RGBImageProfile("X", new String[]{"A", "B", "C"}, new String[]{
                "different_pattern",
                "diff",
                "diff"
        });
        final RGBImageProfile profile4 = new RGBImageProfile("Y", new String[]{"A", "B", "C"});
        final RGBImageProfile profile5 = new RGBImageProfile("X", new String[] {"A", "B", "V"});
        final RGBImageProfile profile6 = new RGBImageProfile("X", new String[] {"A", "B", "C", "D"});

        assertTrue(profile1.equals(profile1));
        assertFalse(profile1.equals(profile3));
        assertTrue(profile1.equals(profile2));
        assertFalse(profile1.equals(profile4));
        assertFalse(profile1.equals(profile5));
        assertFalse(profile1.equals(profile6));

        assertTrue(profile1.hashCode() == profile2.hashCode());
        assertTrue(profile1.hashCode() == profile3.hashCode());
        assertTrue(profile1.hashCode() == profile4.hashCode());
        assertTrue(profile1.hashCode() != profile5.hashCode());
        assertTrue(profile1.hashCode() == profile6.hashCode());
    }

    @Test
    public void testThatComponentsMustNotBeNull() {
        final RGBImageProfile profile = new RGBImageProfile("X");
        try {
            profile.setRedExpression(null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            profile.setGreenExpression(null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            profile.setBlueExpression(null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            profile.setAlphaExpression(null);
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testComponentsAsArrays() {
        final RGBImageProfile profile = new RGBImageProfile("X");
        profile.setRedExpression("radiance_1");
        profile.setGreenExpression("radiance_2");
        profile.setBlueExpression("radiance_4");
        profile.setAlphaExpression("l1_flags.LAND ? 0 : 1");

        final String[] rgbExpressions = profile.getRgbExpressions();
        assertNotNull(rgbExpressions);
        assertEquals(3, rgbExpressions.length);
        assertEquals("radiance_1", rgbExpressions[0]);
        assertEquals("radiance_2", rgbExpressions[1]);
        assertEquals("radiance_4", rgbExpressions[2]);

        final String[] rgbaExpressions = profile.getRgbaExpressions();
        assertNotNull(rgbaExpressions);
        assertEquals(4, rgbaExpressions.length);
        assertEquals("radiance_1", rgbaExpressions[0]);
        assertEquals("radiance_2", rgbaExpressions[1]);
        assertEquals("radiance_4", rgbaExpressions[2]);
        assertEquals("l1_flags.LAND ? 0 : 1", rgbaExpressions[3]);
    }

    @Test
    public void testApplicabilityOfEmptyProfile() {
        final RGBImageProfile profile = new RGBImageProfile("X");
        assertEquals(false, profile.isApplicableTo(createTestProduct()));
    }

    @Test
    public void testApplicabilityIfAlphaComponentIsMissing() {
        final RGBImageProfile profile = new RGBImageProfile("X");
        profile.setRedExpression("U+V");
        profile.setGreenExpression("V+W");
        profile.setBlueExpression("W+X");
        profile.setAlphaExpression("");
        assertEquals(true, profile.isApplicableTo(createTestProduct()));
    }

    @Test
    public void testApplicabilityIfOneComponentIsMissing() {
        final RGBImageProfile profile = new RGBImageProfile("X");
        profile.setRedExpression("U+V");
        profile.setGreenExpression("");
        profile.setBlueExpression("W+X");
        profile.setAlphaExpression("Y+Z");
        assertEquals(true, profile.isApplicableTo(createTestProduct()));
    }

    @Test
    public void testIsApplicable_ifUnknownBandIsUsed() {
        final RGBImageProfile profile = new RGBImageProfile("X");
        profile.setRedExpression("U+V");
        profile.setGreenExpression("V+K"); // unknown band K
        profile.setBlueExpression("W+X");
        profile.setAlphaExpression("");
        assertFalse(profile.isApplicableTo(createTestProduct()));
    }

    @Test
    public void testIsApplicable_bandOfWrongDimension() {
        final RGBImageProfile profile = new RGBImageProfile("X");
        profile.setRedExpression("U+V");
        profile.setGreenExpression("too_large");
        profile.setBlueExpression("W+X");
        profile.setAlphaExpression("");
        assertFalse(profile.isApplicableTo(createTestProduct()));
    }

    @Test
    public void testStoreRgbaExpressions() {
        final Product p1 = createTestProduct();
        RGBImageProfile.storeRgbaExpressions(p1, new String[]{"U", "V", "W", "X"});
        assertNotNull(p1.getBand(RGBImageProfile.RED_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.GREEN_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.BLUE_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.ALPHA_BAND_NAME));
    }

    @Test
    public void testStoreRgbaExpressionsWithoutAlpha() {
        final Product p1 = createTestProduct();
        RGBImageProfile.storeRgbaExpressions(p1, new String[]{"U", "V", "W", ""});
        assertNotNull(p1.getBand(RGBImageProfile.RED_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.GREEN_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.BLUE_BAND_NAME));
        assertNull(p1.getBand(RGBImageProfile.ALPHA_BAND_NAME));
    }

    @Test
    public void testStoreRgbaExpressionsWithoutGreen() {
        final Product p1 = createTestProduct();
        RGBImageProfile.storeRgbaExpressions(p1, new String[]{"U", "", "W", ""});
        assertNotNull(p1.getBand(RGBImageProfile.RED_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.GREEN_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.BLUE_BAND_NAME));
        assertNull(p1.getBand(RGBImageProfile.ALPHA_BAND_NAME));

        assertEquals("0", ((VirtualBand) p1.getBand(RGBImageProfile.GREEN_BAND_NAME)).getExpression());
    }

    @Test
    public void testStoreRgbaExpressionsOverwrite() {
        final Product p1 = createTestProduct();
        RGBImageProfile.storeRgbaExpressions(p1, new String[]{"U", "V", "W", "X"});
        assertNotNull(p1.getBand(RGBImageProfile.RED_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.GREEN_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.BLUE_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.ALPHA_BAND_NAME));
        RGBImageProfile.storeRgbaExpressions(p1, new String[]{"0.3", "2.0", "6.7", ""});
        assertNotNull(p1.getBand(RGBImageProfile.RED_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.GREEN_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.BLUE_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.ALPHA_BAND_NAME)); // since exist before
        assertEquals("0.3", ((VirtualBand) p1.getBand(RGBImageProfile.RED_BAND_NAME)).getExpression());
        assertEquals("2.0", ((VirtualBand) p1.getBand(RGBImageProfile.GREEN_BAND_NAME)).getExpression());
        assertEquals("6.7", ((VirtualBand) p1.getBand(RGBImageProfile.BLUE_BAND_NAME)).getExpression());
        assertEquals("0", ((VirtualBand) p1.getBand(RGBImageProfile.ALPHA_BAND_NAME)).getExpression());
    }

    private Product createTestProduct() {
        final Product product = new Product("N", "T", 4, 4);
        product.addBand("U", ProductData.TYPE_FLOAT32);
        product.addBand("V", ProductData.TYPE_FLOAT32);
        product.addBand("W", ProductData.TYPE_FLOAT32);
        product.addBand("X", ProductData.TYPE_FLOAT32);
        product.addBand("Y", ProductData.TYPE_FLOAT32);
        product.addBand("Z", ProductData.TYPE_FLOAT32);
        product.addBand(new Band("too_large", ProductData.TYPE_FLOAT32, 5, 4));
        return product;
    }

    @Test
    public void testConfigure_withoutPattern() throws Exception {
        RGBImageProfile profile = new RGBImageProfile();
        ConfigurationElement config = mock(ConfigurationElement.class);
        ConfigurationElement nameConfig = mock(ConfigurationElement.class);
        ConfigurationElement redConfig = mock(ConfigurationElement.class);
        ConfigurationElement greenConfig = mock(ConfigurationElement.class);
        ConfigurationElement blueConfig = mock(ConfigurationElement.class);

        when(nameConfig.getValue()).thenReturn("test_name");

        when(redConfig.getValue()).thenReturn("radiance_12");
        when(greenConfig.getValue()).thenReturn("radiance_6");
        when(blueConfig.getValue()).thenReturn("radiance_2");


        when(config.getChild("name")).thenReturn(nameConfig);
        when(config.getChild("red")).thenReturn(redConfig);
        when(config.getChild("green")).thenReturn(greenConfig);
        when(config.getChild("blue")).thenReturn(blueConfig);
        when(config.getChild("alpha")).thenReturn(null);

        profile.configure(config);

        assertEquals("test_name", profile.getName());
        assertEquals("", profile.getAlphaExpression());
        assertEquals("radiance_2", profile.getBlueExpression());
        assertEquals("radiance_12", profile.getRedExpression());
        assertEquals("radiance_6", profile.getGreenExpression());
        assertNull(profile.getPattern());
    }

    @Test
    public void testConfigure() throws Exception {
        RGBImageProfile profile = new RGBImageProfile();
        ConfigurationElement config = mock(ConfigurationElement.class);
        ConfigurationElement nameConfig = mock(ConfigurationElement.class);
        ConfigurationElement redConfig = mock(ConfigurationElement.class);
        ConfigurationElement greenConfig = mock(ConfigurationElement.class);
        ConfigurationElement blueConfig = mock(ConfigurationElement.class);
        ConfigurationElement patternConfig = mock(ConfigurationElement.class);
        ConfigurationElement productTypeConfig = mock(ConfigurationElement.class);
        ConfigurationElement productNameConfig = mock(ConfigurationElement.class);
        ConfigurationElement productDescConfig = mock(ConfigurationElement.class);

        when(nameConfig.getValue()).thenReturn("test_name");

        when(redConfig.getValue()).thenReturn("radiance_12");
        when(greenConfig.getValue()).thenReturn("radiance_6");
        when(blueConfig.getValue()).thenReturn("radiance_2");

        when(productTypeConfig.getValue()).thenReturn("MER_*_1*");
        when(productNameConfig.getValue()).thenReturn("ATS_*_1*");
        when(productDescConfig.getValue()).thenReturn("");

        when(config.getChild("name")).thenReturn(nameConfig);
        when(config.getChild("red")).thenReturn(redConfig);
        when(config.getChild("green")).thenReturn(greenConfig);
        when(config.getChild("blue")).thenReturn(blueConfig);
        when(config.getChild("alpha")).thenReturn(null);
        when(config.getChild("pattern")).thenReturn(patternConfig);
        when(patternConfig.getChild("productType")).thenReturn(productTypeConfig);
        when(patternConfig.getChild("productName")).thenReturn(productNameConfig);
        when(patternConfig.getChild("productDesc")).thenReturn(productDescConfig);

        profile.configure(config);

        assertEquals("test_name", profile.getName());
        assertEquals("", profile.getAlphaExpression());
        assertEquals("radiance_2", profile.getBlueExpression());
        assertEquals("radiance_12", profile.getRedExpression());
        assertEquals("radiance_6", profile.getGreenExpression());
        assertTrue(Arrays.equals(new String[]{"MER_*_1*", "ATS_*_1*", ""}, profile.getPattern()));
    }

    @Test
    public void testEqualExpressions_deprecatedVersion() {
        final RGBImageProfile infoWithoutAlpha = new RGBImageProfile("testing", new String[]{"winnie", "Ferkelchen", "Heffalump"});

        assertTrue(infoWithoutAlpha.equalExpressions(new String[]{"winnie", "Ferkelchen", "Heffalump"}));
        assertFalse(infoWithoutAlpha.equalExpressions(new String[]{"I-Ah", "Ferkelchen", "Heffalump"}));
        assertFalse(infoWithoutAlpha.equalExpressions(new String[]{"winnie", "Ferkelchen", "Heffalump", "alpha"}));

        final RGBImageProfile infoWithAlpha = new RGBImageProfile("testing_2", new String[]{"beta", "gamma", "lametta", "alpha"});
        assertTrue(infoWithAlpha.equalExpressions(new String[]{"beta", "gamma", "lametta", "alpha"}));
        assertFalse(infoWithAlpha.equalExpressions(new String[]{"delta", "gamma", "lametta", "alpha"}));

    }

    @Test
    public void testSetProperties() {
        final RGBImageProfile profile = new RGBImageProfile();
        final Properties properties = new Properties();
        properties.setProperty(RGBImageProfile.PROPERTY_KEY_NAME, "name");
        properties.setProperty(RGBImageProfile.PROPERTY_KEY_ALPHA, "alpha");
        properties.setProperty(RGBImageProfile.PROPERTY_KEY_BLUE, "blue");
        properties.setProperty(RGBImageProfile.PROPERTY_KEY_GREEN, "green");
        properties.setProperty(RGBImageProfile.PROPERTY_KEY_RED, "red");
        properties.setProperty(RGBImageProfile.PROPERTY_KEY_INTERNAL, String.valueOf(true));
        properties.setProperty(RGBImageProfile.PROPERTY_KEY_RED_MIN, "0.1");
        properties.setProperty(RGBImageProfile.PROPERTY_KEY_RED_MAX, "0.2");
        properties.setProperty(RGBImageProfile.PROPERTY_KEY_GREEN_MIN, "0.3");
        properties.setProperty(RGBImageProfile.PROPERTY_KEY_GREEN_MAX, "0.4");
        properties.setProperty(RGBImageProfile.PROPERTY_KEY_BLUE_MIN, "0.5");
        properties.setProperty(RGBImageProfile.PROPERTY_KEY_BLUE_MAX, "0.6");

        profile.setProperties(properties);

        assertEquals("name", profile.getName());
        assertTrue(profile.isInternal());
        assertArrayEquals(new String[]{"red", "green", "blue", "alpha"}, profile.getRgbaExpressions());

        final Range redMinMax = profile.getRedMinMax();
        assertEquals(0.1, redMinMax.getMin(), 1e-8);
        assertEquals(0.2, redMinMax.getMax(), 1e-8);

        final Range greenMinMax = profile.getGreenMinMax();
        assertEquals(0.3, greenMinMax.getMin(), 1e-8);
        assertEquals(0.4, greenMinMax.getMax(), 1e-8);

        final Range blueMinMax = profile.getBlueMinMax();
        assertEquals(0.5, blueMinMax.getMin(), 1e-8);
        assertEquals(0.6, blueMinMax.getMax(), 1e-8);
    }

    @Test
    public void testSetProperties_withoutMinMax() {
        final RGBImageProfile profile = new RGBImageProfile();
        final Properties properties = new Properties();
        properties.setProperty(RGBImageProfile.PROPERTY_KEY_NAME, "Peter");
        properties.setProperty(RGBImageProfile.PROPERTY_KEY_ALPHA, "anna");
        properties.setProperty(RGBImageProfile.PROPERTY_KEY_BLUE, "berta");
        properties.setProperty(RGBImageProfile.PROPERTY_KEY_GREEN, "gerda");
        properties.setProperty(RGBImageProfile.PROPERTY_KEY_RED, "rachel");
        properties.setProperty(RGBImageProfile.PROPERTY_KEY_INTERNAL, String.valueOf(true));

        profile.setProperties(properties);

        assertEquals("Peter", profile.getName());
        assertTrue(profile.isInternal());
        assertArrayEquals(new String[]{"rachel", "gerda", "berta", "anna"}, profile.getRgbaExpressions());

        final Range redMinMax = profile.getRedMinMax();
        assertEquals(Double.NaN, redMinMax.getMin(), 1e-8);
        assertEquals(Double.NaN, redMinMax.getMax(), 1e-8);

        final Range greenMinMax = profile.getGreenMinMax();
        assertEquals(Double.NaN, greenMinMax.getMin(), 1e-8);
        assertEquals(Double.NaN, greenMinMax.getMax(), 1e-8);

        final Range blueMinMax = profile.getBlueMinMax();
        assertEquals(Double.NaN, blueMinMax.getMin(), 1e-8);
        assertEquals(Double.NaN, blueMinMax.getMax(), 1e-8);
    }

    @Test
    public void testGetProperties() {
        final RGBImageProfile imageProfile = new RGBImageProfile("A_name", new String[]{"this", "is", "very", "colourful"});
        imageProfile.setRedMinMax(new Range(1, 2));
        imageProfile.setGreenMinMax(new Range(3, 4));
        imageProfile.setBlueMinMax(new Range(5, 6));

        final Properties properties = new Properties();
        imageProfile.getProperties(properties);

        assertEquals("A_name", properties.getProperty(RGBImageProfile.PROPERTY_KEY_NAME));

        assertEquals("this", properties.getProperty(RGBImageProfile.PROPERTY_KEY_RED));
        assertEquals("1.0", properties.getProperty(RGBImageProfile.PROPERTY_KEY_RED_MIN));
        assertEquals("2.0", properties.getProperty(RGBImageProfile.PROPERTY_KEY_RED_MAX));

        assertEquals("is", properties.getProperty(RGBImageProfile.PROPERTY_KEY_GREEN));
        assertEquals("3.0", properties.getProperty(RGBImageProfile.PROPERTY_KEY_GREEN_MIN));
        assertEquals("4.0", properties.getProperty(RGBImageProfile.PROPERTY_KEY_GREEN_MAX));

        assertEquals("very", properties.getProperty(RGBImageProfile.PROPERTY_KEY_BLUE));
        assertEquals("5.0", properties.getProperty(RGBImageProfile.PROPERTY_KEY_BLUE_MIN));
        assertEquals("6.0", properties.getProperty(RGBImageProfile.PROPERTY_KEY_BLUE_MAX));

        assertEquals("colourful", properties.getProperty(RGBImageProfile.PROPERTY_KEY_ALPHA));
    }

    @Test
    public void testIsValid() {
        final RGBImageProfile imageProfile = new RGBImageProfile("whatever", new String[]{"een", "twee", "drei", "veer"});

        assertTrue(imageProfile.isValid());

        // must be valid no matter what the alpha channel is defined for
        imageProfile.setAlphaExpression("");
        assertTrue(imageProfile.isValid());

        // should return false only if ALL channels are empty
        imageProfile.setRedExpression("");
        assertTrue(imageProfile.isValid());

        imageProfile.setGreenExpression("");
        assertTrue(imageProfile.isValid());

        imageProfile.setBlueExpression("");
        assertFalse(imageProfile.isValid());
    }

    @Test
    public void testSetGetRgbExpressions() {
        final RGBImageProfile imageProfile = new RGBImageProfile("whatever", new String[]{"een", "twee", "drei", "veer"});
        final String[] expected = {"alma", "bert", "charlotte"};

        imageProfile.setRgbExpressions(expected);

        final String[] rgbExpressions = imageProfile.getRgbExpressions();
        assertArrayEquals(expected, rgbExpressions);
    }

    @Test
    public void testSetRgbExpressions_arrayLengthMismatch() {
        final RGBImageProfile imageProfile = new RGBImageProfile("whatever", new String[]{"een", "twee", "drei", "veer"});

        try {
            imageProfile.setRgbExpressions(new String[] {"hoppla", "short"});
        } catch (IllegalArgumentException expected) {
        }

        try {
            imageProfile.setRgbExpressions(new String[] {"oops", "way", "too", "many"});
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testSetGetRgbaExpressions() {
        final RGBImageProfile imageProfile = new RGBImageProfile("whatever", new String[]{"een", "twee", "drei", "veer"});
        final String[] expected = {"alma", "bert", "charlotte", "Alpher"};

        imageProfile.setRgbaExpressions(expected);

        final String[] rgbaExpressions = imageProfile.getRgbaExpressions();
        assertArrayEquals(expected, rgbaExpressions);
    }
    @Test
    public void testSetRgbaExpressions_arrayLengthMismatch() {
        final RGBImageProfile imageProfile = new RGBImageProfile("whatever", new String[]{"een", "twee", "drei", "veer"});

        try {
            imageProfile.setRgbaExpressions(new String[] {"hoppla", "short", "list"});
        } catch (IllegalArgumentException expected) {
        }

        try {
            imageProfile.setRgbaExpressions(new String[] {"oops", "way", "too", "many", "args"});
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testHasAlpha()  {
        final RGBImageProfile withAlpha = new RGBImageProfile("whatever", new String[]{"een", "twee", "drei", "veer"});
        assertTrue(withAlpha.hasAlpha());

        final RGBImageProfile withoutAlpha = new RGBImageProfile("whatever", new String[]{"does", "not", "have"});
        assertFalse(withoutAlpha.hasAlpha());
    }

    @Test
    public void testSetGetPattern(){
        final RGBImageProfile profile = new RGBImageProfile("whatever", new String[]{"yo", "this", "is"});
        final String[] patterns = {"pattern", "array"};

        profile.setPattern(patterns);
        assertArrayEquals(patterns, profile.getPattern());
    }

    @Test
    public void testToString() {
        final RGBImageProfile profile = new RGBImageProfile("whatever", new String[]{"yo", "this", "is", "it"});

        assertEquals("org.esa.snap.core.datamodel.RGBImageProfile[name=whatever, r=yo, g=this, b=is, a=it]", profile.toString());
    }

    @Test
    public void testSetGetMinMax() {
        final RGBImageProfile profile = new RGBImageProfile("whatever", new String[]{"yo", "this", "is", "it"});

        Range range = new Range(0.33, 1.31);
        profile.setRedMinMax(range);
        Range result = profile.getRedMinMax();
        assertEquals(range.getMin(), result.getMin(), 1e-8);
        assertEquals(range.getMax(), result.getMax(), 1e-8);

        range = new Range(0.34, 1.32);
        profile.setGreenMinMax(range);
        result = profile.getGreenMinMax();
        assertEquals(range.getMin(), result.getMin(), 1e-8);
        assertEquals(range.getMax(), result.getMax(), 1e-8);

        range = new Range(0.35, 1.33);
        profile.setBlueMinMax(range);
        result = profile.getBlueMinMax();
        assertEquals(range.getMin(), result.getMin(), 1e-8);
        assertEquals(range.getMax(), result.getMax(), 1e-8);
    }

    @Test
    public void testConstruction_noParameter() {
        final RGBImageProfile imageProfile = new RGBImageProfile();

        assertEquals("", imageProfile.getName());
        assertEquals("", imageProfile.getRedExpression());
        assertEquals("", imageProfile.getGreenExpression());
        assertEquals("", imageProfile.getBlueExpression());
        assertEquals("", imageProfile.getAlphaExpression());
        assertFalse(imageProfile.isInternal());
        assertNull(imageProfile.getPattern());

        final Range redMinMax = imageProfile.getRedMinMax();
        assertEquals(Double.NaN, redMinMax.getMin(), 1e-8);
        assertEquals(Double.NaN, redMinMax.getMax(), 1e-8);

        final Range greenMinMax = imageProfile.getGreenMinMax();
        assertEquals(Double.NaN, greenMinMax.getMin(), 1e-8);
        assertEquals(Double.NaN, greenMinMax.getMax(), 1e-8);

        final Range blueMinMax = imageProfile.getBlueMinMax();
        assertEquals(Double.NaN, blueMinMax.getMin(), 1e-8);
        assertEquals(Double.NaN, blueMinMax.getMax(), 1e-8);
    }

    @Test
    public void testConstruction_onlyName() {
        final RGBImageProfile imageProfile = new RGBImageProfile("Herrmann");

        assertEquals("Herrmann", imageProfile.getName());
        assertEquals("", imageProfile.getRedExpression());
        assertEquals("", imageProfile.getGreenExpression());
        assertEquals("", imageProfile.getBlueExpression());
        assertEquals("", imageProfile.getAlphaExpression());
        assertFalse(imageProfile.isInternal());
        assertNull(imageProfile.getPattern());

        final Range redMinMax = imageProfile.getRedMinMax();
        assertEquals(Double.NaN, redMinMax.getMin(), 1e-8);
        assertEquals(Double.NaN, redMinMax.getMax(), 1e-8);

        final Range greenMinMax = imageProfile.getGreenMinMax();
        assertEquals(Double.NaN, greenMinMax.getMin(), 1e-8);
        assertEquals(Double.NaN, greenMinMax.getMax(), 1e-8);

        final Range blueMinMax = imageProfile.getBlueMinMax();
        assertEquals(Double.NaN, blueMinMax.getMin(), 1e-8);
        assertEquals(Double.NaN, blueMinMax.getMax(), 1e-8);
    }

    @Test
    public void testConstruction_nameAndExpressions() {
        final RGBImageProfile imageProfile = new RGBImageProfile("Winnie", new String[] {"red", "grün", "blue", "alpha"});

        assertEquals("Winnie", imageProfile.getName());
        assertEquals("red", imageProfile.getRedExpression());
        assertEquals("grün", imageProfile.getGreenExpression());
        assertEquals("blue", imageProfile.getBlueExpression());
        assertEquals("alpha", imageProfile.getAlphaExpression());
        assertFalse(imageProfile.isInternal());
        assertNull(imageProfile.getPattern());

        final Range redMinMax = imageProfile.getRedMinMax();
        assertEquals(Double.NaN, redMinMax.getMin(), 1e-8);
        assertEquals(Double.NaN, redMinMax.getMax(), 1e-8);

        final Range greenMinMax = imageProfile.getGreenMinMax();
        assertEquals(Double.NaN, greenMinMax.getMin(), 1e-8);
        assertEquals(Double.NaN, greenMinMax.getMax(), 1e-8);

        final Range blueMinMax = imageProfile.getBlueMinMax();
        assertEquals(Double.NaN, blueMinMax.getMin(), 1e-8);
        assertEquals(Double.NaN, blueMinMax.getMax(), 1e-8);
    }

    @Test
    public void testConstruction_nameExpressionsAndPatterns() {
        final String[] expectedExpression = {"ex", "press", "ion"};
        final RGBImageProfile imageProfile = new RGBImageProfile("Elsa", new String[] {"Rot", "green", "Blau", "alpha"}, expectedExpression);

        assertEquals("Elsa", imageProfile.getName());
        assertEquals("Rot", imageProfile.getRedExpression());
        assertEquals("green", imageProfile.getGreenExpression());
        assertEquals("Blau", imageProfile.getBlueExpression());
        assertEquals("alpha", imageProfile.getAlphaExpression());
        assertFalse(imageProfile.isInternal());
        assertArrayEquals(expectedExpression, imageProfile.getPattern());

        final Range redMinMax = imageProfile.getRedMinMax();
        assertEquals(Double.NaN, redMinMax.getMin(), 1e-8);
        assertEquals(Double.NaN, redMinMax.getMax(), 1e-8);

        final Range greenMinMax = imageProfile.getGreenMinMax();
        assertEquals(Double.NaN, greenMinMax.getMin(), 1e-8);
        assertEquals(Double.NaN, greenMinMax.getMax(), 1e-8);

        final Range blueMinMax = imageProfile.getBlueMinMax();
        assertEquals(Double.NaN, blueMinMax.getMin(), 1e-8);
        assertEquals(Double.NaN, blueMinMax.getMax(), 1e-8);
    }

//    @Test
//    public void testConstruction_nameExpressionsPatternsAndRanges() {
//        final String[] expectedExpression = {"ex", "press", "ion"};
//        final Range[] ranges = new Range[] {new Range(10, 11), new Range(12, 13), new Range(14, 15)};
//        final RGBImageProfile imageProfile = new RGBImageProfile("Elsa",
//                new String[] {"Rot", "green", "Blau", "alpha"},
//                expectedExpression,
//                ranges);
//
//        assertEquals("Elsa", imageProfile.getName());
//        assertEquals("Rot", imageProfile.getRedExpression());
//        assertEquals("green", imageProfile.getGreenExpression());
//        assertEquals("Blau", imageProfile.getBlueExpression());
//        assertEquals("alpha", imageProfile.getAlphaExpression());
//        assertFalse(imageProfile.isInternal());
//        assertArrayEquals(expectedExpression, imageProfile.getPattern());
//
//        final Range redMinMax = imageProfile.getRedMinMax();
//        assertEquals(10.0, redMinMax.getMin(), 1e-8);
//        assertEquals(11.0, redMinMax.getMax(), 1e-8);
//
//        final Range greenMinMax = imageProfile.getGreenMinMax();
//        assertEquals(12.0, greenMinMax.getMin(), 1e-8);
//        assertEquals(13.0, greenMinMax.getMax(), 1e-8);
//
//        final Range blueMinMax = imageProfile.getBlueMinMax();
//        assertEquals(14.0, blueMinMax.getMin(), 1e-8);
//        assertEquals(15.0, blueMinMax.getMax(), 1e-8);
//    }
}
