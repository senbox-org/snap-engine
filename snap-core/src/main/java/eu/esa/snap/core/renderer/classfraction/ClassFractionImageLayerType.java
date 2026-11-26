package eu.esa.snap.core.renderer.classfraction;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.annotations.LayerTypeMetadata;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.multilevel.MultiLevelSource;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;

import java.util.ArrayList;
import java.util.List;


@LayerTypeMetadata(
        name = "ClassFractionImageLayerType",
        aliasNames = {"eu.esa.snap.core.renderer.classfraction.ClassFractionImageLayerType", ClassFractionImageLayerType.LEGACY_TYPE_NAME}
)
public final class ClassFractionImageLayerType extends ImageLayer.Type {


    public static final String LEGACY_TYPE_NAME = "eu.esa.snap.core.renderer.ClassFractionImageLayerType";
    public static final String PROPERTY_NAME_PRODUCT = "product";
    public static final String PROPERTY_NAME_CONFIG_JSON = "classFractionConfig";


    @Override
    public ImageLayer createLayer(LayerContext context, PropertySet configuration) {
        MultiLevelSource source = configuration.getValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE);

        if (source == null) {
            Product product = configuration.getValue(PROPERTY_NAME_PRODUCT);
            ClassFractionRendererConfig config = ClassFractionRendererConfigJson.decode( (String) configuration.getValue(PROPERTY_NAME_CONFIG_JSON));
            source = new ClassFractionMultiLevelSource(config, resolve(product, config));
            configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, source);
        }
        return new ImageLayer(this, source, configuration);
    }

    @Override
    public PropertySet createLayerConfig(LayerContext context) {
        PropertySet properties = super.createLayerConfig(context);
        Property product = Property.create(PROPERTY_NAME_PRODUCT, Product.class);
        product.getDescriptor().setNotNull(true);
        properties.addProperty(product);

        Property config = Property.create(PROPERTY_NAME_CONFIG_JSON, String.class);
        config.getDescriptor().setNotNull(true);
        properties.addProperty(config);
        return properties;
    }

    public ImageLayer createLayer(Product product, ClassFractionRendererConfig config) {
        PropertySet properties = createLayerConfig(null);
        properties.setValue(PROPERTY_NAME_PRODUCT, product);
        properties.setValue(PROPERTY_NAME_CONFIG_JSON, ClassFractionRendererConfigJson.encode(config));

        return createLayer(null, properties);
    }

    public static boolean isRendererTypeName(String value) {
        return ClassFractionImageLayerType.class.getName().equals(value) || LEGACY_TYPE_NAME.equals(value);
    }

    private static List<RasterDataNode> resolve(Product product, ClassFractionRendererConfig config) {
        if (product == null) {
            throw new IllegalArgumentException("Class renderer product is missing");
        }

        List<RasterDataNode> sources = new ArrayList<>();
        for (ClassBandConfig band : config.getClassBandGroup().getBands()) {
            if (!product.getName().equals(band.getProductName())) {
                throw new IllegalArgumentException("Class renderer source product changed: " + band.getProductName());
            }

            RasterDataNode raster = product.getRasterDataNode(band.getBandName());
            if (raster == null) {
                throw new IllegalArgumentException("Class renderer source is unresolved: " + band.getBandName());
            }
            sources.add(raster);
        }
        return sources;
    }
}
