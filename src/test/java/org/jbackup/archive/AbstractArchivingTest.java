/**
 * JBackup is a software managing backups.
 *
 * Copyright (C) 2013-2013 Fabien DUMINY (fabien [dot] duminy [at] webmails [dot] com)
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
package org.jbackup.archive;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.fest.assertions.Assertions;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(Theories.class)
abstract public class AbstractArchivingTest {
    @DataPoint
    public static final boolean NO_LISTENER = false;
    @DataPoint
    public static final boolean LISTENER = true;

    @DataPoint
    public static final EntryData[] NO_ENTRY = {null};
    @DataPoint
    public static final EntryData[] ONE_ENTRY = {new EntryData("entry1", 1L), null};
    @DataPoint
    public static final EntryData[] TWO_ENTRIES = {new EntryData("entry1", 3L), new EntryData("entry2", 5L), null};

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    private final boolean callGetExtension;

    protected AbstractArchivingTest(boolean callGetExtension) {
        this.callGetExtension = callGetExtension;
    }

    @Theory
    public void testDecompress(EntryData[] entries, boolean useListener) throws Exception {
        // preparation of archiver & mocks
        List<Long> expectedNotifications = new ArrayList<Long>();
        long expectedTotalSize = 0L;
        for (EntryData e : entries) {
            if (e != null) {
                expectedTotalSize += e.compressedSize;
                expectedNotifications.add(expectedTotalSize);
            }
        }

        ArchiveInputStream mockInput = mock(ArchiveInputStream.class);
        ArchiveInputStream.Entry first = firstMockEntry(entries);
        ArchiveInputStream.Entry[] next = nextMockEntries(entries);
        when(mockInput.getNextEntry()).thenReturn(first, next);

        ArchiveFactory mockFactory = mock(ArchiveFactory.class);
        when(mockFactory.create(any(InputStream.class))).thenReturn(mockInput);

        File archive = tempFolder.newFile("archive.mock");
        File directory = tempFolder.newFolder("targetDir");
        FileUtils.write(archive, StringUtils.repeat("A", (int) expectedTotalSize));

        Listener listener = useListener ? new Listener() : null;

        // test decompression
        decompress(mockFactory, archive, directory, listener);

        // assertions
        verify(mockFactory, times(1)).create(any(InputStream.class));
        verifyNoMoreInteractions(mockFactory);

        verify(mockInput, times(entries.length)).getNextEntry();
        verify(mockInput, times(1)).close();
        verifyNoMoreInteractions(mockInput);

        assertThatNotificationsAreValid(listener, expectedNotifications, expectedTotalSize);
    }

    abstract protected void decompress(ArchiveFactory mockFactory, File archive, File directory, ProgressListener listener) throws Exception;

    @Theory
    public void testCompress(EntryData[] entries, boolean useListener) throws Exception {
        File archive = tempFolder.newFile("archive.mock");

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

        ArchiveFactory mockFactory = mock(ArchiveFactory.class);
        when(mockFactory.create(any(OutputStream.class))).thenReturn(mockOutput);
        when(mockFactory.getExtension()).thenReturn("mock");

        File[] files = new File[entries.length - 1];
        File sourceDirectory = tempFolder.newFolder("sourceDirectory");
        List<Long> expectedNotifications = new ArrayList<Long>();
        long expectedTotalSize = 0L;
        for (int i = 0; i < files.length; i++) {
            EntryData e = entries[i];
            File file = new File(sourceDirectory, e.name);
            files[i] = file;
            FileUtils.write(file, StringUtils.repeat("A", (int) e.compressedSize));

            expectedTotalSize += file.length();
            expectedNotifications.add(expectedTotalSize);
        }

        Listener listener = useListener ? new Listener() : null;

        // test compression
        compress(mockFactory, sourceDirectory, files, archive, listener);

        // assertions
        verify(mockFactory, times(1)).create(any(OutputStream.class));
        if (callGetExtension) {
            verify(mockFactory, times(1)).getExtension();
        }
        verifyNoMoreInteractions(mockFactory);

        for (File file : files) {
            verify(mockOutput, times(1)).addEntry(eq(file.getAbsolutePath()), any(InputStream.class));
        }
        verify(mockOutput, times(1)).close();
        verifyNoMoreInteractions(mockOutput);

        assertThatNotificationsAreValid(listener, expectedNotifications, expectedTotalSize);
    }

    private void assertThatNotificationsAreValid(Listener listener, List<Long> expectedNotifications, long expectedTotalSize) {
        if (listener != null) {
            if (expectedNotifications.isEmpty()) {
                Assertions.assertThat(listener.notifications).isEmpty();
            } else {
                Assertions.assertThat(listener.notifications).isEqualTo(expectedNotifications);
            }

            Assertions.assertThat(listener.totalSizeCallCount).as("number of calls to totalSizeComputed()").isEqualTo(1);
            Assertions.assertThat(listener.totalSizeCalledBeforeProgress).as("totalSizeComputed() called before progress()").isTrue();
            Assertions.assertThat(listener.totalSize).as("totalSize").isEqualTo(expectedTotalSize);
        }
    }

    abstract protected void compress(ArchiveFactory mockFactory, File sourceDirectory, File[] files, File archive, ProgressListener listener) throws Exception;

    private ArchiveInputStream.Entry[] nextMockEntries(EntryData[] entries) {
        ArchiveInputStream.Entry[] result = new ArchiveInputStream.Entry[entries.length - 1];
        for (int i = 1; i < entries.length; i++) {
            result[i - 1] = newMockEntry(entries[i]);
        }
        return result;
    }

    private ArchiveInputStream.Entry firstMockEntry(EntryData[] entries) {
        return newMockEntry(entries[0]);
    }

    private ArchiveInputStream.Entry newMockEntry(EntryData entry) {
        ArchiveInputStream.Entry result = null;
        if (entry != null) {
            result = mock(ArchiveInputStream.Entry.class);
            when(result.getName()).thenReturn(entry.name);
            when(result.getCompressedSize()).thenReturn(entry.compressedSize);
            when(result.getInput()).thenReturn(new ByteArrayInputStream(new byte[(int) entry.compressedSize]));
        }

        return result;
    }

    private static class EntryData {
        private final String name;
        private final long compressedSize;

        private EntryData(String name, long compressedSize) {
            this.name = name;
            this.compressedSize = compressedSize;
        }
    }

    private static class Listener implements ProgressListener {
        private final List<Long> notifications = new ArrayList<Long>();

        private boolean progressCalled = false;
        private boolean totalSizeCalledBeforeProgress = false;
        private int totalSizeCallCount = 0;

        private long totalSize;

        @Override
        public void totalSizeComputed(long totalSize) {
            totalSizeCallCount++;
            totalSizeCalledBeforeProgress = !progressCalled;
            this.totalSize = totalSize;
        }

        @Override
        public void progress(long totalReadBytes) {
            progressCalled = true;
            notifications.add(totalReadBytes);
        }
    }
}
