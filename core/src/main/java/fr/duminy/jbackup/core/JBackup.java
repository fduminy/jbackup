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
import fr.duminy.jbackup.core.archive.ProgressListener;
import fr.duminy.jbackup.core.util.DefaultFileDeleter;
import fr.duminy.jbackup.core.util.FileDeleter;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Facade class for features.
 */
public class JBackup {
    private static final Logger LOG = LoggerFactory.getLogger(JBackup.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(8,
            new BasicThreadFactory.Builder().namingPattern("jbackup-thread-%d").daemon(false).priority(Thread.MAX_PRIORITY).build());

    public Future<Void> backup(BackupConfiguration config) {
        return backup(config, null);
    }

    public Future<Void> backup(BackupConfiguration config, ProgressListener listener) {
        return executor.submit(createBackupTask(config, listener));
    }

    public Future<Void> restore(BackupConfiguration config, Path archive, Path targetDirectory) {
        return restore(config, archive, targetDirectory, null);
    }

    public Future<Void> restore(BackupConfiguration config, Path archive, Path targetDirectory, ProgressListener listener) {
        return executor.submit(createRestoreTask(config, archive, targetDirectory, listener));
    }

    public Timer shutdown(final TerminationListener listener) throws InterruptedException {
        executor.shutdown();

        Timer timer = null;
        if (listener != null) {
            timer = new Timer(0, null);
            timer.setDelay((int) TimeUnit.SECONDS.toMillis(1));
            final Timer finalTimer = timer;
            timer.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (executor.isTerminated()) {
                        listener.terminated();
                        finalTimer.stop();
                    }
                }
            });
            timer.setRepeats(true);
            timer.start();
        }

        return timer;
    }

    public static interface TerminationListener {
        void terminated();
    }

    BackupTask createBackupTask(BackupConfiguration config, ProgressListener listener) {
        return new BackupTask(this, config, listener);
    }

    RestoreTask createRestoreTask(BackupConfiguration config, Path archive, Path targetDirectory, ProgressListener listener) {
        return new RestoreTask(this, config, archive, targetDirectory, listener);
    }

    FileDeleter createFileDeleter() {
        return new DefaultFileDeleter();
    }

    String generateName(String configName, ArchiveFactory factory) {
        Objects.requireNonNull(factory, "ArchiveFactory is null");

        Calendar date = Calendar.getInstance();
        return String.format("%1$s_%2$tY_%2$tm_%2$td_%2$tH_%2$tM_%2$tS.%3$s", configName, date, factory.getExtension());
    }

}
