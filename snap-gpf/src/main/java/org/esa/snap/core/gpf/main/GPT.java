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

package org.esa.snap.core.gpf.main;

import org.esa.snap.core.util.StopWatch;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;

import java.io.Console;
import java.net.*;
import java.util.Locale;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * The entry point for the GPF command-line tool (Graph Processing Tool, GPT).
 * For usage, see org/esa/snap/core/gpf/main/CommandLineUsage.txt
 * or use the option "-h".
 *
 * @since BEAM 4.10 (renamed from {@code Main}).
 */
public class GPT {

    public static void main(String... args) {
        try {
            final StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            run(args);

            stopWatch.stopAndTrace("GPT timing");
        } catch (Throwable e) {
            String message;
            if (e.getMessage() != null) {
                message = e.getMessage();
            } else {
                message = e.getClass().getName();
            }
            System.err.println("\nError: " + message);
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void run(String[] args) throws Exception {
        if (System.getProperty("snap.context") == null) {
            System.setProperty("snap.context", "snap");
        }
        Locale.setDefault(Locale.ENGLISH); // Force usage of english locale
        SystemUtils.init3rdPartyLibs(GPT.class);
        checkAndSetupProxyAuthentication();
        final CommandLineTool commandLineTool = new CommandLineTool();
        commandLineTool.run(args);
    }

    private static void checkAndSetupProxyAuthentication() throws Exception {
        final String proxyHost = System.getProperty("http.proxyHost");
        final String proxyPortString = System.getProperty("http.proxyPort");
        if (!StringUtils.isNullOrEmpty(proxyHost) && !StringUtils.isNullOrEmpty(proxyPortString)) {
            final int proxyPort = Integer.parseInt(proxyPortString);
            String proxyUser = System.getProperty("proxyUser");
            String proxyPassword = System.getProperty("proxyPassword");
            final Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            final HttpURLConnection urlConnection = (HttpURLConnection) new URI("http://step.esa.int").toURL().openConnection(proxy);
            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_PROXY_AUTH) {
                Logger.getLogger(GPT.class.getName()).warning("Proxy " + proxyHost + ":" + proxyPort + " requires authentication.");
                final Console console = System.console();
                if (StringUtils.isNullOrEmpty(proxyUser)) {
                    System.out.print("proxy user: ");
                    if (console != null) {
                        proxyUser = console.readLine();
                    } else {
                        proxyUser = new Scanner(System.in).nextLine();
                    }
                }
                if(StringUtils.isNullOrEmpty(proxyPassword)){

                    System.out.print("proxy password: ");
                    if (console != null) {
                        proxyPassword = new String(console.readPassword());
                    } else {
                        proxyPassword = new Scanner(System.in).nextLine();
                    }
                }
                final String finalProxyUsername = proxyUser;
                final String finalProxyPassword = proxyPassword;
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(finalProxyUsername, finalProxyPassword.toCharArray());
                    }
                });
            }
        }
    }
}
