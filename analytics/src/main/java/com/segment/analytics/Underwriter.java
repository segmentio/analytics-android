package com.segment.analytics;

import android.util.*;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Underwriter {
    private Mac mac;

    public Underwriter(String key) {
        if (key == null) {
            key = "segment";
        }

        try {
            String type = "HmacSHA1";
            mac = Mac.getInstance(type);
            SecretKeySpec secret = new SecretKeySpec(key.getBytes(), type);
            mac.init(secret);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
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
