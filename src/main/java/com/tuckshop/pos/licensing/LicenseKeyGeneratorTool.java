package com.tuckshop.pos.licensing;

import java.time.LocalDate;

/**
 * Run this yourself, on your own dev machine, to issue a renewal key - it is not wired
 * into any controller or UI, so a shop's cashier or owner has no way to reach it.
 *
 * The shop's packaged jar/exe is a Spring Boot repackaged jar (nested BOOT-INF layout),
 * which a plain "java -cp" can't see into - run this from the project source instead:
 *
 *   mvn -q -o compile
 *   java -cp target/classes com.tuckshop.pos.licensing.LicenseKeyGeneratorTool <deviceId> <yyyy-MM-dd> <secret>
 *
 * <deviceId> is shown on the shop's License page (owner-only).
 * <secret> must match license.secret in that install's application.properties.
 */
public final class LicenseKeyGeneratorTool {

    private LicenseKeyGeneratorTool() {
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java -cp target/classes com.tuckshop.pos.licensing.LicenseKeyGeneratorTool <deviceId> <yyyy-MM-dd> <secret>");
            return;
        }
        String deviceId = args[0];
        LocalDate expiry = LocalDate.parse(args[1]);
        String secret = args[2];
        System.out.println("License key (valid until " + expiry + "): " + LicenseUtil.generateKey(deviceId, expiry, secret));
    }
}


// package com.tuckshop.pos.licensing;

// import java.time.LocalDateTime;

// /**
//  * Run this yourself, on your own dev machine, to issue a renewal key.
//  *
//  * This is not wired into any controller or UI, so a shop's cashier or owner
//  * has no way to reach it.
//  *
//  * Usage:
//  *
//  *   mvn -q -o compile
//  *
//  *   java -cp target/classes \
//  *   com.tuckshop.pos.licensing.LicenseKeyGeneratorTool \
//  *   <deviceId> <yyyy-MM-ddTHH:mm> <secret>
//  *
//  * Example:
//  *
//  *   java -cp target/classes \
//  *   com.tuckshop.pos.licensing.LicenseKeyGeneratorTool \
//  *   A1B2C3D4 2026-08-06T01:15 my-secret
//  *
//  * <deviceId> is shown on the shop's License page (owner-only).
//  *
//  * <secret> must match license.secret in that install's application.properties.
//  */
// public final class LicenseKeyGeneratorTool {

//     private LicenseKeyGeneratorTool() {
//     }

//     public static void main(String[] args) {

//         if (args.length != 3) {
//             System.out.println(
//                     "Usage: java -cp target/classes " +
//                     "com.tuckshop.pos.licensing.LicenseKeyGeneratorTool " +
//                     "<deviceId> <yyyy-MM-ddTHH:mm> <secret>"
//             );
//             return;
//         }

//         String deviceId = args[0];

//         LocalDateTime expiry;

//         try {
//             expiry = LocalDateTime.parse(args[1]);
//         } catch (Exception e) {
//             System.out.println(
//                     "Invalid expiry format. Use: yyyy-MM-ddTHH:mm"
//             );
//             return;
//         }

//         String secret = args[2];

//         String key = LicenseUtil.generateKey(
//                 deviceId,
//                 expiry,
//                 secret
//         );

//         System.out.println(
//                 "License key (valid until " +
//                 expiry +
//                 "): " +
//                 key
//         );
//     }
// }