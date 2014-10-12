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

import com.google.common.base.Supplier;
import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.Cancellable;
import fr.duminy.jbackup.core.JBackupTest;
import fr.duminy.jbackup.core.TestUtils;
import fr.duminy.jbackup.core.archive.*;
import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactory;
import fr.duminy.jbackup.core.util.FileDeleter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.Test;
import org.junit.experimental.theories.Theory;
import org.mockito.InOrder;
import org.mockito.Matchers;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static fr.duminy.jbackup.core.matchers.Matchers.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.*;

public class BackupTaskTest extends AbstractTaskTest {
    @Theory
    public void testCall(TaskListener listener) throws Throwable {
        final ArchiveParameters archiveParameters = createArchiveParameters();

        testCall(ZipArchiveFactory.INSTANCE, archiveParameters, listener, null, null);

        if (listener != null) {
            InOrder inOrder = inOrder(listener);
            inOrder.verify(listener).taskStarted();
            inOrder.verify(listener).taskFinished(null);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testCall_withCancellable() throws Throwable {
        // prepare test
        Cancellable cancellable = mock(Cancellable.class);
        FileDeleter mockDeleter = mock(FileDeleter.class);
        final Compressor mockCompressor = mock(Compressor.class);
        final FileCollector mockFileCollector = mock(FileCollector.class);
        ArchiveParameters archiveParameters = createArchiveParameters();
        final BackupConfiguration config = toBackupConfiguration(ZipArchiveFactory.INSTANCE, archiveParameters);
        TestableBackupTask task = new TestableBackupTask(config, createDeleterSupplier(mockDeleter), null, cancellable);
        task.setMockFileCollector(mockFileCollector);
        task.setMockCompressor(mockCompressor);
        Path expectedArchive = getExpectedArchive(config, task);

        // test
        task.call();

        // assertions
        InOrder inOrder = inOrder(mockCompressor, mockFileCollector);
        inOrder.verify(mockFileCollector, times(1)).collectFiles(anyListOf(SourceWithPath.class),
                fr.duminy.jbackup.core.matchers.Matchers.eq(archiveParameters, expectedArchive),
                isNull(TaskListener.class), eq(cancellable));
        inOrder.verify(mockCompressor, times(1)).compress(
                fr.duminy.jbackup.core.matchers.Matchers.eq(archiveParameters, expectedArchive),
                anyListOf(SourceWithPath.class), isNull(TaskListener.class), eq(cancellable));
        inOrder.verifyNoMoreInteractions();
        assertThat(TaskTestUtils.getCancellable(task)).isSameAs(cancellable);
    }

    @Theory
    public void testCall_NullArchiveFactory(TaskListener listener) throws Throwable {
        // prepare test
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("ArchiveFactory is null");
        Path archive = tempFolder.newFolder().toPath().resolve("archive.mock");
        final ArchiveParameters archiveParameters = new ArchiveParameters(archive, true);
        final BackupConfiguration config = toBackupConfiguration(null, archiveParameters);
        final FileDeleter mockDeleter = mock(FileDeleter.class);

        // test
        new BackupTask(config, createDeleterSupplier(mockDeleter), listener, null).call();

        // assertions
        verify(mockDeleter, times(1)).registerFile(Matchers.eq(archive));
        verifyNoMoreInteractions(mockDeleter);
    }

    @Theory
    public void testCall_deleteArchiveOnError(TaskListener listener) throws Throwable {
        final IOException exception = new IOException("An unexpected error");
        thrown.expect(exception.getClass());
        thrown.expectMessage(exception.getMessage());

        final ArchiveParameters archiveParameters = createArchiveParameters();

        testCall(ZipArchiveFactory.INSTANCE, archiveParameters, listener, exception, null);

        verify(listener).taskStarted();
        verify(listener).taskFinished(eq(exception));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testCall_deleteArchiveOnCancel() throws Throwable {
        Cancellable cancellable = mock(Cancellable.class);
        when(cancellable.isCancelled()).thenReturn(true);
        ArchiveParameters archiveParameters = createArchiveParameters();

        testCall(ZipArchiveFactory.INSTANCE, archiveParameters, null, null, cancellable);
    }

    private void testCall(ArchiveFactory mockFactory, ArchiveParameters archiveParameters, TaskListener listener,
                          Exception exception, Cancellable cancellable) throws Throwable {
        // prepare test
        final FileDeleter mockDeleter = mock(FileDeleter.class);
        final BackupConfiguration config = toBackupConfiguration(mockFactory, archiveParameters);
        final Compressor mockCompressor = mock(Compressor.class);
        final FileCollector mockFileCollector = mock(FileCollector.class);

        TestableBackupTask task = new TestableBackupTask(config, createDeleterSupplier(mockDeleter), listener, cancellable);
        task.setMockCompressor(mockCompressor);
        task.setMockFileCollector(mockFileCollector);
        Path expectedArchive = getExpectedArchive(config, task);

        if (exception != null) {
            doThrow(new ArchiveException(exception)).when(mockCompressor).
                    compress(eq(archiveParameters, expectedArchive), anyListOf(SourceWithPath.class), eq(listener),
                            isNull(Cancellable.class));
        }

        // test
        try {
            task.call();
        } catch (ArchiveException e) {
            throw e.getCause();
        } finally {
            // assertions
            InOrder inOrder = inOrder(mockDeleter, mockCompressor, mockFileCollector);
            if ((cancellable != null) || (exception != null)) {
                inOrder.verify(mockDeleter, times(1)).registerFile(org.mockito.Matchers.eq(expectedArchive));
                inOrder.verify(mockDeleter, times(1)).deleteAll();
            } else {
                inOrder.verify(mockDeleter, times(1)).registerFile(org.mockito.Matchers.eq(expectedArchive));
                inOrder.verify(mockFileCollector, times(1)).collectFiles(anyListOf(SourceWithPath.class),
                        eq(archiveParameters, expectedArchive), eq(listener), isNull(Cancellable.class));
                inOrder.verify(mockCompressor, times(1)).compress(eq(archiveParameters, expectedArchive),
                        anyListOf(SourceWithPath.class), eq(listener), isNull(Cancellable.class));
            }
            inOrder.verifyNoMoreInteractions();
        }
    }

    private Path getExpectedArchive(BackupConfiguration config, TestableBackupTask task) {
        return Paths.get(config.getTargetDirectory()).resolve(task.getGeneratedName());
    }

    private BackupConfiguration toBackupConfiguration(ArchiveFactory mockFactory, ArchiveParameters archiveParameters) {
        final BackupConfiguration config = new BackupConfiguration();
        config.setName("testBackupTask");
        config.setRelativeEntries(archiveParameters.isRelativeEntries());
        final Path archiveDirectory2 = archiveParameters.getArchive().getParent().toAbsolutePath();
        config.setTargetDirectory(archiveDirectory2.toString());
        for (ArchiveParameters.Source source : archiveParameters.getSources()) {
            String dirFilter = toJexlExpression(source.getDirFilter());
            String fileFilter = toJexlExpression(source.getFileFilter());
            final Path sourcePath = source.getSource().toAbsolutePath();
            config.addSource(sourcePath, dirFilter, fileFilter);
        }
        config.setArchiveFactory(mockFactory);
        return config;
    }

    protected static final String getName(IOFileFilter filter) {
        if (filter instanceof JBackupTest.CustomNameFileFilter) {
            return ((JBackupTest.CustomNameFileFilter) filter).getName();
        } else {
            return null;
        }
    }

    private String toJexlExpression(IOFileFilter filter) {
        String fileName = getName(filter);
        return (fileName == null) ? null : "file.name=='" + fileName + "'";
    }

    static Supplier<FileDeleter> createDeleterSupplier(final FileDeleter mockDeleter) {
        return new Supplier<FileDeleter>() {
            @Override
            public FileDeleter get() {
                return mockDeleter;
            }
        };
    }

    private ArchiveParameters createArchiveParameters() throws IOException {
        Path archive = tempFolder.newFolder().toPath().resolve("archive.mock");
        final ArchiveParameters archiveParameters = new ArchiveParameters(archive, true);
        archiveParameters.addSource(TestUtils.createSourceWithFiles(tempFolder, "source"));
        return archiveParameters;
    }

    private static class TestableBackupTask extends BackupTask {
        private final String generatedName = "archive.mock";
        private FileCollector mockFileCollector;
        private Compressor mockCompressor;

        public TestableBackupTask(BackupConfiguration config, Supplier<FileDeleter> deleterSupplier,
                                  TaskListener listener, Cancellable cancellable) {
            super(config, deleterSupplier, listener, cancellable);
        }

        @Override
        FileCollector createFileCollector() {
            if (mockFileCollector == null) {
                return super.createFileCollector();
            }
            return mockFileCollector;
        }

        @Override
        Compressor createCompressor(ArchiveFactory factory) {
            if (mockCompressor == null) {
                return super.createCompressor(factory);
            }
            return mockCompressor;
        }

        @Override
        protected String generateName(String configName, ArchiveFactory factory) {
            return generatedName;
        }

        public void setMockFileCollector(FileCollector mockFileCollector) {
            this.mockFileCollector = mockFileCollector;
        }

        public void setMockCompressor(Compressor mockCompressor) {
            this.mockCompressor = mockCompressor;
        }

        public String getGeneratedName() {
            return generatedName;
        }
    }
}