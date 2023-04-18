/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.dataio.geotiff;

import com.sun.media.jai.codec.TIFFField;
import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.GeoTIFFTagSet;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageMetadata;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.util.geotiff.GeoTIFFMetadata;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public class Utils {

    public static final int PRIVATE_BEAM_TIFF_TAG_NUMBER = 65000;

    /**
     * Tests if the given file is a cloud-optimized GeoTIFF or not.
     *
     * @param filePath The file path
     */
    public static boolean isCOGGeoTIFF(Path filePath) throws Exception {
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(filePath.toFile())) {
            final Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(imageInputStream);
            TIFFImageReader tiffReader = null;
            while (imageReaders.hasNext()) {
                final ImageReader reader = imageReaders.next();
                if (reader instanceof TIFFImageReader) {
                    tiffReader = (TIFFImageReader) reader;
                    tiffReader.setInput(imageInputStream);
                    break;
                }
            }
            if (tiffReader == null) {
                throw new Exception("No tiff reader found!");
            }

            return isCOGGeoTIFF(tiffReader);
        }
    }

    /**
     * Tests if the given image stream is a cloud-optimized GeoTIFF or not.
     *
     * @param tiffReader The tiff reader
     */
    public static boolean isCOGGeoTIFF(TIFFImageReader tiffReader) throws IOException {
        final int numImages = tiffReader.getNumImages(true);
        int numReducedImages = 0;
        if (numImages > 1) {
            for (int i = 1; i < numImages; i++) {
                final TIFFImageMetadata metadata = (TIFFImageMetadata) tiffReader.getImageMetadata(i);
                final TiffFileInfo tiffInfo = new TiffFileInfo(metadata.getRootIFD());
                final it.geosolutions.imageio.plugins.tiff.TIFFField subFileType = tiffInfo.getField(BaselineTIFFTagSet.TAG_SUBFILE_TYPE);
                final it.geosolutions.imageio.plugins.tiff.TIFFField newSubFileType = tiffInfo.getField(BaselineTIFFTagSet.TAG_NEW_SUBFILE_TYPE);
                if (newSubFileType == null && subFileType == null) {
                    break;
                }
                if (newSubFileType != null && BaselineTIFFTagSet.NEW_SUBFILE_TYPE_REDUCED_RESOLUTION == newSubFileType.getAsInt(0)) {
                    numReducedImages++;
                }
                if (newSubFileType == null && BaselineTIFFTagSet.SUBFILE_TYPE_REDUCED_RESOLUTION == subFileType.getAsInt(0)) {
                    numReducedImages++;
                }

                final it.geosolutions.imageio.plugins.tiff.TIFFField imageWidth = tiffInfo.getField(BaselineTIFFTagSet.TAG_IMAGE_WIDTH);
                final TIFFImageMetadata prevMetadata = (TIFFImageMetadata) tiffReader.getImageMetadata(i - 1);
                final TiffFileInfo prevTiffInfo = new TiffFileInfo(prevMetadata.getRootIFD());
                final it.geosolutions.imageio.plugins.tiff.TIFFField prevImageWidth = prevTiffInfo.getField(BaselineTIFFTagSet.TAG_IMAGE_WIDTH);
                if (prevImageWidth.getAsInt(0) < (imageWidth.getAsInt(0) - 1) * 2) {
                    break;
                }
            }
        }
        return numImages > 1 && numReducedImages == numImages - 1;
    }

    static List<TIFFField> createGeoTIFFFields(GeoTIFFMetadata geoTIFFMetadata) {
        final List<TIFFField> list = new ArrayList<>(6);
        // Geo Key Directory
        final int numGeoKeyEntries = geoTIFFMetadata.getNumGeoKeyEntries();
        if (numGeoKeyEntries > 0) {
            final GeoTIFFMetadata.KeyEntry[] keyEntries = geoTIFFMetadata.getGeoKeyEntries();
            final char[] values = new char[keyEntries.length * 4];
            for (int i = 0; i < keyEntries.length; i++) {
                final int[] data = keyEntries[i].getData();
                for (int j = 0; j < data.length; j++) {
                    values[i * 4 + j] = (char) data[j];
                }
            }
            final TIFFField geoKeyDirectory = new TIFFField(GeoTIFFTagSet.TAG_GEO_KEY_DIRECTORY,
                                                            TIFFField.TIFF_SHORT,
                                                            values.length,
                                                            values);
            list.add(geoKeyDirectory);
        }


        // Geo Double Params
        final double[] geoDoubleParams = geoTIFFMetadata.getGeoDoubleParams();
        if (geoDoubleParams != null && geoDoubleParams.length > 0) {
            list.add(new TIFFField(GeoTIFFTagSet.TAG_GEO_DOUBLE_PARAMS,
                                   TIFFField.TIFF_DOUBLE,
                                   geoDoubleParams.length,
                                   geoDoubleParams));
        }

        // Geo ASCII Params
        final String geoAsciiString = geoTIFFMetadata.getGeoAsciiParams();
        if (geoAsciiString != null && geoAsciiString.length() > 0) {
            final StringTokenizer tokenizer = new StringTokenizer(geoAsciiString, "|");
            final String[] geoAsciiStrings = new String[tokenizer.countTokens()];
            for (int i = 0; i < geoAsciiStrings.length; i++) {
                geoAsciiStrings[i] = tokenizer.nextToken().concat("|");
            }
            list.add(new TIFFField(GeoTIFFTagSet.TAG_GEO_ASCII_PARAMS,
                                   TIFFField.TIFF_ASCII,
                                   geoAsciiStrings.length,
                                   geoAsciiStrings));
        }

        // Model Pixel Scale
        final double[] modelPixelScale = geoTIFFMetadata.getModelPixelScale();
        if (Utils.isValidModelPixelScale(modelPixelScale)) {
            list.add(new TIFFField(GeoTIFFTagSet.TAG_MODEL_PIXEL_SCALE,
                                   TIFFField.TIFF_DOUBLE,
                                   modelPixelScale.length,
                                   modelPixelScale));
        }

        // Model Tie Point
        final int numModelTiePoints = geoTIFFMetadata.getNumModelTiePoints();
        if (numModelTiePoints > 0) {
            final double[] tiePointValues = new double[numModelTiePoints * 6];
            for (int i = 0; i < numModelTiePoints; i++) {
                final GeoTIFFMetadata.TiePoint tiePoint = geoTIFFMetadata.getModelTiePointAt(i);
                final double[] data = tiePoint.getData();
                System.arraycopy(data, 0, tiePointValues, i * data.length, data.length);
            }

            list.add(new TIFFField(GeoTIFFTagSet.TAG_MODEL_TIE_POINT,
                                   TIFFField.TIFF_DOUBLE,
                                   tiePointValues.length,
                                   tiePointValues));
        }

        // Model Transformation
        final double[] modelTransformation = geoTIFFMetadata.getModelTransformation();

        if (isValidModelTransformation(modelTransformation)) {
            list.add(new TIFFField(GeoTIFFTagSet.TAG_MODEL_TRANSFORMATION,
                                   TIFFField.TIFF_DOUBLE,
                                   modelTransformation.length,
                                   modelTransformation));
        }

        return list;
    }

    static boolean isValidModelTransformation(double[] modelTransformation) {
        final double[] defaultValues = new double[16];
        return isValidData(modelTransformation, defaultValues);
    }

    static boolean isValidModelPixelScale(double[] modelTransformation) {
        final double[] defaultValues = {1, 1, 0};
        return isValidData(modelTransformation, defaultValues);
    }

    private static boolean isValidData(double[] modelTransformation, double[] defaultValues) {
        if (modelTransformation != null && modelTransformation.length == defaultValues.length) {
            for (int i = 0; i < modelTransformation.length; i++) {
                double v = modelTransformation[i];
                final double dv = defaultValues[i];
                if (v != dv) {
                    return true;
                }
            }
        }
        return false;
    }

    static String[] findSuitableLatLonNames(Product product) {
        final String[] latNames = {"latitude", "latitude_tpg", "lat", "lat_tpg"};
        final String[] lonNames = {"longitude", "longitude_tpg", "lon", "lon_tpg"};
        String[] names = new String[2];
        for (int i = 0; i < latNames.length; i++) {
            String latName = latNames[i];
            String lonName = lonNames[i];
            if (!product.containsRasterDataNode(latName) && !product.containsRasterDataNode(lonName)) {
                names[0] = latName;
                names[1] = lonName;
                return names;
            }
        }
        String lonName = lonNames[0] + "_";
        String latName = latNames[0] + "_";
        int index = 1;
        while (product.containsRasterDataNode(latName + index) || product.containsRasterDataNode(lonName + index)) {
            index++;
        }
        return new String[]{latName + index, lonName + index};
    }

    public static boolean shouldWriteNode(ProductNode node) {
        if (node instanceof VirtualBand) {
            return false;
        } else if (node instanceof FilterBand) {
            return false;
        }
        return true;
    }

    /**
     * Parses a GDAL metadata string and takes the information to set up the bands for a target product with a
     * maximum of information. Acutally implemented and tested for the purpose of improved reading of
     * Proba-V S* GeoTiff products (see SIIITBX-85).
     *
     * @param gdalMetadataXmlString - GDAL metadata XLM string
     * @param productDataType       - product data type
     * @param width                 - product width
     * @param height                - product height
     * @return array of bands
     * @throws Exception -
     */
    public static Band[] setupBandsFromGdalMetadata(String gdalMetadataXmlString, int productDataType, int width, int height)
            throws Exception {

        final DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(gdalMetadataXmlString));

        final Document doc = db.parse(is);
        final NodeList nodes = doc.getElementsByTagName("GDALMetadata");
        final Element element = (Element) nodes.item(0);

        ArrayList<String> bandnameList = new ArrayList<>();
        ArrayList<String> descrList = new ArrayList<>();
        ArrayList<String> unitsList = new ArrayList<>();
        ArrayList<String> nodataList = new ArrayList<>();
        ArrayList<String> offsetList = new ArrayList<>();
        ArrayList<String> scaleList = new ArrayList<>();
        for (int i = 0; i < element.getElementsByTagName("Item").getLength(); i++) {
            final Node node = element.getElementsByTagName("Item").item(i);
            final Node child = node.getFirstChild();
            final CharacterData cd = (CharacterData) child;
            if (node.hasAttributes() && cd != null) {
                for (int j = 0; j < node.getAttributes().getLength(); j++) {
                    final Node attr = node.getAttributes().item(j);
                    if (attr.getNodeName().equals("name")) {
                        switch (attr.getNodeValue()) {
                            case "BAND":
                                bandnameList.add(cd.getData());
                                break;
                            case "DESCRIPTION":
                                descrList.add(cd.getData());
                                break;
                            case "UNITS":
                                unitsList.add(cd.getData());
                                break;
                            case "NODATA":
                                nodataList.add(cd.getData());
                                break;
                            case "OFFSET":
                                offsetList.add(cd.getData());
                                break;
                            case "SCALE":
                                scaleList.add(cd.getData());
                                break;
                        }
                    }
                }
            }
        }

        Iterator<String> descrListIterator = descrList.iterator();
        Iterator<String> unitsListIterator = unitsList.iterator();
        Iterator<String> nodataListIterator = nodataList.iterator();
        Iterator<String> offsetListIterator = offsetList.iterator();
        Iterator<String> scaleListIterator = scaleList.iterator();

        Band[] bands = new Band[bandnameList.size()];
        for (int i = 0; i < bandnameList.size(); i++) {
            bands[i] = new Band(bandnameList.get(i), productDataType, width, height);
            if (descrListIterator.hasNext()) {
                bands[i].setDescription(descrListIterator.next());
            }
            if (unitsListIterator.hasNext()) {
                bands[i].setUnit(unitsListIterator.next());
            }
            if (nodataListIterator.hasNext()) {
                final String nodataValString = nodataListIterator.next();
                if (nodataValString != null) {
                    final double nodataVal = Double.parseDouble(nodataValString);
                    bands[i].setNoDataValue(nodataVal);
                    bands[i].setNoDataValueUsed(true);
                }
            }
            if (offsetListIterator.hasNext()) {
                final String offsetValString = offsetListIterator.next();
                if (offsetValString != null) {
                    final double offsetVal = Double.parseDouble(offsetValString);
                    bands[i].setScalingOffset(offsetVal);
                }
            }
            if (scaleListIterator.hasNext()) {
                final String scaleValString = scaleListIterator.next();
                if (scaleValString != null) {
                    final double scaleVal = Double.parseDouble(scaleValString);
                    bands[i].setScalingFactor(scaleVal);
                }
            }
        }

        return bands;
    }

    /**
     * Checks the magical first bytes of the stream for the GeoTIFF identifier.
     * The stream position is reset after reading the bytes.
     *
     * @param stream the stream to read from.
     * @return 'Tiff', 'BigTiff' or 'Unknown' depending on the result.
     * @throws IOException in case an IO error occurs
     */
    public static String getTiffMode(ImageInputStream stream) throws IOException {
        try {
            stream.mark();
            int byteOrder = stream.readUnsignedShort();
            switch (byteOrder) {
                case 0x4d4d:
                    stream.setByteOrder(ByteOrder.BIG_ENDIAN);
                    break;
                case 0x4949:
                    stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
                    break;
                default:
                    // Fallback
                    stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
                    break;
            }

            int magic = stream.readUnsignedShort();
            switch (magic) {
                case 43:
                    // BIG-TIFF
                    return "BigTiff";
                case 42:
                    // normal TIFF
                    return "Tiff";
                default:
                    return "Unknown";
            }
        } finally {
            stream.reset();
        }
    }
}
