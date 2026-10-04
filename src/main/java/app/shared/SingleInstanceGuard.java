package app.shared;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;

public class SingleInstanceGuard {

    private static FileLock lock;
    private static FileChannel channel;

    /**
     * Sperrt die Datei für diesen Prozess. {@code false} heißt: eine andere Instanz läuft schon.
     *
     * <p>{@code @SuppressWarnings("resource")}, weil Channel und Lock bewusst offen bleiben — die
     * Sperre gilt, solange die Suite läuft, und wird erst im Shutdown-Hook gelöst. Ein
     * try-with-resources würde sie am Ende dieser Methode freigeben und damit ihren Zweck
     * aufheben.</p>
     */
    @SuppressWarnings("resource")
    public static boolean lockInstance(Path lockFile) {
        try {
            channel = new RandomAccessFile(lockFile.toFile(), "rw").getChannel();
            lock = channel.tryLock();

            if (lock == null) {
                return false;
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    lock.release();
                    channel.close();
                } catch (Exception ignored) {}
            }));

            return true;

        } catch (Exception e) {
            throw new RuntimeException("Oops. Probleme beim Versuch sicherzustellen, dass die App nicht bereits läuft.", e);
        }
    }
}