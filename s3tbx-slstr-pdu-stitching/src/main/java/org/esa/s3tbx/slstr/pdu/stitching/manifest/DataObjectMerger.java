package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import org.esa.s3tbx.slstr.pdu.stitching.PDUStitchingException;
import org.esa.snap.core.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * @author Tonio Fincke
 */
class DataObjectMerger extends AbstractElementMerger {

    private final String fileLocation;

    DataObjectMerger(String fileLocation) {
        this.fileLocation = fileLocation;
    }

    @Override
    public void mergeNodes(List<Node> fromParents, Element toParent, Document toDocument) throws PDUStitchingException {
        final String fileName = getFileName(fromParents, toParent);
        final File file = getFile(fileName, toParent);
        final long size = file.length();
        final Element byteStreamElement = toDocument.createElement("byteStream");
        toParent.appendChild(byteStreamElement);
        byteStreamElement.setAttribute("mimeType", "application/x-netcdf");
        byteStreamElement.setAttribute("size", String.valueOf(size));
        final Element fileLocationElement = toDocument.createElement("fileLocation");
        byteStreamElement.appendChild(fileLocationElement);
        fileLocationElement.setAttribute("locatorType", "URL");
        fileLocationElement.setAttribute("href", fileName);
        final Element checksumElement = toDocument.createElement("checksum");
        byteStreamElement.appendChild(checksumElement);
        checksumElement.setAttribute("checksumName", "MD5");
        final String checksum;
        checksum = getChecksum(file);
        if (checksum.equals("")) {
            throw new PDUStitchingException("Could not create checksum for file " + fileName);
        }
        addTextToNode(checksumElement, checksum, toDocument);
    }

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

    //package local for testing
    static String getChecksum(File file) {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            MessageDigest md = MessageDigest.getInstance("MD5");
            FileChannel channel = inputStream.getChannel();
            ByteBuffer buff = ByteBuffer.allocate(2048);
            while (channel.read(buff) != -1) {
                buff.flip();
                md.update(buff);
                buff.clear();
            }
            final byte[] digest = md.digest();
            StringBuilder digestResult = new StringBuilder();
            for (byte aDigest : digest) {
                digestResult.append(Integer.toString((aDigest & 0xff) + 0x100, 16).substring(1));
            }
            return digestResult.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            return "";
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {

                }
            }
        }
    }

}
