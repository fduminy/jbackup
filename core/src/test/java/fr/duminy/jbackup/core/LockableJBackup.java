/**
 * JBackup is a software managing backups.
 *
 * Copyright (C) 2013-2016 Fabien DUMINY (fabien [dot] duminy [at] webmails [dot] com)
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
import fr.duminy.jbackup.core.task.BackupTask;
import fr.duminy.jbackup.core.task.RestoreTask;
import fr.duminy.jbackup.core.task.TaskException;
import fr.duminy.jbackup.core.task.TaskListener;
import fr.duminy.jbackup.core.util.FileDeleter;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class LockableJBackup extends JBackupImpl {
    private final ArchiveFactory archiveFactory;
    private AtomicBoolean compressionLock;
    private AtomicBoolean decompressionLock;

    public LockableJBackup(ArchiveFactory archiveFactory) {
        this.archiveFactory = archiveFactory;
    }

    public void lockCompression() {
        compressionLock = new AtomicBoolean(true);
    }

    public void unlockCompression() {
        if (compressionLock != null) {
            compressionLock.set(false);
        }
    }

    @Override
    BackupTask createBackupTask(BackupConfiguration config, TaskListener taskListener, Cancellable cancellable) {
        return new BackupTask(config, TestUtils.newMockSupplier(), null, cancellable) {
            @Override
            protected void executeTask(FileDeleter deleter) throws TaskException {
                waitUnlocked(compressionLock);
            }
        };
    }

    public void lockDecompression() {
        decompressionLock = new AtomicBoolean(true);
    }

    public void unlockDecompression() {
        if (decompressionLock != null) {
            decompressionLock.set(false);
        }
    }

    @Override
    RestoreTask createRestoreTask(BackupConfiguration config, Path archive, Path targetDirectory, TaskListener taskListener, Cancellable cancellable) {
        return new RestoreTask(config, archive, targetDirectory, TestUtils.newMockSupplier(), null, cancellable) {
            @Override
            protected void executeTask(FileDeleter deleter) throws TaskException {
                waitUnlocked(decompressionLock);
            }
        };
    }

    private static void waitUnlocked(AtomicBoolean lock) throws TaskException {
        if (lock != null) {
            while (lock.get()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new TaskException(e);
                }
            }
        }
    }
}
