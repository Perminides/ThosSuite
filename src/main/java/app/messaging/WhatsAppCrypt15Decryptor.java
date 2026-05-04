package app.messaging;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.InflaterOutputStream;

/**
 * Entschlüsselt eine WhatsApp-crypt15-Backup-Datei in eine temporäre SQLite-Datei.
 *
 * <p>Einziger Einstiegspunkt: {@link #decrypt(String, Path, Path)}.
 * Der Hex-Schlüssel kommt aus der Config, die Pfade werden vom Aufrufer verwaltet.</p>
 *
 * <p><b>Algorithmus:</b>
 * <ol>
 *   <li>Hex-Schlüssel (64 Zeichen) → 32-Byte Root-Key</li>
 *   <li>AES-Schlüssel via zwei HMAC-SHA256-Runden ableiten ("backup encryption")</li>
 *   <li>16-Byte-IV aus dem Protobuf-Header der crypt15-Datei lesen (Fallback: fester Offset)</li>
 *   <li>AES-256-GCM entschlüsseln</li>
 *   <li>zlib-Dekomprimierung</li>
 *   <li>SQLite-Header validieren</li>
 * </ol>
 * </p>
 *
 * <p><b>Entwicklungsgeschichte – wie wir das geknackt haben:</b></p>
 *
 * <p><i>Ausgangslage:</i>
 * WhatsApp-Backups sind deutlich schwerer zugänglich als z.B. Signal.
 * Signal legt den Datenbankschlüssel brav im Benutzerverzeichnis ab.
 * WhatsApp hingegen verwendet plattformgebundene Schlüsselablage (DPAPI,
 * HSM-Server), sodass der Schlüssel ohne Root-Zugriff nicht extrahierbar ist.</p>
 *
 * <p><i>Problem 1 – Schlüsselzugang ohne Root:</i>
 * Das lokale Backup (crypt14) war zwar per USB vom Smartphone abrufbar,
 * der zugehörige Schlüssel lag jedoch im privaten App-Verzeichnis –
 * ohne Root nicht erreichbar. Android 12+ hat außerdem den früher
 * funktionierenden Legacy-APK-Trick endgültig blockiert.
 * Lösung: WhatsApp's Ende-zu-Ende-Backup aktivieren (Einstellungen →
 * Chats → Chat-Backup → E2E-Backup). Dabei wird einmalig ein
 * 64-stelliger Hex-Schlüssel angezeigt, den der Nutzer selbst verwahrt.
 * Ab diesem Zeitpunkt ist kein Root mehr nötig. Das Backup wechselt
 * von crypt14 auf crypt15. Wichtig: Passwort- und Passkey-Variante
 * sind für unsere Zwecke unbrauchbar, da der eigentliche AES-Schlüssel
 * dabei auf WhatsApp's HSM-Servern liegt und nur online abrufbar ist.
 * Nur der 64-Zeichen-Schlüssel ermöglicht lokale Entschlüsselung.</p>
 *
 * <p><i>Problem 2 – Wo liegt die Backup-Datei?</i>
 * Die eigentliche Nachrichten-Datenbank (msgstore.db.crypt15) liegt
 * nicht im erwarteten Backups-Ordner, sondern unter:
 * /sdcard/Android/media/com.whatsapp/WhatsApp/Databases/
 * Dieser Pfad ist ohne Root per USB-Kabel zugänglich.</p>
 *
 * <p><i>Problem 3 – Schlüsselableitung:</i>
 * Der 64-Zeichen-Schlüssel ist nicht direkt der AES-Schlüssel, sondern
 * ein Root-Key. Der eigentliche AES-Schlüssel wird über zwei HMAC-SHA256-
 * Runden abgeleitet (WhatsApp's "encryptionloop"):
 * privateKey = HMAC-SHA256(key=\x00*32, data=rootKey)
 * aesKey     = HMAC-SHA256(key=privateKey, data=b'backup encryption' + \x01)
 * Diese Logik wurde aus dem Open-Source-Projekt wa-crypt-tools
 * (github.com/ElDavoo/wa-crypt-tools) reverse-engineered.</p>
 *
 * <p><i>Problem 4 – Header-Parsing:</i>
 * Die crypt15-Datei beginnt mit einem Protobuf-Header, der u.a. den
 * 16-Byte-IV enthält. Statt einer vollständigen Protobuf-Library wird
 * der Header manuell geparst (minimaler Varint-Walker). Als Fallback
 * dienen die dokumentierten festen Offsets (IV bei Byte 8,
 * Datenbeginn bei Byte 122).</p>
 *
 * <p><i>Problem 5 – Auth-Tag-Position (der letzte Stolperstein):</i>
 * Java's AES/GCM erwartet den Authentication-Tag direkt am Ende des
 * Ciphertexts. In crypt15 ist das Layout jedoch:
 * [...Ciphertext...][16 Byte Auth-Tag][16 Byte MD5-Checksum]
 * Die letzten 32 Bytes müssen also abgetrennt werden, wobei nur die
 * ersten 16 davon (Auth-Tag) an den GCM-Cipher übergeben werden.
 * Die MD5-Checksum (die letzten 16 Bytes) wird für die Entschlüsselung
 * nicht benötigt und ignoriert. Dieser Fehler führte lange zu einer
 * AEADBadTagException und wurde durch Vergleich mit dem Python-Referenz-
 * code in wa-crypt-tools/lib/db/db15.py identifiziert.</p>
 *
 * <p><i>Keine externen Libraries:</i>
 * Die gesamte Implementierung verwendet ausschließlich Java-Bordmittel:
 * javax.crypto für AES-GCM, java.util.zip für zlib-Dekompression,
 * HmacSHA256 aus der Standard-JCE. Keine Protobuf-Library, kein
 * Bouncy Castle.</p>
 */
