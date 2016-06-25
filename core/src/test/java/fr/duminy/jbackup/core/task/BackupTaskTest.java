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
import fr.duminy.jbackup.core.*;
import fr.duminy.jbackup.core.archive.ArchiveException;
import fr.duminy.jbackup.core.archive.ArchiveFactory;
import fr.duminy.jbackup.core.archive.ArchiveParameters;
import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactory;
import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactoryTest;
import fr.duminy.jbackup.core.command.CollectFilesCommand;
import fr.duminy.jbackup.core.command.CompressCommand;
import fr.duminy.jbackup.core.command.JBackupContext;
import fr.duminy.jbackup.core.command.VerifyArchiveCommand;
import fr.duminy.jbackup.core.util.FileDeleter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.Test;
import org.junit.experimental.theories.Theory;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Matchers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

import static fr.duminy.jbackup.core.matchers.Matchers.eq;
import static fr.duminy.jbackup.core.matchers.Matchers.parametersComparator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.*;

public class BackupTaskTest extends AbstractTaskTest {
    @Theory
    public void testCall(TaskListenerEnum listenerEnum, boolean verify) throws Throwable {
        TaskListener listener = listenerEnum.createTaskListener();
        final ArchiveParameters archiveParameters = createArchiveParameters();

        testCall(ZipArchiveFactory.INSTANCE, archiveParameters, listener, null, null, verify);

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
        ArchiveParameters archiveParameters = createArchiveParameters();
        final BackupConfiguration config = toBackupConfiguration(ZipArchiveFactory.INSTANCE, archiveParameters);
        TestableBackupTask task = new TestableBackupTask(config, createDeleterSupplier(mockDeleter), null, cancellable);

        // test
        task.call();

        // assertions
        InOrder inOrder = inOrder(task.mockCompressCommand, task.mockCollectFilesCommand);
        inOrder.verify(task.mockCollectFilesCommand, times(1)).execute(any(JBackupContext.class));
        inOrder.verify(task.mockCompressCommand, times(1)).execute(any(JBackupContext.class));
        inOrder.verifyNoMoreInteractions();
        task.verifyRealCommands();
        assertThat(TaskTestUtils.getCancellable(task)).isSameAs(cancellable);
    }

