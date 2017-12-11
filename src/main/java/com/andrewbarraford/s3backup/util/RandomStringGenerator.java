package com.andrewbarraford.s3backup.util;

import java.security.SecureRandom;

public final class RandomStringGenerator {

    private static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom rnd = new SecureRandom();

    /**
     * Create a random unique string of length n.
     *
     * @param length an integer specifying the length the string should be.
     * @return a String.
     */
    private static String randomString(final int length) {
        final StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        }
        return sb.toString();
    }

    /**
     * S3 naming conventions require all lowercase names. This conforms to that requirement.
     *
     * @param length the length of the unique string to generate.
     * @return a unique lowercase String
     */
    public static String randomStringForUseInBucketNames(final int length) {
        return randomString(length).toLowerCase();
    }


}
