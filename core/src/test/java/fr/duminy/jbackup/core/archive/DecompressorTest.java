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
package fr.duminy.jbackup.core.archive;

import org.junit.Assume;
import org.junit.Test;
import org.junit.experimental.theories.Theory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static fr.duminy.jbackup.core.TestUtils.createFile;
import static fr.duminy.jbackup.core.archive.ArchiveDSL.*;
import static org.mockito.Matchers.any;
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
        when(mockInput.getNextEntry()).thenReturn(mockEntry, null);
        when(mockFactory.create(any(InputStream.class))).thenReturn(mockInput);

        Path archive = createArchivePath();
        final Path baseDirectory = createBaseDirectory();
        Path targetDirectory = baseDirectory.resolve("targetDirectory");

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(String.format("The target directory '%s' doesn't exist.", targetDirectory));

        createDirectory = false;
        decompress(mockFactory, archive, targetDirectory, null);
    }

    @Theory
    public void testDecompress(Data data, boolean useListener) throws Throwable {
        Assume.assumeTrue(data.dataSources.size() == 1);

        // preparation of archiver & mocks
        ErrorType errorType = ErrorType.NO_ERROR;
        Entries entries = data.dataSources.get(0).entries();
        List<Long> expectedNotifications = new ArrayList<>();
        long expectedTotalSize = 0L;
        for (Entry e : entries) {
            if (e != null) {
                expectedTotalSize += e.getCompressedSize();
                expectedNotifications.add(expectedTotalSize);
            }
        }

        ArchiveInputStream mockInput = mock(ArchiveInputStream.class);
        final ArchiveInputStream.Entry firstEntry = entries.firstEntry(); //Do not inline this or the test will fail
        final ArchiveInputStream.Entry[] nextEntries = entries.nextEntries(); //Do not inline this or the test will fail
        when(mockInput.getNextEntry()).thenReturn(firstEntry, nextEntries);

        ArchiveFactory mockFactory = mock(ArchiveFactory.class);
        when(mockFactory.create(any(InputStream.class))).thenReturn(mockInput);

        Path archive = createArchivePath();
        Path targetDirectory = tempFolder.newFolder("targetDir").toPath();
        createFile(archive, expectedTotalSize);

        ProgressListener listener = useListener ? mock(ProgressListener.class) : null;

        // test decompression
        try {
            errorType.setUp(archive);

            decompress(mockFactory, archive, targetDirectory, listener);

            // assertions
            verify(mockFactory, times(1)).create(any(InputStream.class));
            verifyNoMoreInteractions(mockFactory);

            verify(mockInput, times(entries.size() + 1)).getNextEntry();
            verify(mockInput, times(1)).close();
            verifyNoMoreInteractions(mockInput);
        } catch (Throwable t) {
            errorType.verifyExpected(t);
        } finally {
            errorType.tearDown(archive);
        }

        assertThatNotificationsAreValid(listener, expectedNotifications, expectedTotalSize, errorType);
    }

    private void decompress(ArchiveFactory mockFactory, Path archive, Path targetDirectory, ProgressListener listener) throws ArchiveException {
        if (createDirectory) {
            try {
                Files.createDirectories(targetDirectory);
            } catch (IOException e) {
                throw new ArchiveException(e);
            }
        }

        new Decompressor(mockFactory).decompress(archive, targetDirectory, listener);
    }
}
