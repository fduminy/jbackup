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

import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.lang.mutable.MutableLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * A high level class that can (de)compress files in a format managed by the provided {@link ArchiveFactory}.
 * The {@link #main(String[])} method provides a very basic command line tool for the zip format {(thanks to {@link ZipArchiveFactory}.
 */
public class Archiver {
    private static final Logger LOG = LoggerFactory.getLogger(Archiver.class);

    private final ArchiveFactory factory;

    public static void main(String[] args) throws IOException, ArchiverException {
        final String operation = args[0];
        final Path archive = Paths.get(args[1]);
        final ZipArchiveFactory factory = ZipArchiveFactory.INSTANCE;

        switch (operation) {
            case "-c":
                final ArchiveParameters archiveParameters = new ArchiveParameters(archive, true);
                for (int i = 2; i < args.length; i++) {
                    archiveParameters.addSource(Paths.get(args[i]));
                }
                new Archiver(factory).compress(archiveParameters);
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

    public static final class ArchiverException extends Exception {
        public ArchiverException(Throwable cause) {
            super(cause);
        }
    }

    public Archiver(ArchiveFactory factory) {
        this.factory = factory;
    }

    public final void compress(ArchiveParameters archiveParameters) throws ArchiverException {
        compress(archiveParameters, null);
    }

    public final void compress(ArchiveParameters archiveParameters, final ProgressListener listener) throws ArchiverException {
        MutableLong totalSize = new MutableLong();
        final Map<Path, List<Path>> filesBySource;
        try {
            filesBySource = collectFiles(archiveParameters.getSources(), totalSize);
        } catch (IOException ioe) {
            throw new ArchiverException(ioe);
        }
        if (listener != null) {
            listener.totalSizeComputed(totalSize.longValue());
        }

        compress(archiveParameters, listener, filesBySource);
    }

    protected void compress(ArchiveParameters archiveParameters, final ProgressListener listener, Map<Path, List<Path>> filesBySource) throws ArchiverException {
        final String name = archiveParameters.getArchive().toString();
        final MutableLong processedSize = new MutableLong();

        try (OutputStream fos = Files.newOutputStream(archiveParameters.getArchive());
             ArchiveOutputStream output = factory.create(fos);) {
            LOG.info("Backup '{}': creating archive {}", name, archiveParameters.getArchive());
            for (Map.Entry<Path, List<Path>> sourceEntry : filesBySource.entrySet()) {
                for (final Path file : sourceEntry.getValue()) {
                    LOG.info("Backup '{}': compressing file {}", name, file.toAbsolutePath());
                    try (InputStream input = createCountingInputStream(listener, processedSize, Files.newInputStream(file));) {
                        final String path;
                        if (archiveParameters.isRelativeEntries()) {
                            Path source = sourceEntry.getKey();
                            if (Files.isDirectory(source)) {
                                if (source.getParent() == null) {
                                    path = source.relativize(file).toString();
                                } else {
                                    path = source.getParent().relativize(file).toString();
                                }
                            } else {
                                path = file.getFileName().toString();
                            }
                        } else {
                            path = file.toString();
                        }
                        LOG.info("Backup '{}': adding entry {}", new Object[]{name, path});
                        output.addEntry(path, input);
                    }
                }
            }
            LOG.info("Backup '{}': archive {} created ({})", new Object[]{name, archiveParameters.getArchive(), FileUtils.byteCountToDisplaySize(Files.size(archiveParameters.getArchive()))});
        } catch (IOException e) {
            throw new ArchiverException(e);
        } catch (Exception e) {
            throw new ArchiverException(e);
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

    public void decompress(Path archive, Path targetDirectory) throws ArchiverException {
        decompress(archive, targetDirectory, null);
    }

    public void decompress(Path archive, Path targetDirectory, ProgressListener listener) throws ArchiverException {
        if (listener != null) {
            try {
                listener.totalSizeComputed(Files.size(archive));
            } catch (IOException ioe) {
                throw new ArchiverException(ioe);
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
            throw new ArchiverException(e);
        } catch (Exception e) {
            throw new ArchiverException(e);
        }
    }

    private Map<Path, List<Path>> collectFiles(Collection<ArchiveParameters.Source> sources, MutableLong totalSize) throws IOException {
        totalSize.setValue(0L);

        Map<Path, List<Path>> onlyFiles = new HashMap<>();
        FileCollector collector = new FileCollector();
        for (ArchiveParameters.Source source : sources) {
            Path sourcePath = source.getSource();
            if (!sourcePath.isAbsolute()) {
                throw new IllegalArgumentException(String.format("The file '%s' is relative.", sourcePath));
            }

            long size;

            if (Files.isDirectory(sourcePath)) {
                List<Path> collectedFiles = new ArrayList<>();
                size = collector.collect(collectedFiles, sourcePath, source.getDirFilter(), source.getFileFilter(), null);
                onlyFiles.put(sourcePath, collectedFiles);
            } else {
                onlyFiles.put(sourcePath, Collections.singletonList(sourcePath));
                size = Files.size(sourcePath);
            }

            totalSize.add(size);
        }
        return onlyFiles;
    }
}
