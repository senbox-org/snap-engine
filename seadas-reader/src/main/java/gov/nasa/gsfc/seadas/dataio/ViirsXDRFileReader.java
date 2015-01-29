/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package gov.nasa.gsfc.seadas.dataio;

import org.esa.beam.dataio.netcdf.util.NetcdfFileOpener;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.*;
import ucar.ma2.Array;
import ucar.nc2.*;
import ucar.nc2.Dimension;

import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

public class ViirsXDRFileReader extends SeadasFileReader {

    ViirsXDRFileReader(SeadasProductReader productReader) {
        super(productReader);
    }

    @Override
    public Product createProduct() throws ProductIOException {

        try {
            List<Dimension> dims;
            String CollectionShortName = getCollectionShortName();

            if (productReader.getProductType() == SeadasProductReader.ProductType.VIIRS_EDR) {
                String groupName = "All_Data/" + CollectionShortName + "_All";
                Group edrGroup = ncFile.findGroup(groupName);
                dims = edrGroup.getVariables().get(0).getDimensions();
            } else if (productReader.getProductType() == SeadasProductReader.ProductType.VIIRS_SDR) {
                String varName = "All_Data/" + CollectionShortName + "_All/Radiance";
                Variable exampleRadiance = ncFile.findVariable(varName);
                dims = exampleRadiance.getDimensions();
            } else if (productReader.getProductType() == SeadasProductReader.ProductType.VIIRS_GEO) {
                String varName = "All_Data/" + CollectionShortName + "_All/Height";
                Variable exampleRadiance = ncFile.findVariable(varName);
                dims = exampleRadiance.getDimensions();
            } else if (productReader.getProductType() == SeadasProductReader.ProductType.VIIRS_IP) {
                if (CollectionShortName.equals("VIIRS-DualGain-Cal-IP")) {
                    String varName = "All_Data/" + CollectionShortName + "_All/radiance_0";
                    Variable exampleRadiance = ncFile.findVariable(varName);
                    dims = exampleRadiance.getDimensions();
                }
//                todo: One day, maybe, add support for the OBC files.
                else {
                    String message = "Unsupported VIIRS Product: " + CollectionShortName;
                    throw new ProductIOException(message);
                }
            } else {
                String message = "Unsupported VIIRS Product: " + CollectionShortName;
                throw new ProductIOException(message);
            }


            int sceneHeight = dims.get(0).getLength();
            int sceneWidth = dims.get(1).getLength();

            String productName = productReader.getInputFile().getName();

            mustFlipX = mustFlipY = mustFlipVIIRS();
            SeadasProductReader.ProductType productType = productReader.getProductType();

            Product product = new Product(productName, productType.toString(), sceneWidth, sceneHeight);
            product.setDescription(productName);

            setStartEndTime(product);

            product.setFileLocation(productReader.getInputFile());
            product.setProductReader(productReader);

            addGlobalAttributeVIIRS();
            addGlobalMetadata(product);

            variableMap = addBands(product, ncFile.getVariables());

            addGeocoding(product);

            product.setAutoGrouping("IOP:QF:nLw:Radiance:radiance:Reflectance");
            addFlagsAndMasks(product);

            setSpectralBand(product);
            return product;
        } catch (Exception e) {
            throw new ProductIOException(e.getMessage());
        }
    }

    @Override
    protected void setSpectralBand(Product product) {
        //todo Add units
        int spectralBandIndex = 0;
        for (String name : product.getBandNames()) {
            Band band = product.getBandAt(product.getBandIndex(name));
            if (name.matches(".*\\w+_\\d+.*")) {
                String wvlstr = null;
                if (name.matches("IOP.*_\\d+.*")) {
                    wvlstr = name.split("_")[2].split("nm")[0];
                } else if (name.matches("nLw_\\d+nm")) {
                    wvlstr = name.split("_")[1].split("nm")[0];
                }
                if (wvlstr != null){
                    final float wavelength = Float.parseFloat(wvlstr);
                    band.setSpectralWavelength(wavelength);
                    band.setSpectralBandIndex(spectralBandIndex++);
                }
            }
        }
    }

