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

import fr.duminy.jbackup.core.JBackupTest;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.*;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(Theories.class)
abstract public class AbstractArchivingTest {
    @DataPoint
    public static final boolean NO_LISTENER = false;
    @DataPoint
    public static final boolean LISTENER = true;

    @DataPoints
    public static final ErrorType[] ERROR_TYPES = ErrorType.values();

    @DataPoint
    public static final Entries NO_ENTRY = new Entries();
    @DataPoint
    public static final Entries ONE_ENTRY = new Entries().file("entry1", 1L);
    @DataPoint
    public static final Entries TWO_ENTRIES = new Entries().file("entry1", 3L).file("entry2", 5L);

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    private static final Class<AccessDeniedException> ERROR_CLASS = AccessDeniedException.class;

    private final boolean testJBackup;

    protected AbstractArchivingTest() {
        this.testJBackup = (this instanceof JBackupTest);
    }

    @Theory
    public void testDecompress(Entries entries, boolean useListener, ErrorType errorType) throws Throwable {
        boolean errorIsExpected = ErrorType.ERROR == errorType;
        Assume.assumeTrue(!errorIsExpected || testJBackup);

        // preparation of archiver & mocks
        List<Long> expectedNotifications = new ArrayList<>();
        long expectedTotalSize = 0L;
        for (Entry e : entries) {
            if (e != null) {
                expectedTotalSize += e.compressedSize;
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
        Path directory = tempFolder.newFolder("targetDir").toPath();
        Files.write(archive, StringUtils.repeat("A", (int) expectedTotalSize).getBytes());

        ProgressListener listener = useListener ? mock(ProgressListener.class) : null;

        // test decompression
        try {
            if (errorIsExpected) {
                archive.toFile().setReadable(false);
                assertThat(Files.isReadable(archive)).isFalse();
            }

            decompress(mockFactory, archive, directory, listener, errorIsExpected);

            // assertions
            verify(mockFactory, times(1)).create(any(InputStream.class));
            verifyNoMoreInteractions(mockFactory);

            verify(mockInput, times(entries.size() + 1)).getNextEntry();
            verify(mockInput, times(1)).close();
            verifyNoMoreInteractions(mockInput);
        } catch (Throwable t) {
            checkErrorIsExpected(errorIsExpected, t);
        } finally {
            archive.toFile().setReadable(true);
            assertThat(Files.isReadable(archive)).isTrue();
        }

        assertThatNotificationsAreValid(listener, expectedNotifications, expectedTotalSize, errorIsExpected);
    }

    abstract protected void decompress(ArchiveFactory mockFactory, Path archive, Path directory, ProgressListener listener, boolean errorIsExpected) throws Throwable;

    @Theory
    public void testCompress(Entries entries, boolean useListener, ErrorType errorType) throws Throwable {
        boolean errorIsExpected = ErrorType.ERROR == errorType;
        Assume.assumeTrue(!errorIsExpected || testJBackup);

        // preparation of archiver & mocks
        ArchiveOutputStream mockOutput = mock(ArchiveOutputStream.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                InputStream input = (InputStream) invocation.getArguments()[1];
                IOUtils.copy(input, new ByteArrayOutputStream());
                return null;
            }
        }).when(mockOutput).addEntry(any(String.class), any(InputStream.class));

        ArchiveFactory mockFactory = createMockArchiveFactory(mockOutput);

        List<Path> files = new ArrayList<>();
        Path sourceDirectory = createSourcePath();
        List<Long> expectedNotifications = new ArrayList<>();
        long expectedTotalSize = 0L;
        for (Entry entry : entries) {
            Path file = sourceDirectory.resolve(entry.name);
            Files.write(file, StringUtils.repeat("A", (int) entry.compressedSize).getBytes());

            files.add(file);
            expectedTotalSize += Files.size(file);
            expectedNotifications.add(expectedTotalSize);
        }

        ProgressListener listener = useListener ? mock(ProgressListener.class) : null;

        try {
            if (errorIsExpected) {
                sourceDirectory.toFile().setReadable(false);
            }

            // test compression
            final ArchiveParameters archiveParameters = new ArchiveParameters(createArchivePath());
            archiveParameters.addSource(sourceDirectory);
            compress(mockFactory, archiveParameters, files, listener, errorIsExpected);

            // assertions
            verify(mockFactory, times(1)).create(any(OutputStream.class));
            if (testJBackup) {
                verify(mockFactory, times(1)).getExtension();
            }
            verifyNoMoreInteractions(mockFactory);

            if (!errorIsExpected) {
                for (Path file : files) {
                    verify(mockOutput, times(1)).addEntry(eq(file.toFile().getAbsolutePath()), any(InputStream.class));
                }
            }
            verify(mockOutput, times(1)).close();
            verifyNoMoreInteractions(mockOutput);
        } catch (Throwable t) {
            checkErrorIsExpected(errorIsExpected, t);
        } finally {
            sourceDirectory.toFile().setReadable(true);
        }

        assertThatNotificationsAreValid(listener, expectedNotifications, expectedTotalSize, errorIsExpected);
    }

