/*
 *
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
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
 *
 */

package org.esa.snap.dataio.znap;

import com.bc.zarr.DataType;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ZnapConstantsAndUtils {

    public static final int IDX_WIDTH = 1;
    public static final int IDX_X = IDX_WIDTH;
    public static final int IDX_HEIGHT = 0;
    public static final int IDX_Y = IDX_HEIGHT;

    static final String FORMAT_NAME = "ZNAP";
    static final String ZNAP_CONTAINER_EXTENSION = ".znap";
    static final String ZNAP_ZIP_CONTAINER_EXTENSION = ".znap.zip";

    static final String NAME_SNAP_SUBGROUP = "SNAP";
    static final String NAME_VECTOR_DATA_SUBGROUP = "vector_data";

    static final String KEY_SNAP_SUBGROUP = NAME_SNAP_SUBGROUP;
    static final String KEY_SNAP_VECTOR_DATA_SUBGROUP = KEY_SNAP_SUBGROUP + "/vector_data";
    static final String KEY_SNAP_PRODUCT_METADATA_JSON = KEY_SNAP_SUBGROUP + "/product_metadata.json";

    public static final String UNIT_EXTENSION = "_unit";
    public static final String BANDWIDTH = "bandwidth";
    public static final String BANDWIDTH_UNIT = "nm";
    public static final String WAVELENGTH = "wavelength";
    public static final String WAVELENGTH_UNIT = "nm";
    public static final String VALID_PIXEL_EXPRESSION = "valid_pixel_expression";
    public static final String QUICKLOOK_BAND_NAME = "quicklook_band_name";
    public static final String SOLAR_FLUX = "solar_flux";
    public static final String SPECTRAL_BAND_INDEX = "spectral_band_index";
    public static final String VIRTUAL_BAND_EXPRESSION = "virtual_band_expression";

    public static final String NO_DATA_VALUE_USED = "no_data_value_used";

    public static final String IMAGE_INFO = "image_info";
    public static final String STATISTICS = "statistics";
    public static final String COLOR_PALETTE_POINTS = "color_palette_points";
    public static final String COLOR_PALETTE_NUM_COLORS = "color_palette_num_colors";
    public static final String COLOR_PALETTE_DISCRETE = "color_palette_discrete";
    public static final String COLOR_PALETTE_AUTO_DISTRIBUTE = "color_palette_auto_dist";
    public static final String SAMPLE = "sample";
    public static final String LABEL = "label";
    public static final String COLOR_RGBA = "color_rgba";
    public static final String HISTOGRAM_MATCHING = "histogram_matching";
    public static final String LOG_10_SCALED = "log_10_scaled";
    public static final String NO_DATA_COLOR_RGBA = "no_data_color";
    public static final String UNCERTAINTY_BAND_NAME = "uncertainty_band_name";
    public static final String UNCERTAINTY_VISUALISATION_MODE = "uncertainty_visualisation_mode";

    // Product attributes
    public static final String DATASET_AUTO_GROUPING = "dataset_auto_grouping";

    // Tie point grid attributes
    public static final String DISCONTINUITY = "discontinuity";

    // Sample coding attributes
    public static final String FLAG_DESCRIPTIONS = "flag_descriptions";

    // Product header attribute names
    public static final String ATT_NAME_PRODUCT_NAME = "product_name";
    public static final String ATT_NAME_PRODUCT_TYPE = "product_type";
    public static final String ATT_NAME_PRODUCT_DESC = "product_description";
    public static final String ATT_NAME_PRODUCT_SCENE_WIDTH = "product_scene_raster_width";
    public static final String ATT_NAME_PRODUCT_SCENE_HEIGHT = "product_scene_raster_height";
    public static final String ATT_NAME_BINARY_FORMAT = ZnapPreferencesConstants.PROPERTY_NAME_BINARY_FORMAT.replace(".", "_");
    public static final String ATT_NAME_GEOCODING = "geocoding";
    public static final String ATT_NAME_ORIGINAL_RASTER_DATA_NODE_ORDER = "original_raster_data_node_order";

    // Tie point grid attribute names
    public static final String ATT_NAME_OFFSET_X = "offset_x";
    public static final String ATT_NAME_OFFSET_Y = "offset_y";
    public static final String ATT_NAME_SUBSAMPLING_X = "subsampling_x";
    public static final String ATT_NAME_SUBSAMPLING_Y = "subsampling_y";

    // Sample Coding
    public static final String NAME_SAMPLE_CODING = "name_sample_coding";

    // Others
    public static final String NAME_MASKS = "Masks";
    public static final String NAME_FILTER_BANDS = "FilterBands";

    static boolean isExistingEmptyDirectory(Path path) {
        try {
            return Files.isDirectory(path) && Files.list(path).count() == 0;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    static SnapDataType getSnapDataType(DataType zarrDataType) {
        if (zarrDataType == DataType.f8) {
            return SnapDataType.TYPE_FLOAT64;
        } else if (zarrDataType == DataType.f4) {
            return SnapDataType.TYPE_FLOAT32;
        } else if (zarrDataType == DataType.i1) {
            return SnapDataType.TYPE_INT8;
        } else if (zarrDataType == DataType.u1) {
            return SnapDataType.TYPE_UINT8;
        } else if (zarrDataType == DataType.i2) {
            return SnapDataType.TYPE_INT16;
        } else if (zarrDataType == DataType.u2) {
            return SnapDataType.TYPE_UINT16;
        } else if (zarrDataType == DataType.i4) {
            return SnapDataType.TYPE_INT32;
        } else if (zarrDataType == DataType.u4) {
            return SnapDataType.TYPE_UINT32;
        } else {
            throw new IllegalStateException();
        }
    }

    static final SimpleModule metadataModule;

    static {

        final StdSerializer<MetadataElement> metadataElementStdSerializer = new StdSerializer<MetadataElement>(MetadataElement.class) {
            @Override
            public void serialize(MetadataElement value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                gen.writeStartObject();
                {
                    gen.writeStringField("name", value.getName());
                    writeNonEmptyStringField(gen, "description", value.getDescription());
                    final MetadataAttribute[] attributes = value.getAttributes();
                    if (attributes.length > 0) {
                        gen.writeArrayFieldStart("attributes");
                        for (MetadataAttribute attribute : attributes) {
                            gen.writeObject(attribute);
                        }
                        gen.writeEndArray();
                    }
                    final MetadataElement[] elements = value.getElements();
                    if (elements.length > 0) {
                        gen.writeArrayFieldStart("elements");
                        for (MetadataElement element : elements) {
                            gen.writeObject(element);
                        }
                        gen.writeEndArray();
                    }
                }
                gen.writeEndObject();
            }
        };
        final StdDeserializer<?> metadataElementStdDeserializer = new StdDeserializer<MetadataElement>(MetadataElement.class) {
            @Override
            public MetadataElement deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                final ObjectCodec codec = p.getCodec();
                final JsonNode node = codec.readTree(p);
                final String name = node.get("name").asText();
                final MetadataElement element = new MetadataElement(name);
                if (node.has("description")) {
                    element.setDescription(node.get("description").asText());
                }
                if (node.has("attributes")) {
                    final ArrayNode attributes = node.withArray("attributes");
                    for (JsonNode next : attributes) {
                        if (!next.isObject()) {
                            throw new IllegalStateException("Object expected!");
                        }
                        final MetadataAttribute metadataAttribute = codec.readValue(next.traverse(codec), MetadataAttribute.class);
                        element.addAttribute(metadataAttribute);
                    }
                }
                if (node.has("elements")) {
                    final ArrayNode elements = node.withArray("elements");
                    for (JsonNode next : elements) {
                        if (!next.isObject()) {
                            throw new IllegalStateException("Object expected!");
                        }
                        final MetadataElement metadataElement = codec.readValue(next.traverse(codec), MetadataElement.class);
                        element.addElement(metadataElement);
                    }
                }
                return element;
            }
        };
        final StdSerializer<MetadataAttribute> metadataAttributeStdSerializer = new StdSerializer<MetadataAttribute>(MetadataAttribute.class) {
            @Override
            public void serialize(MetadataAttribute value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                gen.writeStartObject();
                {
                    gen.writeStringField("name", value.getName());
                    gen.writeBooleanField("readOnly", value.isReadOnly());
                    gen.writeObjectField("data", value.getData());
                    writeNonEmptyStringField(gen, "unit", value.getUnit());
                    writeNonEmptyStringField(gen, "description", value.getDescription());
                }
                gen.writeEndObject();
            }
        };
        final StdDeserializer<MetadataAttribute> metadataAttributeStdDeserializer = new StdDeserializer<MetadataAttribute>(MetadataAttribute.class) {
            @Override
            public MetadataAttribute deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                final ObjectCodec codec = p.getCodec();
                final JsonNode node = codec.readTree(p);
                final String name = node.get("name").asText();
                final boolean readOnly = node.get("readOnly").asBoolean();
                final ProductData data = codec.readValue(node.get("data").traverse(codec), ProductData.class);
                final MetadataAttribute attribute = new MetadataAttribute(name, data, readOnly);
                if (node.has("unit")) {
                    attribute.setUnit(node.get("unit").asText());
                }
                if (node.has("description")) {
                    attribute.setDescription(node.get("description").asText());
                }
                return attribute;
            }
        };
        final StdSerializer<ProductData> productDataStdSerializer = new StdSerializer<ProductData>(ProductData.class) {
            @Override
            public void serialize(ProductData value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                gen.writeStartObject();
                {
                    gen.writeStringField("type", value.getTypeString());
                    switch (value.getType()) {
                        case ProductData.TYPE_ASCII:
                            gen.writeObjectField("elems", value.toString());
                            break;
                        case ProductData.TYPE_INT8:
                        case ProductData.TYPE_UINT8:
                            gen.writeBinaryField("elems", (byte[]) value.getElems());
                            break;
                        default:
                            gen.writeObjectField("elems", value.getElems());
                            break;
                    }
                }
                gen.writeEndObject();
            }
        };
        final StdDeserializer<ProductData> productDataStdDeserializer = new StdDeserializer<ProductData>(ProductData.class) {
            @Override
            public ProductData deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
                ObjectCodec codec = parser.getCodec();
                JsonNode node = codec.readTree(parser);

                final int type = ProductData.getType(node.get("type").asText());
                final Object o;
                switch (type) {
                    case ProductData.TYPE_ASCII:
                        o = node.get("elems").asText();
                        break;
                    case ProductData.TYPE_INT8:
                    case ProductData.TYPE_UINT8:
                        o = node.get("elems").binaryValue();
                        break;
                    case ProductData.TYPE_INT16:
                    case ProductData.TYPE_UINT16:
                        o = codec.readValue(node.get("elems").traverse(codec), short[].class);
                        break;
                    case ProductData.TYPE_INT32:
                    case ProductData.TYPE_UINT32:
                    case ProductData.TYPE_UTC:
                        o = codec.readValue(node.get("elems").traverse(codec), int[].class);
                        break;
                    case ProductData.TYPE_INT64:
                        o = codec.readValue(node.get("elems").traverse(codec), long[].class);
                        break;
                    case ProductData.TYPE_FLOAT32:
                        o = codec.readValue(node.get("elems").traverse(codec), float[].class);
                        break;
                    case ProductData.TYPE_FLOAT64:
                        o = codec.readValue(node.get("elems").traverse(codec), double[].class);
                        break;
                    default:
                        o = null;
                }
                return ProductData.createInstance(type, o);
            }
        };

        final HashMap<Class<?>, JsonDeserializer<?>> deserializers = new HashMap<>();
        deserializers.put(MetadataElement.class, metadataElementStdDeserializer);
        deserializers.put(MetadataAttribute.class, metadataAttributeStdDeserializer);
        deserializers.put(ProductData.class, productDataStdDeserializer);

        final ArrayList<JsonSerializer<?>> serializers = new ArrayList<>();
        serializers.add(metadataElementStdSerializer);
        serializers.add(metadataAttributeStdSerializer);
        serializers.add(productDataStdSerializer);
        metadataModule = new SimpleModule("Metadata", new Version(1, 0, 0, null, null, null), deserializers, serializers);
    }

    public static MetadataElement[] jsonToMetadata(String jsonMetadataString) throws IOException {
        return readProductMetadata(new StringReader(jsonMetadataString));
    }

    public static String metadataToJson(MetadataElement[] metadata) throws IOException {
        StringWriter stringWriter = new StringWriter();
        writeProductMetadata(stringWriter, metadata);
        return stringWriter.toString();
    }

    public static void writeProductMetadata(Writer out, MetadataElement[] metadata) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(metadataModule);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(out, metadata);
    }

    public static MetadataElement[] readProductMetadata(Reader in) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(metadataModule);
        return objectMapper.readValue(in, MetadataElement[].class);
    }

    public static MetadataElement[] listToMetadata(List<?> metadata) throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(metadataModule);
        final String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(metadata);
        return objectMapper.readValue(json, MetadataElement[].class);
    }

    public static List<Map<String, Object>> metadataToList(MetadataElement[] metadata) throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(metadataModule);
        final String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(metadata);
        TypeReference<List<Map<String, Object>>> typeRef
                = new TypeReference<List<Map<String, Object>>>() {};
        return objectMapper.readValue(json, typeRef);
    }

    private static void writeNonEmptyStringField(JsonGenerator gen, String fieldName, String string) throws IOException {
        if (string != null && string.trim().length() > 0) {
            gen.writeStringField(fieldName, string.trim());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T cast(Object o) {
        return (T) o;
    }
}
