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
package fr.duminy.jbackup.core.task;

import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.archive.ArchiveException;
import fr.duminy.jbackup.core.archive.ArchiveFactory;
import fr.duminy.jbackup.core.archive.Decompressor;
import fr.duminy.jbackup.core.archive.ProgressListener;
import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactoryTest;
import fr.duminy.jbackup.core.util.FileDeleter;
import org.junit.experimental.theories.Theory;

import java.io.IOException;
import java.nio.file.Path;

import static fr.duminy.jbackup.core.task.BackupTaskTest.createDeleterSupplier;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class RestoreTaskTest extends AbstractTaskTest {
    @Theory
    public void testCall(ProgressListener listener) throws Throwable {
        testCall(null, listener);
    }

    @Theory
    public void testCall_deleteFilesOnError(ProgressListener listener) throws Throwable {
        final IOException exception = new IOException("An unexpected error");
        thrown.expect(exception.getClass());
        thrown.expectMessage(exception.getMessage());

        testCall(exception, listener);
    }

    private void testCall(Exception exception, ProgressListener listener) throws Throwable {
        Path archive = ZipArchiveFactoryTest.createArchive(tempFolder.newFolder().toPath());
        Path targetDirectory = tempFolder.newFolder("targetDirectory").toPath();

        // prepare test
        FileDeleter mockDeleter = mock(FileDeleter.class);
        final Decompressor mockDecompressor = mock(Decompressor.class);
        if (exception != null) {
            doThrow(new ArchiveException(exception)).when(mockDecompressor).
                    decompress(eq(archive), eq(targetDirectory), eq(listener));
        }

        BackupConfiguration config = new BackupConfiguration();
        config.setName("testRestoreTask");
        config.setTargetDirectory(targetDirectory.toString());
        config.setArchiveFactory(null);

        RestoreTask task = new RestoreTask(config, archive, targetDirectory, createDeleterSupplier(mockDeleter), listener) {
            @Override
            Decompressor createDecompressor(ArchiveFactory factory) {
                return mockDecompressor;
            }
        };

        // test
        try {
            task.call();
        } catch (ArchiveException e) {
            throw e.getCause();
        } finally {
            // assertions
            verify(mockDeleter, times(1)).registerDirectory(eq(targetDirectory));
            if (exception != null) {
                verify(mockDeleter, times(1)).deleteAll();
            }
            verify(mockDecompressor).decompress(eq(archive), eq(targetDirectory), eq(listener));
            verifyNoMoreInteractions(mockDeleter, mockDecompressor);
        }
    }
}