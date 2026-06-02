package app.shared;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;

public class SingleInstanceGuard {

    private static FileLock lock;
    private static FileChannel channel;

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
            return false;
        }
    }
}