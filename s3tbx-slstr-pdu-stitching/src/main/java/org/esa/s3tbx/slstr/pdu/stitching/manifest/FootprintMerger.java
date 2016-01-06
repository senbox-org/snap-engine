package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import org.esa.s3tbx.slstr.pdu.stitching.PDUStitchingException;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import javax.swing.text.NumberFormatter;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.List;

/**
 * @author Tonio Fincke
 */
class FootprintMerger extends AbstractElementMerger {

    private static final String NAME_OF_GEODETIC_NC_FILE = "geodetic_tx.nc";
    private static final String NAME_OF_LAT_VARIABLE = "latitude_tx";
    private static final String NAME_OF_LON_VARIABLE = "longitude_tx";

    private final File productDir;
    private final NumberFormatter numberFormatter;
    private StringBuilder footprintBuilder;

    FootprintMerger(File productDir) {
        this.productDir = productDir;
        final DecimalFormat format = new DecimalFormat("###0.0000");
        final DecimalFormatSymbols newSymbols = new DecimalFormatSymbols();
        newSymbols.setDecimalSeparator('.');
        format.setDecimalFormatSymbols(newSymbols);
        numberFormatter = new NumberFormatter(format);
    }

    @Override
    public void mergeNodes(List<Node> fromParents, Element toParent, Document toDocument) throws PDUStitchingException {
        final Element posListElement = toDocument.createElement("gml:posList");
        String footprint;
        footprint = createFootprint(fromParents);
        addTextToNode(posListElement, footprint, toDocument);
        toParent.appendChild(posListElement);
    }

