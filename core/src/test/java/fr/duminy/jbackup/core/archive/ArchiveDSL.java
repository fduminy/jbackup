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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static fr.duminy.jbackup.core.TestUtils.createFile;
import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Domain Specific Language (DSL) for testing archives.
 */
public class ArchiveDSL {
    static class Filter {
        private final String dirFilter;
        private final String fileFilter;

        private Filter(String dirFilter, String fileFilter) {
            this.dirFilter = dirFilter;
            this.fileFilter = fileFilter;
        }

        SourceFile andSourceFile(String file) {
            return new SourceFile(this, file);
        }

        SourceDir andSourceDir(String dir) {
            return new SourceDir(this, dir);
        }
    }

    static Filter withoutFilter() {
        return new Filter(null, null);
    }

    static Filter withDirFilter(String dirFilter) {
        return new Filter(dirFilter, null);
    }

    static Filter withFileFilter(String fileFilter) {
        return new Filter(null, fileFilter);
    }

    static abstract class Source {
        private final Filter filter;

        private Source(Filter filter) {
            this.filter = filter;
        }

        DataSource accept(String... acceptedFiles) {
            return new DataSource(this, acceptedFiles);
        }

        abstract Path getPath(Path baseDirectory);

        abstract Path create(Path baseDirectory) throws IOException;
    }

    static class SourceFile extends Source {
        private final String file;

        private SourceFile(Filter filter, String file) {
            super(filter);
            this.file = file;
        }

        public DataSource acceptAll() {
            return accept(file);
        }

        @Override
        Path getPath(Path baseDirectory) {
            return baseDirectory.resolve(this.file);
        }

        @Override
        Path create(Path baseDirectory) throws IOException {
            final Path file = getPath(baseDirectory);
            createFile(file, 123L);
            return file;
        }
    }

    static class SourceDir extends Source {
        private final String dir;

        private SourceDir(Filter filter, String dir) {
            super(filter);
            this.dir = dir;
        }

        public DataSource acceptAll() {
            return accept(); // used only for empty source directories
        }

        @Override
        Path getPath(Path baseDirectory) {
            return baseDirectory.resolve(this.dir);
        }

        @Override
        Path create(Path baseDirectory) throws IOException {
            final Path dir = getPath(baseDirectory);
            Files.createDirectories(dir);
            return dir;
        }
    }

    static Data group(DataSource... dataSources) {
        return new Data(dataSources);
    }

    final static class Data {
        protected final List<DataSource> dataSources;

        private Data(DataSource[] dataSources) {
            this.dataSources = Arrays.asList(dataSources);
        }

        Map<Path, List<Path>> createFiles(Path baseDirectory, ArchiveParameters archiveParameters) throws IOException {
            Map<Path, List<Path>> acceptedFilesBySource = new HashMap<>();
            for (DataSource dataSource : dataSources) {
                List<Path> acceptedFiles = dataSource.createFiles(baseDirectory, archiveParameters);
                acceptedFilesBySource.put(dataSource.source.getPath(baseDirectory), acceptedFiles);
            }
            return acceptedFilesBySource;
        }
    }

    final static class DataSource {
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
                entries.addFile(entry);
            }
            return entries;
        }

        public Entries rejectedEntries() {
            Entries entries = new Entries();
            for (String entry : rejectedFiles) {
                entries.addFile(entry);
            }
            return entries;
        }

        private static Path createFile(Path sourceDirectory, Entry entry) throws IOException {
            Path file = sourceDirectory.resolve(entry.getName());
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

    final static class Entry {
        private final String name;
        private final long compressedSize;

        private Entry(String name, long compressedSize) {
            this.name = name;
            this.compressedSize = compressedSize;
        }

        public String getName() {
            return name;
        }

        public long getCompressedSize() {
            return compressedSize;
        }
    }

    final static class Entries implements Iterable<Entry> {
        private long compressedSize = 1;
        private final List<Entry> entries = new ArrayList<>();

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

        public int size() {
            return entries.size();
        }

        private void addFile(String file) {
            entries.add(new Entry(file, compressedSize++));
        }

        private ArchiveInputStream.Entry newMockEntry(Entry entry) {
            ArchiveInputStream.Entry result = null;
            if (entry != null) {
                result = mock(ArchiveInputStream.Entry.class);
                when(result.getName()).thenReturn(entry.getName());
                when(result.getCompressedSize()).thenReturn(entry.compressedSize);
                when(result.getInput()).thenReturn(new ByteArrayInputStream(new byte[(int) entry.compressedSize]));
            }

            return result;
        }
    }
}
