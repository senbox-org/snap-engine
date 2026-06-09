package org.esa.snap.remote.products.repository.cdse;

import org.esa.snap.remote.products.repository.listener.ProgressListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class CdseZipExtractor {

    Path extract(Path zipFile, Path targetFolder, String productName, ProgressListener progressListener) throws IOException {
        Path targetRoot = targetFolder.toAbsolutePath().normalize();
        Set<String> topLevelNames = new HashSet<>();
        try (ZipFile zip = new ZipFile(zipFile.toFile())) {
            int totalEntries = zip.size();
            int extractedEntries = 0;
            for (ZipEntry entry : java.util.Collections.list(zip.entries())) {
                Path outputPath = targetRoot.resolve(entry.getName()).normalize();
                if (!outputPath.startsWith(targetRoot)) {
                    throw new IOException("ZIP entry '" + entry.getName() + "' resolves outside target directory.");
                }
                rememberTopLevelName(entry.getName(), topLevelNames);
                if (entry.isDirectory()) {
                    Files.createDirectories(outputPath);
                } else {
                    Files.createDirectories(outputPath.getParent());
                    try (InputStream inputStream = zip.getInputStream(entry);
                         OutputStream outputStream = Files.newOutputStream(outputPath)) {
                        inputStream.transferTo(outputStream);
                    }
                }
                extractedEntries++;
                if (progressListener != null && totalEntries > 0) {
                    progressListener.notifyProgress((short) Math.min(100, (extractedEntries * 100) / totalEntries));
                }
            }
        }
        Path namedProductFolder = targetRoot.resolve(stripZipSuffix(productName)).normalize();
        if (Files.isDirectory(namedProductFolder)) {
            return namedProductFolder;
        }
        if (topLevelNames.size() == 1) {
            Path singleFolder = targetRoot.resolve(topLevelNames.iterator().next()).normalize();
            if (Files.isDirectory(singleFolder)) {
                return singleFolder;
            }
        }
        return targetRoot;
    }

    private static void rememberTopLevelName(String entryName, Set<String> topLevelNames) {
        String normalizedName = entryName.replace('\\', '/');
        int separatorIndex = normalizedName.indexOf('/');
        String topLevelName = separatorIndex >= 0 ? normalizedName.substring(0, separatorIndex) : normalizedName;
        if (!topLevelName.isBlank()) {
            topLevelNames.add(topLevelName);
        }
    }

    private static String stripZipSuffix(String productName) {
        return productName != null && productName.toLowerCase().endsWith(".zip")
                ? productName.substring(0, productName.length() - 4)
                : productName;
    }
}
