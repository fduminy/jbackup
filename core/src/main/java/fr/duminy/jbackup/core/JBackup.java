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
import fr.duminy.jbackup.core.task.TaskListener;
import fr.duminy.jbackup.core.util.DefaultFileDeleter;
import fr.duminy.jbackup.core.util.FileDeleter;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.util.concurrent.*;

/**
 * Facade class for features.
 */
public class JBackup {
    private static final Logger LOG = LoggerFactory.getLogger(JBackup.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(8,
            new BasicThreadFactory.Builder().namingPattern("jbackup-thread-%d").daemon(false).priority(Thread.MAX_PRIORITY).build());

    private final Supplier<FileDeleter> deleterSupplier = createDeleterSupplier();

    public Future<Void> backup(final BackupConfiguration config, final ProgressListener listener) {
        return submitNewTask(new TaskFactory<BackupTask>() {
            @Override
            public BackupTask createTask(Cancellable cancellable) {
                return createBackupTask(config, listener, cancellable);
            }
        });
    }

    public Future<Void> restore(final BackupConfiguration config, final Path archive, final Path targetDirectory, final ProgressListener listener) {
        return submitNewTask(new TaskFactory<RestoreTask>() {
            @Override
            public RestoreTask createTask(Cancellable cancellable) {
                return createRestoreTask(config, archive, targetDirectory, listener, cancellable);
            }
        });
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

    BackupTask createBackupTask(BackupConfiguration config, ProgressListener listener, Cancellable cancellable) {
        return new BackupTask(config, deleterSupplier, createTaskListener(listener), null);
    }

    RestoreTask createRestoreTask(BackupConfiguration config, Path archive, Path targetDirectory,
                                  ProgressListener listener, Cancellable cancellable) {
        return new RestoreTask(config, archive, targetDirectory, deleterSupplier, createTaskListener(listener), null);
    }

    Supplier<FileDeleter> createDeleterSupplier() {
        return new Supplier<FileDeleter>() {
            @Override
            public FileDeleter get() {
                return new DefaultFileDeleter();
            }
        };
    }

    private static class JBackupCancellable implements Cancellable {
        private Future<Void> future;

        @Override
        public boolean isCancelled() throws CancellationException {
            return (future != null) && future.isCancelled();
        }

        private void setFuture(Future<Void> future) {
            this.future = future;
        }
    }

    private static interface TaskFactory<T extends Callable<Void>> {
        T createTask(Cancellable cancellable);
    }

    private <T extends Callable<Void>> Future<Void> submitNewTask(TaskFactory<T> taskFactory) {
        JBackupCancellable cancellable = new JBackupCancellable();
        Future<Void> future = executor.submit(taskFactory.createTask(cancellable));
        cancellable.setFuture(future);
        return future;
    }

    private TaskListener createTaskListener(final ProgressListener listener) {
        if (listener == null) {
            return null;
        }

        return new TaskListener() {
            @Override
            public void taskStarted() {
                listener.taskStarted();
            }

            @Override
            public void totalSizeComputed(long totalSize) {
                listener.totalSizeComputed(totalSize);
            }

            @Override
            public void progress(long totalReadBytes) {
                listener.progress(totalReadBytes);
            }

            @Override
            public void taskFinished(Throwable error) {
                listener.taskFinished(error);
            }
        };
    }
}
