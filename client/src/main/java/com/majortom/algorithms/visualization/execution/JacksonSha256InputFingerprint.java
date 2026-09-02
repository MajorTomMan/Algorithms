package com.majortom.algorithms.visualization.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;

/** SHA-256 fingerprint over canonical input JSON, independent of an input object's toString implementation. */
public final class JacksonSha256InputFingerprint implements InputFingerprint {

    private static final int FINGERPRINT_SCHEMA_VERSION = 1;
    private final ObjectMapper mapper;

    public JacksonSha256InputFingerprint(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public String fingerprint(Object input) {
        Objects.requireNonNull(input, "input");
        try {
            ObjectNode envelope = mapper.createObjectNode();
            envelope.put("fingerprintSchemaVersion", FINGERPRINT_SCHEMA_VERSION);
            envelope.set("input", canonicalize(mapper.valueToTree(input)));
            byte[] canonicalBytes = mapper.writeValueAsBytes(canonicalize(envelope));
            return sha256Hex(canonicalBytes);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Unable to canonicalize algorithm input", exception);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to fingerprint algorithm input", exception);
        }
    }

    private JsonNode canonicalize(JsonNode node) {
        if (node == null || node.isValueNode() || node.isMissingNode()) {
            return node;
        }
        if (node.isArray()) {
            ArrayNode result = mapper.createArrayNode();
            for (JsonNode child : node) {
                result.add(canonicalize(child));
            }
            return result;
        }
        if (node.isObject()) {
            ObjectNode result = mapper.createObjectNode();
            ArrayList<String> names = new ArrayList<>();
            Iterator<String> fields = node.fieldNames();
            while (fields.hasNext()) {
                names.add(fields.next());
            }
            Collections.sort(names);
            for (String name : names) {
                result.set(name, canonicalize(node.get(name)));
            }
            return result;
        }
        return node;
    }

    private String sha256Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                result.append(String.format("%02x", value & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
