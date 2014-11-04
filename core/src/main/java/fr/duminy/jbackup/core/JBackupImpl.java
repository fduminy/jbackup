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
import fr.duminy.jbackup.core.task.Task;
import fr.duminy.jbackup.core.task.TaskListener;
import fr.duminy.jbackup.core.util.DefaultFileDeleter;
import fr.duminy.jbackup.core.util.FileDeleter;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.commons.lang3.event.EventListenerSupport;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Implementation of {@link fr.duminy.jbackup.core.JBackup}.
 */
public class JBackupImpl implements JBackup {
    private final ExecutorService executor = Executors.newFixedThreadPool(8,
            new BasicThreadFactory.Builder().namingPattern("jbackup-thread-%d").daemon(false).priority(Thread.MAX_PRIORITY).build());

    private final Supplier<FileDeleter> deleterSupplier = createDeleterSupplier();

    private final Map<String, JBackupTaskListener> listeners = new HashMap<>();

    @Override
    public Future<Void> backup(final BackupConfiguration config) {
        return submitNewTask(new TaskFactory<Task>() {
            @Override
            public Task createTask(Cancellable cancellable) {
                return createBackupTask(config, getTaskListener(config.getName()), cancellable);
            }
        });
    }

    @Override
    public Future<Void> restore(final BackupConfiguration config, final Path archive, final Path targetDirectory) {
        return submitNewTask(new TaskFactory<Task>() {
            @Override
            public Task createTask(Cancellable cancellable) {
                return createRestoreTask(config, archive, targetDirectory, getTaskListener(config.getName()), cancellable);
            }
        });
    }

    @Override
    public void addProgressListener(String configurationName, ProgressListener listener) {
        getTaskListener(configurationName).addListener(listener);
    }

    @Override
    public void removeProgressListener(String configurationName, ProgressListener listener) {
        getTaskListener(configurationName).removeProgressListener(listener);
    }

    @Override
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

    Task createBackupTask(BackupConfiguration config, TaskListener taskListener, Cancellable cancellable) {
        return new BackupTask(config, deleterSupplier, taskListener, cancellable);
    }

    Task createRestoreTask(BackupConfiguration config, Path archive, Path targetDirectory, TaskListener taskListener, Cancellable cancellable) {
        return new RestoreTask(config, archive, targetDirectory, deleterSupplier, taskListener, cancellable);
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

    private JBackupTaskListener getTaskListener(String configurationName) {
        JBackupTaskListener taskListener = listeners.get(configurationName);
        if (taskListener == null) {
            taskListener = new JBackupTaskListener(configurationName);
            listeners.put(configurationName, taskListener);
        }
        return taskListener;
    }

    private <T extends Callable<Void>> Future<Void> submitNewTask(TaskFactory<T> taskFactory) {
        JBackupCancellable cancellable = new JBackupCancellable();
        Future<Void> future = executor.submit(taskFactory.createTask(cancellable));
        cancellable.setFuture(future);
        return future;
    }

    private static class JBackupTaskListener implements TaskListener {
        private final EventListenerSupport<ProgressListener> listeners = EventListenerSupport.create(ProgressListener.class);
        private final String configurationName;

        private JBackupTaskListener(String configurationName) {
            this.configurationName = configurationName;
        }

        @Override
        public void taskStarted() {
            listeners.fire().taskStarted(configurationName);
        }

        @Override
        public void totalSizeComputed(long totalSize) {
            listeners.fire().totalSizeComputed(configurationName, totalSize);
        }

        @Override
        public void progress(long totalReadBytes) {
            listeners.fire().progress(configurationName, totalReadBytes);
        }

        @Override
        public void taskFinished(Throwable error) {
            listeners.fire().taskFinished(configurationName, error);
        }

        public void addListener(ProgressListener listener) {
            listeners.addListener(listener);
        }

        public void removeProgressListener(ProgressListener listener) {
            listeners.removeListener(listener);
        }
    }
}
