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

import fr.duminy.jbackup.core.archive.ArchiveFactory;
import fr.duminy.jbackup.core.archive.ProgressListener;
import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactory;
import fr.duminy.jbackup.core.task.BackupTask;
import fr.duminy.jbackup.core.task.RestoreTask;
import fr.duminy.jbackup.core.task.TaskListener;
import fr.duminy.jbackup.core.task.TaskTestUtils;
import fr.duminy.jbackup.core.util.LogRule;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static fr.duminy.jbackup.core.JBackup.TerminationListener;
import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for class {@link fr.duminy.jbackup.core.JBackup}.
 */
public class JBackupTest {
    @Rule
    public final LogRule logRule = new LogRule();

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

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
        BackupConfiguration config = createConfiguration();
        LockableJBackup jBackup = new LockableJBackup(config.getArchiveFactory());
        if (longTask) {
            jBackup.lockCompression();
        }

        TerminationListener listener = withListener ? mock(TerminationListener.class) : null;
        Future<Void> future;
        Timer timer;
        if (longTask) {
            future = jBackup.backup(config);

            timer = jBackup.shutdown(listener);
            if (withListener) {
                assertThat(timer).as("shutdown timer").isNotNull();
                assertThat(timer.isRunning()).as("timer is running").isTrue();
                verify(listener, never()).terminated();
            } else {
                assertThat(timer).as("shutdown timer").isNull();
            }
            assertThat(future.isDone()).as("isDone").isFalse();

            jBackup.unlockCompression();
            waitResult(future);
        } else {
            future = jBackup.backup(config);
            waitResult(future);
            timer = jBackup.shutdown(listener);
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
    public void testCreateBackupTask() throws Throwable {
        JBackup jBackup = new JBackup();
        BackupConfiguration config = mock(BackupConfiguration.class);
        TaskListener taskListener = mock(TaskListener.class);
        Cancellable cancellable = mock(Cancellable.class);

        BackupTask task = jBackup.createBackupTask(config, taskListener, cancellable);

        assertThat(task).as("returned task").isNotNull();
        assertThat(TaskTestUtils.getCancellable(task)).isSameAs(cancellable);
    }

    @Test
    public void testBackup_withCancellable() throws Throwable {
        // prepare test
        final BackupConfiguration config = createConfiguration();
        final MutableBoolean taskStarted = new MutableBoolean(false);
        final BackupTask mockBackupTask = createAlwaysWaitingTask(BackupTask.class, taskStarted);
        final MutableObject<Cancellable> actualCancellable = new MutableObject<>();
        final MutableObject<TaskListener> actualListener = new MutableObject<>();
        JBackup jBackup = spy(new JBackup() {
            @Override
            BackupTask createBackupTask(BackupConfiguration config, TaskListener listener, Cancellable cancellable) {
                actualListener.setValue(listener);
                actualCancellable.setValue(cancellable);
                return mockBackupTask;
            }
        });

        // test
        try {
            Future<Void> future = jBackup.backup(config);

            // wait task is actually started
            waitTaskStarted(taskStarted, actualCancellable);

            assertThat(actualCancellable.getValue()).as("cancellable").isNotNull();
            assertThat(actualCancellable.getValue().isCancelled()).as("cancelled").isFalse();

            future.cancel(true);
            assertThat(actualCancellable.getValue().isCancelled()).as("cancelled").isTrue();
        } finally {
            jBackup.shutdown(null);
        }

        // assertions
        InOrder inOrder = inOrder(mockBackupTask, jBackup);
        inOrder.verify(jBackup, times(1)).backup(eq(config)); // called above
        inOrder.verify(jBackup, times(1)).createBackupTask(eq(config), eq(actualListener.getValue()), eq(actualCancellable.getValue()));
        inOrder.verify(mockBackupTask, times(1)).call();
        inOrder.verify(jBackup, times(1)).shutdown(isNull(TerminationListener.class)); // called above
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testBackup_withListener() throws Throwable {
        ProgressListener listener = mock(ProgressListener.class);

        testBackup(listener);

        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testBackup_withoutListener() throws Throwable {
        testBackup(null);
    }

    private void testBackup(ProgressListener listener) throws Throwable {
        final BackupConfiguration config = createConfiguration();
        final BackupTask mockBackupTask = mock(BackupTask.class);
        final MutableObject<TaskListener> actualTaskListener = new MutableObject<>();
        JBackup jBackup = spy(new JBackup() {
            @Override
            BackupTask createBackupTask(BackupConfiguration config, TaskListener listener, Cancellable cancellable) {
                actualTaskListener.setValue(listener);
                return mockBackupTask;
            }
        });

        try {
            if (listener != null) {
                jBackup.addProgressListener(config.getName(), listener);
            }
            Future<Void> future = jBackup.backup(config);
            waitResult(future);
        } finally {
            jBackup.shutdown(null);
        }

        verify(jBackup, times(1)).backup(eq(config)); // called above
        verify(jBackup, times(1)).shutdown(isNull(TerminationListener.class)); // called above
        verify(jBackup, times(1)).createBackupTask(eq(config), eq(actualTaskListener.getValue()), notNull(Cancellable.class));
        verify(mockBackupTask, times(1)).call();
        if (listener != null) {
            verify(jBackup, times(1)).addProgressListener(eq(config.getName()), eq(listener));
        }
        verifyNoMoreInteractions(mockBackupTask, jBackup);
    }

    @Test
    public void testCreateRestoreTask() throws Throwable {
        JBackup jBackup = new JBackup();
        BackupConfiguration config = mock(BackupConfiguration.class);
        TaskListener taskListener = mock(TaskListener.class);
        Cancellable cancellable = mock(Cancellable.class);
        Path archive = mock(Path.class);
        Path targetDirectory = mock(Path.class);

        RestoreTask task = jBackup.createRestoreTask(config, archive, targetDirectory, taskListener, cancellable);

        assertThat(task).as("returned task").isNotNull();
        assertThat(TaskTestUtils.getCancellable(task)).isSameAs(cancellable);
    }

    @Test
    public void testRestore_withCancellable() throws Throwable {
        // prepare test
        final Path archive = tempFolder.newFolder().toPath().resolve("archive.zip");
        final Path targetDirectory = tempFolder.newFolder().toPath();
        final BackupConfiguration config = createConfiguration();
        final MutableBoolean taskStarted = new MutableBoolean(false);
        final RestoreTask mockRestoreTask = createAlwaysWaitingTask(RestoreTask.class, taskStarted);
        final MutableObject<Cancellable> actualCancellable = new MutableObject<>();
        final MutableObject<TaskListener> actualListener = new MutableObject<>();
        JBackup jBackup = spy(new JBackup() {
            @Override
            RestoreTask createRestoreTask(BackupConfiguration config, Path archive, Path targetDirectory, TaskListener listener, Cancellable cancellable) {
                actualListener.setValue(listener);
                actualCancellable.setValue(cancellable);
                return mockRestoreTask;
            }
        });

        // test
        try {
            Future<Void> future = jBackup.restore(config, archive, targetDirectory);

            // wait task is actually started
            waitTaskStarted(taskStarted, actualCancellable);

            assertThat(actualCancellable.getValue()).as("cancellable").isNotNull();
            assertThat(actualCancellable.getValue().isCancelled()).as("cancelled").isFalse();

            future.cancel(true);
            assertThat(actualCancellable.getValue().isCancelled()).as("cancelled").isTrue();
        } finally {
            jBackup.shutdown(null);
        }

        // assertions
        InOrder inOrder = inOrder(mockRestoreTask, jBackup);
        inOrder.verify(jBackup, times(1)).restore(eq(config), eq(archive), eq(targetDirectory)); // called above
        inOrder.verify(jBackup, times(1)).createRestoreTask(eq(config), eq(archive), eq(targetDirectory), eq(actualListener.getValue()), eq(actualCancellable.getValue()));
        inOrder.verify(mockRestoreTask, times(1)).call();
        inOrder.verify(jBackup, times(1)).shutdown(isNull(TerminationListener.class)); // called above
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testRestore_withListener() throws Throwable {
        ProgressListener listener = mock(ProgressListener.class);

        testRestore(listener);

        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testRestore_withoutListener() throws Throwable {
        testRestore(null);
    }

    private void testRestore(ProgressListener listener) throws Throwable {
        final Path archive = tempFolder.newFolder().toPath().resolve("archive.zip");
        final Path targetDirectory = tempFolder.newFolder().toPath();
        final BackupConfiguration config = createConfiguration();
        final RestoreTask mockRestoreTask = mock(RestoreTask.class);
        final MutableObject<TaskListener> actualTaskListener = new MutableObject<>();
        JBackup jBackup = spy(new JBackup() {
            @Override
            RestoreTask createRestoreTask(BackupConfiguration config, Path archive, Path targetDirectory, TaskListener taskListener, Cancellable cancellable) {
                actualTaskListener.setValue(taskListener);
                return mockRestoreTask;
            }
        });

        try {
            if (listener != null) {
                jBackup.addProgressListener(config.getName(), listener);
            }
            Future<Void> future = jBackup.restore(config, archive, targetDirectory);
            waitResult(future);
        } finally {
            jBackup.shutdown(null);
        }

        verify(jBackup, times(1)).restore(eq(config), eq(archive), eq(targetDirectory)); // called above
        verify(jBackup, times(1)).shutdown(isNull(TerminationListener.class)); // called above
        verify(jBackup, times(1)).createRestoreTask(eq(config), eq(archive), eq(targetDirectory), eq(actualTaskListener.getValue()), notNull(Cancellable.class));
        verify(mockRestoreTask, times(1)).call();
        if (listener != null) {
            verify(jBackup, times(1)).addProgressListener(eq(config.getName()), eq(listener));
        }
        verifyNoMoreInteractions(mockRestoreTask, jBackup);
    }

    @Test
    public void testAddProgressListener_backup() throws Throwable {
        testAddProgressListener(new BackupAction());
    }

    @Test
    public void testAddProgressListener_restore() throws Throwable {
        testAddProgressListener(new RestoreAction());
    }

    private void testAddProgressListener(JBackupAction action) throws Throwable {
        JBackup jBackup = createMockJBackup();
        ProgressListener listener = mock(ProgressListener.class);
        BackupConfiguration config = action.getConfiguration();

        jBackup.addProgressListener(config.getName(), listener);
        try {
            Future<Void> future = action.executeAction(jBackup);
            waitResult(future);
        } finally {
            jBackup.shutdown(null);
        }

        verifyListenerNotifiedOnlyForConfig(listener, config);
    }

    @Test
    public void testAddProgressListener_backup_TwoListeners() throws Throwable {
        BackupConfiguration config1 = ConfigurationManagerTest.createConfiguration("config1");
        BackupConfiguration config2 = ConfigurationManagerTest.createConfiguration("config2");
        testAddProgressListener_TwoListeners(new BackupAction(config1), new BackupAction(config2));
    }

    @Test
    public void testAddProgressListener_restore_TwoListeners() throws Throwable {
        BackupConfiguration config1 = ConfigurationManagerTest.createConfiguration("config1");
        BackupConfiguration config2 = ConfigurationManagerTest.createConfiguration("config2");
        testAddProgressListener_TwoListeners(new RestoreAction(config1), new RestoreAction(config2));
    }

    private void testAddProgressListener_TwoListeners(JBackupAction action, JBackupAction action2) throws Throwable {
        JBackup jBackup = createMockJBackup();
        ProgressListener listener = mock(ProgressListener.class);
        BackupConfiguration config = action.getConfiguration();
        ProgressListener listener2 = mock(ProgressListener.class);
        BackupConfiguration config2 = action2.getConfiguration();

        jBackup.addProgressListener(config.getName(), listener);
        jBackup.addProgressListener(config2.getName(), listener2);
        try {
            Future<Void> future = action.executeAction(jBackup);
            Future<Void> future2 = action2.executeAction(jBackup);
            waitResult(future);
            waitResult(future2);
        } finally {
            jBackup.shutdown(null);
        }

        verifyListenerNotifiedOnlyForConfig(listener, config);
        verifyListenerNotifiedOnlyForConfig(listener2, config2);
    }

    private void verifyListenerNotifiedOnlyForConfig(ProgressListener listener, BackupConfiguration config) {
        verify(listener, times(1)).taskStarted(eq(config.getName()));
        verify(listener, times(1)).totalSizeComputed(eq(config.getName()), anyLong());
        verify(listener, times(1)).progress(eq(config.getName()), anyLong());
        verify(listener, times(1)).taskFinished(eq(config.getName()), any(Throwable.class));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testRemoveProgressListener_backup() throws Throwable {
        testRemoveProgressListener(new BackupAction());
    }

    @Test
    public void testRemoveProgressListener_restore() throws Throwable {
        testRemoveProgressListener(new RestoreAction());
    }

    private void testRemoveProgressListener(JBackupAction action) throws Throwable {
        final JBackup jBackup = createMockJBackup();
        final ProgressListener listener = mock(ProgressListener.class);
        final BackupConfiguration config = action.getConfiguration();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                jBackup.removeProgressListener(config.getName(), listener);
                return null;
            }
        }).when(listener).taskStarted(anyString());

        jBackup.addProgressListener(config.getName(), listener);
        try {
            Future<Void> future = action.executeAction(jBackup);
            waitResult(future);
        } finally {
            jBackup.shutdown(null);
        }

        verify(listener, times(1)).taskStarted(eq(config.getName()));
        verify(listener, never()).totalSizeComputed(eq(config.getName()), anyLong());
        verify(listener, never()).progress(eq(config.getName()), anyLong());
        verify(listener, never()).taskFinished(eq(config.getName()), any(Throwable.class));
    }

    public static final class CustomNameFileFilter extends NameFileFilter {
        private final String name;

        public CustomNameFileFilter(String name) {
            super(name);
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    protected void waitResult(Future<Void> future) throws Throwable {
        assertThat(future).isNotNull();
        try {
            future.get(); // block until finished and maybe throw an Exception if task has thrown one.
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    private void waitEndOfTimerIfAny(Timer timer) throws InterruptedException {
        if (timer != null) {
            long timeout = currentTimeMillis() + 5 * timer.getDelay();
            while (timer.isRunning()) {
                if (currentTimeMillis() > timeout) {
                    fail("timer not stopped");
                }
                Thread.sleep(100);
            }
        }
    }

    private BackupConfiguration createConfiguration() throws IOException {
        ArchiveFactory archiveFactory = ZipArchiveFactory.INSTANCE;
        BackupConfiguration config = new BackupConfiguration();
        config.setName("test");
        Path targetDirectory = tempFolder.newFolder().toPath().toAbsolutePath();
        Files.delete(targetDirectory);
        config.setTargetDirectory(targetDirectory.toString());
        config.setArchiveFactory(archiveFactory);
        config.addSource(TestUtils.createSourceWithFiles(tempFolder, "source"));
        return config;
    }

    private void waitTaskStarted(MutableBoolean taskStarted, MutableObject<Cancellable> actualCancellable) throws InterruptedException {
        long start = currentTimeMillis();
        do {
            Thread.sleep(100);
        }
        while (((actualCancellable.getValue() == null) || taskStarted.isFalse()) && ((currentTimeMillis() - start) < 1000));
    }

    private <T extends Callable<Void>> T createAlwaysWaitingTask(final Class<T> taskClass, final MutableBoolean taskStarted) throws Exception {
        final T mockTask = mock(taskClass);
        when(mockTask.call()).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                taskStarted.setValue(true);
                while (true) {
                    Thread.sleep(100);
                }
            }
        });
        return mockTask;
    }

    private abstract class JBackupAction {
        protected final BackupConfiguration config;

        private JBackupAction() throws IOException {
            this(createConfiguration());
        }

        private JBackupAction(BackupConfiguration config) throws IOException {
            this.config = config;
        }

        public abstract Future<Void> executeAction(JBackup jBackup) throws IOException;

        public final BackupConfiguration getConfiguration() {
            return config;
        }
    }

    private class BackupAction extends JBackupAction {
        private BackupAction() throws IOException {
        }

        private BackupAction(BackupConfiguration config) throws IOException {
            super(config);
        }

        public Future<Void> executeAction(JBackup jBackup) throws IOException {
            return jBackup.backup(config);
        }
    }

    private class RestoreAction extends JBackupAction {
        private RestoreAction() throws IOException {
        }

        private RestoreAction(BackupConfiguration config) throws IOException {
            super(config);
        }

        public Future<Void> executeAction(JBackup jBackup) throws IOException {
            return jBackup.restore(config, null, null);
        }
    }

    private JBackup createMockJBackup() {
        return new JBackup() {
            @Override
            RestoreTask createRestoreTask(BackupConfiguration config, Path archive, Path targetDirectory, final TaskListener taskListener, Cancellable cancellable) {
                return new RestoreTask(config, archive, targetDirectory, null, taskListener, cancellable) {
                    @Override
                    protected void execute() throws Exception {
                        simulateTask(taskListener);
                    }
                };
            }

            @Override
            BackupTask createBackupTask(BackupConfiguration config, final TaskListener taskListener, Cancellable cancellable) {
                return new BackupTask(config, null, taskListener, cancellable) {
                    @Override
                    protected void execute() throws Exception {
                        simulateTask(taskListener);
                    }
                };
            }

            private void simulateTask(TaskListener taskListener) {
                taskListener.totalSizeComputed(1);
                taskListener.progress(1);
            }
        };
    }
}
