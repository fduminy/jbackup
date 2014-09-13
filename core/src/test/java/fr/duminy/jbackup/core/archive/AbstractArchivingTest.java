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
import fr.duminy.jbackup.core.TestUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static fr.duminy.jbackup.core.TestUtils.createFile;
import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(Theories.class)
abstract public class AbstractArchivingTest {
    @DataPoint
    public static final EntryType RELATIVE = EntryType.RELATIVE;
    @DataPoint
    public static final EntryType ABSOLUTE = EntryType.ABSOLUTE;

    @DataPoint
    public static final boolean NO_LISTENER = false;
    @DataPoint
    public static final boolean LISTENER = true;

    @DataPoints
    public static final ErrorType[] ERROR_TYPES = ErrorType.values();

    @DataPoint
    public static final Data SRC_FILE_WITHOUT_FILTER = group(withoutFilter().andSourceFile("file1").acceptAll());
    @DataPoint
    public static final Data SRC_FILE_WITH_DIR_FILTER = group(withDirFilter("dir123").andSourceFile("file1").acceptAll());
    @DataPoint
    public static final Data SRC_FILE_WITH_DIR_FILTER2 = group(withDirFilter("${parentDir}").andSourceFile("file1").acceptAll());
    @DataPoint
    public static final Data SRC_FILE_WITH_FILE_FILTER = group(withDirFilter("file1").andSourceFile("file1").acceptAll());
    @DataPoint
    public static final Data SRC_FILE_WITH_FILE_FILTER2 = group(withFileFilter("file2").andSourceFile("file1").acceptAll());

    @DataPoint
    public static final Data SRC_DIR_WITHOUT_FILTER_AND_FILES = group(withoutFilter().andSourceDir("dir1").acceptAll());
    @DataPoint
    public static final Data SRC_DIR_WITHOUT_FILTER = group(withoutFilter().andSourceDir("dir1").accept("dir2/file2", "dir3/file3"));
    @DataPoint
    public static final Data SRC_DIR_WITH_DIR_FILTER = group(withDirFilter("dir2").andSourceDir("dir1").accept("dir2/file2").butReject("dir3/file3"));
    @DataPoint
    public static final Data SRC_DIR_WITH_FILE_FILTER = group(withFileFilter("file2").andSourceDir("dir1").accept("dir2/file2").butReject("dir3/file3"));

    @DataPoint
    public static final Data SRC_FILE_AND_SRC_DIR = group(
            withFileFilter("file3").andSourceFile("file1").acceptAll(),
            withDirFilter("dir2").andSourceDir("dir1").accept("dir2/file2").butReject("dir3/file3"));
    @DataPoint
    public static final Data TWO_SRC_FILES = group(
            withFileFilter("file3").andSourceFile("file4").acceptAll(),
            withFileFilter("file4").andSourceFile("file3").acceptAll());
    @DataPoint
    public static final Data TWO_SRC_DIRS = group(
            withDirFilter("dir5").andSourceDir("dir1").accept("dir5/file5").butReject("dir6/file6"),
            withDirFilter("dir6").andSourceDir("dir2").accept("dir6/file6").butReject("dir5/file5"));

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    protected boolean createDirectory = true;

    Path createArchivePath() throws IOException {
        return tempFolder.newFile("archive.mock").toPath();
    }

    Path createBaseDirectory() {
        return tempFolder.newFolder("baseDirectory").toPath().toAbsolutePath();
    }

    ArchiveFactory createMockArchiveFactory(ArchiveOutputStream mockOutput) throws Exception {
        ArchiveFactory mockFactory = mock(ArchiveFactory.class);
        when(mockFactory.create(any(OutputStream.class))).thenReturn(mockOutput);
        when(mockFactory.getExtension()).thenReturn("mock");
        return mockFactory;
    }

