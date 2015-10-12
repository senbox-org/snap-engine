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

package org.esa.s3tbx.dataio.landsat.geotiff;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.VirtualDir;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.image.SourceImageScaler;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.snap.runtime.Config;

import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This reader is capable of reading Landsat data products
 * where each bands is distributes as a single GeoTIFF image.
 */
public class LandsatGeotiffReader extends AbstractProductReader {

    enum Resolution {
        DEFAULT,
        L8_PANCHROMATIC,
        L8_REFLECTIVE,

    }

    private static final Logger LOG = Logger.getLogger(LandsatGeotiffReader.class.getName());

    static final String SYSPROP_READ_AS = "s3tbx.landsat.readAs";
    private static final String RADIANCE_UNITS = "W/(m^2*sr*µm)";
    private static final String REFLECTANCE_UNITS = "dl";

    private final Resolution targetResolution;

    private LandsatMetadata landsatMetadata;
    private List<Product> bandProducts;
    private VirtualDir input;

    public LandsatGeotiffReader(ProductReaderPlugIn readerPlugin) {
        this(readerPlugin, Resolution.DEFAULT);
    }

    public LandsatGeotiffReader(ProductReaderPlugIn readerPlugin, Resolution targetResolution) {
        super(readerPlugin);
        this.targetResolution = targetResolution;
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        input = LandsatGeotiffReaderPlugin.getInput(getInput());
        String[] list = input.list("");
        File mtlFile = null;
        for (String fileName : list) {
            final File file = input.getFile(fileName);
            if (isMetadataFile(file)) {
                mtlFile = file;
                break;
            }
        }
        if (mtlFile == null) {
            throw new IOException("Can not find metadata file.");
        }
        if (!mtlFile.canRead()) {
            throw new IOException("Can not read metadata file: " + mtlFile.getAbsolutePath());
        }
        landsatMetadata = LandsatMetadataFactory.create(mtlFile);
        // todo - retrieving the product dimension needs a revision
        Dimension productDim;
        switch (targetResolution) {
            case L8_REFLECTIVE:
                productDim = landsatMetadata.getReflectanceDim();
                if (productDim == null) {
                    productDim = landsatMetadata.getThermalDim();
                }
                break;
            case L8_PANCHROMATIC:
                productDim = landsatMetadata.getPanchromaticDim();
                break;
            default:
                productDim = landsatMetadata.getPanchromaticDim();
                if (productDim == null) {
                    productDim = landsatMetadata.getReflectanceDim();
                }
                if (productDim == null) {
                    productDim = landsatMetadata.getThermalDim();
                }
        }

        MetadataElement metadataElement = landsatMetadata.getMetaDataElementRoot();
        Product product = new Product(getProductName(mtlFile), landsatMetadata.getProductType(), productDim.width, productDim.height);
        product.setFileLocation(mtlFile);
        product.getMetadataRoot().addElement(metadataElement);

        ProductData.UTC utcCenter = landsatMetadata.getCenterTime();
        product.setStartTime(utcCenter);
        product.setEndTime(utcCenter);

        addBands(product, input);

        return product;
    }

    private static String getProductName(File mtlfile) {
        String filename = mtlfile.getName();
        int extensionIndex = filename.toLowerCase().indexOf("_mtl.txt");
        return filename.substring(0, extensionIndex);
    }

