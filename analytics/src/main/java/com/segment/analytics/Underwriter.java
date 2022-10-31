package com.segment.analytics;

import android.util.*;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Underwriter {
    private final Mac mac;

    public Underwriter(String key) throws NoSuchAlgorithmException, InvalidKeyException {
        if (key == null || key.isEmpty()) {
            throw new InvalidKeyException("Key to the Underwriter cannot be null or empty");
        }

        String type = "HmacSHA1";
        mac = Mac.getInstance(type);
        SecretKeySpec secret = new SecretKeySpec(key.getBytes(), type);
        mac.init(secret);
    }

    public void update(byte[] bytes) {
        mac.update(bytes);
    }

    public String sign() {
        byte[] digest = mac.doFinal();
        return Base64.encodeToString(digest, Base64.NO_WRAP);
    }

    public void reset() {
        mac.reset();
    }
}