    protected final void assertThatNotificationsAreValid(ProgressListener listener, List<String> actualEntries, Map<String, Path> expectedEntryToFile, ErrorType errorType) throws IOException {
        List<Long> expectedNotifications = new ArrayList<>();
        long expectedTotalSize = 0L;
        for (String actualEntry : actualEntries) {
            Path path = expectedEntryToFile.get(actualEntry);
            expectedTotalSize += Files.size(path);
            expectedNotifications.add(expectedTotalSize);
        }
        assertThatNotificationsAreValid(listener, expectedNotifications, expectedTotalSize, errorType);
    }

    protected final void assertThatNotificationsAreValid(ProgressListener listener, List<Long> expectedNotifications, long expectedTotalSize, ErrorType errorType) throws IOException {
        if (listener != null) {
            InOrder inOrder = inOrder(listener);

            // 1 - taskStarted
            inOrder.verify(listener, never()).taskStarted();

            if (!errorType.isError()) {
                // 2 - totalSizeComputed
                inOrder.verify(listener, times(1)).totalSizeComputed(expectedTotalSize);

                // 3 - progress
                ArgumentCaptor<Long> argument = ArgumentCaptor.forClass(Long.class);
                inOrder.verify(listener, times(expectedNotifications.size())).progress(argument.capture());
                assertThat(argument.getAllValues()).as("progress notifications").isEqualTo(expectedNotifications);
            }

            // 4 - taskFinished
            inOrder.verify(listener, never()).taskFinished(any(Throwable.class));

            inOrder.verifyNoMoreInteractions();
        }
    }

    private static class Filter {
        private final String dirFilter;
        private final String fileFilter;

        private Filter(String dirFilter, String fileFilter) {
            this.dirFilter = dirFilter;
            this.fileFilter = fileFilter;
        }

        public SourceFile andSourceFile(String file) {
            return new SourceFile(this, file);
        }

        public SourceDir andSourceDir(String dir) {
            return new SourceDir(this, dir);
        }
    }

    private static Filter withoutFilter() {
        return new Filter(null, null);
    }

    private static Filter withDirFilter(String dirFilter) {
        return new Filter(dirFilter, null);
    }

    private static Filter withFileFilter(String fileFilter) {
        return new Filter(null, fileFilter);
    }

    private static abstract class Source {
        private final Filter filter;

        private Source(Filter filter) {
            this.filter = filter;
        }

        public DataSource acceptAll() {
            return new DataSource(this);
        }

        public DataSource accept(String... acceptedFiles) {
            return new DataSource(this, acceptedFiles);
        }

        abstract public Path getPath(Path baseDirectory);

        abstract public Path create(Path baseDirectory) throws IOException;
    }

    private static class SourceFile extends Source {
        private final String file;

        private SourceFile(Filter filter, String file) {
            super(filter);
            this.file = file;
        }

        @Override
        public Path getPath(Path baseDirectory) {
            return baseDirectory.resolve(this.file);
        }

        @Override
        public Path create(Path baseDirectory) throws IOException {
            final Path file = getPath(baseDirectory);
            createFile(file, 123L);
            return file;
        }
    }

    private static class SourceDir extends Source {
        private final String dir;

        private SourceDir(Filter filter, String dir) {
            super(filter);
            this.dir = dir;
        }

        @Override
        public Path getPath(Path baseDirectory) {
            return baseDirectory.resolve(this.dir);
        }

        @Override
        public Path create(Path baseDirectory) throws IOException {
            final Path dir = getPath(baseDirectory);
            Files.createDirectories(dir);
            return dir;
        }
    }

    public static Data group(DataSource... dataSources) {
        return new Data(dataSources);
    }

    protected final static class Data {
        protected final List<DataSource> dataSources;

        private Data(DataSource[] dataSources) {
            this.dataSources = Arrays.asList(dataSources);
        }

        public Map<Path, List<Path>> createFiles(Path baseDirectory, ArchiveParameters archiveParameters) throws IOException {
            Map<Path, List<Path>> acceptedFilesBySource = new HashMap<>();
            for (DataSource dataSource : dataSources) {
                List<Path> acceptedFiles = dataSource.createFiles(baseDirectory, archiveParameters);
                acceptedFilesBySource.put(dataSource.source.getPath(baseDirectory), acceptedFiles);
            }
            return acceptedFilesBySource;
        }
    }

