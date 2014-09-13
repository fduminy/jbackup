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
import org.apache.commons.io.filefilter.NameFileFilter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static fr.duminy.jbackup.core.JBackup.TerminationListener;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

/**
 * Tests for class {@link fr.duminy.jbackup.core.JBackup}.
 */
public class JBackupTest {
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

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
        TestUtils.createFile(file, 10);
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
    public void testShutdown_withoutListener_alreadyTerminatedTask() throws Throwable {
        testShutdownWithoutListener(false);
    }

    @Test
    public void testShutdown_withoutListener_longTask() throws Throwable {
        testShutdownWithoutListener(true);
    }

    @Test
    public void testShutdown_withListener_alreadyTerminatedTask() throws Throwable {
        testShutdownWithListener(false);
    }

    @Test
    public void testShutdown_withListener_longTask() throws Throwable {
        testShutdownWithListener(true);
    }

    private void testShutdownWithListener(boolean longTask) throws Throwable {
        testShutdown(longTask, true);
    }

    private void testShutdownWithoutListener(boolean longTask) throws Throwable {
        testShutdown(longTask, false);
    }

    private void testShutdown(boolean longTask, boolean withListener) throws Throwable {
        ArchiveFactory archiveFactory = ZipArchiveFactory.INSTANCE;
        LockableJBackup jbackup = new LockableJBackup(archiveFactory);
        if (longTask) {
            jbackup.lockCompression();
        }

        BackupConfiguration config = new BackupConfiguration();
        config.setName("testShutdown");
        Path targetDirectory = tempFolder.newFolder().toPath().toAbsolutePath();
        Files.delete(targetDirectory);
        config.setTargetDirectory(targetDirectory.toString());
        config.setArchiveFactory(archiveFactory);

        TerminationListener listener = withListener ? mock(TerminationListener.class) : null;
        Future<Void> future;
        Timer timer;
        if (longTask) {
            future = jbackup.backup(config);

            timer = jbackup.shutdown(listener);
            if (withListener) {
                assertThat(timer).as("shutdown timer").isNotNull();
                assertThat(timer.isRunning()).as("timer is running").isTrue();
                verify(listener, never()).terminated();
            } else {
                assertThat(timer).as("shutdown timer").isNull();
            }
            assertThat(future.isDone()).as("isDone").isFalse();

            jbackup.unlockCompression();
            waitResult(future);
        } else {
            future = jbackup.backup(config);
            waitResult(future);
            timer = jbackup.shutdown(listener);
            if (withListener) {
                assertThat(timer).as("shutdown timer").isNotNull();
            } else {
                assertThat(timer).as("shutdown timer").isNull();
            }
        }
        waitEndOfTimerIfAny(timer);
        if (withListener) {
            verify(listener, times(1)).terminated();
            verifyNoMoreInteractions(listener);
        }
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

    private void decompress(ArchiveFactory mockFactory, Path archive, Path targetDirectory, ProgressListener listener) throws Throwable {
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
        jbackup.shutdown(null);

        verify(mockDeleter, times(1)).registerDirectory(eq(targetDirectory));
        if (error) {
            verify(mockDeleter, times(1)).deleteAll();
        }
        verifyNoMoreInteractions(mockDeleter);
    }

    private void compress(ArchiveFactory mockFactory, ArchiveParameters archiveParameters, List<Path> expectedFiles, ProgressListener listener, boolean errorIsExpected) throws Throwable {
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
            BackupTask createBackupTask(BackupConfiguration config, ProgressListener listener) {
                return new BackupTask(this, config, listener) {
                    @Override
                    void compress(ArchiveFactory factory, ArchiveParameters archiveParameters, List<SourceWithPath> collectedFiles) throws ArchiveException {
                        for (SourceWithPath file : collectedFiles) {
                            actualFiles.add(file.getPath());
                        }

                        // now compress files in the order given by expectedFiles
                        // (otherwise the test will fail on some platforms)
                        super.compress(factory, archiveParameters, collectedFiles);
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

        jbackup.shutdown(null);

        Path archive = archiveDirectory.resolve(generatedName[0]);
        verify(mockDeleter, times(1)).registerFile(eq(archive));
        if (error) {
            verify(mockDeleter, times(1)).deleteAll();
        }
        verifyNoMoreInteractions(mockDeleter);
    }

    public static final class CustomNameFileFilter extends NameFileFilter {
        private final String name;

        public CustomNameFileFilter(String name) {
            super(name);
            this.name = name;
        }
    }

    protected static final String getName(IOFileFilter filter) {
        if (filter instanceof CustomNameFileFilter) {
            return ((CustomNameFileFilter) filter).name;
        } else {
            return null;
        }
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

    private void waitEndOfTimerIfAny(Timer timer) throws InterruptedException {
        if (timer != null) {
            long timeout = System.currentTimeMillis() + 5 * timer.getDelay();
            while (timer.isRunning()) {
                if (System.currentTimeMillis() > timeout) {
                    fail("timer not stopped");
                }
                Thread.sleep(100);
            }
        }
    }
}
