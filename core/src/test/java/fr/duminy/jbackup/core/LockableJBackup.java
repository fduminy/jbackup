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

import fr.duminy.jbackup.core.archive.*;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LockableJBackup extends JBackup {
    private class MockArchiver extends Archiver {
        public MockArchiver(ArchiveFactory factory) {
            super(factory);
        }

        @Override
        public void compress(ArchiveParameters archiveParameters, List<SourceWithPath> files, ProgressListener listener) throws ArchiverException {
            waitUnlocked(compressionLock);
        }

        @Override
        public void decompress(Path archive, Path targetDirectory, ProgressListener listener) throws ArchiverException {
            waitUnlocked(decompressionLock);
        }

        private void waitUnlocked(AtomicBoolean lock) throws ArchiverException {
            if (lock != null) {
                while (lock.get()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new ArchiverException(e);
                    }
                }
            }
        }
    }

    private final ArchiveFactory archiveFactory;
    private AtomicBoolean compressionLock;
    private AtomicBoolean decompressionLock;

    public LockableJBackup(ArchiveFactory archiveFactory) {
        this.archiveFactory = archiveFactory;
    }

    @Override
    Archiver createArchiver(ArchiveFactory factory) {
        return new MockArchiver(archiveFactory);
    }

    public void lockCompression() {
        compressionLock = new AtomicBoolean(true);
    }

    public void unlockCompression() {
        if (compressionLock != null) {
            compressionLock.set(false);
        }
    }

    public void lockDecompression() {
        decompressionLock = new AtomicBoolean(true);
    }

    public void unlockDecompression() {
        if (decompressionLock != null) {
            decompressionLock.set(false);
        }
    }
}
