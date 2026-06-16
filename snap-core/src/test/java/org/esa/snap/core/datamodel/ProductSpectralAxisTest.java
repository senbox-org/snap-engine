package org.esa.snap.core.datamodel;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class ProductSpectralAxisTest {

    @Test
    public void getSpectralAxes_returnsDefaultAxisFromSpectralBands() {
        Product product = new Product("p", "t", 1, 1);
        addSpectralBand(product, "b2", 600.0f);
        addSpectralBand(product, "invalid", 0.0f);
        addFlagBand(product, "flags", 700.0f);
        addSpectralBand(product, "b1", 500.0f);

        List<ProductSpectralAxis> axes = product.getSpectralAxes();

        assertEquals(1, axes.size());
        ProductSpectralAxis axis = axes.get(0);
        assertEquals(ProductSpectralAxis.DEFAULT_ID, axis.getId());
        assertEquals(ProductSpectralAxis.DEFAULT_NAME, axis.getName());
        assertEquals(List.of("b1", "b2"), axis.getBandNames());
    }

    @Test
    public void getSpectralAxes_returnsEmptyListWithoutSpectralBands() {
        Product product = new Product("p", "t", 1, 1);
        addSpectralBand(product, "invalid", 0.0f);
        addFlagBand(product, "flags", 700.0f);

        assertEquals(List.of(), product.getSpectralAxes());
    }

    @Test
    public void getSpectralAxes_returnsExplicitAxes() {
        Product product = new Product("p", "t", 1, 1);
        ProductSpectralAxis explicit = new ProductSpectralAxis("custom", "Custom", List.of("b"));

        product.setSpectralAxes(List.of(explicit));

        List<ProductSpectralAxis> axes = product.getSpectralAxes();
        assertEquals(1, axes.size());
        assertSame(explicit, axes.get(0));
    }

    @Test
    public void setSpectralAxes_copiesInputListAndReturnsImmutableList() {
        Product product = new Product("p", "t", 1, 1);
        ProductSpectralAxis first = new ProductSpectralAxis("first", "First", List.of("b1"));
        ProductSpectralAxis second = new ProductSpectralAxis("second", "Second", List.of("b2"));
        List<ProductSpectralAxis> input = new ArrayList<>(List.of(first));

        product.setSpectralAxes(input);
        input.add(second);

        List<ProductSpectralAxis> axes = product.getSpectralAxes();
        assertEquals(List.of(first), axes);
        expectUnsupportedOperation(() -> axes.add(second));
    }

    @Test
    public void setSpectralAxes_withNullOrEmptyListFallsBackToDefaultAxis() {
        Product product = new Product("p", "t", 1, 1);
        addSpectralBand(product, "default", 500.0f);
        ProductSpectralAxis explicit = new ProductSpectralAxis("custom", "Custom", List.of("explicit"));

        product.setSpectralAxes(List.of(explicit));
        product.setSpectralAxes(null);
        assertEquals(List.of("default"), product.getSpectralAxes().get(0).getBandNames());

        product.setSpectralAxes(List.of(explicit));
        product.setSpectralAxes(List.of());
        assertEquals(List.of("default"), product.getSpectralAxes().get(0).getBandNames());
    }

    @Test
    public void setSpectralAxes_firesChangeEventOnlyIfAxesChange() {
        Product product = new Product("p", "t", 1, 1);
        ProductSpectralAxis explicit = new ProductSpectralAxis("custom", "Custom", List.of("b"));
        List<ProductNodeEvent> events = new ArrayList<>();
        product.addProductNodeListener(new ProductNodeListenerAdapter() {
            @Override
            public void nodeChanged(ProductNodeEvent event) {
                events.add(event);
            }
        });

        product.setSpectralAxes(List.of(explicit));
        product.setSpectralAxes(List.of(explicit));
        product.setSpectralAxes(null);

        assertEquals(2, events.size());
        assertEquals(Product.PROPERTY_NAME_SPECTRAL_AXES, events.get(0).getPropertyName());
        assertEquals(null, events.get(0).getOldValue());
        assertEquals(List.of(explicit), events.get(0).getNewValue());
        assertEquals(Product.PROPERTY_NAME_SPECTRAL_AXES, events.get(1).getPropertyName());
        assertEquals(List.of(explicit), events.get(1).getOldValue());
        assertEquals(null, events.get(1).getNewValue());
    }

    @Test
    public void constructor_copiesBandNamesAndReturnsImmutableList() {
        List<String> bandNames = new ArrayList<>(List.of("b1"));

        ProductSpectralAxis axis = new ProductSpectralAxis("id", "Name", bandNames);
        bandNames.add("b2");

        assertNotSame(bandNames, axis.getBandNames());
        assertEquals(List.of("b1"), axis.getBandNames());
        expectUnsupportedOperation(() -> axis.getBandNames().add("b3"));
    }

    @Test
    public void toString_returnsName() {
        ProductSpectralAxis axis = new ProductSpectralAxis("id", "Name", List.of("b"));

        assertEquals("Name", axis.toString());
    }

    private static void addSpectralBand(Product product, String name, float wavelength) {
        Band band = new Band(name, ProductData.TYPE_FLOAT32, 1, 1);
        band.setSpectralWavelength(wavelength);
        product.addBand(band);
    }

    private static void addFlagBand(Product product, String name, float wavelength) {
        Band band = new Band(name, ProductData.TYPE_UINT8, 1, 1);
        band.setSpectralWavelength(wavelength);
        FlagCoding flagCoding = new FlagCoding(name);
        flagCoding.addFlag("flag", 1, "flag");
        band.setSampleCoding(flagCoding);
        product.getFlagCodingGroup().add(flagCoding);
        product.addBand(band);
    }

    private static void expectUnsupportedOperation(Runnable action) {
        try {
            action.run();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("Expected UnsupportedOperationException");
    }
}
