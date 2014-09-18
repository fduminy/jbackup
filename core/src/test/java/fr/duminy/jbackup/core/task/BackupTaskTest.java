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
import fr.duminy.components.swing.form.StringPathTypeMapper;
import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.Cancellable;
import fr.duminy.jbackup.core.JBackupTest;
import fr.duminy.jbackup.core.TestUtils;
import fr.duminy.jbackup.core.archive.*;
import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactory;
import fr.duminy.jbackup.core.util.FileDeleter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.experimental.theories.Theory;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static fr.duminy.jbackup.core.matchers.Matchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.*;

public class BackupTaskTest extends AbstractTaskTest {
    @Theory
    public void testCall(ProgressListener listener) throws Throwable {
        Path archive = tempFolder.newFolder().toPath().resolve("archive.mock");
        final ArchiveParameters archiveParameters = new ArchiveParameters(archive, true);
        Path source = tempFolder.newFolder("source").toPath();
        Files.createDirectories(source);
        Path file = source.resolve("file");
        TestUtils.createFile(file, 10);
        archiveParameters.addSource(source);

        testCall(ZipArchiveFactory.INSTANCE, archiveParameters, listener, null);

        if (listener != null) {
            InOrder inOrder = Mockito.inOrder(listener);
            inOrder.verify(listener).taskStarted();
            inOrder.verify(listener).taskFinished(null);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Theory
    public void testCall_NullArchiveFactory(ProgressListener listener) throws Throwable {
        // prepare test
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("ArchiveFactory is null");
        Path archive = tempFolder.newFolder().toPath().resolve("archive.mock");
        final ArchiveParameters archiveParameters = new ArchiveParameters(archive, true);
        final BackupConfiguration config = toBackupConfiguration(null, archiveParameters);
        final FileDeleter mockDeleter = mock(FileDeleter.class);

        // test
        new BackupTask(config, createDeleterSupplier(mockDeleter), listener).call();

        // assertions
        verify(mockDeleter, times(1)).registerFile(Matchers.eq(archive));
        verifyNoMoreInteractions(mockDeleter);
    }

    @Theory
    public void testCall_deleteArchiveOnError(ProgressListener listener) throws Throwable {
        final IOException exception = new IOException("An unexpected error");
        thrown.expect(exception.getClass());
        thrown.expectMessage(exception.getMessage());

        Path archive = tempFolder.newFolder().toPath().resolve("archive.mock");
        final ArchiveParameters archiveParameters = new ArchiveParameters(archive, true);
        Path source = tempFolder.newFolder("source").toPath();
        Files.createDirectories(source);
        Path file = source.resolve("file");
        TestUtils.createFile(file, 10);
        archiveParameters.addSource(source);

        testCall(ZipArchiveFactory.INSTANCE, archiveParameters, listener, exception);

        verify(listener).taskStarted();
        verify(listener).taskFinished(eq(exception));
        verifyNoMoreInteractions(listener);
    }

    private void testCall(ArchiveFactory mockFactory, ArchiveParameters archiveParameters, ProgressListener listener,
                          Exception exception) throws Throwable {
        // prepare test
        final String[] generatedName = new String[1];
        final FileDeleter mockDeleter = mock(FileDeleter.class);
        final BackupConfiguration config = toBackupConfiguration(mockFactory, archiveParameters);

        final Compressor mockCompressor = mock(Compressor.class);
        final FileCollector mockFileCollector = mock(FileCollector.class);
        if (exception != null) {
            doThrow(new ArchiveException(exception)).when(mockCompressor).
                    compress(any(ArchiveParameters.class), anyListOf(SourceWithPath.class), eq(listener), any(Cancellable.class));
        }

        BackupTask task = new BackupTask(config, createDeleterSupplier(mockDeleter), listener) {
            @Override
            Compressor createCompressor(ArchiveFactory factory) {
                return mockCompressor;
            }

            @Override
            FileCollector createFileCollector() {
                return mockFileCollector;
            }

            @Override
            protected String generateName(String configName, ArchiveFactory factory) {
                generatedName[0] = super.generateName(configName, factory);
                return generatedName[0];
            }
        };

        // test
        try {
            task.call();
        } catch (ArchiveException e) {
            throw e.getCause();
        } finally {
            // assertions
            Path expectedArchive = Paths.get(config.getTargetDirectory()).resolve(generatedName[0]);
            InOrder inOrder = Mockito.inOrder(mockDeleter, mockCompressor, mockFileCollector);
            if (exception != null) {
                inOrder.verify(mockDeleter, times(1)).registerFile(org.mockito.Matchers.eq(expectedArchive));
                inOrder.verify(mockDeleter, times(1)).deleteAll();
            } else {
                inOrder.verify(mockDeleter, times(1)).registerFile(org.mockito.Matchers.eq(expectedArchive));
                inOrder.verify(mockFileCollector, times(1)).collectFiles(anyListOf(SourceWithPath.class), eq(archiveParameters, expectedArchive), eq(listener), any(Cancellable.class));
                inOrder.verify(mockCompressor, times(1)).compress(eq(archiveParameters, expectedArchive), anyListOf(SourceWithPath.class), eq(listener), any(Cancellable.class));
            }
            inOrder.verifyNoMoreInteractions();
        }
    }

    private BackupConfiguration toBackupConfiguration(ArchiveFactory mockFactory, ArchiveParameters archiveParameters) {
        final BackupConfiguration config = new BackupConfiguration();
        config.setName("testBackupTask");
        config.setRelativeEntries(archiveParameters.isRelativeEntries());
        final Path archiveDirectory2 = archiveParameters.getArchive().getParent().toAbsolutePath();
        config.setTargetDirectory(StringPathTypeMapper.toString(archiveDirectory2));
        for (ArchiveParameters.Source source : archiveParameters.getSources()) {
            String dirFilter = toJexlExpression(source.getDirFilter());
            String fileFilter = toJexlExpression(source.getFileFilter());
            final Path sourcePath = Paths.get(StringPathTypeMapper.toString(source.getSource().toAbsolutePath()));
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
}