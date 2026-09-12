package com.majortom.algorithms.practice.runtime.worker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

final class WorkerCodec {
    private WorkerCodec() {}

    static String encode(Object value) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
                output.writeObject(value);
            }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Practice worker arguments/results must be serializable", exception);
        }
    }

    static Object decode(String encoded) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(encoded);
            try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                return input.readObject();
            }
        } catch (IOException | ClassNotFoundException exception) {
            throw new IllegalArgumentException("Unable to decode Practice worker payload", exception);
        }
    }
}