    protected final static class DataSource {
        private final Source source;
        private final String[] acceptedFiles;
        private String[] rejectedFiles = new String[0];

        public DataSource(Source source, String... acceptedFiles) {
            this.source = source;
            this.acceptedFiles = acceptedFiles;
        }

        public DataSource butReject(String... rejectedFiles) {
            this.rejectedFiles = rejectedFiles;
            return this;
        }

        public Entries entries() {
            Entries entries = new Entries();
            for (String entry : acceptedFiles) {
                entries.file(entry);
            }
            return entries;
        }

        public Entries rejectedEntries() {
            Entries entries = new Entries();
            for (String entry : rejectedFiles) {
                entries.file(entry);
            }
            return entries;
        }

        private static Path createFile(Path sourceDirectory, Entry entry) throws IOException {
            Path file = sourceDirectory.resolve(entry.name);
            TestUtils.createFile(file, entry.compressedSize);
            return file;
        }

        public List<Path> createFiles(Path baseDirectory, ArchiveParameters archiveParameters) throws IOException {
            Path source = this.source.create(baseDirectory);

            Filter filter = this.source.filter;
            IOFileFilter dirFilter = (filter.dirFilter == null) ? trueFileFilter() : new JBackupTest.CustomNameFileFilter(filter.dirFilter);
            IOFileFilter fileFilter = (filter.fileFilter == null) ? trueFileFilter() : new JBackupTest.CustomNameFileFilter(filter.fileFilter);
            archiveParameters.addSource(source, dirFilter, fileFilter);

            List<Path> acceptedFiles;
            if (Files.isDirectory(source)) {
                acceptedFiles = new ArrayList<>();
                for (Entry entry : entries()) {
                    acceptedFiles.add(createFile(source, entry));
                }
                for (Entry entry : rejectedEntries()) {
                    createFile(source, entry);
                }
            } else {
                acceptedFiles = Collections.singletonList(source);
            }

            return acceptedFiles;
        }
    }

    protected final static class Entry {
        private final String name;
        protected final long compressedSize;

        private Entry(String name, long compressedSize) {
            this.name = name;
            this.compressedSize = compressedSize;
        }
    }

    protected final static class Entries implements Iterable<Entry> {
        private long compressedSize = 1;
        private final List<Entry> entries = new ArrayList<>();

        public Entries file(String file) {
            entries.add(new Entry(file, compressedSize++));
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

    protected static enum ErrorType {
        NO_ERROR {
            @Override
            public Class<? extends Throwable> getExpectedErrorClass() {
                return null;
            }
        },
        ACCESS_DENIED {
            @Override
            public Class<? extends Throwable> getExpectedErrorClass() {
                return AccessDeniedException.class;
            }

            @Override
            public void setUp(List<Path> protectedFiles) {
                TestUtils.setReadable(protectedFiles, false);
            }

            @Override
            public void tearDown(List<Path> protectedFiles) {
                TestUtils.setReadable(protectedFiles, true);
            }
        };

        public void verifyExpected(Throwable t) throws Throwable {
            if (t instanceof ArchiveException) {
                t = t.getCause();
            }
            if (!isError() || !t.getClass().equals(getExpectedErrorClass())) {
                throw t;
            }
        }

        abstract public Class<? extends Throwable> getExpectedErrorClass();

        public final boolean isError() {
            return getExpectedErrorClass() != null;
        }

        public void setUp(List<Path> protectedFiles) {
        }

        public final void setUp(Path protectedFile) {
            setUp(Collections.singletonList(protectedFile));
        }

        public void tearDown(List<Path> protectedFiles) {
        }

        public final void tearDown(Path protectedFile) {
            tearDown(Collections.singletonList(protectedFile));
        }
    }

    protected static enum EntryType {
        RELATIVE,
        ABSOLUTE;
    }
}
