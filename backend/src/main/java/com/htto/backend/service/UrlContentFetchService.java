package com.htto.backend.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UrlContentFetchService {

    static final int MAX_DOWNLOAD_BYTES = 5 * 1024 * 1024;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;

    public UrlContentFetchService() {
        this(HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    UrlContentFetchService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public FetchedUrlContent fetch(String url) {
        URI uri = validateUrl(url);
        validateNetworkTarget(uri);

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .header("Accept", "text/plain,text/html,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,*/*")
                .build();

        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cannot fetch URL. HTTP status: " + statusCode
                );
            }

            long contentLength = response.headers()
                    .firstValueAsLong("Content-Length")
                    .orElse(-1L);
            if (contentLength > MAX_DOWNLOAD_BYTES) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL content exceeds 5MB limit");
            }

            String contentType = response.headers()
                    .firstValue("Content-Type")
                    .orElse("");
            byte[] content = readLimited(response.body());
            return new FetchedUrlContent(uri, contentType, content);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot fetch URL content");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL fetch was interrupted");
        }
    }

    URI validateUrl(String url) {
        if (!StringUtils.hasText(url)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL is required");
        }

        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL is invalid");
        }

        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL scheme is required");
        }
        String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
        if (!normalizedScheme.equals("http") && !normalizedScheme.equals("https")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only http and https URLs are allowed");
        }
        if (!StringUtils.hasText(uri.getHost())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL host is required");
        }
        return uri;
    }

    private void validateNetworkTarget(URI uri) {
        String host = uri.getHost();
        if (host.equalsIgnoreCase("localhost")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Localhost URLs are not allowed");
        }

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot resolve URL host");
        }

        for (InetAddress address : addresses) {
            if (isBlockedAddress(address)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "URL host resolves to a local or private address"
                );
            }
        }
    }

    private boolean isBlockedAddress(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || isUniqueLocalIpv6(address);
    }

    private boolean isUniqueLocalIpv6(InetAddress address) {
        if (!(address instanceof Inet6Address)) {
            return false;
        }
        byte[] bytes = address.getAddress();
        return bytes.length > 0 && (bytes[0] & 0xfe) == 0xfc;
    }

    private byte[] readLimited(InputStream inputStream) throws IOException {
        try (inputStream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                total += read;
                if (total > MAX_DOWNLOAD_BYTES) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL content exceeds 5MB limit");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }
}
