/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.detailed.filters.csp;

import java.nio.charset.*;
import java.security.*;
import java.util.Base64;

// #csp-hash-generator
public class CSPHashGenerator {

    private final String digestAlgorithm;
    private final MessageDigest digestInstance;

    public CSPHashGenerator(String digestAlgorithm) throws NoSuchAlgorithmException {
        this.digestAlgorithm = digestAlgorithm;
        switch (digestAlgorithm) {
            case "sha256":
                this.digestInstance = MessageDigest.getInstance("SHA-256");
                break;
            case "sha384":
                this.digestInstance = MessageDigest.getInstance("SHA-384");
                break;
            case "sha512":
                this.digestInstance = MessageDigest.getInstance("SHA-512");
                break;
            default:
                throw new IllegalArgumentException("Unknown digest " + digestAlgorithm);
        }
    }

    public String generateUTF8(String str) {
        return generate(str, StandardCharsets.UTF_8);
    }

    public String generate(String str, Charset charset) {
        byte[] bytes = str.getBytes(charset);
        return encode(digestInstance.digest(bytes));
    }

    private String encode(byte[] digestBytes) {
        String rawHash = Base64.getMimeEncoder().encodeToString(digestBytes);
        return String.format("'%s-%s'", digestAlgorithm, rawHash);
    }
}
// #csp-hash-generator