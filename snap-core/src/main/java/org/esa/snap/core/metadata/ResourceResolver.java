package org.esa.snap.core.metadata;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

import org.esa.snap.core.util.SystemUtils;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

//TODO re-implement this class
/**
 * The Class ResourceResolver.
 * Implemented by Smyrnian in http://stackoverflow.com/questions/2342808/problem-validating-an-xml-file-using-java-with-an-xsd-having-an-include
 * Modified by obarrilero
 * "This LSResourceResolver implementation assumes that all XSD files have a unique name.
 * If you have some XSD files with same name but different content (at different paths) in your schema structure,
 * this resolver will fail to include the other XSD files except the first one found."
 *
 */
public class ResourceResolver implements LSResourceResolver {

    /** The logger. */
    private final Logger logger = SystemUtils.LOG;

    /** The schema base path. */
    private final String schemaBasePath;

    private final ClassLoader classLoader;

    /** The path map. */
    private Map<String, String> pathMap = new HashMap<String, String>();


    public ResourceResolver(String schemaBasePath, ClassLoader classLoader) {
        this.schemaBasePath = schemaBasePath;
        this.classLoader = classLoader;
    }

    @Override
    public LSInput resolveResource(String type, String namespaceURI,
                                   String publicId, String systemId, String baseURI) {
        // The base resource that includes this current resource
        String baseResourceName = null;
        String baseResourcePath = null;
        // Extract the current resource name
        String currentResourceName = systemId.substring(systemId
                                                                .lastIndexOf("/") + 1);

        // If this resource hasn't been added yet
        if (!pathMap.containsKey(currentResourceName)) {
            if (baseURI != null) {
                baseResourceName = baseURI
                        .substring(baseURI.lastIndexOf("/") + 1);
            }

            // we dont need "./" since getResourceAsStream cannot understand it
            if (systemId.startsWith("./")) {
                systemId = systemId.substring(2, systemId.length());
            }

            // If the baseResourcePath has already been discovered, get that
            // from pathMap
            if (pathMap.containsKey(baseResourceName)) {
                baseResourcePath = pathMap.get(baseResourceName);
            } else {
                // The baseResourcePath should be the schemaBasePath
                baseResourcePath = schemaBasePath;
            }

            // Read the resource as input stream
            String normalizedPath = getNormalizedPath(baseResourcePath, systemId);

           try (InputStream resourceAsStream = classLoader
                    .getResourceAsStream(normalizedPath)) {

               // if the current resource is not in the same path with base
               // resource, add current resource's path to pathMap
               if (systemId.contains("/")) {
                   pathMap.put(currentResourceName, normalizedPath.substring(0, normalizedPath.lastIndexOf("/") + 1));
               } else {
                   // The current resource should be at the same path as the base
                   // resource
                   pathMap.put(systemId, baseResourcePath);
               }
               Scanner s = new Scanner(resourceAsStream).useDelimiter("\\A");
               String s1 = s.next().replaceAll("\\n", " ") // the parser cannot understand elements broken down multiple lines e.g. (<xs:element \n name="buxing">)
                       .replace("\\t", " ") // these two about whitespaces is only for decoration
                       .replaceAll("\\s+", " ").replaceAll("[^\\x20-\\x7e]", ""); // some files has a special character as a first character indicating utf-8 file
               InputStream is = new ByteArrayInputStream(s1.getBytes());

               return new Input(publicId, systemId, is);
           } catch (IOException e) {
               e.printStackTrace();
           }
        }

        return null;
    }

    /**
     * Gets the normalized path.
     *
     * @param basePath the base path
     * @param relativePath the relative path
     * @return the normalized path
     */
    private String getNormalizedPath(String basePath, String relativePath){
        if(!relativePath.startsWith("../")){
            return basePath + relativePath;
        }
        else{
            while(relativePath.startsWith("../")){
                basePath = basePath.substring(0,basePath.substring(0, basePath.length()-1).lastIndexOf("/")+1);
                relativePath = relativePath.substring(3);
            }
            return basePath+relativePath;
        }
    }

    private class Input implements LSInput {
        private String publicId;

        private String systemId;

        public String getPublicId() {
            return publicId;
        }

        public void setPublicId(String publicId) {
            this.publicId = publicId;
        }

        public String getBaseURI() {
            return null;
        }

        public InputStream getByteStream() {
            return null;
        }

        public boolean getCertifiedText() {
            return false;
        }

        public Reader getCharacterStream() {
            return null;
        }

        public String getEncoding() {
            return null;
        }

        public String getStringData() {
            synchronized (inputStream) {
                try {
                    byte[] input = new byte[inputStream.available()];
                    inputStream.read(input);
                    String contents = new String(input);
                    return contents;
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.severe("Exception " + e);
                    return null;
                }
            }
        }

        public void setBaseURI(String baseURI) {
        }

        public void setByteStream(InputStream byteStream) {
        }

        public void setCertifiedText(boolean certifiedText) {
        }

        public void setCharacterStream(Reader characterStream) {
        }

        public void setEncoding(String encoding) {
        }

        public void setStringData(String stringData) {
        }

        public String getSystemId() {
            return systemId;
        }

        public void setSystemId(String systemId) {
            this.systemId = systemId;
        }

        public BufferedInputStream getInputStream() {
            return inputStream;
        }

        public void setInputStream(BufferedInputStream inputStream) {
            this.inputStream = inputStream;
        }

        private BufferedInputStream inputStream;

        public Input(String publicId, String sysId, InputStream input) {
            this.publicId = publicId;
            this.systemId = sysId;
            this.inputStream = new BufferedInputStream(input);
        }
    }
}
