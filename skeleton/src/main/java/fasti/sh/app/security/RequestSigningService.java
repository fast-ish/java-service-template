package fasti.sh.app.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Service for signing and verifying service-to-service requests.
 *
 * Implements HMAC-SHA256 request signing similar to AWS Signature Version 4.
 *
 * Usage for outgoing requests:
 * <pre>
 * Map<String, String> headers = requestSigningService.signRequest(
 *     "POST",
 *     "/api/v1/orders",
 *     requestBody,
 *     existingHeaders
 * );
 * // Add headers to your HTTP client
 * </pre>
 *
 * Usage for incoming requests (via filter/interceptor):
 * <pre>
 * boolean valid = requestSigningService.verifyRequest(
 *     request.getMethod(),
 *     request.getRequestURI(),
 *     requestBody,
 *     headersMap
 * );
 * </pre>
 */
@Service
public class RequestSigningService {

    private static final Logger log = LoggerFactory.getLogger(RequestSigningService.class);

    private static final String ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_HEADER = "X-Signature";
    private static final String TIMESTAMP_HEADER = "X-Timestamp";
    private static final String SERVICE_ID_HEADER = "X-Service-ID";
    private static final long MAX_TIMESTAMP_DRIFT_SECONDS = 300; // 5 minutes

    @Value("${security.signing.secret:}")
    private String signingSecret;

    @Value("${security.signing.service-id:}")
    private String serviceId;

    @Value("${security.signing.enabled:false}")
    private boolean signingEnabled;

    /**
     * Sign an outgoing request.
     *
     * @return Map of headers to add to the request
     */
    public Map<String, String> signRequest(String method, String path,
                                           String body, Map<String, String> existingHeaders) {
        if (!signingEnabled || signingSecret.isEmpty()) {
            return Map.of();
        }

        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String stringToSign = buildStringToSign(method, path, body, timestamp, existingHeaders);
        String signature = computeSignature(stringToSign);

        return Map.of(
            SIGNATURE_HEADER, signature,
            TIMESTAMP_HEADER, timestamp,
            SERVICE_ID_HEADER, serviceId
        );
    }

    /**
     * Verify an incoming signed request.
     */
    public boolean verifyRequest(String method, String path, String body,
                                 Map<String, String> headers) {
        if (!signingEnabled) {
            return true;
        }

        String providedSignature = headers.get(SIGNATURE_HEADER);
        String timestamp = headers.get(TIMESTAMP_HEADER);
        String callerServiceId = headers.get(SERVICE_ID_HEADER);

        if (providedSignature == null || timestamp == null) {
            log.warn("Missing signature or timestamp header");
            return false;
        }

        // Check timestamp to prevent replay attacks
        if (!isTimestampValid(timestamp)) {
            log.warn("Request timestamp too old or in future: {}", timestamp);
            return false;
        }

        // Compute expected signature
        String stringToSign = buildStringToSign(method, path, body, timestamp, headers);
        String expectedSignature = computeSignature(stringToSign);

        boolean valid = constantTimeEquals(expectedSignature, providedSignature);

        if (!valid) {
            log.warn("Invalid request signature from service: {}", callerServiceId);
        }

        return valid;
    }

    /**
     * Build the canonical string to sign.
     */
    private String buildStringToSign(String method, String path, String body,
                                     String timestamp, Map<String, String> headers) {
        StringBuilder sb = new StringBuilder();

        // HTTP method
        sb.append(method.toUpperCase()).append("\n");

        // Path
        sb.append(path).append("\n");

        // Timestamp
        sb.append(timestamp).append("\n");

        // Sorted headers (only include specific ones)
        TreeMap<String, String> sortedHeaders = new TreeMap<>();
        if (headers != null) {
            headers.forEach((k, v) -> {
                String lower = k.toLowerCase();
                if (lower.startsWith("x-") && !lower.equals(SIGNATURE_HEADER.toLowerCase())) {
                    sortedHeaders.put(lower, v);
                }
            });
        }

        String headerString = sortedHeaders.entrySet().stream()
            .map(e -> e.getKey() + ":" + e.getValue())
            .collect(Collectors.joining("\n"));
        sb.append(headerString).append("\n");

        // Body hash
        String bodyHash = body != null ? sha256Hex(body) : sha256Hex("");
        sb.append(bodyHash);

        return sb.toString();
    }

    /**
     * Compute HMAC-SHA256 signature.
     */
    private String computeSignature(String stringToSign) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                signingSecret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            mac.init(keySpec);
            byte[] signature = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to compute signature", e);
            throw new RuntimeException("Signature computation failed", e);
        }
    }

    /**
     * Check if timestamp is within acceptable range.
     */
    private boolean isTimestampValid(String timestamp) {
        try {
            long requestTime = Long.parseLong(timestamp);
            long now = Instant.now().getEpochSecond();
            long diff = Math.abs(now - requestTime);
            return diff <= MAX_TIMESTAMP_DRIFT_SECONDS;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /**
     * Compute SHA-256 hash of a string.
     */
    private String sha256Hex(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
