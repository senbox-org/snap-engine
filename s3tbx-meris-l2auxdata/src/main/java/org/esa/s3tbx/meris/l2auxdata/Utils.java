/*
 * $Id: Utils.java,v 1.1 2007/03/27 12:51:41 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.l2auxdata;

import org.esa.snap.core.datamodel.Product;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Utils {

    private Utils() {
    }

    public static double computeSeasonalFactor(double daysSince2000, double sunEarthDistanceSquare) {
        // Semi-major axis of ellipse Earth orbit around Sun in meters
        final double a = 149597870.0 * 1000.0;
        // Eccentricity of ellipse Earth orbit around Sun
        final double e = 0.017;
        // Perihelion in 2000 was the 03.01.2000 05:00
        final double daysToPerihelionIn2000 = 3.0 + 5.0 / 24.0;
        final double daysOfYear = 365.25;
        final double theta = 2 * Math.PI * ((daysSince2000 - daysToPerihelionIn2000) / daysOfYear);
        final double r = a * (1.0 - e * e) / (1.0 + e * Math.cos(theta));
        return r * r / sunEarthDistanceSquare;
    }

    public static boolean isProductRR(Product product) {
        return product.getProductType().indexOf("_RR") > 0;
    }

    public static boolean isProductFR(Product product) {
        return (product.getProductType().indexOf("_FR") > 0) ||
                (product.getProductType().indexOf("_FS") > 0);
    }

    static void downloadAndInstallAuxdata(Path targetDir, URL remoteZipFileUrl) throws L2AuxDataException {
        final URL fileUrl;
        try {
            fileUrl = remoteZipFileUrl;
            final URLConnection urlConnection;
            if (System.getProperty("http.proxyHost") == null || System.getProperty("http.proxyPort") == null) {
                urlConnection = fileUrl.openConnection();
            } else {
                urlConnection = fileUrl.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(System.getProperty("http.proxyHost"), Integer.parseInt(System.getProperty("http.proxyPort")))));
            }
            InputStream inputStream = urlConnection.getInputStream();
            ZipInputStream zis = new ZipInputStream(inputStream);
            ZipEntry entry;
            while((entry = zis.getNextEntry()) != null) {
                Path filepath = targetDir.resolve(entry.getName());
                if (!entry.isDirectory()) {
                    // if the entry is a file, extracts it
                    Files.copy(zis, filepath);
                } else {
                    // if the entry is a directory, make the directory
                    Files.createDirectories(filepath);
                }
            }
        } catch (IOException e) {
            throw new L2AuxDataException("Not able to download auxiliary data from " + remoteZipFileUrl, e);
        }
    }
}