    private void addBands(Product product, VirtualDir folder) throws IOException {
        final GeoTiffProductReaderPlugIn plugIn = new GeoTiffProductReaderPlugIn();
        final MetadataAttribute[] productAttributes = landsatMetadata.getProductMetadata().getAttributes();
        final Pattern pattern = landsatMetadata.getOpticalBandFileNamePattern();

        bandProducts = new ArrayList<>();
        for (MetadataAttribute metadataAttribute : productAttributes) {
            String attributeName = metadataAttribute.getName();
            Matcher matcher = pattern.matcher(attributeName);
            if (matcher.matches()) {
                String bandNumber = matcher.group(1);
                String fileName = metadataAttribute.getData().getElemString();

                File bandFile = folder.getFile(fileName);
                ProductReader productReader = plugIn.createReaderInstance();
                Product bandProduct = productReader.readProductNodes(bandFile, null);
                if (bandProduct != null) {
                    bandProducts.add(bandProduct);
                    Band srcBand = bandProduct.getBandAt(0);
                    String bandName = landsatMetadata.getBandNamePrefix(bandNumber);
                    Band band = addBandToProduct(bandName, srcBand, product);
                    band.setScalingFactor(landsatMetadata.getScalingFactor(bandNumber));
                    band.setScalingOffset(landsatMetadata.getScalingOffset(bandNumber));

                    band.setNoDataValue(0.0);
                    band.setNoDataValueUsed(true);

                    band.setSpectralWavelength(landsatMetadata.getWavelength(bandNumber));
                    band.setSpectralBandwidth(landsatMetadata.getBandwidth(bandNumber));

                    band.setDescription(landsatMetadata.getBandDescription(bandNumber));
                    band.setUnit(RADIANCE_UNITS);
                    final Preferences preferences = Config.instance("s3tbx").load().preferences();
                    final String readAs = preferences.get(LandsatGeotiffReader.SYSPROP_READ_AS, null);
                    if (readAs != null) {
                        switch (readAs.toLowerCase()) {
                            case "reflectance":
                                band.setDescription(landsatMetadata.getBandDescription(bandNumber) + " , as TOA Reflectance");
                                band.setUnit(REFLECTANCE_UNITS);
                                break;
                            default:
                                LOG.warning(String.format("Property '%s' has unsupported value '%s'",
                                                          LandsatGeotiffReader.SYSPROP_READ_AS, readAs));
                        }
                    }
                }
            } else if (attributeName.equals(landsatMetadata.getQualityBandNameKey())) {
                String fileName = metadataAttribute.getData().getElemString();
                File bandFile = folder.getFile(fileName);
                ProductReader productReader = plugIn.createReaderInstance();
                Product bandProduct = productReader.readProductNodes(bandFile, null);
                if (bandProduct != null) {
                    bandProducts.add(bandProduct);
                    Band srcBand = bandProduct.getBandAt(0);
                    String bandName = "flags";

                    Band band = addBandToProduct(bandName, srcBand, product);
                    band.setNoDataValue(0.0);
                    band.setNoDataValueUsed(true);
                    band.setDescription("Quality Band");

                    FlagCoding flagCoding = createFlagCoding(bandName);
                    band.setSampleCoding(flagCoding);
                    product.getFlagCodingGroup().add(flagCoding);
                    List<Mask> masks = createMasks(product);
                    for (Mask mask : masks) {
                        product.getMaskGroup().add(mask);
                    }

                }
            }
        }

        ImageLayout imageLayout = new ImageLayout();
        for (Product bandProduct : bandProducts) {
            if (product.getSceneGeoCoding() == null &&
                    product.getSceneRasterWidth() == bandProduct.getSceneRasterWidth() &&
                    product.getSceneRasterHeight() == bandProduct.getSceneRasterHeight()) {
                product.setSceneGeoCoding(bandProduct.getSceneGeoCoding());
                Dimension tileSize = bandProduct.getPreferredTileSize();
                if (tileSize == null) {
                    tileSize = ImageManager.getPreferredTileSize(bandProduct);
                }
                product.setPreferredTileSize(tileSize);
                imageLayout.setTileWidth(tileSize.width);
                imageLayout.setTileHeight(tileSize.height);
                break;
            }
        }


        if (Resolution.DEFAULT.equals(targetResolution)) {
            for (int i = 0; i < bandProducts.size(); i++) {
                Product bandProduct = bandProducts.get(i);
                Band band = product.getBandAt(i);
                final MultiLevelImage sourceImage = bandProduct.getBandAt(0).getSourceImage();
                band.setSourceImage(sourceImage);
                band.setGeoCoding(bandProduct.getSceneGeoCoding());
            }
        } else {
            MultiLevelImage targetImage = null;
            for (Product bandProduct : bandProducts) {
                if (product.getSceneRasterWidth() == bandProduct.getSceneRasterWidth() &&
                        product.getSceneRasterHeight() == bandProduct.getSceneRasterHeight()) {
                    targetImage = bandProduct.getBandAt(0).getSourceImage();
                    break;
                }
            }
            if (targetImage == null) {
                throw new IllegalStateException("Could not determine target image");
            }
            for (int i = 0; i < bandProducts.size(); i++) {
                Product bandProduct = bandProducts.get(i);
                final MultiLevelImage sourceImage = bandProduct.getBandAt(0).getSourceImage();
                final RenderingHints renderingHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
                float[] scalings = new float[2];
                scalings[0] = product.getSceneRasterWidth() / (float) bandProduct.getSceneRasterWidth();
                scalings[1] = product.getSceneRasterHeight() / (float) bandProduct.getSceneRasterHeight();

                Band band = product.getBandAt(i);
                PlanarImage image = SourceImageScaler.scaleMultiLevelImage(targetImage, sourceImage, scalings, null, renderingHints,
                                                                           band.getNoDataValue(),
                                                                           Interpolation.getInstance(Interpolation.INTERP_NEAREST));
                band.setSourceImage(image);
            }
        }
    }

    private Band addBandToProduct(String bandName, Band srcBand, Product product) {
        Dimension bandDimension = getBandDimension(srcBand, targetResolution);
        Band band = new Band(bandName, srcBand.getDataType(), bandDimension.width, bandDimension.height);
        product.addBand(band);
        return band;
    }

    private Dimension getBandDimension(Band srcBand, Resolution targetResolution) {
        switch (targetResolution) {
            case L8_REFLECTIVE:
                return landsatMetadata.getReflectanceDim();
            case L8_PANCHROMATIC:
                return landsatMetadata.getPanchromaticDim();
            default:
                return srcBand.getRasterSize();
        }
    }

