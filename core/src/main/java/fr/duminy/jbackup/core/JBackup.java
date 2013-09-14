/**
 * JBackup is a software managing backups.
 *
 * Copyright (C) 2013-2013 Fabien DUMINY (fabien [dot] duminy [at] webmails [dot] com)
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
import fr.duminy.jbackup.core.archive.Archiver;
import fr.duminy.jbackup.core.archive.FileCollector;
import fr.duminy.jbackup.core.archive.ProgressListener;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.concurrent.*;

/**
 * Main class.
 */
public class JBackup {
    private static final Logger LOG = LoggerFactory.getLogger(JBackup.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(8);

    public Future<Object> backup(BackupConfiguration config) throws IOException, InterruptedException {
        return backup(config, null);
    }

    public Future<Object> backup(BackupConfiguration config, ProgressListener listener) throws IOException, InterruptedException {
        return executor.submit(new BackupTask(this, config, listener));
    }

    public Future<Object> restore(BackupConfiguration config, File archive, File directory) throws IOException, InterruptedException {
        return restore(config, archive, directory, null);
    }

    public Future<Object> restore(BackupConfiguration config, File archive, File targetDirectory, ProgressListener listener) throws IOException, InterruptedException {
        return executor.submit(new RestoreTask(this, config, archive, targetDirectory, listener));
    }

    public void shutdown() throws InterruptedException {
        executor.shutdown();
        while (!executor.isTerminated()) {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        }
    }

    Archiver createArchiver(ArchiveFactory factory) {
        return new Archiver(factory);
    }

    private static String generateName(String configName, ArchiveFactory factory) {
        Calendar date = Calendar.getInstance();
        return String.format("%1$s_%2$tY_%2$tm_%2$td_%2$tH_%2$tM_%2$tS.%3$s", configName, date, factory.getExtension());
    }

    private static abstract class Task implements Callable<Object> {
        protected final JBackup jbackup;
        protected final ProgressListener listener;

        protected Task(JBackup jbackup, ProgressListener listener) {
            this.jbackup = jbackup;
            this.listener = listener;
        }

        @Override
        public final Object call() throws IOException {
            if (listener == null) {
                execute();
            } else {
                Throwable error = null;
                try {
                    try {
                        listener.taskStarted();
                        execute();
                    } catch (Throwable e) {
                        error = e;
                        throw e;
                    }
                } finally {
                    listener.taskFinished(error);
                }
            }

            return null;
        }

        abstract protected void execute() throws IOException;
    }

    private static class BackupTask extends Task {
        private final BackupConfiguration config;
        private final ProgressListener listener;

        private BackupTask(JBackup jbackup, BackupConfiguration config, ProgressListener listener) {
            super(jbackup, listener);
            this.config = config;
            this.listener = listener;
        }

        @Override
        protected void execute() throws IOException {
            ArchiveFactory factory = config.getArchiveFactory();

            File target = new File(config.getTargetDirectory());
            Files.createDirectories(target.toPath());

            String archiveName = generateName(config.getName(), config.getArchiveFactory());

            File archive = new File(target, archiveName);

            Collection<File> files = new ArrayList<>(10000);
            long size = 0L;
            for (BackupConfiguration.Source filter : config.getSources()) {
                IOFileFilter dirFilter = config.createIOFileFilter("_dir", filter.getDirFilter());
                IOFileFilter fileFilter = config.createIOFileFilter("_file", filter.getFileFilter());
                FileCollector walker = new FileCollector(dirFilter, fileFilter);
                size += walker.collect(files, filter.getSourceDirectory());
            }
            LOG.info("Backup '{}': {} files ({}) to compress", new Object[]{config.getName(), files.size(), FileUtils.byteCountToDisplaySize(size)});

            jbackup.createArchiver(factory).compress(files.toArray(new File[files.size()]), archive, listener);
        }
    }

    private static class RestoreTask extends Task {
        private final BackupConfiguration config;
        private final File archive;
        private final File targetDirectory;
        private final ProgressListener listener;

        private RestoreTask(JBackup jbackup, BackupConfiguration config, File archive, File targetDirectory, ProgressListener listener) {
            super(jbackup, listener);
            this.config = config;
            this.archive = archive;
            this.targetDirectory = targetDirectory;
            this.listener = listener;
        }

        @Override
        protected void execute() throws IOException {
            ArchiveFactory factory = config.getArchiveFactory();
            jbackup.createArchiver(factory).decompress(archive, targetDirectory, listener);
        }
    }
}
