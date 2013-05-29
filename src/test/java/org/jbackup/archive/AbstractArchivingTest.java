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
import org.junit.Rule;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(Theories.class)
abstract public class AbstractArchivingTest {
    @DataPoint
    public static final String[] NO_ENTRY = {null};
    @DataPoint
    public static final String[] ONE_ENTRY = {"entry1", null};
    @DataPoint
    public static final String[] TWO_ENTRIES = new String[]{"entry1", "entry2", null};

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    private final boolean callGetExtension;

    protected AbstractArchivingTest(boolean callGetExtension) {
        this.callGetExtension = callGetExtension;
    }

    @Theory
    public void testDecompress(String[] entries) throws Exception {
        // preparation of archiver & mocks
        ArchiveInputStream mockInput = mock(ArchiveInputStream.class);
        ArchiveInputStream.Entry first = firstMockEntry(entries);
        ArchiveInputStream.Entry[] next = nextMockEntries(entries);
        when(mockInput.getNextEntry()).thenReturn(first, next);

        ArchiveFactory mockFactory = mock(ArchiveFactory.class);
        when(mockFactory.create(any(InputStream.class))).thenReturn(mockInput);

        File archive = tempFolder.newFile("archive.mock");
        File directory = tempFolder.newFolder("targetDir");

        // test decompression
        decompress(mockFactory, archive, directory);

        // assertions
        verify(mockFactory, times(1)).create(any(InputStream.class));
        verifyNoMoreInteractions(mockFactory);

        verify(mockInput, times(entries.length)).getNextEntry();
        verify(mockInput, times(1)).close();
        verifyNoMoreInteractions(mockInput);
    }

    abstract protected void decompress(ArchiveFactory mockFactory, File archive, File directory) throws Exception;

    @Theory
    public void testCompress(String[] entries) throws Exception {
        // preparation of archiver & mocks
        ArchiveOutputStream mockOutput = mock(ArchiveOutputStream.class);

        ArchiveFactory mockFactory = mock(ArchiveFactory.class);
        when(mockFactory.create(any(OutputStream.class))).thenReturn(mockOutput);
        when(mockFactory.getExtension()).thenReturn("mock");

        File[] files = new File[entries.length - 1];
        File sourceDirectory = tempFolder.newFolder("sourceDirectory");
        for (int i = 0; i < files.length; i++) {
            files[i] = new File(sourceDirectory, entries[i]);
            FileUtils.write(files[i], "some data");
        }
        File archive = tempFolder.newFile("archive.mock");

        // test compression
        compress(mockFactory, sourceDirectory, files, archive);

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
    }

    abstract protected void compress(ArchiveFactory mockFactory, File sourceDirectory, File[] files, File archive) throws Exception;

    private ArchiveInputStream.Entry[] nextMockEntries(String[] entries) {
        ArchiveInputStream.Entry[] result = new ArchiveInputStream.Entry[entries.length - 1];
        for (int i = 1; i < entries.length; i++) {
            result[i - 1] = newMockEntry(entries[i]);
        }
        return result;
    }

    private ArchiveInputStream.Entry firstMockEntry(String[] entries) {
        return newMockEntry(entries[0]);
    }

    private ArchiveInputStream.Entry newMockEntry(String entryName) {
        ArchiveInputStream.Entry entry = null;
        if (entryName != null) {
            entry = mock(ArchiveInputStream.Entry.class);
            when(entry.getName()).thenReturn(entryName);
            when(entry.getInput()).thenReturn(new ByteArrayInputStream(new byte[0]));
        }

        return entry;
    }
}
