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

import fr.duminy.jbackup.core.archive.ArchiveFactory;
import fr.duminy.jbackup.core.archive.ArchiveParameters;
import fr.duminy.jbackup.core.archive.Archiver;
import fr.duminy.jbackup.core.archive.ProgressListener;
import fr.duminy.jbackup.core.util.DefaultFileDeleter;
import fr.duminy.jbackup.core.util.FileDeleter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * Main class.
 */
public class JBackup {
    private static final Logger LOG = LoggerFactory.getLogger(JBackup.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(8,
            new BasicThreadFactory.Builder().namingPattern("jbackup-thread-%d").daemon(false).priority(Thread.MAX_PRIORITY).build());

    public Future<Void> backup(BackupConfiguration config) {
        return backup(config, null);
    }

    public Future<Void> backup(BackupConfiguration config, ProgressListener listener) {
        return executor.submit(new BackupTask(this, config, listener));
    }

    public Future<Void> restore(BackupConfiguration config, Path archive, Path targetDirectory) {
        return restore(config, archive, targetDirectory, null);
    }

    public Future<Void> restore(BackupConfiguration config, Path archive, Path targetDirectory, ProgressListener listener) {
        return executor.submit(new RestoreTask(this, config, archive, targetDirectory, listener));
    }

    public void shutdown() throws InterruptedException {
        executor.shutdown();
        while (!executor.isTerminated()) {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        }
    }

    FileDeleter createFileDeleter() {
        return new DefaultFileDeleter();
    }

    Archiver createArchiver(ArchiveFactory factory) {
        return new Archiver(factory);
    }

    String generateName(String configName, ArchiveFactory factory) {
        Objects.requireNonNull(factory, "ArchiveFactory is null");

        Calendar date = Calendar.getInstance();
        return String.format("%1$s_%2$tY_%2$tm_%2$td_%2$tH_%2$tM_%2$tS.%3$s", configName, date, factory.getExtension());
    }

    private static abstract class Task implements Callable<Void> {
        protected final JBackup jbackup;
        protected final ProgressListener listener;
        protected final BackupConfiguration config;

        protected Task(JBackup jbackup, ProgressListener listener, BackupConfiguration config) {
            this.jbackup = jbackup;
            this.listener = listener;
            this.config = config;
        }

        @Override
        public final Void call() throws Exception {
            Throwable error = null;
            try {
                if (listener != null) {
                    listener.taskStarted();
                }
                execute();
            } catch (Exception e) {
                LOG.error("Error in " + Task.this.getClass().getSimpleName() + " for configuration '" + config.getName() + "'", e);
                error = e;
                throw e;
            } finally {
                if (listener != null) {
                    listener.taskFinished(error);
                }
            }

            return null;
        }

        abstract protected void execute() throws Exception;
    }

    private class BackupTask extends Task {
        private BackupTask(JBackup jbackup, BackupConfiguration config, ProgressListener listener) {
            super(jbackup, listener, config);
        }

        @Override
        protected void execute() throws Exception {
            ArchiveFactory factory = config.getArchiveFactory();

            Path target = Paths.get(config.getTargetDirectory());
            Files.createDirectories(target);

            String archiveName = generateName(config.getName(), config.getArchiveFactory());

            Path archive = target.resolve(archiveName);

            final ArchiveParameters archiveParameters = new ArchiveParameters(archive, config.isRelativeEntries());
            for (BackupConfiguration.Source filter : config.getSources()) {
                IOFileFilter dirFilter = config.createIOFileFilter("_dir", filter.getDirFilter());
                IOFileFilter fileFilter = config.createIOFileFilter("_file", filter.getFileFilter());
                Path source = Paths.get(filter.getSourceDirectory());
                archiveParameters.addSource(source, dirFilter, fileFilter);
            }

            FileDeleter deleter = createFileDeleter();
            try {
                deleter.registerFile(archiveParameters.getArchive());

                jbackup.createArchiver(factory).compress(archiveParameters, listener);
            } catch (Exception e) {
                deleter.deleteAll();
                throw e;
            }
        }
    }

    private class RestoreTask extends Task {
        private final Path archive;
        private final Path targetDirectory;

        private RestoreTask(JBackup jbackup, BackupConfiguration config, Path archive, Path targetDirectory, ProgressListener listener) {
            super(jbackup, listener, config);
            this.archive = archive;
            this.targetDirectory = targetDirectory;
        }

        @Override
        protected void execute() throws Exception {
            FileDeleter deleter = createFileDeleter();
            try {
                deleter.registerDirectory(targetDirectory);

                ArchiveFactory factory = config.getArchiveFactory();
                jbackup.createArchiver(factory).decompress(archive, targetDirectory, listener);
            } catch (Exception e) {
                deleter.deleteAll();
                throw e;
            }
        }
    }
}
