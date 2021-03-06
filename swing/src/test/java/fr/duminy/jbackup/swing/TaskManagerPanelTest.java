/**
 * JBackup is a software managing backups.
 *
 * Copyright (C) 2013-2017 Fabien DUMINY (fabien [dot] duminy [at] webmails [dot] com)
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

import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.JBackup;
import org.assertj.swing.exception.ComponentLookupException;
import org.assertj.swing.fixture.JPanelFixture;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theory;
import org.mockito.InOrder;

import java.nio.file.Path;

import static fr.duminy.jbackup.core.ConfigurationManagerTest.createConfiguration;
import static fr.duminy.jbackup.swing.ProgressPanelTest.assertThatPanelHasTitle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;

/**
 * Tests for class {@link TaskManagerPanel}.
 */
public class TaskManagerPanelTest extends TaskManagerComponentTest<ProgressPanel, TaskManagerPanel> {
    @DataPoint
    public static final int INIT_NO_CONFIG = 0;
    @DataPoint
    public static final int INIT_1_CONFIG = 1;
    @DataPoint
    public static final int INIT_2_CONFIGS = 2;

    @Override
    protected TaskManagerPanel createComponent(JBackup jBackup) {
        return new TaskManagerPanel(jBackup);
    }

    @Test
    public void testInit() throws Exception {
        verifyNoTaskAreRunning();
    }

    @Theory
    public void testBackup(int nbConfigurations) throws Exception {
        final BackupConfiguration[] configs = new BackupConfiguration[nbConfigurations];
        for (int i = 0; i < nbConfigurations; i++) {
            configs[i] = createConfiguration("name" + i);
            runBackup(configs[i]);
        }

        int expectedComponentCount = (configs.length == 0) ? 1 : configs.length;
        assertThat(panel.getComponentCount()).as("componentCount").isEqualTo(expectedComponentCount);
        for (int i = 0; i < configs.length; i++) {
            boolean lastConfig = (i == (configs.length - 1));
            verifyBackup(configs[i], lastConfig);
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

        verifyBackup(config, true);
        notifyTaskFinished(config, error);
        verifyNoTaskAreRunning();
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

        int expectedComponentCount = (configs.length == 0) ? 1 : configs.length;
        assertThat(panel.getComponentCount()).as("componentCount").isEqualTo(expectedComponentCount);
        for (int i = 0; i < configs.length; i++) {
            boolean lastConfig = (i == (configs.length - 1));
            verifyRestore(archives[i], targetDirectories[i], configs[i], lastConfig);
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

        verifyRestore(archive, targetDirectory, config, true);
        notifyTaskFinished(config, error);
        verifyNoTaskAreRunning();
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

    protected final ProgressPanel verifyRestore(Path archive, Path targetDirectory, BackupConfiguration config, boolean lastConfig) {
        ProgressPanel pPanel = findComponentFor(config);
        assertThatPanelHasTitle(pPanel, config.getName());

        InOrder inOrder = inOrder(getJBackup());
        inOrder.verify(getJBackup(), times(1)).addProgressListener(config.getName(), pPanel);
        inOrder.verify(getJBackup(), times(1)).restore(config, archive, targetDirectory);
        if (lastConfig) {
            inOrder.verifyNoMoreInteractions();
        }

        return pPanel;
    }

    protected final ProgressPanel verifyBackup(BackupConfiguration config, boolean lastConfig) {
        ProgressPanel pPanel = findComponentFor(config);

        InOrder inOrder = inOrder(getJBackup());
        inOrder.verify(getJBackup(), times(1)).addProgressListener(config.getName(), pPanel);
        inOrder.verify(getJBackup(), times(1)).backup(config);
        if (lastConfig) {
            inOrder.verifyNoMoreInteractions();
        }

        return pPanel;
    }

    private ProgressPanel findComponentFor(BackupConfiguration config) {
        try {
            return (ProgressPanel) window.panel(config.getName()).target();
        } catch (ComponentLookupException cle) {
            return null;
        }
    }

    private void verifyNoTaskAreRunning() {
        assertThat(panel.getComponentCount()).as("componentCount").isEqualTo(1);
        new JPanelFixture(getRobot(), panel).label().requireText("No task are running");
    }
}
