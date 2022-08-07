package net.modfest.platform.log;

import net.modfest.platform.ModFestPlatform;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;

public class LogFile {
    private final FileOutputStream fileOutputStream;

    public LogFile(String name) {
        File file = new File(ModFestPlatform.logDir, name + ".txt");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            this.fileOutputStream = new FileOutputStream(file, true);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void log(String message, Object... arguments) {
        try {
            fileOutputStream.write(String.format("[" + ModFestPlatform.FORMATTER.format(Instant.now()) + "] " + message + "\n", arguments)
                    .getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() throws IOException {
        fileOutputStream.close();
    }
}
