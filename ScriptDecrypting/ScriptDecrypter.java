import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Base64;

public class ScriptDecrypter {
    static final String UID = "1";

    static int hex(char c) {
        if (c >= '0' && c <= '9') return c - 48;
        if (c >= 'A' && c <= 'F') return c - 55;
        if (c >= 'a' && c <= 'f') return c - 87;
        return 0;
    }
    public static void main(String[] a) throws Exception {
        String line = new String(Files.readAllBytes(Paths.get(a[0])), StandardCharsets.UTF_8).trim();
        String b64 = line.split("\"")[1];
        Cipher c = Cipher.getInstance("AES");
        c.init(2, new SecretKeySpec("K8ai@rUb^mj7X91P".getBytes(StandardCharsets.UTF_8), "AES"));
        String s = new String(c.doFinal(Base64.getDecoder().decode(b64)), StandardCharsets.UTF_8);
        String body = s.substring(2);
        int prev = (byte) (hex(s.charAt(0)) << 4 | hex(s.charAt(1)));
        byte[] b = new byte[body.length() / 2];
        for (int i = 0; i < b.length; i++) b[i] = (byte) (hex(body.charAt(2 * i)) << 4 | hex(body.charAt(2 * i + 1)));
        byte[] f = {-36,-73,44,-25,-37,-37,-28,35,-44,-11,-122,-9,-50,16,11,19};
        byte[] k = new byte[16];
        for (int n = 0; n < 16; n++) k[n] = (byte) ((f[n] & 0xFF) - 29 & 0xFF ^ 0x6D);
        int cc = ("fMv8h2138nby12hi1dwnjojnlqkewd" + "voiqvdwbu9v 84 v9u1 2v").length() + 21;
        byte[] out = new byte[b.length];
        char p = (char) prev;
        for (int n = 0; n < b.length; n++) {
            int v = (b[n] & 0xFF ^ k[n % 16] & 0xFF);
            v = v - cc & 0xFF;
            out[n] = (byte) ((v ^ p & 0xFF) & 0xFF);
            p = (char) b[n];
        }
        String decoded = new String(out, StandardCharsets.UTF_8)
                .replace("size * 0.05506", "size * 0.05" + UID);
        Files.write(Paths.get(a[1]), decoded.getBytes(StandardCharsets.UTF_8));
    }
}
