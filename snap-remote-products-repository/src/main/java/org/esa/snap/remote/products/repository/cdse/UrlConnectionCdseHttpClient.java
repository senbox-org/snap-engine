package org.esa.snap.remote.products.repository.cdse;

import org.esa.snap.remote.products.repository.HTTPServerException;
import org.esa.snap.remote.products.repository.listener.ProgressListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

class UrlConnectionCdseHttpClient implements CdseHttpClient {

    private static final int TIMEOUT_MILLIS = 300000;
    private static final int MAX_REDIRECTS = 8;

    @Override
    public CdseHttpResponse execute(CdseHttpRequest request) throws IOException {
        HttpURLConnection connection = open(request);
        try {
            int statusCode = connection.getResponseCode();
            return new CdseHttpResponse(statusCode, readAll(responseStream(connection, statusCode)));
        } finally {
            connection.disconnect();
        }
    }

    @Override
    public void download(CdseHttpRequest request, Path targetFile, ProgressListener progressListener) throws IOException {
        HttpURLConnection connection = open(request);
        try {
            int statusCode = connection.getResponseCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new HTTPServerException(statusCode, new String(readAll(responseStream(connection, statusCode)), StandardCharsets.UTF_8));
            }
            long contentLength = connection.getContentLengthLong();
            if (progressListener != null && contentLength > 0) {
                progressListener.notifyApproximateSize(contentLength);
            }
            Files.createDirectories(targetFile.getParent());
            try (InputStream inputStream = connection.getInputStream();
                 OutputStream outputStream = Files.newOutputStream(targetFile)) {
                copy(inputStream, outputStream, contentLength, progressListener);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static HttpURLConnection open(CdseHttpRequest request) throws IOException {
        CdseHttpRequest currentRequest = request;
        for (int redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount++) {
            HttpURLConnection connection = openOnce(currentRequest);
            int statusCode = connection.getResponseCode();
            if (!isRedirect(statusCode)) {
                return connection;
            }
            String location = connection.getHeaderField("Location");
            connection.disconnect();
            if (location == null || location.isBlank()) {
                throw new IOException("Redirect response from " + currentRequest.getUrl() + " does not contain a Location header.");
            }
            String redirectedUrl = URI.create(currentRequest.getUrl()).resolve(location).toString();
            String method = statusCode == HttpURLConnection.HTTP_SEE_OTHER ? "GET" : currentRequest.getMethod();
            String body = "GET".equals(method) ? null : currentRequest.getBody();
            currentRequest = new CdseHttpRequest(method, redirectedUrl, currentRequest.getHeaders(), body);
        }
        throw new IOException("Too many redirects while accessing " + request.getUrl());
    }

    private static HttpURLConnection openOnce(CdseHttpRequest request) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(request.getUrl()).toURL().openConnection();
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod(request.getMethod());
        for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
            connection.setRequestProperty(header.getKey(), header.getValue());
        }
        if (request.getBody() != null) {
            connection.setDoOutput(true);
            byte[] bodyBytes = request.getBody().getBytes(StandardCharsets.UTF_8);
            connection.setRequestProperty("Content-Length", Integer.toString(bodyBytes.length));
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(bodyBytes);
            }
        }
        return connection;
    }

    private static boolean isRedirect(int statusCode) {
        return statusCode == HttpURLConnection.HTTP_MOVED_PERM
                || statusCode == HttpURLConnection.HTTP_MOVED_TEMP
                || statusCode == HttpURLConnection.HTTP_SEE_OTHER
                || statusCode == 307
                || statusCode == 308;
    }

    private static InputStream responseStream(HttpURLConnection connection, int statusCode) throws IOException {
        if (statusCode >= 200 && statusCode < 300) {
            return connection.getInputStream();
        }
        InputStream errorStream = connection.getErrorStream();
        return errorStream != null ? errorStream : InputStream.nullInputStream();
    }

    private static byte[] readAll(InputStream inputStream) throws IOException {
        try (InputStream closeableInputStream = inputStream;
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = closeableInputStream.read(buffer)) >= 0) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        }
    }

    private static void copy(InputStream inputStream, OutputStream outputStream, long contentLength, ProgressListener progressListener) throws IOException {
        byte[] buffer = new byte[1024 * 1024];
        long downloadedBytes = 0;
        short lastProgress = -1;
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, bytesRead);
            downloadedBytes += bytesRead;
            if (progressListener != null && contentLength > 0) {
                short progress = (short) Math.min(100, (downloadedBytes * 100) / contentLength);
                if (progress != lastProgress) {
                    progressListener.notifyProgress(progress);
                    lastProgress = progress;
                }
            }
        }
        if (progressListener != null) {
            progressListener.notifyProgress((short) 100);
        }
    }
}