    Path createArchivePath() throws IOException {
        return tempFolder.newFile("archive.mock").toPath();
    }

    Path createSourcePath() {
        return tempFolder.newFolder("sourceDirectory").toPath().toAbsolutePath();
    }

    ArchiveFactory createMockArchiveFactory(ArchiveOutputStream mockOutput) throws IOException {
        ArchiveFactory mockFactory = mock(ArchiveFactory.class);
        when(mockFactory.create(any(OutputStream.class))).thenReturn(mockOutput);
        when(mockFactory.getExtension()).thenReturn("mock");
        return mockFactory;
    }

    private void checkErrorIsExpected(boolean error, Throwable t) throws Throwable {
        if (!error || !t.getClass().equals(ERROR_CLASS)) {
            throw t;
        }
    }

    private void assertThatNotificationsAreValid(ProgressListener listener, List<Long> expectedNotifications, long expectedTotalSize, boolean errorIsExpected) {
        if (listener != null) {
            InOrder inOrder = inOrder(listener);

            // 1 - taskStarted
            if (testJBackup) {
                inOrder.verify(listener, times(1)).taskStarted();
            } else {
                inOrder.verify(listener, never()).taskStarted();
            }

            if (!errorIsExpected) {
                // 2 - totalSizeComputed
                inOrder.verify(listener, times(1)).totalSizeComputed(expectedTotalSize);

                // 3 - progress
                if (!expectedNotifications.isEmpty()) {
                    ArgumentCaptor<Long> argument = ArgumentCaptor.forClass(Long.class);
                    inOrder.verify(listener, times(expectedNotifications.size())).progress(argument.capture());
                    assertThat(argument.getAllValues()).as("progress notifications").isEqualTo(expectedNotifications);
                }
            }

            // 4 - taskFinished
            if (testJBackup) {
                inOrder.verify(listener, times(1)).taskFinished(errorIsExpected ? any(ERROR_CLASS) : null);
            } else {
                inOrder.verify(listener, never()).taskFinished(any(Throwable.class));
            }

            inOrder.verifyNoMoreInteractions();
        }
    }

    abstract protected void compress(ArchiveFactory mockFactory, ArchiveParameters archiveParameters, List<Path> expectedFiles, ProgressListener listener, boolean errorIsExpected) throws Throwable;

    private static class Entry {
        private final String name;
        private final long compressedSize;

        private Entry(String name, long compressedSize) {
            this.name = name;
            this.compressedSize = compressedSize;
        }
    }

    private static class Entries implements Iterable<Entry> {
        private final List<Entry> entries = new ArrayList<>();

        public Entries file(String file, long compressedSize) {
            entries.add(new Entry(file, compressedSize));
            return this;
        }

        @Override
        public Iterator<Entry> iterator() {
            return entries.iterator();
        }

        public ArchiveInputStream.Entry firstEntry() {
            return entries.isEmpty() ? null : newMockEntry(entries.get(0));
        }

        public ArchiveInputStream.Entry[] nextEntries() {
            ArchiveInputStream.Entry[] result = new ArchiveInputStream.Entry[entries.size()];
            if (!entries.isEmpty()) {
                for (int i = 1; i < entries.size(); i++) {
                    result[i - 1] = newMockEntry(entries.get(i));
                }
                result[result.length - 1] = null; // indicates no more entries
            }
            return result;
        }

        private ArchiveInputStream.Entry newMockEntry(Entry entry) {
            ArchiveInputStream.Entry result = null;
            if (entry != null) {
                result = mock(ArchiveInputStream.Entry.class);
                when(result.getName()).thenReturn(entry.name);
                when(result.getCompressedSize()).thenReturn(entry.compressedSize);
                when(result.getInput()).thenReturn(new ByteArrayInputStream(new byte[(int) entry.compressedSize]));
            }

            return result;
        }

        public int size() {
            return entries.size();
        }
    }

    private static enum ErrorType {
        NO_ERROR,
        ERROR;
    }
}
