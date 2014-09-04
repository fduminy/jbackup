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
package fr.duminy.jbackup.core;

import fr.duminy.components.swing.form.StringPathTypeMapper;
import fr.duminy.jbackup.core.archive.*;
import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactory;
import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactoryTest;
import fr.duminy.jbackup.core.util.FileDeleter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for class {@link fr.duminy.jbackup.core.JBackup}.
 */
public class JBackupTest extends AbstractArchivingTest {
    private boolean error = false;

    @Test
    public void testCompress_NullArchiveFactory() throws Throwable {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("ArchiveFactory is null");
        Path archive = tempFolder.newFolder().toPath().resolve("archive.mock");

        final ArchiveParameters archiveParameters = new ArchiveParameters(archive, true);
        compress(null, archiveParameters, Collections.<Path>emptyList(), mock(ProgressListener.class), false);
    }

    @Test
    public void testCompress_deleteArchiveOnError() throws Throwable {
        Path archive = tempFolder.newFolder().toPath().resolve("archive.mock");

        final ArchiveParameters archiveParameters = new ArchiveParameters(archive, true);
        Path source = tempFolder.newFolder("source").toPath();
        Files.createDirectories(source);
        Path file = source.resolve("file");
        Files.write(file, StringUtils.repeat("A", 10).getBytes());
        archiveParameters.addSource(source);

        error = true;
        ProgressListener listener = mock(ProgressListener.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                throw new IOException("An unexpected error");
            }
        }).when(listener).progress(anyLong());
        compress(ZipArchiveFactory.INSTANCE, archiveParameters, Collections.singletonList(file), listener, false);
    }

    @Test
    public void testDecompress_deleteFilesOnError() throws Throwable {
        Path archive = ZipArchiveFactoryTest.createArchive(tempFolder.newFolder().toPath());
        Path targetDirectory = tempFolder.newFolder("targetDirectory").toPath();

        error = true;
        ProgressListener listener = mock(ProgressListener.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                throw new IOException("An unexpected error");
            }
        }).when(listener).progress(anyLong());

        decompress(ZipArchiveFactory.INSTANCE, archive, targetDirectory, listener);
    }

    @Override
    protected void decompress(ArchiveFactory mockFactory, Path archive, Path targetDirectory, ProgressListener listener) throws Throwable {
        final FileDeleter mockDeleter = mock(FileDeleter.class);
        JBackup jbackup = new JBackup() {
            @Override
            FileDeleter createFileDeleter() {
                return mockDeleter;
            }
        };
        BackupConfiguration config = new BackupConfiguration();
        config.setName("testDecompress");
        config.setTargetDirectory(null); // unused
        config.setArchiveFactory(mockFactory);

        Future<Void> future;
        if (listener == null) {
            future = jbackup.restore(config, archive, targetDirectory);
        } else {
            future = jbackup.restore(config, archive, targetDirectory, listener);
        }

        waitResult(future);
        jbackup.shutdown();

        verify(mockDeleter, times(1)).registerDirectory(eq(targetDirectory));
        if (error) {
            verify(mockDeleter, times(1)).deleteAll();
        }
        verifyNoMoreInteractions(mockDeleter);
    }

    @Override
    protected void compress(ArchiveFactory mockFactory, ArchiveParameters archiveParameters, List<Path> expectedFiles, ProgressListener listener, boolean errorIsExpected) throws Throwable {
        final List<Path> actualFiles = new ArrayList<>();
        final String[] generatedName = new String[1];
        final FileDeleter mockDeleter = mock(FileDeleter.class);
        JBackup jbackup = new JBackup() {
            @Override
            String generateName(String configName, ArchiveFactory factory) {
                generatedName[0] = super.generateName(configName, factory);
                return generatedName[0];
            }

            @Override
            Archiver createArchiver(ArchiveFactory factory) {
                return new Archiver(factory) {
                    @Override
                    protected void compress(ArchiveParameters archiveParameters, ProgressListener listener, Map<Path, List<Path>> filesBySources) throws ArchiverException {
                        for (List<Path> files : filesBySources.values()) {
                            actualFiles.addAll(files);
                        }

                        // now compress files in the order given by expectedFiles
                        // (otherwise the test will fail on some platforms)
                        super.compress(archiveParameters, listener, filesBySources);
                    }
                };
            }

            @Override
            FileDeleter createFileDeleter() {
                return mockDeleter;
            }
        };

        final BackupConfiguration config = new BackupConfiguration();
        config.setName("testCompress");
        config.setRelativeEntries(archiveParameters.isRelativeEntries());
        final Path archiveDirectory = archiveParameters.getArchive().getParent().toAbsolutePath();
        config.setTargetDirectory(StringPathTypeMapper.toString(archiveDirectory));
        for (ArchiveParameters.Source source : archiveParameters.getSources()) {
            String dirFilter = toJexlExpression(source.getDirFilter());
            String fileFilter = toJexlExpression(source.getFileFilter());
            final Path sourcePath = Paths.get(StringPathTypeMapper.toString(source.getSource().toAbsolutePath()));
            config.addSource(sourcePath, dirFilter, fileFilter);
        }
        config.setArchiveFactory(mockFactory);

        Future<Void> future;
        if (listener == null) {
            future = jbackup.backup(config);
        } else {
            future = jbackup.backup(config, listener);
        }

        waitResult(future);

        // ensure that actual files are as expected
        if (!errorIsExpected) {
            expectedFiles = new ArrayList<>(expectedFiles);
            Collections.sort(actualFiles);
            Collections.sort(expectedFiles);
            assertThat(actualFiles).isEqualTo(expectedFiles);
        }

        jbackup.shutdown();

        Path archive = archiveDirectory.resolve(generatedName[0]);
        verify(mockDeleter, times(1)).registerFile(eq(archive));
        if (error) {
            verify(mockDeleter, times(1)).deleteAll();
        }
        verifyNoMoreInteractions(mockDeleter);
    }

    private String toJexlExpression(IOFileFilter filter) {
        String fileName = getName(filter);
        return (fileName == null) ? null : "file.name=='" + fileName + "'";
    }

    protected void waitResult(Future<Void> future) throws Throwable {
        assertThat(future).isNotNull();
        try {
            future.get(); // block until finished and maybe throw an Exception if task has thrown one.
        } catch (ExecutionException e) {
            if (!error) {
                throw e.getCause();
            }
        }
    }
}