    @Override
    protected Band addNewBand(Product product, Variable variable) {
        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();
        Band band = null;
        String[] factors = {"Radiance", "Reflectance", "BulkSST", "SkinSST"};
        int variableRank = variable.getRank();
        if (variableRank == 2) {
            final int[] dimensions = variable.getShape();
            final int height = dimensions[0];
            final int width = dimensions[1];
            if (height == sceneRasterHeight && width == sceneRasterWidth) {
                String name;

                String groupName = variable.getGroup().getShortName();
                name = groupName + '.'+ variable.getShortName();

                final int dataType = getProductDataType(variable);
                band = new Band(name, dataType, width, height);

                product.addBand(band);
                try {
                    String varname = variable.getShortName();

                    for (String v : factors) {
                        if (v.equals(varname)) {
                            String facvar = v + "Factors";
                            Group group = ncFile.getRootGroup().findGroup("All_Data").getGroups().get(0);
                            Variable factor = group.findVariable(facvar);
                            if (factor != null)     {
                                Array slpoff = factor.read();
                                float slope = slpoff.getFloat(0);

                                float intercept = slpoff.getFloat(1);

                                band.setScalingFactor((double) slope);
                                band.setScalingOffset((double) intercept);
                            }
                        }
                    }
                    //todo Add valid expression - _FillValue is not working properly - viirs uses more than one...ugh.
//                    if (varname.equals("Chlorophyll_a")) {
//                        band.setValidPixelExpression("Chlorophyll_a > 0.0 && Chlorophyll_a < 100.0");
//                    }

                    band.setNoDataValue((double) variable.findAttribute("_FillValue").getNumericValue().floatValue());
                } catch (Exception ignored) {

                }
            }
        }
        return band;
    }

