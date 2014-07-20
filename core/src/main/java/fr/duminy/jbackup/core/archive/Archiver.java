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

import fr.duminy.components.swing.form.StringPathTypeMapper;
import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.lang.mutable.MutableLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * A high level class that can (de)compress files in a format managed by the provided {@link ArchiveFactory}.
 * The {@link #main(String[])} method provides a very basic command line tool for the zip format {(thanks to {@link ZipArchiveFactory}.
 */
public class Archiver {
    private static final Logger LOG = LoggerFactory.getLogger(Archiver.class);

    private final ArchiveFactory factory;

    public static void main(String[] args) throws IOException {
        final String operation = args[0];
        final Path archive = Paths.get(args[1]);
        final ZipArchiveFactory factory = new ZipArchiveFactory();

        switch (operation) {
            case "-c":
                new Archiver(factory).compress(toFiles(args, 2), archive);
                break;
            case "-d":
                Path directory = null;

                if (args.length > 2) {
                    directory = Paths.get(args[2]);
                }

                new Archiver(factory).decompress(archive, directory);
                break;
            default:
                throw new IOException("unsupported operation: " + operation);
        }
    }

    private static Path[] toFiles(String[] files, int fromIndex) {
        Path[] result = new Path[files.length - (fromIndex + 1)];
        for (int i = fromIndex; i < files.length; i++) {
            result[i - fromIndex] = Paths.get(files[i]);
        }
        return result;
    }

    public Archiver(ArchiveFactory factory) {
        this.factory = factory;
    }

    public void compress(Path[] files, Path archive) throws IOException {
        compress(files, archive, null);

    }

    public void compress(Path[] files, final Path archive, final ProgressListener listener) throws IOException {
        MutableLong totalSize = new MutableLong();
        files = filterFiles(files, totalSize);
        if (listener != null) {
            listener.totalSizeComputed(totalSize.longValue());
        }

        final String name = archive.toString();
        OutputStream fos = Files.newOutputStream(archive);
        final ArchiveOutputStream output = factory.create(fos);
        final MutableLong processedSize = new MutableLong();
        try {
            LOG.info("Backup '{}': creating archive {}", name, archive);
            for (final Path file : files) {
                if (!Files.isDirectory(file)) {
                    LOG.info("Backup '{}': compressing file {}", name, file.toAbsolutePath());
                    final InputStream input = createCountingInputStream(listener, processedSize, Files.newInputStream(file));
                    try {
                        String path = StringPathTypeMapper.toString(file);
                        LOG.info("Backup '{}': adding entry {}", new Object[]{name, path});
                        output.addEntry(path, input);
                    } finally {
                        IOUtils.closeQuietly(input);
                    }
                }
            }
            LOG.info("Backup '{}': archive {} created ({})", new Object[]{name, archive, FileUtils.byteCountToDisplaySize(Files.size(archive))});
        } finally {
            IOUtils.closeQuietly(output);
            IOUtils.closeQuietly(fos);
        }
    }

    private InputStream createCountingInputStream(final ProgressListener listener, final MutableLong processedSize, final InputStream input) {
        InputStream result = input;

        if (listener != null) {
            result = new NotifyingInputStream(listener, processedSize, input);
        }

        return result;
    }

    private static class NotifyingInputStream extends CountingInputStream {
        private final ProgressListener listener;
        private final MutableLong processedSize;

        /**
         * Constructs a new CountingInputStream.
         *
         * @param input the InputStream to delegate to
         */
        public NotifyingInputStream(final ProgressListener listener, final MutableLong processedSize, final InputStream input) {
            super(input);
            this.listener = listener;
            this.processedSize = processedSize;
        }

        @Override
        protected synchronized void afterRead(int n) {
            super.afterRead(n);

            if (n > 0) {
                processedSize.add(n);
                listener.progress(processedSize.longValue());
            }
        }
    }

    public void decompress(Path archive, Path directory) throws IOException {
        decompress(archive, directory, null);
    }

    public void decompress(Path archive, Path directory, ProgressListener listener) throws IOException {
        if (listener != null) {
            listener.totalSizeComputed(Files.size(archive));
        }

        directory = (directory == null) ? Paths.get(".") : directory;

        MutableLong processedSize = new MutableLong();

        try (InputStream archiveStream = Files.newInputStream(archive);
             ArchiveInputStream input = factory.create(archiveStream)) {
            ArchiveInputStream.Entry entry = input.getNextEntry();
            while (entry != null) {
                InputStream entryStream = createCountingInputStream(listener, processedSize, entry.getInput());
                try {
                    Files.copy(entryStream, directory.resolve(entry.getName()));
                } finally {
                    entry.close();
                }
                entry = input.getNextEntry();
            }
        }
    }

    private Path[] filterFiles(Path[] files, MutableLong totalSize) throws IOException {
        totalSize.setValue(0L);

        List<Path> onlyFiles = new ArrayList<>();
        FileCollector collector = new FileCollector(); //TODO replace by jdk Files.walkFileTree(.....)
        for (Path file : files) {
            long size;

            if (Files.isDirectory(file)) {
                List<File> collectedFiles = new ArrayList<>();
                size = collector.collect(collectedFiles, file);
                for (File collectedFile : collectedFiles) {
                    onlyFiles.add(collectedFile.toPath());
                }
            } else {
                onlyFiles.add(file);
                size = Files.size(file);
            }

            totalSize.add(size);
        }
        return onlyFiles.toArray(new Path[onlyFiles.size()]);
    }
}
