/**
 * JBackup is a software managing backups.
 *
 * Copyright (C) 2013-2013 Fabien DUMINY (fabien [dot] duminy [at] webmails [dot] com)
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
import com.google.common.collect.Iterables;
import fr.duminy.components.swing.AbstractSwingTest;
import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.ConfigurationManager;
import fr.duminy.jbackup.core.ConfigurationManagerTest;
import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactoryTest;
import org.apache.commons.io.IOUtils;
import org.fest.assertions.Assertions;
import org.fest.swing.fixture.JListFixture;
import org.fest.swing.fixture.JOptionPaneFixture;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static fr.duminy.jbackup.swing.ConfigurationManagerPanel.COMPARATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for class {@link ConfigurationManagerPanel}.
 */
@RunWith(Theories.class)
public class ConfigurationManagerPanelTest extends AbstractSwingTest {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationManagerPanelTest.class);
    @DataPoint
    public static final int INIT_NO_CONFIG = 0;
    @DataPoint
    public static final int INIT_1_CONFIG = 1;
    @DataPoint
    public static final int INIT_2_CONFIGS = 2;
    @DataPoint
    public static final int INIT_3_CONFIGS = 3;

    private ConfigurationManagerPanel panel;
    private File configDir;
    private ConfigurationManager manager;
    private BackupConfigurationActions configActions;

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Theory
    public void testInit(int nbConfigurations) {
        List<BackupConfiguration> expectedConfigs = init(nbConfigurations);
        assertFormValues(expectedConfigs);
    }

    @Theory
    public void testAdd(int nbConfigurations) throws Exception {
        List<BackupConfiguration> expectedConfigs = init(nbConfigurations);
        BackupConfiguration config = new BackupConfiguration();
        config.setName(ConfigurationManagerPanel.DEFAULT_CONFIG_NAME);
        expectedConfigs.add(config);

        window.button("addButton").click();

        assertFormValues(expectedConfigs);
        assertThat(manager.getBackupConfigurations()).usingElementComparator(COMPARATOR).containsOnlyOnce(Iterables.toArray(expectedConfigs, BackupConfiguration.class));
    }

    @Theory
    public void testRemove(int nbConfigurations) throws Exception {
        assumeTrue(nbConfigurations > 0);

        List<BackupConfiguration> expectedConfigs = init(nbConfigurations);
        BackupConfiguration config = manager.getBackupConfigurations().get(0);
        for (BackupConfiguration c : expectedConfigs) {
            if (c.getName().equals(config.getName())) {
                expectedConfigs.remove(c);
                break;
            }
        }

        window.list("configurations").selectItem(config.getName());
        window.button("removeButton").click();

        assertFormValues(expectedConfigs);
        assertThat(manager.getBackupConfigurations()).usingElementComparator(COMPARATOR).containsOnlyOnce(Iterables.toArray(expectedConfigs, BackupConfiguration.class));
    }

    @Theory
    public void testBackup(int nbConfigurations) throws Exception {
        assumeTrue(nbConfigurations > 0);
        List<BackupConfiguration> configs = init(nbConfigurations);

        BackupConfiguration expectedConfig = configs.get(nbConfigurations - 1);
        window.list("configurations").selectItem(expectedConfig.getName());
        window.button("backupButton").requireToolTip(Messages.BACKUP_MESSAGE).click();

        ArgumentCaptor<BackupConfiguration> actualConfig = ArgumentCaptor.forClass(BackupConfiguration.class);
        verify(configActions, times(1)).backup(actualConfig.capture());
        verifyNoMoreInteractions(configActions);

        ConfigurationManagerTest.assertAreEquals(expectedConfig, actualConfig.getValue());
    }

    @Test
    public void testBackup_noSelection() throws Exception {
        init(1);

        window.list("configurations").selectItem(0).clearSelection();
        window.button("backupButton").requireDisabled();
    }

    @Test
    public void testBackup_multipleSelection() throws Exception {
        init(3);

        window.list("configurations").selectItems(0, 2);
        window.button("backupButton").requireEnabled();
    }

    @Theory
    public void testRestore_cancel(int nbConfigurations) throws Exception {
        testRestore(nbConfigurations, false);
    }

    @Theory
    public void testRestore_ok(int nbConfigurations) throws Exception {
        testRestore(nbConfigurations, true);
    }

    private void testRestore(int nbConfigurations, boolean ok) throws Exception {
        assumeTrue(nbConfigurations > 0);
        List<BackupConfiguration> configs = init(nbConfigurations);

        BackupConfiguration expectedConfig = configs.get(nbConfigurations - 1);
        File expectedArchive = createArchive(expectedConfig);
        File expectedTargetDirectory = tempFolder.newFile("targetDir");
        window.list("configurations").selectItem(expectedConfig.getName());
        window.button("restoreButton").requireToolTip(Messages.RESTORE_MESSAGE).click();

        JOptionPaneFixture dialog = window.optionPane();
        dialog.requireQuestionMessage();
        dialog.requireTitle("Restore backup '" + expectedConfig.getName() + "'");
        dialog.panel("archive").textBox("pathField").requireText(expectedArchive.getAbsolutePath());
        dialog.panel("targetDirectory").textBox("pathField").requireText("").setText(expectedTargetDirectory.getAbsolutePath());
        if (ok) {
            dialog.okButton().click();

            ArgumentCaptor<BackupConfiguration> actualConfig = ArgumentCaptor.forClass(BackupConfiguration.class);
            verify(configActions, times(1)).restore(actualConfig.capture(), eq(expectedArchive), eq(expectedTargetDirectory));
            verifyNoMoreInteractions(configActions);

            ConfigurationManagerTest.assertAreEquals(expectedConfig, actualConfig.getValue());
        } else {
            dialog.cancelButton().click();

            verify(configActions, never()).restore(any(BackupConfiguration.class), any(File.class), any(File.class));
            verifyNoMoreInteractions(configActions);
        }
    }

    @Test
    public void testRestore_noSelection() throws Exception {
        init(1);

        window.list("configurations").selectItem(0).clearSelection();
        window.button("restoreButton").requireDisabled();
    }

    @Test
    public void testRestore_multipleSelection() throws Exception {
        init(3);

        window.list("configurations").selectItems(0, 2);
        window.button("restoreButton").requireDisabled();
    }

    private File createArchive(BackupConfiguration config) throws IOException {
        File result = new File(config.getTargetDirectory(), "archive.zip");
        IOUtils.copy(ZipArchiveFactoryTest.getArchive(), new FileOutputStream(result));
        return result;
    }

    private List<BackupConfiguration> init(int nbConfigurations) {
        try {
            configDir = tempFolder.newFolder();

            // add configurations in the directory
            ConfigurationManager tmpManager = new ConfigurationManager(configDir);
            List<BackupConfiguration> configs = createConfigurations(nbConfigurations);
            for (BackupConfiguration config : configs) {
                tmpManager.addBackupConfiguration(config);
            }

            manager = new ConfigurationManager(configDir);
            configActions = mock(BackupConfigurationActions.class);

            final ConfigurationManager mgr = manager;
            panel = buildAndShowWindow(new Supplier<ConfigurationManagerPanel>() {
                @Override
                public ConfigurationManagerPanel get() {
                    try {
                        return new ConfigurationManagerPanel(mgr, configActions);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            return configs;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<BackupConfiguration> createConfigurations(int nbConfigurations) {
        List<BackupConfiguration> configs = new ArrayList<>();
        for (int i = 0; i < nbConfigurations; i++) {
            configs.add(createConfiguration(i));
        }
        return configs;
    }

    private BackupConfiguration createConfiguration(int i) {
        Path targetDirectory = tempFolder.newFolder("archiveDirectory").toPath();
        return ConfigurationManagerTest.createConfiguration("name" + i, targetDirectory);
    }

    private void assertFormValues(List<BackupConfiguration> configs) {
        JListFixture jl = window.list("configurations").requireVisible().requireEnabled();
        String[] renderedConfigs = new String[configs.size()];
        for (int i = 0; i < renderedConfigs.length; i++) {
            renderedConfigs[i] = configs.get(i).getName();
        }

        String[] actualContents = jl.contents();
        Arrays.sort(actualContents); //TODO enable sort directly in the ListModel
        Assertions.assertThat(actualContents).as("configurations").isEqualTo(renderedConfigs);
    }
}
