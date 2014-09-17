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
import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactory;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
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
        LockableJBackup jBackup = new LockableJBackup(archiveFactory);
        if (longTask) {
            jBackup.lockCompression();
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
            future = jBackup.backup(config, null);

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
            future = jBackup.backup(config, null);
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
