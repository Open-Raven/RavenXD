package xyz.daikirai.script;

import xyz.daikirai.client.Account;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class ScriptCrypto {
    private static final String KEY_HEAD = "K8ai@rUb";
    private static final String LOG_KEY_HEAD = "I^C@pIZ";
    private static final byte[] HEX_KEY = new byte[]{-36, -73, 44, -25, -37, -37, -28, 35, -44, -11, -122, -9, -50, 16, 11, 19};

    private ScriptCrypto() {
    }

    public static String md5(String string) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(string.getBytes(StandardCharsets.UTF_8));
            return String.format("%032x", new BigInteger(1, messageDigest.digest()));
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            return "";
        }
    }

    public static String logKey() {
        return LOG_KEY_HEAD + "!k4N#lO8}";
    }

    private static String shiftString() {
        return "fMv8h2138nby12hi1dwnjojnlqkewd" + "voiqvdwbu9v 84 v9u1 2v";
    }

    public static String decryptLine(String string) {
        String string2 = string.trim();
        String string3 = "^mj7X";
        if (string2.startsWith("decrypt - \"") && string2.endsWith("\"") && string2.length() > "decrypt - \"\"".length()) {
            String string4;
            string3 = string3 + "91";
            String string5 = string2.split("\"")[1];
            try {
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(2, new SecretKeySpec((KEY_HEAD + string3 + "P").getBytes(StandardCharsets.UTF_8), "AES"));
                byte[] byArray = cipher.doFinal(Base64.getDecoder().decode(string5));
                string4 = new String(byArray, StandardCharsets.UTF_8);
            } catch (Exception exception) {
                return null;
            }
            string4 = decryptHex(string4);
            if (!string4.isEmpty()) {
                return string4;
            }
        }
        return null;
    }

    public static String trailer(String string) {
        if (string.length() <= 32) {
            return null;
        }
        return string.substring(string.length() - 32);
    }

    public static String source(String string) {
        if (string.length() <= 32) {
            return null;
        }
        return string.substring(0, string.length() - 32);
    }

    private static int hexDigit(char c) {
        if (c >= '0' && c <= '9') {
            return c - 48;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 65 + 10;
        }
        if (c >= 'a' && c <= 'f') {
            return c - 97 + 10;
        }
        return 0;
    }

    public static String decryptHex(String string) {
        int n;
        char c;
        char c2;
        if (string == null || string.length() < 2) {
            return "";
        }
        String string2 = string.substring(2);
        if (string2.length() % 2 != 0) {
            return "";
        }
        String string3 = string.substring(0, 2);
        int n2 = hexDigit(string3.charAt(0));
        int n3 = hexDigit(string3.charAt(1));
        byte by = (byte) (n2 << 4 | n3);
        int n4 = string2.length() / 2;
        byte[] byArray = new byte[n4];
        for (int i = 0; i < n4; ++i) {
            c2 = string2.charAt(i * 2);
            c = string2.charAt(i * 2 + 1);
            int n5 = hexDigit(c2);
            n = hexDigit(c);
            byArray[i] = (byte) (n5 << 4 | n);
        }
        byte[] byArray2 = new byte[byArray.length];
        c2 = (char) by;
        c = (char) (shiftString().length() + 21);
        byte[] byArray3 = new byte[HEX_KEY.length];
        for (n = 0; n < HEX_KEY.length; ++n) {
            byArray3[n] = (byte) ((HEX_KEY[n] & 0xFF) - 29 & 0xFF ^ 0x6D);
        }
        for (n = 0; n < byArray.length; ++n) {
            int n6 = byArray[n] & 0xFF ^ byArray3[n % byArray3.length] & 0xFF;
            n6 = n6 - c & 0xFF;
            int n7 = n6 ^ c2 & 0xFF;
            byArray2[n] = (byte) (n7 & 0xFF);
            c2 = (char) byArray[n];
        }
        return new String(byArray2, StandardCharsets.UTF_8).replace("size * 0.05506", "size * 0.05" + Account.UID);
    }
}
