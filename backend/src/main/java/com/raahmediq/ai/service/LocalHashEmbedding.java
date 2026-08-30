package com.raahmediq.ai.service;

import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class LocalHashEmbedding {
    public static final String MODEL = "LOCAL_HASH_V1_192";
    private static final int DIMENSIONS = 192;
    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}]{2,}");

    public String embed(String text) {
        float[] vector = vector(text);
        ByteBuffer buffer = ByteBuffer.allocate(DIMENSIONS * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : vector) buffer.putFloat(value);
        return Base64.getEncoder().encodeToString(buffer.array());
    }

    public double similarity(String question, String encodedVector, String content) {
        float[] query = vector(expand(question));
        float[] document = decode(encodedVector);
        double cosine = 0;
        for (int i = 0; i < DIMENSIONS; i++) cosine += query[i] * document[i];
        Set<String> queryTokens = tokens(expand(question));
        Set<String> documentTokens = tokens(content);
        long overlap = queryTokens.stream().filter(documentTokens::contains).count();
        double lexicalBoost = queryTokens.isEmpty() ? 0 : Math.min(0.22, (overlap * 0.16) / queryTokens.size());
        return cosine + lexicalBoost;
    }

    public String sha256(String text) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private float[] vector(String text) {
        float[] values = new float[DIMENSIONS];
        for (String token : tokens(text)) {
            int hash = token.hashCode();
            int index = Math.floorMod(hash, DIMENSIONS);
            values[index] += (hash & 1) == 0 ? 1f : -1f;
        }
        double norm = 0;
        for (float value : values) norm += value * value;
        if (norm > 0) {
            float divisor = (float) Math.sqrt(norm);
            for (int i = 0; i < values.length; i++) values[i] /= divisor;
        }
        return values;
    }

    private float[] decode(String encoded) {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        if (bytes.length != DIMENSIONS * Float.BYTES) return new float[DIMENSIONS];
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        float[] values = new float[DIMENSIONS];
        for (int i = 0; i < values.length; i++) values[i] = buffer.getFloat();
        return values;
    }

    private Set<String> tokens(String text) {
        Set<String> result = new HashSet<>();
        var matcher = TOKEN.matcher(text == null ? "" : text.toLowerCase(Locale.ROOT));
        while (matcher.find()) result.add(stem(matcher.group()));
        return result;
    }

    private String stem(String token) {
        if (token.length() > 5 && token.endsWith("ing")) return token.substring(0, token.length() - 3);
        if (token.length() > 4 && token.endsWith("ed")) return token.substring(0, token.length() - 2);
        if (token.length() > 4 && token.endsWith("es")) return token.substring(0, token.length() - 2);
        if (token.length() > 3 && token.endsWith("s")) return token.substring(0, token.length() - 1);
        return token;
    }

    private String expand(String question) {
        String value = question == null ? "" : question.toLowerCase(Locale.ROOT);
        return value.replace("meds", "medicine prescription")
                .replace("medicine", "medicine prescription prescribed")
                .replace("return", "return follow up")
                .replace("report", "report document result finding")
                .replace("scan", "scan mri ct x ray report");
    }
}
