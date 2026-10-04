package com.nms.nbi.client;

import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;

@Service
public class NmsGatewayClientService {

    private static final String TOKEN_URL =
            "http://localhost:8081/realms/nms-realm/protocol/openid-connect/token";

    private static final String API_URL =
            "https://localhost:8443/api/v1/devices";

    private final HttpClient httpClient;

    public NmsGatewayClientService() throws Exception {
        this.httpClient = createHttpClient();
    }

    public String getDevices() throws Exception {

        String clientId = System.getenv("NMS_CLIENT_ID");
        String clientSecret = System.getenv("NMS_CLIENT_SECRET");

        if (clientId == null || clientSecret == null) {
            throw new IllegalStateException(
                    "NMS_CLIENT_ID and NMS_CLIENT_SECRET environment variables are required."
            );
        }

        String token = getAccessToken(clientId, clientSecret);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Kong NMS API returned HTTP " + response.statusCode()
            );
        }

        return response.body();
    }

    private String getAccessToken(
            String clientId,
            String clientSecret
    ) throws Exception {

        String formData =
                "grant_type=client_credentials"
                        + "&client_id=" + encode(clientId)
                        + "&client_secret=" + encode(clientSecret);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .timeout(Duration.ofSeconds(10))
                .header(
                        "Content-Type",
                        "application/x-www-form-urlencoded"
                )
                .POST(
                        HttpRequest.BodyPublishers.ofString(formData)
                )
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Keycloak token request failed with HTTP "
                            + response.statusCode()
            );
        }

        return extractAccessToken(response.body());
    }

    private String extractAccessToken(String json) {

        String marker = "\"access_token\":\"";

        int start = json.indexOf(marker);

        if (start == -1) {
            throw new RuntimeException(
                    "access_token was not found in Keycloak response."
            );
        }

        start += marker.length();

        int end = json.indexOf("\"", start);

        if (end == -1) {
            throw new RuntimeException(
                    "Invalid token response from Keycloak."
            );
        }

        return json.substring(start, end);
    }

    private String encode(String value) {
        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }

    private HttpClient createHttpClient() throws Exception {

        KeyStore trustStore = KeyStore.getInstance("PKCS12");

        try (InputStream inputStream = Files.newInputStream(
                Path.of("kong-truststore.p12"))) {

            trustStore.load(
                    inputStream,
                    "changeit".toCharArray()
            );
        }

        var trustManagerFactory =
                javax.net.ssl.TrustManagerFactory.getInstance(
                        javax.net.ssl.TrustManagerFactory
                                .getDefaultAlgorithm()
                );

        trustManagerFactory.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");

        sslContext.init(
                null,
                trustManagerFactory.getTrustManagers(),
                null
        );

        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .sslContext(sslContext)
                .build();
    }
}
