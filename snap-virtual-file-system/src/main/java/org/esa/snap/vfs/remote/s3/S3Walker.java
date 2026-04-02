package org.esa.snap.vfs.remote.s3;

import org.esa.snap.vfs.remote.AbstractRemoteWalker;
import org.esa.snap.vfs.remote.HttpUtils;
import org.esa.snap.vfs.remote.IRemoteConnectionBuilder;
import org.esa.snap.vfs.remote.VFSPath;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Walker for S3 VFS.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
class S3Walker extends AbstractRemoteWalker {

    private final String address;
    private final String bucket;
    private final String delimiter;
    private final String root;

    /**
     * Creates the new walker for S3  VFS
     *
     * @param address                 The address of S3 service. (mandatory)
     * @param bucket                  The bucket name (mandatory)
     * @param delimiter               The VFS path delimiter
     * @param root                    The root of S3 provider
     * @param remoteConnectionBuilder The connection builder
     */
    S3Walker(String address, String bucket, String delimiter, String root, IRemoteConnectionBuilder remoteConnectionBuilder) {
        super(remoteConnectionBuilder);

        this.address = address;
        this.bucket = bucket;
        this.delimiter = delimiter;
        this.root = root;
    }

    /**
     * Append the new request parameter represented by name and value to the request parameters builder.
     *
     * @param params The request parameters builder
     * @param name   The name of new request parameter
     * @param value  The value of new request parameter
     */
    private static void addParam(StringBuilder params, String name, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        if (!params.isEmpty()) {
            params.append("&");
        }
        params.append(name).append("=").append(URLEncoder.encode(value, StandardCharsets.UTF_8).replaceAll("\\+", "%20"));
    }

    /**
     * Gets a list of VFS files and directories from to the given prefix.
     *
     * @param dir The VFS path to traverse
     * @return The list of VFS files and directories
     * @throws IOException If an I/O error occurs
     */
    @Override
    public synchronized List<BasicFileAttributes> walk(VFSPath dir) throws IOException {
        String dirPath = dir.toString();
        String s3Prefix = buildPrefix(dirPath + (dirPath.endsWith("/") ? "" : "/"));
        String fileSystemRoot = dir.getFileSystem().getRoot().getPath();
        List<BasicFileAttributes> items = new ArrayList<>();
        String nextContinuationToken = "";

        S3ResponseHandler handler;
        do {
            handler = new S3ResponseHandler(this.root + this.delimiter + s3Prefix, items, this.delimiter);
            String s3URL = buildS3URL(s3Prefix, nextContinuationToken);
            final URL url;
            try {
                url = new URI(s3URL).toURL();
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
            HttpURLConnection connection = this.remoteConnectionBuilder.buildConnection(fileSystemRoot, url, "GET", null);
            try {
                int responseCode = connection.getResponseCode();
                if (HttpUtils.isValidResponseCode(responseCode)) {
                    try (InputStream inputStream = connection.getInputStream();
                         BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream, 10 * 1024)) {

                        try {
                            SAXParserFactory spf = SAXParserFactory.newInstance();
                            spf.setNamespaceAware(true);
                            SAXParser saxParser = spf.newSAXParser();
                            XMLReader xmlReader = saxParser.getXMLReader();
                            xmlReader.setContentHandler(handler);
                            xmlReader.parse(new InputSource(bufferedInputStream));
                        } catch (SAXException | ParserConfigurationException ex) {
                            throw new IOException(ex);
                        }
                    }
                } else {
                    Logger.getLogger(HttpUtils.class.getName()).warning("HTTP error response:");
                    Logger.getLogger(HttpUtils.class.getName()).warning(() -> {
                        try {
                            return HttpUtils.readString(connection.getErrorStream()).replaceAll("<AWSAccessKeyId>.*</AWSAccessKeyId>","<AWSAccessKeyId>***</AWSAccessKeyId>");
                        } catch (IOException ignored) {
                        }
                        return "";
                    });
                    throw new IOException(url + ": response code " + responseCode + ": " + connection.getResponseMessage());
                }
            } finally {
                connection.disconnect();
            }
            nextContinuationToken = handler.getNextContinuationToken();
        } while (handler.getIsTruncated());

        if (!handler.isFound()) {
            throw new IOException("file " + dir + " not found.");
        }
        return items;
    }

    private String buildPrefix(String prefix) {
        prefix = prefix.replace(this.root, "");
        prefix = prefix.replaceAll("^/", "");
        return prefix;
    }

    private String buildS3URL(String prefix, String nextContinuationToken) {
        String currentBucket = this.bucket;
        currentBucket = (currentBucket != null && !currentBucket.isEmpty()) ? currentBucket + this.delimiter : "";
        StringBuilder paramBase = new StringBuilder();
        addParam(paramBase, "prefix", prefix);
        addParam(paramBase, "delimiter", this.delimiter);
        StringBuilder params = new StringBuilder(paramBase);
        addParam(params, "marker", nextContinuationToken);
        String s3URL = this.address + (this.address.endsWith(this.delimiter) ? "" : this.delimiter) + currentBucket;
        if (!params.isEmpty()) {
            s3URL += "?" + params;
        }
        return s3URL;
    }
}
