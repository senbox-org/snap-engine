/*
 * $Id: DpmConfig.java,v 1.1 2007/03/27 12:51:41 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.l2auxdata;

import org.esa.snap.core.util.SystemUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Iterator;

/**
 * Represents the configuration for the MERIS Level 2 Processor.
 */
public class DpmConfig {

    public static final String REMOTE_AUXDATA_PATH = "http://step.esa.int/auxdata/meris-l2auxdata/meris_l2.zip";

    private static final String AUXDATA_DIRNAME = "meris_l2";
    private static final String MERIS_L2_CONF = "meris_l2_config.xml";

    private Element rootElement;
    private File auxdataTargetDir;

    /**
     * Constructs a new configuration.
     *
     * @throws L2AuxDataException
     *          if the configuration could not be loaded from the file
     */
    public DpmConfig() throws L2AuxDataException {
        final Path auxdataDirPath = SystemUtils.getAuxDataPath().resolve(AUXDATA_DIRNAME).toAbsolutePath();
        if (!Files.isRegularFile(auxdataDirPath.resolve(MERIS_L2_CONF))) {
            try {
                Utils.downloadAndInstallAuxdata(auxdataDirPath, new URL(REMOTE_AUXDATA_PATH));
            } catch (MalformedURLException e) {
                throw new L2AuxDataException("Not able to download auxiliary data.", e);
            }
        }
        auxdataTargetDir = auxdataDirPath.toFile();

        File configFile = new File(auxdataTargetDir, MERIS_L2_CONF);
        FileReader reader = null;
        try {
            reader = new FileReader(configFile);
            init(reader);
        } catch (FileNotFoundException e) {
            throw new L2AuxDataException("Configuration file not found: " + configFile.getPath());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Gets the directory of the MERIS Level 2 auxiliary databases.
     *
     * @return the auxiliary databases directory, never {@code null}
     * @throws L2AuxDataException if the directory could not be retrieved from this configuration
     */
    public File getAuxDataDir() throws L2AuxDataException {
        final Element auxDataConfigElement = getMandatoryChild(rootElement, "aux_data_config");
        final String auxDataDirPath = getOptionalAttribute(auxDataConfigElement, "dir");
        final File auxDataDir;
        if (auxDataDirPath != null) {
            auxDataDir = new File(auxDataDirPath);
        } else {
            auxDataDir = auxdataTargetDir;
        }
        return auxDataDir;
    }

    // todo - check if really in use or just tested

    /**
     * Gets the file path for the given database.
     *
     * @param name           the database name, e.g. {@code "landaero"} or {@code "case2"}
     * @param acquisitionDate the acquisition date of a given level 1b input product, can be {@code null}
     * @return the file path to the database file, never {@code null}
     * @throws L2AuxDataException if the file could not be retrieved from this configuration
     */
    public File getAuxDatabaseFile(String name, Date acquisitionDate) throws L2AuxDataException {
        String auxDatabaseFilename = null;
        final Element auxDataConfigElement = getMandatoryChild(rootElement, "aux_data_config");
        final Element auxDataDefaultsElement = getMandatoryChild(auxDataConfigElement, "aux_data_defaults");
        final Iterator auxDatabaseElementIt = getMandatoryChildren(auxDataDefaultsElement, "aux_database");
        while (auxDatabaseElementIt.hasNext()) {
            Element auxDatabaseElement = (Element) auxDatabaseElementIt.next();
            final String auxDatabaseName = getMandatoryAttribute(auxDatabaseElement, "name");
            if (name.equalsIgnoreCase(auxDatabaseName)) {
                auxDatabaseFilename = getMandatoryAttribute(auxDatabaseElement, "file");
                break;
            }
        }

        if (acquisitionDate != null) {
            // todo - loop through "aux_data_overrides" children and compare given dates with "aquisition_start" and "aquisition_end"
        }

        if (auxDatabaseFilename == null) {
            throw new L2AuxDataException("Auxiliary database name not specified in configuration: " + name);
        }

        File auxDataDir = getAuxDataDir();
        return new File(new File(auxDataDir, name), auxDatabaseFilename);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Private implementation helpers

    private Element getMandatoryChild(Element parent, String name) throws L2AuxDataException {
        final Element child = parent.getChild(name);
        if (child == null) {
            throw new L2AuxDataException("Missing element '" + name + "' in element '" + parent.getName() + "'");
        }
        return child;
    }

    private Iterator getMandatoryChildren(Element parent, String name) throws L2AuxDataException {
        final Iterator iterator = (parent.getChildren(name)).iterator();
        if (!iterator.hasNext()) {
            throw new L2AuxDataException("Missing element(s) '" + name + "' in element '" + parent.getName() + "'");
        }
        return iterator;
    }


    private String getOptionalAttribute(Element element, String name) {
        return element.getAttributeValue(name);
    }

    private String getMandatoryAttribute(Element element, String name) throws L2AuxDataException {
        final String value = element.getAttributeValue(name);
        if (value == null) {
            throw new L2AuxDataException("Missing attribute '" + name + "' in element '" + element.getName() + "'");
        }
        return value;
    }


    private void init(Reader reader) throws L2AuxDataException {
        final SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setValidation(false); // todo - provide XML schema or DTD for config
        try {
            final Document document = saxBuilder.build(reader);
            rootElement = document.getRootElement();
        } catch (JDOMException | IOException e) {
            throw new L2AuxDataException("Failed to load configuration", e);
        }
    }
}
