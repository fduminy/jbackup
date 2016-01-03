/**
 * JBackup is a software managing backups.
 *
 * Copyright (C) 2013-2015 Fabien DUMINY (fabien [dot] duminy [at] webmails [dot] com)
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
package fr.duminy.jbackup.swing;

import fr.duminy.components.swing.AbstractSwingTest;
import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.JBackup;
import fr.duminy.jbackup.core.archive.ProgressListener;
import fr.duminy.jbackup.core.util.LogRule;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.edt.GuiTask;
import org.assertj.swing.exception.UnexpectedException;
import org.junit.Rule;
import org.junit.experimental.theories.Theories;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import javax.swing.*;
import java.nio.file.Path;

import static fr.duminy.jbackup.core.ConfigurationManagerTest.createConfiguration;
import static org.mockito.Mockito.*;

/**
 * Tests for class {@link fr.duminy.jbackup.swing.TaskManagerComponent}.
 */
@RunWith(Theories.class)
abstract public class TaskManagerComponentTest<T extends JComponent & ProgressListener, C extends TaskManagerComponent<T>> extends AbstractSwingTest {
    @Rule
    public final LogRule logRule = new LogRule();

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private JBackup jBackup;
    protected C panel;
    private ArgumentCaptor<ProgressListener> actualListeners;

    @Override
    public final void onSetUp() {
        super.onSetUp();

        jBackup = mock(JBackup.class);
        actualListeners = ArgumentCaptor.forClass(ProgressListener.class);
        doNothing().when(jBackup).addProgressListener(anyString(), actualListeners.capture());

        try {
            panel = buildAndShowWindow(() -> createComponent(jBackup));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    abstract protected C createComponent(JBackup jBackup);

    protected final JBackup getJBackup() {
        return jBackup;
    }

    protected final BackupConfiguration createConfigurationForDuplicateTaskTest() throws Exception {
        BackupConfiguration config = createConfiguration();
        thrown.expect(DuplicateTaskException.class);
        thrown.expectMessage("There is already a task running for configuration '" + config.getName() + "'");
        return config;
    }

    protected final void runBackup(final BackupConfiguration config) throws DuplicateTaskException {
        runBackup(panel, config);
    }

    protected final void runRestore(final Path archive, final Path targetDirectory, final BackupConfiguration config) throws DuplicateTaskException {
        runRestore(panel, archive, targetDirectory, config);
    }

    public static void runBackup(final BackupConfigurationActions actions, final BackupConfiguration config) throws DuplicateTaskException {
        try {
            GuiActionRunner.execute(new GuiTask() {
                protected void executeInEDT() throws DuplicateTaskException {
                    actions.backup(config);
                }
            });
        } catch (UnexpectedException e) {
            throw (DuplicateTaskException) e.getCause();
        }
    }

    public static void runRestore(final BackupConfigurationActions actions, final Path archive, final Path targetDirectory, final BackupConfiguration config) throws DuplicateTaskException {
        try {
            GuiActionRunner.execute(new GuiTask() {
                protected void executeInEDT() throws DuplicateTaskException {
                    actions.restore(config, archive, targetDirectory);
                }
            });
        } catch (UnexpectedException e) {
            throw (DuplicateTaskException) e.getCause();
        }
    }

    protected final void notifyTaskFinished(final BackupConfiguration config, final Exception error) {
        GuiActionRunner.execute(new GuiTask() {
            protected void executeInEDT() throws DuplicateTaskException {
                for (ProgressListener listener : actualListeners.getAllValues()) {
                    listener.taskFinished(config.getName(), error);
                }
            }
        });
    }
}
