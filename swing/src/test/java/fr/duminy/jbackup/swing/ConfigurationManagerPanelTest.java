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
import org.fest.assertions.Assertions;
import org.fest.swing.fixture.JListFixture;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static fr.duminy.jbackup.swing.ConfigurationManagerPanel.COMPARATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

/**
 * Tests for class {@link ConfigurationManagerPanel}.
 */
@RunWith(Theories.class)
public class ConfigurationManagerPanelTest extends AbstractSwingTest {
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

            final ConfigurationManager mgr = manager;
            panel = buildAndShowWindow(new Supplier<ConfigurationManagerPanel>() {
                @Override
                public ConfigurationManagerPanel get() {
                    try {
                        return new ConfigurationManagerPanel(mgr);
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
        return ConfigurationManagerTest.createConfiguration("name" + i);
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