    private List<Mask> createMasks(Product product) {
        ArrayList<Mask> masks = new ArrayList<>();
        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();

        masks.add(Mask.BandMathsType.create("designated_fill",
                                            "Designated Fill",
                                            width, height,
                                            "flags.designated_fill",
                                            ColorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("dropped_frame",
                                            "Dropped Frame",
                                            width, height,
                                            "flags.dropped_frame",
                                            ColorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("terrain_occlusion",
                                            "Terrain Occlusion",
                                            width, height,
                                            "flags.terrain_occlusion",
                                            ColorIterator.next(),
                                            0.5));
        masks.addAll(createConfidenceMasks("water_confidence", "Water confidence", width, height));
        masks.addAll(createConfidenceMasks("vegetation_confidence", "Vegetation confidence", width, height));
        masks.addAll(createConfidenceMasks("snow_ice_confidence", "Snow/ice confidence", width, height));
        masks.addAll(createConfidenceMasks("cirrus_confidence", "Cirrus confidence", width, height));
        masks.addAll(createConfidenceMasks("cloud_confidence", "Cloud confidence", width, height));

        return masks;
    }

    private List<Mask> createConfidenceMasks(String flagMaskBaseName, String descriptionBaseName, int width, int height) {
        List<Mask> masks = new ArrayList<>();
        masks.add(Mask.BandMathsType.create(flagMaskBaseName + "_low",
                                            descriptionBaseName + " 0-35%",
                                            width, height,
                                            "flags." + flagMaskBaseName + "_one and not flags." + flagMaskBaseName + "_two",
                                            ColorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create(flagMaskBaseName + "_mid",
                                            descriptionBaseName + " 36-64%",
                                            width, height,
                                            "not flags." + flagMaskBaseName + "_one and flags." + flagMaskBaseName + "_two",
                                            ColorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create(flagMaskBaseName + "_high",
                                            descriptionBaseName + " 65-100%",
                                            width, height,
                                            "flags." + flagMaskBaseName + "_one and flags." + flagMaskBaseName + "_two",
                                            ColorIterator.next(),
                                            0.5));
        return masks;
    }

    private FlagCoding createFlagCoding(String bandName) {
        FlagCoding flagCoding = new FlagCoding(bandName);
        flagCoding.addFlag("designated_fill", 1, "Designated Fill");
        flagCoding.addFlag("dropped_frame", 2, "Dropped Frame");
        flagCoding.addFlag("terrain_occlusion", 4, "Terrain Occlusion");
        flagCoding.addFlag("reserved_1", 8, "Reserved for a future 1-bit class artifact designation");
        flagCoding.addFlag("water_confidence_one", 16, "Water confidence bit one");
        flagCoding.addFlag("water_confidence_two", 32, "Water confidence bit two");
        flagCoding.addFlag("reserved_2_one", 64, "Reserved for a future 2-bit class artifact designation");
        flagCoding.addFlag("reserved_2_two", 128, "Reserved for a future 2-bit class artifact designation");
        flagCoding.addFlag("vegetation_confidence_one", 256, "Vegetation confidence bit one");
        flagCoding.addFlag("vegetation_confidence_two", 512, "Vegetation confidence bit two");
        flagCoding.addFlag("snow_ice_confidence_one", 1024, "Snow/ice confidence bit one");
        flagCoding.addFlag("snow_ice_confidence_two", 2048, "Snow/ice confidence bit two");
        flagCoding.addFlag("cirrus_confidence_one", 4096, "Cirrus confidence bit one");
        flagCoding.addFlag("cirrus_confidence_two", 8192, "Cirrus confidence bit two");
        flagCoding.addFlag("cloud_confidence_one", 16384, "Cloud confidence bit one");
        flagCoding.addFlag("cloud_confidence_two", 32768, "Cloud confidence bit two");
        return flagCoding;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY,
                                          Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        // all bands use source images as source for its data
        throw new IllegalStateException();
    }

    @Override
    public void close() throws IOException {
        for (Product bandProduct : bandProducts) {
            bandProduct.closeIO();
        }
        bandProducts.clear();
        input.close();
        input = null;
        super.close();
    }

    static boolean isMetadataFile(File file) {
        final String filename = file.getName().toLowerCase();
        return filename.endsWith("_mtl.txt");
    }

    private static class ColorIterator {

        static ArrayList<Color> colors;
        static Iterator<Color> colorIterator;

        static {
            colors = new ArrayList<>();
            colors.add(Color.red);
            colors.add(Color.red.darker());
            colors.add(Color.red.darker().darker());
            colors.add(Color.blue);
            colors.add(Color.blue.darker());
            colors.add(Color.blue.darker().darker());
            colors.add(Color.green);
            colors.add(Color.green.darker());
            colors.add(Color.green.darker().darker());
            colors.add(Color.yellow);
            colors.add(Color.yellow.darker());
            colors.add(Color.yellow.darker().darker());
            colors.add(Color.magenta);
            colors.add(Color.magenta.darker());
            colors.add(Color.magenta.darker().darker());
            colors.add(Color.pink);
            colors.add(Color.pink.darker());
            colors.add(Color.pink.darker().darker());
            colorIterator = colors.iterator();
        }

        static Color next() {
            if (!colorIterator.hasNext()) {
                colorIterator = colors.iterator();
            }
            return colorIterator.next();
        }
    }

}
