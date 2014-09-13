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

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.experimental.theories.Theory;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.duminy.jbackup.core.TestUtils.createFile;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class CompressorTest extends AbstractArchivingTest {
    @Test
    public void testCompress_relativeFilesMustThrowAnException() throws Throwable {
        ArchiveFactory mockFactory = createMockArchiveFactory(mock(ArchiveOutputStream.class));
        Path relativeFile = Paths.get("testCompressRelativeFile.tmp");

        try {
            createFile(relativeFile, 1);

            thrown.expect(IllegalArgumentException.class);
            thrown.expectMessage(String.format("The file '%s' is relative.", relativeFile));

            final ArchiveParameters archiveParameters = new ArchiveParameters(createArchivePath(), false);
            archiveParameters.addSource(relativeFile);
            compress(mockFactory, archiveParameters, null);
        } finally {
            Files.delete(relativeFile);
        }
    }

    @Theory
    public void testCompress(Data data, boolean useListener, EntryType entryType) throws Throwable {
        // preparation of archiver & mocks
        boolean relativeEntries = EntryType.RELATIVE.equals(entryType);
        ErrorType errorType = ErrorType.NO_ERROR;
        ArchiveOutputStream mockOutput = mock(ArchiveOutputStream.class);
        ArgumentCaptor<String> pathArgument = ArgumentCaptor.forClass(String.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                InputStream input = (InputStream) invocation.getArguments()[1];
                IOUtils.copy(input, new ByteArrayOutputStream());
                return null;
            }
        }).when(mockOutput).addEntry(pathArgument.capture(), any(InputStream.class));

        ArchiveFactory mockFactory = createMockArchiveFactory(mockOutput);
        Path baseDirectory = createBaseDirectory();
        ProgressListener listener = useListener ? mock(ProgressListener.class) : null;

        final ArchiveParameters archiveParameters = new ArchiveParameters(createArchivePath(), relativeEntries);
        Map<Path, List<Path>> expectedFilesBySource = data.createFiles(baseDirectory, archiveParameters);
        List<Path> expectedFiles = mergeFiles(expectedFilesBySource);
        Map<String, Path> expectedEntryToFile = new HashMap<>();

        try {
            errorType.setUp(expectedFiles);

            // test compression
            compress(mockFactory, archiveParameters, listener);

            // assertions
            verify(mockFactory, times(1)).create(any(OutputStream.class));
            verifyNoMoreInteractions(mockFactory);

            for (Map.Entry<Path, List<Path>> sourceEntry : expectedFilesBySource.entrySet()) {
                for (Path file : sourceEntry.getValue()) {
                    assertTrue("test self-check:  files must be absolute", file.isAbsolute());
                    final String expectedEntry;
                    if (relativeEntries) {
                        Path source = sourceEntry.getKey();
                        if (Files.isDirectory(source)) {
                            expectedEntry = source.getParent().relativize(file).toString();
                        } else {
                            expectedEntry = file.getFileName().toString();
                        }
                    } else {
                        expectedEntry = file.toString();
                    }
                    expectedEntryToFile.put(expectedEntry, file);
                    verify(mockOutput, times(1)).addEntry(eq(expectedEntry), any(InputStream.class));
                }
            }
            verify(mockOutput, times(1)).close();
            verifyNoMoreInteractions(mockOutput);
        } catch (Throwable t) {
            errorType.verifyExpected(t);
        } finally {
            errorType.tearDown(expectedFiles);
        }

        assertThatNotificationsAreValid(listener, pathArgument.getAllValues(), expectedEntryToFile, errorType);
    }

    private void compress(ArchiveFactory mockFactory, ArchiveParameters archiveParameters, ProgressListener listener) throws ArchiveException {
        List<SourceWithPath> collectedFiles = new ArrayList<>();
        new FileCollector().collectFiles(collectedFiles, archiveParameters, listener, null);
        new Compressor(mockFactory).compress(archiveParameters, collectedFiles, listener);
    }

    private List<Path> mergeFiles(Map<Path, List<Path>> filesBySource) {
        List<Path> files = new ArrayList<>();
        for (List<Path> filesForSource : filesBySource.values()) {
            files.addAll(filesForSource);
        }
        return files;
    }
}
