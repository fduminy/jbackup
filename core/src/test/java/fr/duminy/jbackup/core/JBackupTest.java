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

import fr.duminy.jbackup.core.archive.AbstractArchivingTest;
import fr.duminy.jbackup.core.archive.ArchiveFactory;
import fr.duminy.jbackup.core.archive.Archiver;
import fr.duminy.jbackup.core.archive.ProgressListener;
import org.apache.commons.lang.mutable.MutableObject;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for class {@link fr.duminy.jbackup.core.JBackup}.
 */
public class JBackupTest extends AbstractArchivingTest {
    @Override
    protected void decompress(ArchiveFactory mockFactory, File archive, File directory, ProgressListener listener, boolean errorIsExpected) throws Throwable {
        JBackup jbackup = new JBackup();
        File archiveDirectory = new File(archive.getParent());
        BackupConfiguration config = new BackupConfiguration();
        config.setName("testDecompress");
        config.setTargetDirectory(archiveDirectory.getAbsolutePath());
        config.setArchiveFactory(mockFactory);

        Future<Object> future;
        if (listener == null) {
            future = jbackup.restore(config, archive, directory);
            assertThat(future).isNotNull();
        } else {
            future = jbackup.restore(config, archive, directory, listener);
            assertThat(future).isNotNull();
        }

        waitResult(future);
        jbackup.shutdown();
    }

    @Override
    protected void compress(ArchiveFactory mockFactory, File sourceDirectory, final File[] expectedFiles, File archive, ProgressListener listener, boolean errorIsExpected) throws Throwable {
        final MutableObject actualFilesWrapper = new MutableObject();
        JBackup jbackup = new JBackup() {
            @Override
            Archiver createArchiver(ArchiveFactory factory) {
                return new Archiver(factory) {
                    @Override
                    public void compress(File[] actualFiles, File archive, ProgressListener listener) throws IOException {
                        actualFilesWrapper.setValue(actualFiles);

                        // now compress files in the order given by expectedFiles
                        // (otherwise the test will fail on some platforms)
                        super.compress(actualFiles, archive, listener);
                    }
                };
            }
        };

        BackupConfiguration config = new BackupConfiguration();
        config.setName("testCompress");
        config.setTargetDirectory(archive.getParent());
        config.addSource(sourceDirectory.getAbsolutePath());
        config.setArchiveFactory(mockFactory);

        Future<Object> future;
        if (listener == null) {
            future = jbackup.backup(config);
        } else {
            future = jbackup.backup(config, listener);
        }

        waitResult(future);

        // ensure that actual files are as expected
        if (!errorIsExpected) {
            assertThat(actualFilesWrapper.getValue()).isEqualTo(expectedFiles);
        }

        jbackup.shutdown();
    }

    protected void waitResult(Future<Object> future) throws Throwable {
        assertThat(future).isNotNull();
        try {
            future.get(); // block until finished and maybe throw an Exception if task has thrown one.
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }
}