    public void addGeocoding(final Product product) throws ProductIOException {
        //todo: refine logic to get correct navGroup
        File inputFile = productReader.getInputFile();
        NetcdfFile geofile = null;
        String navGroup = "All_Data/VIIRS-MOD-GEO-TC_All";
        String geoFileName = null;
        int strlen = inputFile.getName().length();
        int detectorsInScan;
        Group geocollection = null;
        Group collection = ncFile.getRootGroup().findGroup("Data_Products").getGroups().get(0);
        String shortName = getCollectionShortName();

        String dsType = collection.findAttribute("N_Dataset_Type_Tag").getStringValue();

        if (!dsType.equals("GEO")){
            Attribute geoRef = findAttribute("N_GEO_Ref");
            if (geoRef != null) {
                geoFileName = geoRef.getStringValue().trim();
            } else {
                String platform =  findAttribute("Platform_Short_Name").getStringValue().toLowerCase();
                String procdomain = collection.findAttribute("N_Processing_Domain").getStringValue().toLowerCase();
                String datasource = findAttribute("N_Dataset_Source").getStringValue().toLowerCase();
                long orbitnum = 0;
                String startDate = null;
                String startTime = null;
                String endTime = null;
                String createDate = findAttribute("N_HDF_Creation_Date").getStringValue();
                String createTime = findAttribute("N_HDF_Creation_Time").getStringValue();
                List<Variable> dataProductList = collection.getVariables();
                for (Variable var : dataProductList) {
                    if (var.getShortName().contains("_Aggr")) {
                        orbitnum = var.findAttribute("AggregateBeginningOrbitNumber").getNumericValue().longValue();
                        startDate = var.findAttribute("AggregateBeginningDate").getStringValue().trim();
                        startTime = var.findAttribute("AggregateBeginningTime").getStringValue().trim().substring(0, 8);
                        endTime = var.findAttribute("AggregateEndingTime").getStringValue().trim().substring(0, 8);
                    }
                }
                StringBuilder geoFile = new StringBuilder();

                if (shortName.contains("DNB")){
                    geoFile.append("GDNBO");
                } else if (shortName.contains("VIIRS-I")){
                    geoFile.append("GITCO");
                } else if (dsType.equals("EDR") || shortName.contains("VIIRS-M")){
                    geoFile.append("GMTCO");
                }
                else if (shortName.contains("VIIRS-DualGain")){
                    geoFile.append("ICDBG");
                }
                geoFile.append('_');
                geoFile.append(platform);
                geoFile.append("_d");
                geoFile.append(startDate);
                geoFile.append("_t");
                geoFile.append(startTime);
                geoFile.deleteCharAt(geoFile.toString().length()-2);
                geoFile.append("_e");
                geoFile.append(endTime);
                geoFile.deleteCharAt(geoFile.toString().length()-2);
                geoFile.append("_b");
                geoFile.append(String.format("%05d",orbitnum));
                geoFile.append("_c");
                geoFile.append(createDate).append(createTime);
                geoFile.deleteCharAt(geoFile.toString().length()-1);
                geoFile.deleteCharAt(geoFile.toString().length()-7);
                geoFile.append("_");
                geoFile.append(datasource);
                geoFile.append("_");
                geoFile.append(procdomain);
                geoFile.append(".h5");
                geoFileName =  geoFile.toString();
            }

            try {

                String path = inputFile.getParent();
                File geocheck = new File(path, geoFileName);
                // remove the create time segment and try again
                if (!geocheck.exists() || geoFileName == null) {
                    File geodir = new File(path);
                    final String geoFileName_filter = inputFile.getName().substring(5, strlen).split("_c\\d{20}_")[0];

                    FilenameFilter filter = new FilenameFilter(){
                        public boolean accept
                        (File dir, String name) {
                            return name.contains(geoFileName_filter);
                        }
                    };
                    String[] geofilelist = geodir.list(filter);

                    for (String gf:geofilelist){

                        // check to make sure the geo file name is the same length as the input file
                        if(strlen != gf.length()) {
                            continue;
                        }

                        if (!gf.startsWith("ICDBG")){
                            if (!gf.startsWith("G")){
                                continue;
                            }
                        }
                        if (shortName.contains("DNB") && gf.startsWith("GDNBO")){
                            geocheck = new File(path,  gf);
                            break;
                        } else if (shortName.contains("VIIRS-I")){
                            if ( gf.startsWith("GITCO")){
                                geocheck = new File(path,  gf);
                                break;
                            } else if ( gf.startsWith("GIMGO")){
                                geocheck = new File(path,  gf);
                                // prefer the GITCO, so keep looking just in case;
                            }

                        } else if (shortName.contains("VIIRS-DualGain")){
                            if ( gf.startsWith("ICDBG")){
                                geocheck = new File(path,  gf);
                                break;
                            }
                        } else if (dsType.equals("EDR") || shortName.contains("VIIRS-M")){
                            if ( gf.startsWith("GMTCO")){
                                geocheck = new File(path,  gf);
                                break;
                            } else if ( gf.startsWith("GMODO")){
                                geocheck = new File(path,  gf);
                                //prefer the GMTCO, so keep looking just in case;
                            }

                        }
                    }
                    if (!geocheck.exists()){
                        return;
                    }
                }
                geofile = NetcdfFileOpener.open(geocheck.getPath());
                List<Group> navGroups = geofile.findGroup("All_Data").getGroups();
                for (Group ng : navGroups) {
                    if (ng.getShortName().contains("GEO")){
                        navGroup = ng.getFullName();
                        break;
                    }
                }
            } catch (Exception e) {
                throw new ProductIOException(e.getMessage());
            }
        }

        // see if we have M, I or day/night bands in this file
        // M and day/night bands are 16 detectors/scan (default)
        // I bands are 32 detectors/scan
        detectorsInScan = 16;
        if(shortName.startsWith("VIIRS-I") && shortName.endsWith("-SDR")) {
            detectorsInScan = 32;
        }

        try{
            final String longitude = "Longitude";
            final String latitude = "Latitude";

            if (!dsType.equals("GEO")){
                Band latBand = new Band("latitude", ProductData.TYPE_FLOAT32, product.getSceneRasterWidth(), product.getSceneRasterHeight());
                Band lonBand = new Band("longitude", ProductData.TYPE_FLOAT32, product.getSceneRasterWidth(), product.getSceneRasterHeight());
                product.addBand(latBand);
                product.addBand(lonBand);


                Array latarr = geofile.findVariable(navGroup + "/" + latitude).read();
                Array lonarr = geofile.findVariable(navGroup + "/" + longitude).read();

                float[] latitudes;
                float[] longitudes;
                if (mustFlipX && mustFlipY) {
                    latitudes = (float[]) latarr.flip(0).flip(1).copyTo1DJavaArray();
                    longitudes = (float[]) lonarr.flip(0).flip(1).copyTo1DJavaArray();
                } else {
                    latitudes = (float[]) latarr.getStorage();
                    longitudes = (float[]) lonarr.getStorage();
                }

                ProductData lats = ProductData.createInstance(latitudes);
                latBand.setData(lats);
                ProductData lons = ProductData.createInstance(longitudes);
                lonBand.setData(lons);

                product.setGeoCoding(new BowtiePixelGeoCoding(latBand, lonBand, detectorsInScan));
            } else {
                product.setGeoCoding(new BowtiePixelGeoCoding(product.getBand(latitude), product.getBand(longitude), detectorsInScan));
            }
        }catch (Exception e) {
            throw new ProductIOException(e.getMessage());
        }

    }

    public boolean mustFlipVIIRS() throws ProductIOException {
        List<Variable> vars = ncFile.getVariables();
        for (Variable var : vars) {
            if (var.getShortName().contains("_Gran_")) {
                List<Attribute> attrs = var.getAttributes();
                for (Attribute attr : attrs) {
                    if (attr.getShortName().equals("Ascending_Descending_Indicator")) {
                        return attr.getNumericValue().longValue() == 0;
                    }
                }
            }
        }
        throw new ProductIOException("Cannot find Ascending/Decending_Indicator");
    }


