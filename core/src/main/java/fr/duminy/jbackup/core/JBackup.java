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
import fr.duminy.jbackup.core.archive.ProgressListener;
import fr.duminy.jbackup.core.task.BackupTask;
import fr.duminy.jbackup.core.task.RestoreTask;
import fr.duminy.jbackup.core.util.DefaultFileDeleter;
import fr.duminy.jbackup.core.util.FileDeleter;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
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

    private final Supplier<FileDeleter> deleterSupplier = new Supplier<FileDeleter>() {
        @Override
        public FileDeleter get() {
            return new DefaultFileDeleter();
        }
    };

    public Future<Void> backup(BackupConfiguration config, ProgressListener listener) {
        return executor.submit(createBackupTask(config, listener));
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
        return new BackupTask(config, deleterSupplier, listener, null);
    }

    RestoreTask createRestoreTask(BackupConfiguration config, Path archive, Path targetDirectory, ProgressListener listener) {
        return new RestoreTask(config, archive, targetDirectory, deleterSupplier, listener, null);
    }
}