    @Theory
    public void testCall_NullArchiveFactory(TaskListenerEnum listenerEnum) throws Throwable {
        // prepare test
        TaskListener listener = listenerEnum.createTaskListener();
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
    public void testCall_deleteArchiveOnError(TaskListenerEnum listenerEnum, boolean verify) throws Throwable {
        TaskListener listener = listenerEnum.createTaskListener();
        final Exception exception;
        if (verify) {
            exception = new BackupTask.VerificationFailedException("Archive verification failed");
            thrown.expect(exception.getClass());
        } else {
            exception = new IOException("An unexpected error");
            thrown.expect(CommandException.class);
            thrown.expectCause(equalTo(exception));
        }
        thrown.expectMessage(exception.getMessage());

        final ArchiveParameters archiveParameters = createArchiveParameters();

        testCall(ZipArchiveFactory.INSTANCE, archiveParameters, listener, exception, null, verify);

        verify(listener).taskStarted();
        verify(listener).taskFinished(eq(exception));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testCall_deleteArchiveOnCancel() throws Throwable {
        Cancellable cancellable = mock(Cancellable.class);
        when(cancellable.isCancelled()).thenReturn(true);
        ArchiveParameters archiveParameters = createArchiveParameters();

        testCall(ZipArchiveFactory.INSTANCE, archiveParameters, null, null, cancellable, false);
    }

    @SuppressWarnings("unchecked")
    private void testCall(ArchiveFactory mockFactory, ArchiveParameters archiveParameters, TaskListener listener,
                          Exception exception, Cancellable cancellable, boolean verify) throws Throwable {
        // prepare test
        final FileDeleter mockDeleter = mock(FileDeleter.class);
        final BackupConfiguration config = toBackupConfiguration(mockFactory, archiveParameters);
        config.setVerify(verify);

        TestableBackupTask task = new TestableBackupTask(config, createDeleterSupplier(mockDeleter), listener, cancellable);
        Path expectedArchive = getExpectedArchive(config, task);
        Files.copy(ZipArchiveFactoryTest.getArchive(), expectedArchive);

        if (exception != null) {
            if (exception instanceof BackupTask.VerificationFailedException) {
                doThrow(exception).when(task.mockCompressCommand).execute(any(JBackupContext.class));
            } else {
                doThrow(new CommandException(exception)).when(task.mockCompressCommand)
                                                        .execute(any(JBackupContext.class));
            }
        }

        // test
        try {
            task.call();
        } catch (ArchiveException e) {
            throw e.getCause();
        } finally {
            // assertions
            InOrder inOrder = inOrder(mockDeleter, task.mockCompressCommand, task.mockCollectFilesCommand,
                                      task.mockVerifyArchiveCommand);

            ArgumentCaptor<JBackupContext> contextCollectFiles = ArgumentCaptor.forClass(JBackupContext.class);
            inOrder.verify(task.mockCollectFilesCommand, times(1)).execute(contextCollectFiles.capture());

            ArgumentCaptor<JBackupContext> contextCompress = ArgumentCaptor.forClass(JBackupContext.class);
            inOrder.verify(task.mockCompressCommand, times(1)).execute(contextCompress.capture());
            assertThat(contextCollectFiles.getValue()).isNotNull().isSameAs(contextCompress.getValue());

            if ((cancellable != null) || (exception != null)) {
                inOrder.verify(mockDeleter, times(1)).deleteAll();
            } else {
                ArgumentCaptor<JBackupContext> contextVerify = ArgumentCaptor.forClass(JBackupContext.class);
                if (config.isVerify()) {
                    inOrder.verify(task.mockVerifyArchiveCommand, times(1)).execute(contextVerify.capture());
                    assertThat(contextCollectFiles.getValue()).isSameAs(contextVerify.getValue());
                } else {
                    inOrder.verify(task.mockVerifyArchiveCommand, never()).execute(contextVerify.capture());
                }
            }
            inOrder.verifyNoMoreInteractions();
            task.verifyRealCommands();
            verifyContext(mockFactory, archiveParameters, listener, cancellable, mockDeleter, expectedArchive,
                          contextCollectFiles.getValue());
        }
    }

    private void verifyContext(ArchiveFactory mockFactory, ArchiveParameters archiveParameters, TaskListener listener,
                               Cancellable cancellable, FileDeleter mockDeleter, Path expectedArchive,
                               JBackupContext context) {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(context.getListener()).as("context.listener").isSameAs(listener);
        soft.assertThat(context.getCancellable()).as("context.cancellable").isSameAs(cancellable);
        soft.assertThat(context.getFactory()).as("context.factory").isSameAs(mockFactory);
        soft.assertThat(context.getFileDeleter()).as("context.fileDeleter").isSameAs(mockDeleter);
        soft.assertAll();

        org.assertj.core.api.SoftAssertions soft2 = new org.assertj.core.api.SoftAssertions();
        soft2.assertThat(context.getArchivePath()).as("context.archivePath").isEqualTo(expectedArchive);
        soft2.assertThat(context.getArchiveParameters()).as("context.parameters.sources")
             .usingComparator(parametersComparator(expectedArchive)).isEqualTo(archiveParameters);
        soft2.assertAll();
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
        if (filter instanceof JBackupImplTest.CustomNameFileFilter) {
            return ((JBackupImplTest.CustomNameFileFilter) filter).getName();
        } else {
            return null;
        }
    }

    private String toJexlExpression(IOFileFilter filter) {
        String fileName = getName(filter);
        return (fileName == null) ? null : "file.name=='" + fileName + "'";
    }

    static Supplier<FileDeleter> createDeleterSupplier(final FileDeleter mockDeleter) {
        return () -> mockDeleter;
    }

    private ArchiveParameters createArchiveParameters() throws IOException {
        Path archive = tempFolder.newFolder().toPath().resolve("archive.mock");
        final ArchiveParameters archiveParameters = new ArchiveParameters(archive, true);
        archiveParameters.addSource(TestUtils.createSourceWithFiles(tempFolder, "source"));
        return archiveParameters;
    }

    private static class TestableBackupTask extends BackupTask {
        private final String generatedName = "archive.mock";
        private final VerifyArchiveCommand mockVerifyArchiveCommand;
        private final CompressCommand mockCompressCommand;
        private final CollectFilesCommand mockCollectFilesCommand;
        private VerifyArchiveCommand realVerifyArchiveCommand;
        private CompressCommand realCompressCommand;
        private CollectFilesCommand realCollectFilesCommand;

        public TestableBackupTask(BackupConfiguration config, Supplier<FileDeleter> deleterSupplier,
                                  TaskListener listener, Cancellable cancellable) {
            super(config, deleterSupplier, listener, cancellable);
            mockCompressCommand = mock(CompressCommand.class);
            mockCollectFilesCommand = mock(CollectFilesCommand.class);
            mockVerifyArchiveCommand = mock(VerifyArchiveCommand.class);
        }

        @Override CollectFilesCommand createCollectFilesCommand() {
            realCollectFilesCommand = super.createCollectFilesCommand();
            return mockCollectFilesCommand;
        }

        @Override CompressCommand createCompressCommand(ArchiveFactory factory) {
            realCompressCommand = super.createCompressCommand(factory);
            return mockCompressCommand;
        }

        @Override VerifyArchiveCommand createVerifyArchiveCommand() {
            realVerifyArchiveCommand = super.createVerifyArchiveCommand();
            return mockVerifyArchiveCommand;
        }

        @Override
        protected String generateName(String configName, ArchiveFactory factory) {
            return generatedName;
        }

        public String getGeneratedName() {
            return generatedName;
        }

        public void verifyRealCommands() {
            SoftAssertions soft = new SoftAssertions();
            soft.assertThat(realCollectFilesCommand).as("realCollectFilesCommand").isNotNull();
            soft.assertThat(realCompressCommand).as("realCompressCommand").isNotNull();
            if (config.isVerify()) {
                soft.assertThat(realVerifyArchiveCommand).as("realVerifyArchiveCommand").isNotNull();
            } else {
                soft.assertThat(realVerifyArchiveCommand).as("realVerifyArchiveCommand").isNull();
            }
            soft.assertAll();
        }
    }
}