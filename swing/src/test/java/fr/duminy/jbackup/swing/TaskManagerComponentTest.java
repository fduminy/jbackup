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
package fr.duminy.jbackup.swing;

import com.google.common.base.Supplier;
import fr.duminy.components.swing.AbstractSwingTest;
import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.JBackup;
import fr.duminy.jbackup.core.archive.ProgressListener;
import fr.duminy.jbackup.core.util.LogRule;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.exception.UnexpectedException;
import org.junit.Rule;
import org.junit.experimental.theories.Theories;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.nio.file.Path;

import static fr.duminy.jbackup.core.ConfigurationManagerTest.createConfiguration;
import static org.mockito.Mockito.mock;

/**
 * Tests for class {@link fr.duminy.jbackup.swing.TaskManagerComponent}.
 */
@RunWith(Theories.class)
abstract public class TaskManagerComponentTest<T extends ProgressListener, C extends TaskManagerComponent<T>> extends AbstractSwingTest {
    @Rule
    public final LogRule logRule = new LogRule();

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private JBackup jBackup;
    private C panel;

    @Override
    public final void onSetUp() {
        super.onSetUp();

        jBackup = mock(JBackup.class);

        try {
            panel = buildAndShowWindow(new Supplier<C>() {
                @Override
                public C get() {
                    return createComponent(jBackup);
                }
            });
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
        try {
            GuiActionRunner.execute(new GuiTask() {
                protected void executeInEDT() throws DuplicateTaskException {
                    panel.backup(config);
                }
            });
        } catch (UnexpectedException e) {
            throw (DuplicateTaskException) e.getCause();
        }
    }

    protected final void runRestore(final Path archive, final Path targetDirectory, final BackupConfiguration config) throws DuplicateTaskException {
        try {
            GuiActionRunner.execute(new GuiTask() {
                protected void executeInEDT() throws DuplicateTaskException {
                    panel.restore(config, archive, targetDirectory);
                }
            });
        } catch (UnexpectedException e) {
            throw (DuplicateTaskException) e.getCause();
        }
    }

    protected final void notifyTaskFinished(final BackupConfiguration config, final Exception error, final ProgressListener pPanel) {
        GuiActionRunner.execute(new GuiTask() {
            protected void executeInEDT() throws DuplicateTaskException {
                pPanel.taskFinished(config.getName(), error);
            }
        });
    }
}