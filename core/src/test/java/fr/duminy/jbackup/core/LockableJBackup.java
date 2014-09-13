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

import fr.duminy.jbackup.core.archive.ArchiveException;
import fr.duminy.jbackup.core.archive.ArchiveFactory;
import fr.duminy.jbackup.core.archive.ProgressListener;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class LockableJBackup extends JBackup {
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
    BackupTask createBackupTask(BackupConfiguration config, ProgressListener listener) {
        return new BackupTask(this, config, listener) {
            @Override
            protected void execute() throws Exception {
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
    RestoreTask createRestoreTask(BackupConfiguration config, Path archive, Path targetDirectory, ProgressListener listener) {
        return new RestoreTask(this, config, archive, targetDirectory, listener) {
            @Override
            protected void execute() throws Exception {
                waitUnlocked(decompressionLock);
            }
        };
    }

    private static void waitUnlocked(AtomicBoolean lock) throws ArchiveException {
        if (lock != null) {
            while (lock.get()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new ArchiveException(e);
                }
            }
        }
    }
}
