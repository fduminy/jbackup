/**
 * JBackup is a software managing backups.
 *
 * Copyright (C) 2013-2017 Fabien DUMINY (fabien [dot] duminy [at] webmails [dot] com)
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
package fr.duminy.jbackup.core.archive;

import fr.duminy.jbackup.core.Cancellable;
import fr.duminy.jbackup.core.task.TaskListener;
import org.junit.Assume;
import org.junit.Test;
import org.junit.experimental.theories.Theory;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static fr.duminy.jbackup.core.TestUtils.createFile;
import static fr.duminy.jbackup.core.archive.ArchiveDSL.*;
import static org.mockito.Mockito.*;

public class DecompressorTest extends AbstractArchivingTest {
    @Test
    public void testDecompress_missingTargetDirectoryMustThrowAnException() throws Throwable {
        ArchiveFactory mockFactory = createMockArchiveFactory(mock(ArchiveOutputStream.class));
        ArchiveInputStream.Entry mockEntry = mock(ArchiveInputStream.Entry.class);
        when(mockEntry.getName()).thenReturn("mockEntry");
        when(mockEntry.getCompressedSize()).thenReturn(1L);
        when(mockEntry.getInput()).thenReturn(new ByteArrayInputStream(new byte[1]));
        ArchiveInputStream mockInput = mock(ArchiveInputStream.class);
        when(mockInput.getNextEntry()).thenReturn(mockEntry, (ArchiveInputStream.Entry) null);
        when(mockFactory.create(any(InputStream.class))).thenReturn(mockInput);

        Path archive = createArchivePath();
        final Path baseDirectory = createBaseDirectory();
        Path targetDirectory = baseDirectory.resolve("targetDirectory");

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(String.format("The target directory '%s' doesn't exist.", targetDirectory));

        createDirectory = false;
        decompress(mockFactory, archive, targetDirectory, null, null);
    }


    @Test
    public void testDecompress_withCancellable_cancelAfterFirstFile() throws Throwable {
        testDecompress_withCancellable(true);
    }

    @Test
    public void testDecompress_withCancellable_neverCancelled() throws Throwable {
        testDecompress_withCancellable(false);
    }

    private void testDecompress_withCancellable(boolean cancelAfterFirstFile) throws Throwable {
        // prepare
        Cancellable cancellable = mock(Cancellable.class);
        when(cancellable.isCancelled()).thenReturn(false, cancelAfterFirstFile ? true : false);

        ArchiveInputStream mockInput = createMockArchiveInputStream(TWO_SRC_FILES);
        ArchiveFactory mockFactory = mock(ArchiveFactory.class);
        when(mockFactory.create(any(InputStream.class))).thenReturn(mockInput);

        Path archive = createArchivePath();
        Path targetDirectory = tempFolder.newFolder("targetDir").toPath();
        createFile(archive, 10);

        // test decompression
        decompress(mockFactory, archive, targetDirectory, null, cancellable);

        // assertions
        InOrder inOrder = Mockito.inOrder(cancellable, mockInput);
        inOrder.verify(cancellable, times(1)).isCancelled();
        inOrder.verify(mockInput, times(1)).getNextEntry();
        inOrder.verify(cancellable, times(1)).isCancelled();
        if (!cancelAfterFirstFile) {
            inOrder.verify(mockInput, times(1)).getNextEntry();
        }
        inOrder.verify(mockInput, times(1)).close();
        inOrder.verifyNoMoreInteractions();
    }

    @Theory
    public void testDecompress(Data data, boolean useListener) throws Throwable {
        Assume.assumeTrue(data.size() == 1);

        // preparation of archiver & mocks
        ErrorType errorType = ErrorType.NO_ERROR;
        List<Long> expectedNotifications = new ArrayList<>();
        long expectedTotalSize = 0L;
        for (Entry e : data.entries()) {
            if (e != null) {
                expectedTotalSize += e.getCompressedSize();
                expectedNotifications.add(expectedTotalSize);
            }
        }

        ArchiveInputStream mockInput = createMockArchiveInputStream(data);
        ArchiveFactory mockFactory = mock(ArchiveFactory.class);
        when(mockFactory.create(any(InputStream.class))).thenReturn(mockInput);

        Path archive = createArchivePath();
        Path targetDirectory = tempFolder.newFolder("targetDir").toPath();
        createFile(archive, expectedTotalSize);

        TaskListener listener = useListener ? mock(TaskListener.class) : null;

        // test decompression
        try {
            errorType.setUp(archive);

            decompress(mockFactory, archive, targetDirectory, listener, null);

            // assertions
            verify(mockFactory, times(1)).create(any(InputStream.class));
            verifyNoMoreInteractions(mockFactory);

            verify(mockInput, times(data.entries().size() + 1)).getNextEntry();
            verify(mockInput, times(1)).close();
            verifyNoMoreInteractions(mockInput);
        } catch (Throwable t) {
            errorType.verifyExpected(t);
        } finally {
            errorType.tearDown(archive);
        }

        assertThatNotificationsAreValid(listener, expectedNotifications, expectedTotalSize, errorType);
    }

    private ArchiveInputStream createMockArchiveInputStream(Data data) throws IOException {
        ArchiveInputStream mockInput = mock(ArchiveInputStream.class);

        Entries entries = data.entries();
        final ArchiveInputStream.Entry[] nextEntries = entries.nextEntries(); //Do not inline this or the test will fail
        final ArchiveInputStream.Entry firstEntry = entries.firstEntry(); //Do not inline this or the test will fail
        when(mockInput.getNextEntry()).thenReturn(firstEntry, nextEntries);

        return mockInput;
    }

    private void decompress(ArchiveFactory mockFactory, Path archive, Path targetDirectory, TaskListener listener, Cancellable cancellable) throws ArchiveException {
        if (createDirectory) {
            try {
                Files.createDirectories(targetDirectory);
            } catch (IOException e) {
                throw new ArchiveException(e);
            }
        }

        new Decompressor(mockFactory).decompress(archive, targetDirectory, listener, cancellable);
    }
}
