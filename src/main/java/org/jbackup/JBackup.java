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
package org.jbackup;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.jbackup.archive.ArchiveFactory;
import org.jbackup.archive.Archiver;
import org.jbackup.archive.FileCollector;
import org.jbackup.archive.ProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main class.
 */
public class JBackup {
    private static final Logger LOG = LoggerFactory.getLogger(JBackup.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(8);

    public void backup(BackupConfiguration config) throws IOException, InterruptedException {
        backup(config, null);
    }

    public void backup(BackupConfiguration config, ProgressListener listener) throws IOException, InterruptedException {
        executor.submit(new BackupTask(config, listener));
    }

    public void restore(BackupConfiguration config, File archive, File directory) throws IOException, InterruptedException {
        restore(config, archive, directory, null);
    }

    public void restore(BackupConfiguration config, File archive, File targetDirectory, ProgressListener listener) throws IOException, InterruptedException {
        executor.submit(new RestoreTask(config, archive, targetDirectory, listener));
    }

    public void shutdown() throws InterruptedException {
        executor.shutdown();
        while (!executor.isTerminated()) {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        }
    }

    private static String generateName(String configName, ArchiveFactory factory) {
        Calendar date = Calendar.getInstance();
        return String.format("%1$s_%2$tY_%2$tm_%2$td_%2$tH_%2$tM_%2$tS.%3$s", configName, date, factory.getExtension());
    }

    private static abstract class Task implements Callable<Object> {
    }

    private static class BackupTask extends Task {
        private final BackupConfiguration config;
        private final ProgressListener listener;

        private BackupTask(BackupConfiguration config, ProgressListener listener) {
            this.config = config;
            this.listener = listener;
        }

        @Override
        public Object call() throws IOException {
            ArchiveFactory factory = config.getArchiveFactory();

            File target = new File(config.getTargetDirectory());
            target.mkdirs();

            String archiveName = generateName(config.getName(), config.getArchiveFactory());

            File archive = new File(target, archiveName);

            Collection<File> files = new ArrayList<File>(10000);
            long size = 0L;
            for (BackupConfiguration.Source filter : config.getSources()) {
                IOFileFilter dirFilter = config.createIOFileFilter("_dir", filter.getDirFilter());
                IOFileFilter fileFilter = config.createIOFileFilter("_file", filter.getFileFilter());
                FileCollector walker = new FileCollector(dirFilter, fileFilter);
                size += walker.collect(files, filter.getSourceDirectory());
            }
            LOG.info("Backup '{}': {} files ({}) to compress", new Object[]{config.getName(), files.size(), FileUtils.byteCountToDisplaySize(size)});

            new Archiver(factory).compress(files.toArray(new File[files.size()]), archive, listener);
            return null;
        }
    }

    private static class RestoreTask extends Task {
        private final BackupConfiguration config;
        private final File archive;
        private final File targetDirectory;
        private final ProgressListener listener;

        private RestoreTask(BackupConfiguration config, File archive, File targetDirectory, ProgressListener listener) {
            this.config = config;
            this.archive = archive;
            this.targetDirectory = targetDirectory;
            this.listener = listener;
        }

        @Override
        public Object call() throws Exception {
            ArchiveFactory factory = config.getArchiveFactory();
            new Archiver(factory).decompress(archive, targetDirectory, listener);
            return null;
        }
    }
}
