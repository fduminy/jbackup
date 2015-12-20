/**
 * JBackup is a software managing backups.
 *
 * Copyright (C) 2013-2015 Fabien DUMINY (fabien [dot] duminy [at] webmails [dot] com)
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
package fr.duminy.jbackup.core.task;

import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.Cancellable;
import fr.duminy.jbackup.core.archive.*;
import fr.duminy.jbackup.core.util.FileDeleter;
import fr.duminy.jbackup.core.util.InputStreamComparator;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class BackupTask extends FileCreatorTask {
    private static final Logger LOG = LoggerFactory.getLogger(BackupTask.class);

    public static class VerificationFailedException extends Exception {
        public VerificationFailedException(String message) {
            super(message);
        }
    }

    public BackupTask(BackupConfiguration config, Supplier<FileDeleter> deleterSupplier,
                      TaskListener listener, Cancellable cancellable) {
        super(config, deleterSupplier, listener, cancellable);
    }

    @Override
    protected void executeTask(FileDeleter deleter) throws Exception {
        ArchiveFactory factory = config.getArchiveFactory();

        Path target = Paths.get(config.getTargetDirectory());
        Files.createDirectories(target);

        String archiveName = generateName(config.getName(), config.getArchiveFactory());

        Path archive = target.resolve(archiveName);

        final ArchiveParameters archiveParameters = new ArchiveParameters(archive, config.isRelativeEntries());
        for (BackupConfiguration.Source filter : config.getSources()) {
            IOFileFilter dirFilter = config.createIOFileFilter("_dir", filter.getDirFilter());
            IOFileFilter fileFilter = config.createIOFileFilter("_file", filter.getFileFilter());
            Path source = Paths.get(filter.getPath());
            archiveParameters.addSource(source, dirFilter, fileFilter);
        }

        deleter.registerFile(archiveParameters.getArchive());

        List<SourceWithPath> collectedFiles = new ArrayList<>();
        createFileCollector().collectFiles(collectedFiles, archiveParameters, listener, cancellable);
        compress(factory, archiveParameters, collectedFiles, cancellable);
        if (config.isVerify()) {
            LOG.info("Verifing archive {}", archiveParameters.getArchive());
            ArchiveVerifier verifier = createVerifier(new InputStreamComparator());
            try (InputStream archiveInputStream = Files.newInputStream(archive)) {
                final boolean valid = verifier.verify(factory, archiveInputStream, collectedFiles);
                if (valid) {
                    LOG.info("Archive {} valid", archiveParameters.getArchive());
                } else {
                    LOG.error("Archive {} corrupted", archiveParameters.getArchive());
                }
                if (!valid) {
                    throw new VerificationFailedException("Archive verification failed");
                }
            }
        }
    }

    protected void compress(ArchiveFactory factory, ArchiveParameters archiveParameters, List<SourceWithPath> collectedFiles, Cancellable cancellable) throws ArchiveException {
        createCompressor(factory).compress(archiveParameters, collectedFiles, listener, cancellable);
    }

    FileCollector createFileCollector() {
        return new FileCollector();
    }

    Compressor createCompressor(ArchiveFactory factory) {
        return new Compressor(factory);
    }

    ArchiveVerifier createVerifier(InputStreamComparator comparator) {
        return new ArchiveVerifier(comparator);
    }

    protected String generateName(String configName, ArchiveFactory factory) {
        Objects.requireNonNull(factory, "ArchiveFactory is null");

        Calendar date = Calendar.getInstance();
        return String.format("%1$s_%2$tY_%2$tm_%2$td_%2$tH_%2$tM_%2$tS.%3$s", configName, date, factory.getExtension());
    }
}
