/**
 * JBackup is a software managing backups.
 *
 * Copyright (C) 2013-2014 Fabien DUMINY (fabien [dot] duminy [at] webmails [dot] com)
 *
 * JBackup is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * JBackup is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 */
package fr.duminy.jbackup.core;

import com.google.common.base.Supplier;
import fr.duminy.jbackup.core.util.FileDeleter;
import fr.duminy.jbackup.core.util.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.Mockito.mock;

public class TestUtils {
    public static Path createFile(Path file, long size) throws IOException {
        return createFile(file, StringUtils.repeat("A", (int) size));
    }

    public static Path createFile(Path file, String content) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }

        Files.write(file, content.getBytes());
        return file;
    }

    public static void setReadable(List<Path> protectedFiles, final boolean readable) {
        for (Path file : protectedFiles) {
            try {
                PathUtils.setReadable(file, readable);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static Path createSourceWithFiles(TemporaryFolder tempFolder, String sourceName) throws IOException {
        Path source = tempFolder.newFolder(sourceName).toPath();
        Files.createDirectories(source);
        Path file = source.resolve("file");
        createFile(file, 10);
        return source;
    }

    public static Supplier<FileDeleter> newMockSupplier() {
        return new Supplier<FileDeleter>() {
            @Override
            public FileDeleter get() {
                return mock(FileDeleter.class);
            }
        };
    }

    public static void sleep(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            // ignore
        }
    }
}
