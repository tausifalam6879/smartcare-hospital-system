package com.smartcare.bloodreaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcare.bloodreaction.config.BloodReactionModelProperties;
import com.smartcare.common.error.ConflictException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@Component
public class BloodReactionModelClient {
    private final BloodReactionModelProperties properties;
    private final ObjectMapper mapper;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public BloodReactionModelClient(BloodReactionModelProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
    }

    public PanelInference analyze(ImageInput antiA, ImageInput antiB, ImageInput antiD) {
        String boundary = "SmartCare-" + UUID.randomUUID();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.baseUrl() + "/analyze-abo-panel"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipart(boundary, antiA, antiB, antiD)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new ConflictException("Local blood-reaction model did not accept this panel.");
            return mapper.readValue(response.body(), PanelInference.class);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ConflictException("Local blood-reaction model request was interrupted.");
        } catch (IOException | IllegalArgumentException exception) {
            throw new ConflictException("Local blood-reaction model is unavailable. Start the ML service and retry.");
        }
    }

    private static byte[] multipart(String boundary, ImageInput antiA, ImageInput antiB, ImageInput antiD) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writePart(output, boundary, "antiAFile", antiA);
        writePart(output, boundary, "antiBFile", antiB);
        writePart(output, boundary, "antiDFile", antiD);
        output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }

    private static void writePart(ByteArrayOutputStream output, String boundary, String name, ImageInput input) throws IOException {
        output.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"; filename=\""
                + input.filename().replace("\"", "_") + "\"\r\nContent-Type: " + input.contentType() + "\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        output.write(input.content());
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    public record ImageInput(String filename, String contentType, byte[] content) { }
    public record Reaction(boolean agglutinationDetected, BigDecimal agglutinationProbability, BigDecimal confidence,
                           boolean manualReviewRequired, String modelName, String modelVersion) { }
    public record PanelInference(Reaction antiA, Reaction antiB, Reaction antiD, String bloodGroup,
                                 String interpretationStatus, String explanation, String safetyNotice) { }
}