public class WhatsAppCrypt15Decryptor {

    private static final int    FALLBACK_IV_OFFSET   = 8;
    private static final int    FALLBACK_DATA_OFFSET = 122;
    private static final byte[] BACKUP_ENCRYPTION    = "backup encryption".getBytes();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Entschlüsselt {@code inputFile} mit dem angegebenen 64-stelligen Hex-Root-Key
     * und schreibt die resultierende SQLite-Datenbank nach {@code outputFile}.
     *
     * @param hexKey     64-stelliger Hex-Schlüssel aus der Config (E2E-Backup-Schlüssel)
     * @param inputFile  Pfad zur verschlüsselten .crypt15-Datei
     * @param outputFile Pfad zur Zieldatei (z.B. im System-Temp-Verzeichnis)
     * @throws IllegalArgumentException  wenn der Hex-Schlüssel nicht 64 Zeichen hat
     * @throws IllegalStateException     [FAILFAST] wenn der SQLite-Header der entschlüsselten Datei ungültig ist
     * @throws Exception                 bei kryptographischen oder I/O-Fehlern
     */
    public void decrypt(String hexKey, Path inputFile, Path outputFile) throws Exception {
        byte[] rootKey     = hexStringToBytes(hexKey);
        byte[] aesKey      = deriveAesKey(rootKey);
        byte[] fileBytes   = Files.readAllBytes(inputFile);

        HeaderInfo header        = parseHeader(fileBytes);
        byte[]     encrypted     = extractEncryptedData(fileBytes, header.dataOffset);
        byte[]     decryptedZlib = decryptAesGcm(aesKey, header.iv, encrypted);
        byte[]     sqliteBytes   = zlibDecompress(decryptedZlib);

        validateSqliteHeader(sqliteBytes);
        Files.write(outputFile, sqliteBytes);
    }

    // -------------------------------------------------------------------------
    // Schlüsselableitung  (mirrors wa-crypt-tools encryptionloop / Key15.get())
    // -------------------------------------------------------------------------

