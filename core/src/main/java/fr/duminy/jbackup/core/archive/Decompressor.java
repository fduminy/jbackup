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
package fr.duminy.jbackup.core.archive;

import org.apache.commons.lang.mutable.MutableLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static fr.duminy.jbackup.core.archive.NotifyingInputStream.createCountingInputStream;

/**
 * A high level class that can decompress files in a format managed by the provided {@link fr.duminy.jbackup.core.archive.ArchiveFactory}.
 */
public class Decompressor {
    private static final Logger LOG = LoggerFactory.getLogger(Decompressor.class);

    private final ArchiveFactory factory;

    public Decompressor(ArchiveFactory factory) {
        this.factory = factory;
    }

    public void decompress(Path archive, Path targetDirectory, ProgressListener listener) throws ArchiveException {
        if (listener != null) {
            try {
                listener.totalSizeComputed(Files.size(archive));
            } catch (IOException ioe) {
                throw new ArchiveException(ioe);
            }
        }

        targetDirectory = (targetDirectory == null) ? Paths.get(".") : targetDirectory;
        if (!Files.exists(targetDirectory)) {
            throw new IllegalArgumentException(String.format("The target directory '%s' doesn't exist.", targetDirectory));
        }

        MutableLong processedSize = new MutableLong();

        try (InputStream archiveStream = Files.newInputStream(archive);
             ArchiveInputStream input = factory.create(archiveStream)) {
            ArchiveInputStream.Entry entry = input.getNextEntry();
            while (entry != null) {
                InputStream entryStream = createCountingInputStream(listener, processedSize, entry.getInput());
                try {
                    Path file = targetDirectory.resolve(entry.getName());
                    Files.createDirectories(file.getParent());
                    Files.copy(entryStream, file);
                } finally {
                    entry.close();
                }
                entry = input.getNextEntry();
            }
        } catch (IOException e) {
            throw new ArchiveException(e);
        } catch (Exception e) {
            throw new ArchiveException(e);
        }
    }
}
