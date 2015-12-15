package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import org.esa.s3tbx.slstr.pdu.stitching.PDUStitchingException;
import org.esa.snap.core.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * @author Tonio Fincke
 */
public class DataObjectMerger extends AbstractElementMerger {

    private final String fileLocation;

    DataObjectMerger(String fileLocation) {
        this.fileLocation = fileLocation;
    }

    @Override
    public void mergeNodes(List<Node> fromParents, Element toParent, Document toDocument) throws PDUStitchingException {
        final String fileName = getFileName(fromParents, toParent);
        final File file = getFile(fileName, toParent);
//        for (int i = 0; i < fromParents.size(); i++) {
//        final NodeList dataObjectChildNodes = fromParents.get(0).getChildNodes();
//        for (int l = 0; l < dataObjectChildNodes.getLength(); l++) {
//            if (dataObjectChildNodes.item(l).getNodeName().equals("byteStream")) {
        final long size = file.length();
        appendLineBreakAndIndent(toParent, toDocument, 3);
        final Element byteStreamElement = toDocument.createElement("byteStream");
        toParent.appendChild(byteStreamElement);
        byteStreamElement.setAttribute("mimeType", "application/x-netcdf");
        byteStreamElement.setAttribute("size", String.valueOf(size));
//                final NodeList byteStreamChildNodes = dataObjectChildNodes.item(l).getChildNodes();
//                for (int k = 0; k < byteStreamChildNodes.getLength(); k++) {
//                    if (byteStreamChildNodes.item(k).getNodeName().equals("fileLocation")) {
        final Element fileLocationElement = toDocument.createElement("fileLocation");
        appendLineBreakAndIndent(byteStreamElement, toDocument, 4);
        byteStreamElement.appendChild(fileLocationElement);
//        appendLineBreakAndIndent(byteStreamElement, toDocument, 4);
        fileLocationElement.setAttribute("locatorType", "URL");
        fileLocationElement.setAttribute("href", fileName);
//                    } else if (byteStreamChildNodes.item(k).getNodeName().equals("checksum")) {
        final Element checksumElement = toDocument.createElement("checksum");
        appendLineBreakAndIndent(byteStreamElement, toDocument, 4);
        byteStreamElement.appendChild(checksumElement);
//        appendLineBreakAndIndent(byteStreamElement, toDocument, 4);
        checksumElement.setAttribute("checksumName", "MD5");
        final String checksum;
        try {
            checksum = getChecksum(file);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new PDUStitchingException("Could not create checksum for file " + fileName);
        }
        addTextToNode(checksumElement, checksum, toDocument);
        appendLineBreakAndIndent(byteStreamElement, toDocument, 3);
        appendLineBreakAndIndent(toParent, toDocument, 2);
//                    } else {
//                        final String textContent = byteStreamChildNodes.item(k).getTextContent();
//                        final Text textNode = toDocument.createTextNode(textContent);
//                        byteStreamElement.appendChild(textNode);
//                    }
//                            final NamedNodeMap fileLocationAttributes = byteStreamChildNodes.item(k).getAttributes();
//                            final Node hrefAttribute = fileLocationAttributes.getNamedItem("href");
//                            if (hrefAttribute != null) {
//                                if (i == 0) {
//                                    fileName = hrefAttribute.getNodeValue();
//                                } else {
//                                    if (!fileName.equals(hrefAttribute.getNodeValue())) {
//                                        throw new PDUStitchingException(
//                                                "Different href attributes given for dataObject with ID " + toParent.getAttribute("ID"));
//                                    }
//                                }
//                            } else {
//                                throw new PDUStitchingException(
//                                        "No href attribute given for dataObject with ID " + toParent.getAttribute("ID"));
//                            }
//                            break;
//                        }
//                }
//            } else {
//                final String textContent = dataObjectChildNodes.item(l).getTextContent();
//                final Text textNode = toDocument.createTextNode(textContent);
//                toParent.appendChild(textNode);
//            }
    }

    private void appendLineBreakAndIndent(Element toParent, Document toDocument, int treeDepth) {
//        final String textContent = dataObjectChildNodes.item(l).getTextContent();
        StringBuilder stringBuilder = new StringBuilder("\n");
        for (int i = 0; i < 2 * treeDepth; i++) {
            stringBuilder.append(" ");
        }
        final Text textNode = toDocument.createTextNode(stringBuilder.toString());
        toParent.appendChild(textNode);
    }

//}


//}

    private File getFile(String fileName, Element toParent) throws PDUStitchingException {
        if (StringUtils.isNullOrEmpty(fileName)) {
            throw new PDUStitchingException(
                    "No href attribute given for dataObject with ID " + toParent.getAttribute("ID"));
        }
        final File file = new File(fileLocation, fileName);
        if (!file.exists()) {
            throw new PDUStitchingException(
                    "Invalid href attribute given for dataObject with ID " + toParent.getAttribute("ID"));
        }
        return file;
    }

    private String getFileName(List<Node> fromParents, Element toParent) throws PDUStitchingException {
        String fileName = "";
        for (int i = 0; i < fromParents.size(); i++) {
            final NodeList dataObjectChildNodes = fromParents.get(i).getChildNodes();
            for (int l = 0; l < dataObjectChildNodes.getLength(); l++) {
                if (dataObjectChildNodes.item(l).getNodeName().equals("byteStream")) {
                    final NodeList byteStreamChildNodes = dataObjectChildNodes.item(l).getChildNodes();
                    for (int k = 0; k < byteStreamChildNodes.getLength(); k++) {
                        if (byteStreamChildNodes.item(k).getNodeName().equals("fileLocation")) {
                            final NamedNodeMap fileLocationAttributes = byteStreamChildNodes.item(k).getAttributes();
                            final Node hrefAttribute = fileLocationAttributes.getNamedItem("href");
                            if (hrefAttribute != null) {
                                if (i == 0) {
                                    fileName = hrefAttribute.getNodeValue();
                                } else {
                                    if (!fileName.equals(hrefAttribute.getNodeValue())) {
                                        throw new PDUStitchingException(
                                                "Different href attributes given for dataObject with ID " + toParent.getAttribute("ID"));
                                    }
                                }
                            } else {
                                throw new PDUStitchingException(
                                        "No href attribute given for dataObject with ID " + toParent.getAttribute("ID"));
                            }
                            break;
                        }
                    }
                    break;
                }
            }
        }
        return fileName;
    }

    static String getChecksum(File file) throws NoSuchAlgorithmException, IOException {
        final MessageDigest md = MessageDigest.getInstance("MD5");
        final DigestInputStream digestInputStream = new DigestInputStream(new FileInputStream(file), md);
        int read = digestInputStream.read();
        while (read >= 0) {
            read = digestInputStream.read();
        }
        digestInputStream.close();
        final byte[] digest = md.digest();
        StringBuilder digestResult = new StringBuilder();
        for (byte aDigest : digest) {
            digestResult.append(Integer.toString((aDigest & 0xff) + 0x100, 16).substring(1));
        }
        return digestResult.toString();
    }

}