    /**
     * Leitet den 32-Byte AES-Schlüssel aus dem 32-Byte Root-Key ab.
     *
     * <pre>
     *   privateKey = HMAC-SHA256(key=\x00*32, data=rootKey)
     *   output     = HMAC-SHA256(key=privateKey, data="" + "backup encryption" + \x01)
     * </pre>
     */
    private byte[] deriveAesKey(byte[] rootKey) throws Exception {
        byte[] privateSeed = new byte[32];
        byte[] privateKey  = hmacSha256(privateSeed, rootKey);

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(privateKey, "HmacSHA256"));
        mac.update(new byte[0]);
        mac.update(BACKUP_ENCRYPTION);
        mac.update((byte) 1);
        return mac.doFinal();
    }

    private byte[] hmacSha256(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    // -------------------------------------------------------------------------
    // Header-Parsing
    // -------------------------------------------------------------------------

    private static final class HeaderInfo {
        byte[] iv;
        int    dataOffset;
    }

    /**
     * Extrahiert IV und Daten-Offset aus dem crypt15-Header.
     * Versucht zuerst einen minimalen manuellen Protobuf-Parse,
     * fällt bei Fehler auf feste Offsets zurück.
     */
    private HeaderInfo parseHeader(byte[] file) {
        HeaderInfo info = new HeaderInfo();
        try {
            int pos          = 0;
            int protobufSize = file[pos++] & 0xFF;
            if (file[pos] == 0x01) pos++;

            byte[] protobuf    = new byte[protobufSize];
            System.arraycopy(file, pos, protobuf, 0, protobufSize);
            int dataOffset = pos + protobufSize;

            byte[] iv = extractIvFromProtobuf(protobuf);
            if (iv != null && iv.length == 16) {
                info.iv         = iv;
                info.dataOffset = dataOffset;
                return info;
            }
        } catch (Exception e) {
            // Fallback auf feste Offsets
        }

        info.iv = new byte[16];
        System.arraycopy(file, FALLBACK_IV_OFFSET, info.iv, 0, 16);
        info.dataOffset = FALLBACK_DATA_OFFSET;
        return info;
    }

    /**
     * Minimaler manueller Protobuf-Parser: sucht im BackupPrefix-Message
     * nach einem bytes-Feld mit genau 16 Bytes (der IV).
     * Protobuf Wire-Format: tag (varint) = fieldNumber << 3 | wireType,
     * WireType 2 = length-delimited.
     */
    private byte[] extractIvFromProtobuf(byte[] data) {
        int pos = 0;
        while (pos < data.length) {
            int[] tagResult = readVarint(data, pos);
            if (tagResult == null) break;
            int tag      = tagResult[0];
            pos          = tagResult[1];
            int wireType = tag & 0x07;

            if (wireType == 2) {
                int[] lenResult = readVarint(data, pos);
                if (lenResult == null) break;
                int fieldLen = lenResult[0];
                pos          = lenResult[1];
                if (pos + fieldLen > data.length) break;

                byte[] fieldData = new byte[fieldLen];
                System.arraycopy(data, pos, fieldData, 0, fieldLen);
                pos += fieldLen;

                if (fieldLen == 16) return fieldData;

                byte[] nested = extractIvFromProtobuf(fieldData);
                if (nested != null) return nested;

            } else if (wireType == 0) {
                int[] skip = readVarint(data, pos);
                if (skip == null) break;
                pos = skip[1];
            } else if (wireType == 1) {
                pos += 8;
            } else if (wireType == 5) {
                pos += 4;
            } else {
                break;
            }
        }
        return null;
    }

    /** Liest einen Varint aus {@code data} ab {@code offset}. Gibt null bei Fehler zurück. */
    private int[] readVarint(byte[] data, int offset) {
        int result = 0;
        int shift  = 0;
        int pos    = offset;
        while (pos < data.length) {
            int b = data[pos++] & 0xFF;
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return new int[]{result, pos};
            shift += 7;
            if (shift >= 35) return null;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Entschlüsselung
    // -------------------------------------------------------------------------

    /**
     * Entschlüsselt mit AES-256-GCM.
     * Layout der verschlüsselten Daten (aus db15.py):
     * [...Ciphertext...][16 Byte Auth-Tag][16 Byte MD5-Checksum]
     * Die MD5-Checksum wird ignoriert.
     */
    private byte[] decryptAesGcm(byte[] aesKey, byte[] iv, byte[] encrypted) throws Exception {
        int    ciphertextLen     = encrypted.length - 32;
        byte[] ciphertext        = new byte[ciphertextLen];
        byte[] authTag           = new byte[16];
        System.arraycopy(encrypted, 0,             ciphertext, 0, ciphertextLen);
        System.arraycopy(encrypted, ciphertextLen, authTag,    0, 16);

        byte[] ciphertextWithTag = new byte[ciphertextLen + 16];
        System.arraycopy(ciphertext, 0, ciphertextWithTag, 0,             ciphertextLen);
        System.arraycopy(authTag,    0, ciphertextWithTag, ciphertextLen, 16);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
            Cipher.DECRYPT_MODE,
            new SecretKeySpec(aesKey, "AES"),
            new GCMParameterSpec(128, iv)
        );
        return cipher.doFinal(ciphertextWithTag);
    }

    // -------------------------------------------------------------------------
    // Hilfsmethoden
    // -------------------------------------------------------------------------

    private byte[] extractEncryptedData(byte[] file, int dataOffset) {
        int    len  = file.length - dataOffset;
        byte[] data = new byte[len];
        System.arraycopy(file, dataOffset, data, 0, len);
        return data;
    }

    private byte[] zlibDecompress(byte[] compressed) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InflaterOutputStream inflater = new InflaterOutputStream(out)) {
            inflater.write(compressed);
        }
        return out.toByteArray();
    }

    private byte[] hexStringToBytes(String hex) {
        if (hex.length() != 64)
            throw new IllegalArgumentException(
                "[FAILFAST] Hex-Schlüssel muss 64 Zeichen haben, hat aber: " + hex.length());
        byte[] result = new byte[32];
        for (int i = 0; i < 32; i++)
            result[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        return result;
    }

    private void validateSqliteHeader(byte[] data) {
        String magic = "SQLite format 3";
        for (int i = 0; i < magic.length(); i++) {
            if (data[i] != (byte) magic.charAt(i))
                throw new IllegalStateException(
                    "[FAILFAST] Entschlüsselte Datei beginnt nicht mit SQLite-Header — " +
                    "Schlüssel oder IV vermutlich falsch.");
        }
    }
}