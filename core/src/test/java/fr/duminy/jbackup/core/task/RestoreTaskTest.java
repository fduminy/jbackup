/**
 * JBackup is a software managing backups.
 *
 * Copyright (C) 2013-2016 Fabien DUMINY (fabien [dot] duminy [at] webmails [dot] com)
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

import fr.duminy.components.chain.CommandException;
import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.Cancellable;
import fr.duminy.jbackup.core.SoftAssertions;
import fr.duminy.jbackup.core.archive.ArchiveException;
import fr.duminy.jbackup.core.archive.ArchiveFactory;
import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactory;
import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactoryTest;
import fr.duminy.jbackup.core.command.DecompressCommand;
import fr.duminy.jbackup.core.command.JBackupContext;
import fr.duminy.jbackup.core.util.FileDeleter;
import org.junit.Test;
import org.junit.experimental.theories.Theory;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

import static fr.duminy.jbackup.core.task.BackupTaskTest.createDeleterSupplier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class RestoreTaskTest extends AbstractTaskTest {
    @Test
    public void testCall_withCancellable() throws Throwable {
        // prepare test
        Path archive = ZipArchiveFactoryTest.createArchive(tempFolder.newFolder().toPath());
        Path targetDirectory = tempFolder.newFolder("targetDirectory").toPath();
        FileDeleter mockDeleter = mock(FileDeleter.class);
        Cancellable mockCancellable = mock(Cancellable.class);
        BackupConfiguration config = createConfiguration(null, targetDirectory);

        TestableRestoreTask task = new TestableRestoreTask(config, archive, targetDirectory,
                createDeleterSupplier(mockDeleter), null, mockCancellable);

        // test
        task.call();

        // assertions
        verify(task.mockDecompressCommand, times(1)).execute(any(JBackupContext.class));
        verifyNoMoreInteractions(task.mockDecompressCommand);
        assertThat(TaskTestUtils.getCancellable(task)).isSameAs(mockCancellable);
    }

    @Theory
    public void testCall(TaskListenerEnum listenerEnum) throws Throwable {
        testCall(null, listenerEnum.createTaskListener(), null);
    }

    @Theory
    public void testCall_deleteFilesOnError(TaskListenerEnum listenerEnum) throws Throwable {
        final IOException exception = new IOException("An unexpected error");
        thrown.expect(CommandException.class);
        thrown.expectCause(equalTo(exception));
        thrown.expectMessage(exception.getMessage());

        testCall(exception, listenerEnum.createTaskListener(), null);
    }

    @Test
    public void testCall_deleteFilesOnCancel() throws Throwable {
        Cancellable cancellable = mock(Cancellable.class);
        when(cancellable.isCancelled()).thenReturn(true);

        testCall(null, null, cancellable);
    }

    private void testCall(Exception exception, TaskListener listener, Cancellable cancellable) throws Throwable {
        // prepare test
        Path archive = ZipArchiveFactoryTest.createArchive(tempFolder.newFolder().toPath());
        Path targetDirectory = tempFolder.newFolder("targetDirectory").toPath();
        FileDeleter mockDeleter = mock(FileDeleter.class);
        ArchiveFactory factory = ZipArchiveFactory.INSTANCE;
        BackupConfiguration config = createConfiguration(factory, targetDirectory);
        TestableRestoreTask task = new TestableRestoreTask(config, archive, targetDirectory,
                                                           createDeleterSupplier(mockDeleter), listener, cancellable);
        if (exception != null) {
            doThrow(new CommandException(exception)).when(task.mockDecompressCommand)
                                                    .execute(any(JBackupContext.class));
        }

        // test
        try {
            task.call();
        } catch (ArchiveException e) {
            throw e.getCause();
        } finally {
            // assertions
            verify(mockDeleter, times(1)).registerDirectory(eq(targetDirectory));
            if ((cancellable != null) || (exception != null)) {
                verify(mockDeleter, times(1)).deleteAll();
            }
            ArgumentCaptor<JBackupContext> context = ArgumentCaptor.forClass(JBackupContext.class);
            verify(task.mockDecompressCommand).execute(context.capture());
            verifyNoMoreInteractions(mockDeleter, task.mockDecompressCommand);
            task.verifyRealCommands();
            verifyContext(factory, listener, archive, cancellable, mockDeleter, targetDirectory, context.getValue());
        }
    }

    private void verifyContext(ArchiveFactory mockFactory, TaskListener listener, Path archivePath,
                               Cancellable cancellable, FileDeleter mockDeleter, Path targetDirectory,
                               JBackupContext context) {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(context.getCancellable()).as("context.cancellable").isSameAs(cancellable);
        soft.assertThat(context.getFactory()).as("context.factory").isSameAs(mockFactory);
        soft.assertThat(context.getFileDeleter()).as("context.fileDeleter").isSameAs(mockDeleter);
        soft.assertThat(context.getListener()).as("context.listener").isSameAs(listener);
        soft.assertAll();

        org.assertj.core.api.SoftAssertions soft2 = new org.assertj.core.api.SoftAssertions();
        soft2.assertThat(context.getTargetDirectory()).as("context.targetDirectory").isEqualTo(targetDirectory);
        soft2.assertThat(context.getArchivePath()).as("context.archivePath").isEqualTo(archivePath);
        soft2.assertAll();
    }

    private BackupConfiguration createConfiguration(ArchiveFactory mockFactory, Path targetDirectory) {
        BackupConfiguration config = new BackupConfiguration();
        config.setName("testRestoreTask");
        config.setTargetDirectory(targetDirectory.toString());
        config.setArchiveFactory(mockFactory);
        return config;
    }

    private static class TestableRestoreTask extends RestoreTask {
        private final DecompressCommand mockDecompressCommand = mock(DecompressCommand.class);
        private DecompressCommand realDecompressCommand;

        public TestableRestoreTask(BackupConfiguration config, Path archive, Path targetDirectory,
                                   Supplier<FileDeleter> deleterSupplier, TaskListener listener,
                                   Cancellable cancellable) {
            super(config, archive, targetDirectory, deleterSupplier, listener, cancellable);
        }

        @Override
        DecompressCommand createDecompressCommand() {
            realDecompressCommand = super.createDecompressCommand();
            return mockDecompressCommand;
        }

        public void verifyRealCommands() {
            assertThat(realDecompressCommand).as("realDecompressCommand").isNotNull();
        }
    }
}