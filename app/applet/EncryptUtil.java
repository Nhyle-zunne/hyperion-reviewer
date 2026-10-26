import java.io.File;
import java.nio.file.Files;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptUtil {
    public static void main(String[] args) throws Exception {
        int[] a = {186, 37, 40, 56, 208, 70, 77, 77, 49, 140, 108, 238, 27, 237, 38, 131};
        int[] b = {54, 195, 118, 105, 37, 110, 229, 154, 71, 100, 190, 64, 176, 245, 192, 212};
        byte[] keyBytes = new byte[a.length + b.length];
        for (int i = 0; i < a.length; i++) {
            int m = ((i * 37 + 11) ^ (i << 3)) & 0xFF;
            keyBytes[i] = (byte) (a[i] ^ m ^ 0x5A);
        }
        for (int i = 0; i < b.length; i++) {
            int idx = a.length + i;
            int m = ((idx * 37 + 11) ^ (idx << 3)) & 0xFF;
            keyBytes[idx] = (byte) (b[i] ^ m ^ 0x5A);
        }

        File inputFile = new File("app/src/main/assets/questions/deck/oic_nw_c4.csv");
        byte[] plainBytes = Files.readAllBytes(inputFile.toPath());

        byte[] nonce = new byte[12];
        new SecureRandom().nextBytes(nonce);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(128, nonce));
        byte[] ciphertext = cipher.doFinal(plainBytes);

        byte[] output = new byte[nonce.length + ciphertext.length];
        System.arraycopy(nonce, 0, output, 0, nonce.length);
        System.arraycopy(ciphertext, 0, output, nonce.length, ciphertext.length);

        File outputFile = new File("app/src/main/assets/questions/deck/oic_nw_c4.csv.enc");
        Files.write(outputFile.toPath(), output);
        System.out.println("Successfully encrypted " + inputFile.length() + " bytes into " + outputFile.length() + " bytes.");

        Cipher decryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
        decryptCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(128, output, 0, 12));
        byte[] decrypted = decryptCipher.doFinal(output, 12, output.length - 12);

        if (java.util.Arrays.equals(plainBytes, decrypted)) {
            System.out.println("Verification SUCCESS: Decrypted bytes match original file exactly!");
        } else {
            System.err.println("Verification FAILED!");
        }
    }
}
