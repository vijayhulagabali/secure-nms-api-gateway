package com.nms.nbi.client;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class NmsApiClient {

    private static final String TOKEN_URL =
            "http://localhost:8081/realms/nms-realm/protocol/openid-connect/token";

    private static final String API_URL =
            "https://localhost:8443/api/v1/devices";

    private static HttpClient createHttpClient() throws Exception {

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
                        javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm()
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

    public static void main(String[] args) throws Exception {

        HttpClient HTTP_CLIENT = createHttpClient();

        String clientId = System.getenv("NMS_CLIENT_ID");
        String clientSecret = System.getenv("NMS_CLIENT_SECRET");

        if (clientId == null || clientSecret == null) {
            System.err.println(
                    "Please set NMS_CLIENT_ID and NMS_CLIENT_SECRET environment variables."
            );
            System.exit(1);
        }

        String token = getAccessToken(
                HTTP_CLIENT,
                clientId,
                clientSecret
        );

        System.out.println("Access token obtained successfully.");
        System.out.println("Calling NMS API through Kong...");

        HttpRequest apiRequest = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response =
                HTTP_CLIENT.send(
                        apiRequest,
                        HttpResponse.BodyHandlers.ofString()
                );

        System.out.println("HTTP Status: " + response.statusCode());
        System.out.println("Response:");
        System.out.println(response.body());
    }

    private static String getAccessToken(
            HttpClient HTTP_CLIENT,
            String clientId,
            String clientSecret
    ) throws Exception {

        String formData =
                "grant_type=client_credentials"
                        + "&client_id=" + encode(clientId)
                        + "&client_secret=" + encode(clientSecret);

        HttpRequest tokenRequest = HttpRequest.newBuilder()
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

        HttpResponse<String> response =
                HTTP_CLIENT.send(
                        tokenRequest,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Failed to obtain access token. Keycloak returned HTTP "
                            + response.statusCode()
                            + ": "
                            + response.body()
            );
        }

        return extractAccessToken(response.body());
    }

    private static String extractAccessToken(String json) {

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

    private static String encode(String value) {
        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }
}