    private void setStartEndTime(Product product) throws ProductIOException {
        List<Variable> dataProductList = ncFile.getRootGroup().findGroup("Data_Products").getGroups().get(0).getVariables();
        for (Variable var : dataProductList) {
            if (var.getShortName().contains("DR_Aggr")) {
                String startDate = var.findAttribute("AggregateBeginningDate").getStringValue().trim();
                String startTime = var.findAttribute("AggregateBeginningTime").getStringValue().trim();
                String endDate = var.findAttribute("AggregateEndingDate").getStringValue().trim();
                String endTime = var.findAttribute("AggregateEndingTime").getStringValue().trim();

                final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyyMMddHHmmss");
                try {
                    String startTimeString = startDate + startTime.substring(0, 6);
                    String endTimeString = endDate + endTime.substring(0, 6);
                    final Date startdate = dateFormat.parse(startTimeString);
                    String startmicroSeconds = startTime.substring(startTimeString.length() - 7, startTimeString.length() - 1);

                    final Date enddate = dateFormat.parse(endTimeString);
                    String endmicroSeconds = endTime.substring(endTimeString.length() - 7, startTimeString.length() - 1);

                    if (mustFlipY) {
                        product.setStartTime(ProductData.UTC.create(enddate, Long.parseLong(endmicroSeconds)));
                        product.setEndTime(ProductData.UTC.create(startdate, Long.parseLong(startmicroSeconds)));
                    } else {
                        product.setStartTime(ProductData.UTC.create(startdate, Long.parseLong(startmicroSeconds)));
                        product.setEndTime(ProductData.UTC.create(enddate, Long.parseLong(endmicroSeconds)));
                    }

                } catch (ParseException e) {
                    throw new ProductIOException("Unable to parse start/end time attributes");
                }

            }
        }
    }

    private String getCollectionShortName() throws ProductIOException {
        List<Attribute> gattr = ncFile.getGlobalAttributes();
        for (Attribute attr : gattr) {
            if (attr.getShortName().endsWith("Collection_Short_Name")) {
                return attr.getStringValue();
            }
        }
        throw new ProductIOException("Cannot find collection short name");
    }

    public void addGlobalAttributeVIIRS() {
        List<Group> DataProductGroups = ncFile.getRootGroup().findGroup("Data_Products").getGroups();

        for (Group dpgroup : DataProductGroups) {
            String groupname = dpgroup.getShortName();
//            if (groupname.matches("VIIRS-.*DR$")) {
            if (groupname.matches("VIIRS-")) {
                List<Variable> vars = dpgroup.getVariables();
                for (Variable var : vars) {
                    String varname = var.getShortName();
                    if (varname.matches(".*_(Aggr|Gran_0)$")) {
                        List<Attribute> attrs = var.getAttributes();
                        for (Attribute attr : attrs) {
                            globalAttributes.add(attr);

                        }
                    }

                }
            }

        }
    }

