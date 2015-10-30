package org.esa.s3tbx.slstr.pdu.stitching;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.converters.DateFormatConverter;
import com.bc.ceres.core.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Tonio Fincke
 */
public class SlstrPduStitcher {

    private static final String SLSTR_L1B_NAME_PATTERN = "S3.?_SL_1_RBT_.*(.SEN3)?";
    private static final DateFormatConverter SLSTR_DATE_FORMAT_CONVERTER =
            new DateFormatConverter(new SimpleDateFormat("yyyyMMdd'T'HHmmss"));
    private static final ImageSize NULL_IMAGE_SIZE = new ImageSize("null", 0, 0, 0, 0);

    public static File createStitchedSlstrL1BFile(File targetDirectory, File[] slstrProductFiles) throws IllegalArgumentException, IOException, PDUStitchingException {
        Assert.notNull(slstrProductFiles);
        if (slstrProductFiles.length == 0) {
            throw new IllegalArgumentException("No product files provided");
        }
        final Pattern slstrNamePattern = Pattern.compile(SLSTR_L1B_NAME_PATTERN);
        for (File slstrProductFile : slstrProductFiles) {
            if (slstrProductFile == null ||
                    !slstrProductFile.getName().equals("xfdumanifest.xml") ||
                    slstrProductFile.getParentFile() == null ||
                    !slstrNamePattern.matcher(slstrProductFile.getParentFile().getName()).matches()) {
                throw new IllegalArgumentException("The PDU Stitcher only supports Slstr L1B products");
            }
        }
        if (slstrProductFiles.length == 1) {
            try {
                final File originalParentDirectory = slstrProductFiles[0].getParentFile();
                final String parentDirectoryName = originalParentDirectory.getName();
                final File stitchedParentDirectory = new File(targetDirectory, parentDirectoryName);
                Files.copy(originalParentDirectory.getParentFile().toPath(), stitchedParentDirectory.toPath());
                final File[] files = originalParentDirectory.listFiles();
                if (files != null) {
                    for (File originalFile : files) {
                        Files.copy(originalFile.toPath(), new File(stitchedParentDirectory, originalFile.getName()).toPath());
                    }
                }
                return new File(stitchedParentDirectory, "xfdumanifest.xml");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        final Date now = Calendar.getInstance().getTime();
        SlstrNameDecomposition[] slstrNameDecompositions = new SlstrNameDecomposition[slstrProductFiles.length];
        Document[] manifestDocuments = new Document[slstrProductFiles.length];
        List<String> ncFileNames = new ArrayList<>();
        Map<String, ImageSize[]> idToImageSizes = new HashMap<>();
        for (int i = 0; i < slstrProductFiles.length; i++) {
            slstrNameDecompositions[i] = decomposeSlstrName(slstrProductFiles[i].getParentFile().getName());
            manifestDocuments[i] = createXmlDocument(new FileInputStream(slstrProductFiles[i]));
            final ImageSize[] imageSizes = extractImageSizes(manifestDocuments[i]);
            for (ImageSize imageSize : imageSizes) {
                if (idToImageSizes.containsKey(imageSize.getIdentifier())) {
                    idToImageSizes.get(imageSize.getIdentifier())[i] = imageSize;
                } else {
                    final ImageSize[] mapImageSizes = new ImageSize[slstrProductFiles.length];
                    mapImageSizes[i] = imageSize;
                    idToImageSizes.put(imageSize.getIdentifier(), mapImageSizes);
                }
            }
            collectFiles(ncFileNames, manifestDocuments[i]);
        }
        final String stitchedProductFileName = createParentDirectoryNameOfStitchedFile(slstrNameDecompositions, now);
        File stitchedProductFileParentDirectory = new File(targetDirectory, stitchedProductFileName);
        if (!stitchedProductFileParentDirectory.mkdirs()) {
            throw new PDUStitchingException("Could not create product directory");
        }
        Map<String, ImageSize> idToTargetImageSize = new HashMap<>();
        for (String id : idToImageSizes.keySet()) {
            idToTargetImageSize.put(id, createTargetImageSize(idToImageSizes.get(id)));
        }
        for (int i = 0; i < ncFileNames.size(); i++) {
            List<File> ncFiles = new ArrayList<>();
            List<Integer> indexesOfMissingFiles = new ArrayList<>();
            final String ncFileName = ncFileNames.get(i);
            String id = ncFileName.substring(ncFileName.length() - 5, ncFileName.length() - 3);
            if (id.equals("tx")) {
                id = "tn";
            }
            ImageSize[] imageSizes = idToImageSizes.get(id);
            if (imageSizes == null) {
                imageSizes = new ImageSize[ncFileNames.size()];
                Arrays.fill(imageSizes, NULL_IMAGE_SIZE);
            }
            ImageSize targetImageSize = idToTargetImageSize.get(id);
            if (targetImageSize == null) {
                targetImageSize = NULL_IMAGE_SIZE;
            }
            for (int j = 0; j < slstrProductFiles.length; j++) {
                File ncFile = new File(slstrProductFiles[i].getParentFile(), ncFileName);
                if (ncFile.exists()) {
                    ncFiles.add(ncFile);
                } else {
                    indexesOfMissingFiles.add(j);
                }
            }
            final File[] ncFilesArray = ncFiles.toArray(new File[ncFiles.size()]);
            if (indexesOfMissingFiles.size() > 0) {
                ImageSize[] newImageSizes = new ImageSize[ncFiles.size()];
                int missingFileCounter = 0;
                for (int k = 0; k < imageSizes.length; k++) {
                    if (k != missingFileCounter) {
                        newImageSizes[k - missingFileCounter] = imageSizes[k];
                    } else {
                        missingFileCounter++;
                    }
                }
                imageSizes = newImageSizes;
                targetImageSize = createTargetImageSize(imageSizes);
            }
            try {
                NcFileStitcher.stitchNcFiles(ncFileName, stitchedProductFileParentDirectory, now,
                                             ncFilesArray, targetImageSize, imageSizes);
            } catch (PDUStitchingException e) {
                e.printStackTrace();
            }
        }
        File stitchedProductFile = new File(stitchedProductFileParentDirectory, "xfdumanifest.xml");
        if (!stitchedProductFile.createNewFile()) {
            throw new PDUStitchingException("Could not create manifest file");
        }
        return stitchedProductFile;
    }

    static ImageSize createTargetImageSize(ImageSize[] imageSizes) {
        int startOffset = Integer.MAX_VALUE;
        int trackOffset = Integer.MAX_VALUE;
        int highestStart = Integer.MIN_VALUE;
        int highestTrack = Integer.MIN_VALUE;
        for (ImageSize imageSize : imageSizes) {
            if (imageSize.getStartOffset() < startOffset) {
                startOffset = imageSize.getStartOffset();
            }
            if (imageSize.getTrackOffset() < trackOffset) {
                trackOffset = imageSize.getTrackOffset();
            }
            if (imageSize.getStartOffset() + imageSize.getRows() > highestStart) {
                highestStart = imageSize.getStartOffset() + imageSize.getRows();
            }
            if (imageSize.getTrackOffset() + imageSize.getColumns() > highestTrack) {
                highestTrack = imageSize.getTrackOffset() + imageSize.getColumns();
            }
        }
        return new ImageSize(imageSizes[0].getIdentifier(), startOffset, trackOffset, highestStart - startOffset, highestTrack - trackOffset);
    }

    static void collectFiles(List<String> ncFileNames, Document manifestDocument) {
        final NodeList fileLocationNodes = manifestDocument.getElementsByTagName("fileLocation");
        for (int i = 0; i < fileLocationNodes.getLength(); i++) {
            final String ncFileName = fileLocationNodes.item(i).getAttributes().getNamedItem("href").getNodeValue();
            if (!ncFileNames.contains(ncFileName)) {
                ncFileNames.add(ncFileName);
            }
        }
    }

    static ImageSize[] extractImageSizes(Document manifestDocument) {
        final NodeList nadirElements = manifestDocument.getElementsByTagName("slstr:nadirImageSize");
        final NodeList obliqueElements = manifestDocument.getElementsByTagName("slstr:obliqueImageSize");
        final ImageSize[] imageSizes = new ImageSize[obliqueElements.getLength() + obliqueElements.getLength()];
        for (int i = 0; i < nadirElements.getLength(); i++) {
            imageSizes[i] = extractImageSizeFromNode(nadirElements.item(i), "n");
        }
        for (int i = 0; i < obliqueElements.getLength(); i++) {
            imageSizes[nadirElements.getLength() + i] = extractImageSizeFromNode(obliqueElements.item(i), "o");
        }
        return imageSizes;
    }

    private static ImageSize extractImageSizeFromNode(Node element, String idExtension) {
        String id = getId(element.getAttributes().getNamedItem("grid").getNodeValue()) + idExtension;
        int startOffset = -1;
        int trackOffset = -1;
        int rows = -1;
        int columns = -1;
        final NodeList elementChildNodes = element.getChildNodes();
        for (int j = 0; j < elementChildNodes.getLength(); j++) {
            final Node node = elementChildNodes.item(j);
            if (node.getNodeName().equals("sentinel3:startOffset")) {
                startOffset = Integer.parseInt(node.getChildNodes().item(0).getNodeValue());
            } else if (node.getNodeName().equals("sentinel3:trackOffset")) {
                trackOffset = Integer.parseInt(node.getChildNodes().item(0).getNodeValue());
            } else if (node.getNodeName().equals("sentinel3:rows")) {
                rows = Integer.parseInt(node.getChildNodes().item(0).getNodeValue());
            } else if (node.getNodeName().equals("sentinel3:columns")) {
                columns = Integer.parseInt(node.getChildNodes().item(0).getNodeValue());
            }
        }
        return new ImageSize(id, startOffset, trackOffset, rows, columns);
    }

    private static String getId(String gridName) {
        switch (gridName) {
            case "1 km":
                return "i";
            case "0.5 km stripe A":
                return "a";
            case "0.5 km stripe B":
                return "b";
            case "0.5 km TDI":
                return "c";
            case "Tie Points":
                return "t";
            default:
                return "";
        }
    }

    private static Document createXmlDocument(InputStream inputStream) throws IOException {
        final String msg = "Cannot create document from manifest XML file.";
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException(msg, e);
        }
    }

    static String createParentDirectoryNameOfStitchedFile(SlstrNameDecomposition[] slstrNameDecompositions, Date now) {
        Date startTime = extractStartTime(slstrNameDecompositions);
        Date stopTime = extractStopTime(slstrNameDecompositions);
        final StringBuilder slstrNameStringBuilder = new StringBuilder("S3A_SL_1_RBT___");
        String[] slstrNameParts = new String[]{SLSTR_DATE_FORMAT_CONVERTER.format(startTime),
                SLSTR_DATE_FORMAT_CONVERTER.format(stopTime), SLSTR_DATE_FORMAT_CONVERTER.format(now),
                slstrNameDecompositions[0].duration, slstrNameDecompositions[0].cycleNumber, slstrNameDecompositions[0].relativeOrbitNumber,
                slstrNameDecompositions[0].frameAlongTrackCoordinate, slstrNameDecompositions[0].fileGeneratingCentre, slstrNameDecompositions[0].platform,
                slstrNameDecompositions[0].timelinessOfProcessingWorkflow, slstrNameDecompositions[0].baselineCollectionOrDataUsage};
        for (String namePart : slstrNameParts) {
            slstrNameStringBuilder.append("_").append(namePart);
        }
        slstrNameStringBuilder.append(".SEN3");
        return slstrNameStringBuilder.toString();
    }

    private static Date extractStartTime(SlstrNameDecomposition[] slstrNameDecompositions) {
        Date earliestDate = new GregorianCalendar(3000, 1, 1).getTime();
        for (SlstrNameDecomposition slstrNameDecomposition : slstrNameDecompositions) {
            final Date startTime = slstrNameDecomposition.startTime;
            if (startTime.before(earliestDate)) {
                earliestDate = startTime;
            }
        }
        return earliestDate;
    }

    private static Date extractStopTime(SlstrNameDecomposition[] slstrNameDecompositions) {
        Date latestDate = new GregorianCalendar(1800, 1, 1).getTime();
        for (SlstrNameDecomposition slstrNameDecomposition : slstrNameDecompositions) {
            final Date stopTime = slstrNameDecomposition.stopTime;
            if (stopTime.after(latestDate)) {
                latestDate = stopTime;
            }
        }
        return latestDate;
    }

    static SlstrNameDecomposition decomposeSlstrName(String slstrName) {
        final SlstrNameDecomposition slstrNameDecomposition = new SlstrNameDecomposition();
        try {
            slstrNameDecomposition.startTime = SLSTR_DATE_FORMAT_CONVERTER.parse(slstrName.substring(16, 31));
        } catch (ConversionException e) {
            e.printStackTrace();
        }
        try {
            slstrNameDecomposition.stopTime = SLSTR_DATE_FORMAT_CONVERTER.parse(slstrName.substring(32, 47));
        } catch (ConversionException e) {
            e.printStackTrace();
        }
        slstrNameDecomposition.duration = slstrName.substring(64, 68);
        slstrNameDecomposition.cycleNumber = slstrName.substring(69, 72);
        slstrNameDecomposition.relativeOrbitNumber = slstrName.substring(73, 76);
        slstrNameDecomposition.frameAlongTrackCoordinate = slstrName.substring(77, 81);
        slstrNameDecomposition.fileGeneratingCentre = slstrName.substring(82, 85);
        slstrNameDecomposition.platform = slstrName.substring(86, 87);
        slstrNameDecomposition.timelinessOfProcessingWorkflow = slstrName.substring(88, 90);
        slstrNameDecomposition.baselineCollectionOrDataUsage = slstrName.substring(91, 94);
        return slstrNameDecomposition;
    }

    static class SlstrNameDecomposition {
        Date startTime;
        Date stopTime;
        String duration;
        String cycleNumber;
        String relativeOrbitNumber;
        String frameAlongTrackCoordinate;
        String fileGeneratingCentre;
        String platform;
        String timelinessOfProcessingWorkflow;
        String baselineCollectionOrDataUsage;
    }

}
