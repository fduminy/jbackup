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

import fr.duminy.jbackup.core.Cancellable;
import fr.duminy.jbackup.core.task.TaskListener;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;

import static fr.duminy.jbackup.core.archive.NotifyingInputStream.createCountingInputStream;

/**
 * A high level class that can compress files in a format managed by the provided {@link fr.duminy.jbackup.core.archive.ArchiveFactory}.
 */
public class Compressor {
    private static final Logger LOG = LoggerFactory.getLogger(Compressor.class);

    private final ArchiveFactory factory;

    public Compressor(ArchiveFactory factory) {
        this.factory = factory;
    }

    public void compress(ArchiveParameters archiveParameters, List<SourceWithPath> files, final TaskListener listener, Cancellable cancellable) throws ArchiveException {
        final String name = archiveParameters.getArchive().toString();
        final MutableLong processedSize = new MutableLong();

        try (OutputStream fos = Files.newOutputStream(archiveParameters.getArchive());
             ArchiveOutputStream output = factory.create(fos)) {
            LOG.info("Backup '{}': creating archive {}", name, archiveParameters.getArchive());
            for (final SourceWithPath file : files) {
                if ((cancellable != null) && cancellable.isCancelled()) {
                    break;
                }

                LOG.info("Backup '{}': compressing file {}", name, file.getPath().toAbsolutePath());
                try (InputStream input = createCountingInputStream(listener, processedSize, Files.newInputStream(file.getPath()))) {
                    final String path = archiveParameters.isRelativeEntries() ? file.getRelativePath() : file.getAbsolutePath();
                    LOG.info("Backup '{}': adding entry {}", new Object[]{name, path});
                    output.addEntry(path, input);
                }
            }
            LOG.info("Backup '{}': archive {} created ({})", new Object[]{name, archiveParameters.getArchive(), FileUtils.byteCountToDisplaySize(Files.size(archiveParameters.getArchive()))});
        } catch (Exception e) {
            throw new ArchiveException(e);
        }
    }
}