    @Override
    protected void addFlagsAndMasks(Product product) {
        String[] bandNames = product.getBandNames();
        for (String band : bandNames) {

            Band QFBand;
            String bandID = null;
            String[] nameParts = band.split("-");
            if (nameParts.length > 1) {
                bandID = nameParts[1];
            }
            if (band.endsWith("QF1_VIIRSOCCEDR")){
                QFBand = product.getBand(band);

                FlagCoding flagCoding = new FlagCoding(band);
                flagCoding.addFlag(bandID+"-412Qual", 0x01, "412nm OC quality");
                flagCoding.addFlag(bandID+"-445Qual", 0x02, "445nm OC quality");
                flagCoding.addFlag(bandID+"-488Qual", 0x04, "488nm OC quality");
                flagCoding.addFlag(bandID+"-555Qual", 0x08, "555nm OC quality");
                flagCoding.addFlag(bandID+"-672Qual", 0x10, "672nm OC quality");
                flagCoding.addFlag(bandID+"-ChlQual", 0x20, "Chlorophyll a quality");
                flagCoding.addFlag(bandID+"-IOP412aQual", 0x40, "IOP (a) 412nm quality");
                flagCoding.addFlag(bandID+"-IOP412sQual", 0x80, "IOP (s) 412nm quality");

                product.getFlagCodingGroup().add(flagCoding);
                QFBand.setSampleCoding(flagCoding);


                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-412Qual", "Quality flag (poor): nLw at 412nm",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-412Qual' ",band,bandID),
                        Color.YELLOW, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-445Qual", "Quality flag (poor): nLw at 445nm",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-445Qual' ",band,bandID),
                        Color.CYAN, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-488Qual", "Quality flag (poor): nLw at 488nm",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-488Qual' ",band,bandID),
                        Color.LIGHT_GRAY, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-555Qual", "Quality flag (poor): nLw at 555nm",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-555Qual' ",band,bandID),
                        Color.MAGENTA, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-672Qual", "Quality flag (poor): nLw at 672nm",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-672Qual' ",band,bandID),
                        Color.BLUE, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-ChlQual", "Quality flag (poor): Chlorophyll a",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-ChlQual' ",band,bandID),
                        Color.GREEN, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-IOP412aQual", "Quality flag (poor): IOP (absorption) at 412nm",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-IOP412aQual' ",band,bandID),
                        Color.ORANGE, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-IOP412sQual", "Quality flag (poor): IOP (absorption) at 412nm",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-IOP412sQual' ",band,bandID),
                        Color.PINK, 0.2));

            }
            if (band.endsWith("QF2_VIIRSOCCEDR")){
                QFBand = product.getBand(band);

                FlagCoding flagCoding = new FlagCoding(band);
                flagCoding.addFlag(bandID+"-IOP445aQual", 0x01, "IOP (a) 445nm quality");
                flagCoding.addFlag(bandID+"-IOP445sQual", 0x02, "IOP (s) 445nm quality");
                flagCoding.addFlag(bandID+"-IOP488aQual", 0x04, "IOP (a) 488nm quality");
                flagCoding.addFlag(bandID+"-IOP488sQual", 0x08, "IOP (s) 488nm quality");
                flagCoding.addFlag(bandID+"-IOP555aQual", 0x10, "IOP (a) 555nm quality");
                flagCoding.addFlag(bandID+"-IOP555sQual", 0x20, "IOP (s) 555nm quality");
                flagCoding.addFlag(bandID+"-IOP672aQual", 0x40, "IOP (a) 672nm quality");
                flagCoding.addFlag(bandID+"-IOP672sQual", 0x80, "IOP (s) 672nm quality");
                product.getFlagCodingGroup().add(flagCoding);
                QFBand.setSampleCoding(flagCoding);


                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-IOP445aQual", "Quality flag (poor): IOP (absorption) at 445nm",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-IOP445aQual' ",band,bandID),
                        Color.YELLOW, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-IOP445sQual", "Quality flag (poor): IOP (scattering) at 445nm",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-IOP445sQual' ",band,bandID),
                        Color.CYAN, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-IOP488aQual", "Quality flag (poor): IOP (absorption) at 488nm",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-IOP488aQual' ",band,bandID),
                        Color.LIGHT_GRAY, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-IOP488sQual", "Quality flag (poor): IOP (scattering) at 488nm",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-IOP488sQual' ",band,bandID),
                        Color.MAGENTA, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-IOP555aQual", "Quality flag (poor): IOP (absorption) at 555nm",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-IOP555aQual' ",band,bandID),
                        Color.BLUE, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-IOP555sQual", "Quality flag (poor): IOP (scattering) at 555nm",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-IOP555sQual' ",band,bandID),
                        Color.GREEN, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-IOP672aQual", "Quality flag (poor): IOP (absorption) at 672nm",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-IOP672aQual' ",band,bandID),
                        Color.ORANGE, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-IOP672sQual", "Quality flag (poor): IOP (scattering) at 672nm",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-IOP672sQual' ",band,bandID),
                        Color.PINK, 0.2));
            }
            if (band.endsWith("QF3_VIIRSOCCEDR")){
                QFBand = product.getBand(band);

                FlagCoding flagCoding = new FlagCoding(band);
                flagCoding.addFlag(bandID+"-SDRQual", 0x01, "Input radiance quality");
                flagCoding.addFlag(bandID+"-O3Qual", 0x02, "Input total Ozone Column quality");
                flagCoding.addFlag(bandID+"-WindSpeed", 0x04, "Wind speed > 8m/s (possible whitecap formation)");
                flagCoding.addFlag(bandID+"-AtmWarn", 0x08, "Epsilon value out-of-range for aerosol models (0.85 > eps > 1.35)");

                product.getFlagCodingGroup().add(flagCoding);
                QFBand.setSampleCoding(flagCoding);


                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-SDRQual", "Input radiance quality (poor)",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-SDRQual'",band,bandID),
                        Color.YELLOW, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-O3Qual", "Input Ozone quality (poor)",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-O3Qual'",band,bandID),
                        Color.CYAN, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-WindSpeed", "Wind speed > 8m/s",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-WindSpeed'",band,bandID),
                        Color.LIGHT_GRAY, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-AtmWarn", "Atmospheric correction warning",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-AtmWarn'",band,bandID),
                        Color.MAGENTA, 0.25));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-AtmFail_O3", "Atmospheric correction failure - Ozone correction",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x70 ==  0x10",band),
                        SeadasFileReader.FailRed, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-AtmFail_WC", "Atmospheric correction failure - Whitecap correction",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x70 ==  0x20",band),
                        SeadasFileReader.FailRed, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-AtmFail_pol", "Atmospheric correction failure - Polarization correction",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x70 ==  0x30",band),
                        SeadasFileReader.FailRed, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-AtmFail_rayleigh", "Atmospheric correction failure - Rayliegh correction",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x70 ==  0x40",band),
                        SeadasFileReader.FailRed, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-AtmFail_aerosol", "Atmospheric correction failure - Aerosol correction",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x70 ==  0x50",band),
                        SeadasFileReader.FailRed, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-AtmFail_difftran", "Atmospheric correction failure - Diffuse transmission zero",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x70 ==  0x60",band),
                        SeadasFileReader.FailRed, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-AtmFail_NO", "Atmospheric correction failure - no correction possible",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x70 ==  0x70",band),
                        SeadasFileReader.FailRed, 0.0));
            }

            if (band.endsWith("QF4_VIIRSOCCEDR")){
                QFBand = product.getBand(band);
                FlagCoding flagCoding = new FlagCoding(band);

                flagCoding.addFlag(bandID+"-Ice_Snow", 0x04, "Snow or Ice detected");
                flagCoding.addFlag(bandID+"-HighSolZ", 0x08, "Solar Zenith Angle > 70 deg.");
                flagCoding.addFlag(bandID+"-Glint", 0x10, "Sun Glint");
                flagCoding.addFlag(bandID+"-HighSenZ", 0x20, "Senzor Zenith Angle > 53 deg.");
                flagCoding.addFlag(bandID+"-Shallow", 0x40, "Shallow Water");

                product.getFlagCodingGroup().add(flagCoding);
                QFBand.setSampleCoding(flagCoding);


                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-Ocean", "Ocean",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x03 == 0x00",band),
                        Color.BLUE, 0.7));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-CoastalWater", "Coastal Water mask",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x03 == 0x01",band),
                        Color.GRAY, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-InlandWater", "Inland water mask",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x03 == 0x02",band),
                        Color.DARK_GRAY, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-Land", "Land mask",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x03 == 0x03",band),
                        SeadasFileReader.LandBrown, 0.0));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-Ice/Snow", "Ice/snow mask.",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-Ice_Snow'",band,bandID),
                        Color.lightGray, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-HighSolZ", "Solar Zenith angle > 70 deg.",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-HighSolZ'",band,bandID),
                        SeadasFileReader.Purple, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-Glint", "Sun Glint.",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-Glint'",band,bandID),
                        SeadasFileReader.BrightPink, 0.1));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-HighSenZ", "Sensor Zenith angle > 53 deg.",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-HighSenZ'",band,bandID),
                        SeadasFileReader.LightCyan, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-ShallowWater", "Shallow Water mask.",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-Shallow'",band,bandID),
                        SeadasFileReader.BurntUmber, 0.5));
            }
            if (band.endsWith("QF5_VIIRSOCCEDR")){
                QFBand = product.getBand(band);
                FlagCoding flagCoding = new FlagCoding(band);
                flagCoding.addFlag(bandID+"-Straylight", 0x04, "Adjacent pixel not clear, possible straylight contaminated");
                flagCoding.addFlag(bandID+"-Cirrus", 0x08, "Thin Cirrus cloud detected");
                flagCoding.addFlag(bandID+"-Shadow", 0x10, "Cloud shadow detected");
                flagCoding.addFlag(bandID+"-HighAer", 0x20, "Non-cloud obstruction (heavy aerosol load) detected");
                flagCoding.addFlag(bandID+"-AbsAer", 0x40, "Strongly absorbing aerosol detected");
                flagCoding.addFlag(bandID+"-HighAOT", 0x80, "Aerosol optical thickness @ 555nm > 0.3");

                product.getFlagCodingGroup().add(flagCoding);
                QFBand.setSampleCoding(flagCoding);


                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-Clear", "Confidently Cloud-free.",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x03 == 0x00",band),
                        SeadasFileReader.Cornflower, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-LikelyClear", "Probably cloud-free",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x03 == 0x01",band),
                        Color.LIGHT_GRAY, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-LikelyCloud", "Probably cloud contaminated.",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x03 == 0x02",band),
                        Color.DARK_GRAY, 0.25));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-Cloud", "Confidently Cloudy.",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x03 == 0x03",band),
                        Color.WHITE, 0.0));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-Straylight", "Adjacent pixel not clear, possible straylight contaminated.",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-Straylight'",band,bandID),
                        Color.YELLOW, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-Cirrus", "Thin Cirrus cloud detected.",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-Cirrus'",band,bandID),
                        Color.BLUE, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-CloudShadow", "Cloud shadow detected.",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-Shadow'",band,bandID),
                        Color.GRAY, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-HighAer", "Non-cloud obstruction (heavy aerosol load) detected.",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-HighAer'",band,bandID),
                        SeadasFileReader.LightPink, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-AbsAer", "Strongly absorbing aerosol detected.",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-AbsAer'",band,bandID),
                        Color.ORANGE, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-HighAOT", "Aerosol optical thickness @ 555nm > 0.3.",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-HighAOT'",band,bandID),
                        Color.MAGENTA, 0.5));
            }
            if (band.endsWith("QF6_VIIRSOCCEDR")){
                QFBand = product.getBand(band);
                FlagCoding flagCoding = new FlagCoding(band);
                flagCoding.addFlag(bandID+"-Turbid", 0x01, "Turbid water detected (Rrs @ 555nm > 0.012)");
                flagCoding.addFlag(bandID+"-Coccolithophore", 0x02, "Coccolithophores detected");
                flagCoding.addFlag(bandID+"-HighCDOM", 0x04, "CDOM absorption @ 410nm > 2 m^-1");

                product.getFlagCodingGroup().add(flagCoding);
                QFBand.setSampleCoding(flagCoding);


                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-Turbid", "Turbid water detected (Rrs @ 555nm > 0.012)",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-Turbid'",band,bandID),
                        SeadasFileReader.LightBrown, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-Coccolithophore", "Coccolithophores detected",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-Coccolithophore'",band,bandID),
                        Color.CYAN, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-HighCDOM", "CDOM absorption @ 410nm > 2 m^-1.",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-HighCDOM'",band,bandID),
                        SeadasFileReader.Mustard, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-ChlFail", "No Chlorophyll retrieval possible.",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x18 == 0x00",band),
                        SeadasFileReader.FailRed, 0.0));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-LowChl", "Chlorophyll < 1 mg m^-3",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x18 == 0x08",band),
                        SeadasFileReader.Coral, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-ModChl", "Chlorophyll between 1 and 10 mg m^-3",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s'  & 0x18 == 0x10",band),
                        SeadasFileReader.DarkGreen, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-HighChl", "Chlorphyll > 10 mg m^-3",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s'   & 0x18 == 0x10",band),
                        Color.RED, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-CarderEmp", "Carder Empirical algorithm used.",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0xE0 == 0x20",band),
                        SeadasFileReader.NewGreen, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-UnpackPig", "Phytoplankton with packaged pigment",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0xE0 == 0x40",band),
                        SeadasFileReader.TealGreen, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-WtPigGlobal", "Weighted packaged pigment - global",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0xE0 == 0x80",band),
                        Color.GRAY, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-WtPigFull", "Weighted fully packaged pigment",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0xE0 == 0xA0",band),
                        Color.LIGHT_GRAY, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-FullPackPig", "Phytoplankton with fully packaged pigment",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0xE0 == 0xC0",band),
                        SeadasFileReader.TealBlue, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-NoOCC", "No ocean color chlorphyll retrieval",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0xE0 == 0xE0",band),
                        Color.BLACK, 0.1));
            }
            if (band.endsWith("QF7_VIIRSOCCEDR")){
                QFBand = product.getBand(band);
                FlagCoding flagCoding = new FlagCoding(band);
                flagCoding.addFlag(bandID+"-nLwWarn", 0x01, "nLw out-of-range (< 0.1 or > 40 W m^-2 um^-1 sr^-1)");
                flagCoding.addFlag(bandID+"-ChlWarn", 0x02, "Chlorophyll out-of-range (< 0.05 or > 50 mg m^-3)");
                flagCoding.addFlag(bandID+"-IOPaWarn", 0x04, "IOP absorption out-of-range (< 0.01 or  > 10 m^-1)");
                flagCoding.addFlag(bandID+"-IOPsWarn", 0x08, "IOP scattering out-of-range (< 0.01 or  > 50 m^-1)");
                flagCoding.addFlag(bandID+"-SSTWarn", 0x10, "Input Skin SST poor quality");
                flagCoding.addFlag(bandID+"-Bright", 0x20, "Bright Target flag");


                product.getFlagCodingGroup().add(flagCoding);
                QFBand.setSampleCoding(flagCoding);


                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-nLwWarn", "nLw out-of-range (< 0.1 or > 40 W m^-2 um^-1 sr^-1)",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-nLwWarn'",band,bandID),
                        Color.BLUE, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-ChlWarn", "Chlorophyll out-of-range (< 0.05 or > 50 mg m^-3)",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-ChlWarn'",band,bandID),
                        Color.LIGHT_GRAY, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-IOPaWarn", "IOP absorption out-of-range (< 0.01 or  > 10 m^-1)",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-IOPaWarn'",band,bandID),
                        Color.DARK_GRAY, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-IOPsWarn", "IOP scattering out-of-range (< 0.01 or  > 50 m^-1)",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-IOPsWarn'",band,bandID),
                        Color.GREEN, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-SSTWarn", "Input Skin SST poor quality.",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-SSTWarn'",band,bandID),
                        Color.LIGHT_GRAY, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"-Bright", "Bright Target flag",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s.%s-Bright'",band,bandID),
                        Color.GRAY, 0.2));

            }
            if (band.endsWith("QF1_VIIRSMBANDSDR")){
                QFBand = product.getBand(band);
                FlagCoding flagCoding = new FlagCoding(band);
                flagCoding.addFlag(bandID+"-CalQualGood", 0x00, "Calibration quality - Good");
                flagCoding.addFlag(bandID+"-CalQualBad", 0x01, "Calibration quality - Bad");
                flagCoding.addFlag(bandID+"-NoCal", 0x02, "No Calibration");
                flagCoding.addFlag(bandID+"-NoSatPix", 0x03, "No saturated");
                flagCoding.addFlag(bandID+"-LowSatPix", 0x03, "Some pixels saturated");
                flagCoding.addFlag(bandID+"-SatPix", 0x03, "All pixels saturated");
                flagCoding.addFlag(bandID+"-DataOK", 0x04, "All required data available");
                flagCoding.addFlag(bandID+"-BadEvRDR", 0x08, "Missing EV RDR data.");
                flagCoding.addFlag(bandID+"-BadCalData", 0x10, "Missing cal data (SV, CV, SD, etc)");
                flagCoding.addFlag(bandID+"-BadTherm", 0x20, "Missing Thermistor data");
                flagCoding.addFlag(bandID+"-InRange", 0x40, "All calibrated data within LUT thresholds");
                flagCoding.addFlag(bandID+"-BadRad", 0x40, "Radiance out-of-range LUT threshold");
                flagCoding.addFlag(bandID+"-BadRef", 0x40, "Reflectance out-of-range LUT threshold");
                flagCoding.addFlag(bandID+"-BadRadRef", 0x40, "Both Radiance & Reflectance out-of-range LUT threshold");


                product.getFlagCodingGroup().add(flagCoding);
                QFBand.setSampleCoding(flagCoding);


                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"CalQualGood", "Calibration quality - Good",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x02 == 0x00", band),
                        Color.BLUE, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"CalQualBad", "Calibration quality - Bad",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x02 == 0x01", band),
                        Color.GRAY, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"NoCal", "No Calibration",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x02 == 0x02", band),
                        Color.DARK_GRAY, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"NoSatPix", "No saturated",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x0C == 0x00", band),
                        Color.GREEN, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"LowSatPix", "Some pixels saturated.",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x0C == 0x04", band),
                        Color.lightGray, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"SatPix", "All pixels saturated",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x0C == 0x08", band),
                        Color.MAGENTA, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"DataOK", "All required data available",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x30 == 0x00", band),
                        Color.YELLOW, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"BadEvRDR", "Missing EV RDR data.",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x30 == 0x10", band),
                        Color.orange, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"BadCalData", "Missing cal data (SV, CV, SD, etc).",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x30 == 0x20", band),
                        Color.BLUE, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"BadTherm", "Missing Thermistor data.",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0x30 == 0x30", band),
                        Color.BLUE, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"InRange", "All calibrated data within LUT thresholds.",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0xC0 == 0x00", band),
                        Color.BLUE, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"BadRad", "Radiance out-of-range LUT threshold.",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0xC0 == 0x40", band),
                        Color.BLUE, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"BadRef", "Reflectance out-of-range LUT threshold.",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0xC0 == 0x80", band),
                        Color.BLUE, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create(bandID+"BadRadRef", "Both Radiance & Reflectance out-of-range LUT threshold.",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), String.format("'%s' & 0xC0 == 0xC0", band),
                        Color.BLUE, 0.5));
            }
        }
    }
}
