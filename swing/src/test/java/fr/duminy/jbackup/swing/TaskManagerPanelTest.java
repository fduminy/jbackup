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
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.exception.UnexpectedException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.nio.file.Path;

import static fr.duminy.jbackup.core.ConfigurationManagerTest.createConfiguration;
import static fr.duminy.jbackup.swing.ProgressPanelTest.assertThatPanelHasTitle;
import static org.mockito.Mockito.*;

/**
 * Tests for class {@link TaskManagerPanel}.
 */
@RunWith(Theories.class)
public class TaskManagerPanelTest extends AbstractSwingTest {
    @DataPoint
    public static final int INIT_NO_CONFIG = 0;
    @DataPoint
    public static final int INIT_1_CONFIG = 1;
    @DataPoint
    public static final int INIT_2_CONFIGS = 2;

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private JBackup jBackup;
    private TaskManagerPanel panel;

    @Override
    public void onSetUp() {
        super.onSetUp();

        jBackup = mock(JBackup.class);

        try {
            panel = buildAndShowWindow(new Supplier<TaskManagerPanel>() {
                @Override
                public TaskManagerPanel get() {
                    return new TaskManagerPanel(jBackup);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Theory
    public void testBackup(int nbConfigurations) throws Exception {
        final BackupConfiguration[] configs = new BackupConfiguration[nbConfigurations];
        for (int i = 0; i < nbConfigurations; i++) {
            configs[i] = createConfiguration("name" + i);
            runBackup(configs[i]);
        }

        for (BackupConfiguration config : configs) {
            verifyBackup(config);
        }
    }

    @Test
    public void testBackup_afterTaskFinishedWithoutError() throws Exception {
        testBackup_afterTaskFinished(null);
    }

    @Test
    public void testBackup_afterTaskFinishedWithError() throws Exception {
        testBackup_afterTaskFinished(new Exception("Something wrong happened"));
    }

    private void testBackup_afterTaskFinished(Exception error) throws Exception {
        BackupConfiguration config = createConfiguration();
        runBackup(config);

        ProgressPanel pPanel = verifyBackup(config);
        notifyTaskFinished(error, pPanel);

        reset(jBackup);
        runBackup(config);
        verifyBackup(config);
    }

    @Theory
    public void testRestore(int nbConfigurations) throws Exception {
        final Path[] archives = new Path[nbConfigurations];
        final Path[] targetDirectories = new Path[nbConfigurations];
        final BackupConfiguration[] configs = new BackupConfiguration[nbConfigurations];
        for (int i = 0; i < nbConfigurations; i++) {
            configs[i] = createConfiguration("name" + i);
            archives[i] = tempFolder.newFile().toPath();
            targetDirectories[i] = tempFolder.newFolder().toPath();
            runRestore(archives[i], targetDirectories[i], configs[i]);
        }

        for (int i = 0; i < configs.length; i++) {
            BackupConfiguration config = configs[i];
            verifyRestore(archives[i], targetDirectories[i], config);
        }
    }

    @Test
    public void testRestore_afterTaskFinishedWithoutError() throws Exception {
        testRestore_afterTaskFinished(null);
    }

    @Test
    public void testRestore_afterTaskFinishedWithError() throws Exception {
        testRestore_afterTaskFinished(new Exception("Something wrong happened"));
    }

    private void testRestore_afterTaskFinished(Exception error) throws Exception {
        Path archive = tempFolder.newFile().toPath();
        Path targetDirectory = tempFolder.newFolder().toPath();
        BackupConfiguration config = createConfiguration();
        runRestore(archive, targetDirectory, config);

        ProgressPanel pPanel = verifyRestore(archive, targetDirectory, config);
        notifyTaskFinished(error, pPanel);

        reset(jBackup);
        runRestore(archive, targetDirectory, config);
        verifyRestore(archive, targetDirectory, config);
    }

    @Test
    public void testDuplicateTask_backup() throws Exception {
        BackupConfiguration config = createConfigurationForDuplicateTaskTest();

        runBackup(config);
        runBackup(config);
    }

    @Test
    public void testDuplicateTask_restore() throws Exception {
        BackupConfiguration config = createConfigurationForDuplicateTaskTest();
        Path archive = tempFolder.newFile().toPath();
        Path targetDirectory = tempFolder.newFolder().toPath();

        runRestore(archive, targetDirectory, config);
        runRestore(archive, targetDirectory, config);
    }

    @Test
    public void testDuplicateTask_restoreAndBackup() throws Exception {
        BackupConfiguration config = createConfigurationForDuplicateTaskTest();
        Path archive = tempFolder.newFile().toPath();
        Path targetDirectory = tempFolder.newFolder().toPath();

        runRestore(archive, targetDirectory, config);
        runBackup(config);
    }


    @Test
    public void testDuplicateTask_backupAndRestore() throws Exception {
        BackupConfiguration config = createConfigurationForDuplicateTaskTest();
        Path archive = tempFolder.newFile().toPath();
        Path targetDirectory = tempFolder.newFolder().toPath();

        runBackup(config);
        runRestore(archive, targetDirectory, config);
    }

    private BackupConfiguration createConfigurationForDuplicateTaskTest() throws Exception {
        BackupConfiguration config = createConfiguration();
        thrown.expect(DuplicateTaskException.class);
        thrown.expectMessage("There is already a task running for configuration '" + config.getName() + "'");
        return config;
    }

    private ProgressPanel verifyRestore(Path archive, Path targetDirectory, BackupConfiguration config) {
        ProgressPanel pPanel = (ProgressPanel) window.panel(config.getName()).requireVisible().component();
        assertThatPanelHasTitle(pPanel, config.getName());
        verify(jBackup, times(1)).restore(config, archive, targetDirectory, pPanel);
        return pPanel;
    }

    private ProgressPanel verifyBackup(BackupConfiguration config) {
        ProgressPanel pPanel = (ProgressPanel) window.panel(config.getName()).requireVisible().component();
        assertThatPanelHasTitle(pPanel, config.getName());
        verify(jBackup, times(1)).backup(config, pPanel);
        return pPanel;
    }

    private void runBackup(final BackupConfiguration config) throws DuplicateTaskException {
        try {
            GuiActionRunner.execute(new GuiQuery<Object>() {
                protected Object executeInEDT() throws DuplicateTaskException {
                    panel.backup(config);
                    return null;
                }
            });
        } catch (UnexpectedException e) {
            throw (DuplicateTaskException) e.getCause();
        }
    }

    private void runRestore(final Path archive, final Path targetDirectory, final BackupConfiguration config) throws DuplicateTaskException {
        try {
            GuiActionRunner.execute(new GuiQuery<Object>() {
                protected Object executeInEDT() throws DuplicateTaskException {
                    panel.restore(config, archive, targetDirectory);
                    return null;
                }
            });
        } catch (UnexpectedException e) {
            throw (DuplicateTaskException) e.getCause();
        }
    }

    private void notifyTaskFinished(final Exception error, final ProgressPanel pPanel) {
        GuiActionRunner.execute(new GuiQuery<Object>() {
            protected Object executeInEDT() throws DuplicateTaskException {
                pPanel.taskFinished(error);
                return null;
            }
        });
    }
}