    private String createFootprint(List<Node> fromParents) {
        NetcdfFile geodeticNcFile;
        try {
            final File file = new File(productDir + File.separator + NAME_OF_GEODETIC_NC_FILE);
            if (!file.exists()) {
                return createFootprintWithoutNcFile(fromParents);
            }
            geodeticNcFile = NetcdfFileOpener.open(file);
            if (geodeticNcFile == null) {
                return createFootprintWithoutNcFile(fromParents);
            }
            final List<Variable> geodeticVariables = geodeticNcFile.getVariables();
            Variable latVariable = null;
            Variable lonVariable = null;
            for (final Variable variable : geodeticVariables) {
                if (variable.getFullName().equals(NAME_OF_LAT_VARIABLE)) {
                    latVariable = variable;
                } else if (variable.getFullName().equals(NAME_OF_LON_VARIABLE)) {
                    lonVariable = variable;
                }
            }
            if (latVariable == null || lonVariable == null) {
                return createFootprintWithoutNcFile(fromParents);
            }
            final int numTieRows = latVariable.getDimension(0).getLength();
            final int numTieColumns = lonVariable.getDimension(1).getLength();
            final int[] stride = new int[]{200, 12};
            footprintBuilder = new StringBuilder();
            final Section leftBorderSection = new Section(new int[]{0, 0}, new int[]{numTieRows, 1}, stride);
            final Section lowerBorderSection = new Section(new int[]{numTieRows - 1, 0}, new int[]{1, numTieColumns}, stride);
            final Section lowerRightPixelSection = new Section(new int[]{numTieRows - 1, numTieColumns - 1}, new int[]{1, 1});
            final Section rightBorderSection = new Section(new int[]{0, numTieColumns - 1}, new int[]{numTieRows, 1}, stride);
            final Section upperBorderSection = new Section(new int[]{0, 0}, new int[]{1, numTieColumns}, stride);
            final double[] leftBorderLats = (double[]) latVariable.read(leftBorderSection).get1DJavaArray(Double.class);
            final double[] leftBorderLons = (double[]) lonVariable.read(leftBorderSection).get1DJavaArray(Double.class);
            final double[] lowerBorderLats = (double[]) latVariable.read(lowerBorderSection).get1DJavaArray(Double.class);
            final double[] lowerBorderLons = (double[]) lonVariable.read(lowerBorderSection).get1DJavaArray(Double.class);
            final double lowerRightLat = latVariable.read(lowerRightPixelSection).getDouble(0);
            final double lowerRightLon = lonVariable.read(lowerRightPixelSection).getDouble(0);
            final double[] rightBorderLats = (double[]) latVariable.read(rightBorderSection).get1DJavaArray(Double.class);
            final double[] rightBorderLons = (double[]) lonVariable.read(rightBorderSection).get1DJavaArray(Double.class);
            final double[] upperBorderLats = (double[]) latVariable.read(upperBorderSection).get1DJavaArray(Double.class);
            final double[] upperBorderLons = (double[]) lonVariable.read(upperBorderSection).get1DJavaArray(Double.class);
            int numX = lowerBorderLats.length;
            int numY = leftBorderLats.length;
            //add left border
            for (int i = 0; i < numY; i++) {
                append(leftBorderLats[i], leftBorderLons[i]);
            }
            //ensure lower left coordinate is not added twice
            if (!areEqual(leftBorderLats[numY - 1], lowerBorderLats[0])) {
                append(lowerBorderLats[0]);
            }
            if (!areEqual(leftBorderLons[numY - 1], lowerBorderLons[0])) {
                append(lowerBorderLons[0]);
            }
            //add lower border
            for (int i = 1; i < numX; i++) {
                append(lowerBorderLats[i], lowerBorderLons[i]);
            }
            //ensure lower right coordinate is added exactly one time
            if (!areEqual(lowerBorderLats[numX - 1], lowerRightLat)) {
                append(lowerRightLat);
            }
            if (!areEqual(lowerBorderLons[numX - 1], lowerRightLon)) {
                append(lowerRightLon);
            }
            if (!areEqual(rightBorderLats[numY - 1], lowerRightLat)) {
                append(rightBorderLats[numY - 1]);
            }
            if (!areEqual(rightBorderLons[numY - 1], lowerRightLon)) {
                append(rightBorderLons[numY - 1]);
            }
            //add right border
            for (int i = numY - 2; i >= 0; i--) {
                append(rightBorderLats[i], rightBorderLons[i]);
            }
            //ensure upper right coordinate is not added twice
            if (!areEqual(upperBorderLats[numX - 1], rightBorderLats[0])) {
                append(upperBorderLats[numX - 1]);
            }
            if (!areEqual(upperBorderLons[numX - 1], rightBorderLons[0])) {
                append(upperBorderLons[numX - 1]);
            }
            //add upper border
            for (int i = numX - 2; i > 0; i--) {
                append(upperBorderLats[i], upperBorderLons[i]);
            }
            footprintBuilder.append(format(upperBorderLats[0])).append(" ").append(format(upperBorderLons[0]));
            return footprintBuilder.toString();
        } catch (IOException | InvalidRangeException | ParseException e) {
            return createFootprintWithoutNcFile(fromParents);
        }
    }

    private void append(double d) throws ParseException {
        footprintBuilder.append(format(d)).append(" ");
    }

    private void append(double d1, double d2) throws ParseException {
        footprintBuilder.append(format(d1)).append(" ").append(format(d2)).append(" ");
    }

    private String format(double d) throws ParseException {
        return numberFormatter.valueToString(d);
    }

    private boolean areEqual(double d1, double d2) {
        return Math.abs(d1 - d2) < 1e-4;
    }

    private String createFootprintWithoutNcFile(List<Node> fromParents) {
        //todo actually merge?
        footprintBuilder = new StringBuilder();
        for (int i = 0; i < fromParents.size(); i++) {
            final NodeList childNodes = fromParents.get(i).getChildNodes();
            for (int j = 0; j < childNodes.getLength(); j++) {
                if (childNodes.item(j).getNodeName().equals("gml:posList")) {
                    footprintBuilder.append(childNodes.item(j).getTextContent());
                    if (i < fromParents.size() - 1) {
                        footprintBuilder.append(" ");
                    }
                    break;
                }
            }
        }
        return footprintBuilder.toString();
    }

}
