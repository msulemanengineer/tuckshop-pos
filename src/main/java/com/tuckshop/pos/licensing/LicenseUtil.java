package com.tuckshop.pos.licensing;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HexFormat;

/**
 * Offline license key scheme: a key is HMAC-SHA256(deviceId + expiryEpochDay, secret),
 * with the expiry date encoded directly in the key so activation only needs one code
 * pasted in - no server round-trip, matches this app's fully-offline design.
 *
 * Only whoever holds `secret` can produce a key that verifyAndGetExpiry() accepts for a
 * given device ID - that's what makes it a real gate rather than decoration. Anyone who
 * decompiles this jar can find the secret, same as any other client-side license check;
 * it's meant to enforce an agreed business term, not withstand a determined attacker.
 */
public final class LicenseUtil {

    private LicenseUtil() {
    }

    public static String generateKey(String deviceId, LocalDate expiry, String secret) {
        long epochDay = expiry.toEpochDay();
        String signature = hmac(deviceId + "|" + epochDay, secret).substring(0, 12).toUpperCase();
        return Long.toString(epochDay, 36).toUpperCase() + "-" + signature;
    }

    /** Returns the expiry date the key grants, or null if the key is malformed or doesn't match this device. */
    public static LocalDate verifyAndGetExpiry(String deviceId, String key, String secret) {
        if (key == null || deviceId == null) {
            return null;
        }
        try {
            String[] parts = key.trim().toUpperCase().split("-");
            if (parts.length != 2) {
                return null;
            }
            long epochDay = Long.parseLong(parts[0], 36);
            String expectedSignature = hmac(deviceId + "|" + epochDay, secret).substring(0, 12).toUpperCase();
            if (!expectedSignature.equals(parts[1])) {
                return null;
            }
            return LocalDate.ofEpochDay(epochDay);
        } catch (Exception e) {
            return null;
        }
    }

    private static String hmac(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}


// package com.tuckshop.pos.licensing;

// import javax.crypto.Mac;
// import javax.crypto.spec.SecretKeySpec;
// import java.nio.charset.StandardCharsets;
// import java.time.Instant;
// import java.time.LocalDateTime;
// import java.time.ZoneOffset;
// import java.util.HexFormat;

// /**
//  * Offline license key scheme:
//  *
//  * HMAC-SHA256(deviceId + expiryEpochMinute, secret)
//  *
//  * The expiry timestamp is encoded directly into the key, so activation
//  * requires only one code pasted into the application.
//  */
// public final class LicenseUtil {

//     private LicenseUtil() {
//     }

//     /**
//      * Generates a license key that expires at the given LocalDateTime.
//      */
//     public static String generateKey(
//             String deviceId,
//             LocalDateTime expiry,
//             String secret
//     ) {
//         long epochMinute = expiry
//                 .toInstant(ZoneOffset.UTC)
//                 .getEpochSecond() / 60;

//         String signature = hmac(
//                 deviceId + "|" + epochMinute,
//                 secret
//         ).substring(0, 12).toUpperCase();

//         return Long.toString(epochMinute, 36).toUpperCase()
//                 + "-"
//                 + signature;
//     }

//     /**
//      * Verifies the license key and returns the expiry time.
//      *
//      * Returns null if:
//      * - key is malformed
//      * - signature doesn't match
//      * - device ID doesn't match
//      */
//     public static LocalDateTime verifyAndGetExpiry(
//             String deviceId,
//             String key,
//             String secret
//     ) {
//         if (key == null || deviceId == null) {
//             return null;
//         }

//         try {
//             String[] parts = key
//                     .trim()
//                     .toUpperCase()
//                     .split("-");

//             if (parts.length != 2) {
//                 return null;
//             }

//             long epochMinute = Long.parseLong(parts[0], 36);

//             String expectedSignature = hmac(
//                     deviceId + "|" + epochMinute,
//                     secret
//             ).substring(0, 12).toUpperCase();

//             if (!expectedSignature.equals(parts[1])) {
//                 return null;
//             }

//             long epochSecond = epochMinute * 60;

//             return LocalDateTime.ofInstant(
//                     Instant.ofEpochSecond(epochSecond),
//                     ZoneOffset.UTC
//             );

//         } catch (Exception e) {
//             return null;
//         }
//     }

//     private static String hmac(String data, String secret) {
//         try {
//             Mac mac = Mac.getInstance("HmacSHA256");

//             mac.init(
//                     new SecretKeySpec(
//                             secret.getBytes(StandardCharsets.UTF_8),
//                             "HmacSHA256"
//                     )
//             );

//             byte[] out = mac.doFinal(
//                     data.getBytes(StandardCharsets.UTF_8)
//             );

//             return HexFormat.of().formatHex(out);

//         } catch (Exception e) {
//             throw new RuntimeException(e);
//         }
//     }
// }