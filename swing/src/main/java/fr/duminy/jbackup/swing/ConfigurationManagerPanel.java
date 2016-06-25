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
package fr.duminy.jbackup.swing;

import fr.duminy.components.swing.form.DefaultFormBuilder;
import fr.duminy.components.swing.form.FormBuilder;
import fr.duminy.components.swing.form.StringPathTypeMapper;
import fr.duminy.components.swing.list.AbstractMutableListModel;
import fr.duminy.components.swing.listpanel.ListPanel;
import fr.duminy.components.swing.listpanel.SimpleItemManager;
import fr.duminy.components.swing.path.JPath;
import fr.duminy.components.swing.path.JPathBuilder;
import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.ConfigurationException;
import fr.duminy.jbackup.core.ConfigurationManager;
import fr.duminy.jbackup.core.archive.ArchiveFactory;
import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.lang3.builder.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Comparator;

import static fr.duminy.components.swing.listpanel.SimpleItemManager.ContainerType.DIALOG;
import static fr.duminy.components.swing.listpanel.StandardListPanelFeature.EDITING;

/**
 * Panel representing a {@link fr.duminy.jbackup.core.ConfigurationManager}.
 */
public class ConfigurationManagerPanel extends ListPanel<BackupConfiguration, JList<BackupConfiguration>> {
    private static final Builder<JPath> DIRECTORIES_BUILDER = new JPathBuilder().select(JPath.SelectionMode.DIRECTORIES_ONLY);

    static final Comparator<BackupConfiguration> COMPARATOR = new BeanComparator("name");
    static final String BACKUP_BUTTON_NAME = "backupButton";
    static final String RESTORE_BUTTON_NAME = "restoreButton";

    public static final String DEFAULT_CONFIG_NAME = "newConfiguration";

    public ConfigurationManagerPanel(final ConfigurationManager manager, final BackupConfigurationActions configActions, JComponent parent, ArchiveFactory... factories) throws Exception {
        super(createList(manager), new SimpleItemManager<BackupConfiguration>(BackupConfiguration.class, createBuilder(parent, factories), parent, "Configuration", DIALOG) {
            @Override
            protected void initItem(BackupConfiguration item) {
                item.setName(DEFAULT_CONFIG_NAME);
            }
        });

        addFeature(EDITING);
        addUserButton(BACKUP_BUTTON_NAME, new BackupAction(configActions));
        addUserButton(RESTORE_BUTTON_NAME, new RestoreAction(this, configActions));
    }

    private static FormBuilder<BackupConfiguration> createBuilder(final JComponent parent, final ArchiveFactory... factories) {
        return new DefaultFormBuilder<BackupConfiguration>(BackupConfiguration.class) {
            @Override
            protected void configureBuilder(org.formbuilder.FormBuilder<BackupConfiguration> builder) {
                super.configureBuilder(builder);
                builder.useForProperty("sources", new SourceListTypeMapper(parent));
                builder.useForProperty("archiveFactory", new ArchiveFactoryTypeMapper(factories));
                builder.useForProperty("targetDirectory", new StringPathTypeMapper(DIRECTORIES_BUILDER));
                UIManager.getDefaults().put(BackupConfiguration.class.getSimpleName() + ".xmlVersion.hidden", true);
            }
        };
    }

    private static JList<BackupConfiguration> createList(ConfigurationManager manager) {
        //TODO enable sorting : JList list = new JList(new SortedListModel(new DefaultListModel(), COMPARATOR));
        JList<BackupConfiguration> list = new JList<>(new Model(manager));
        list.setCellRenderer(BackupConfigurationRenderer.INSTANCE);
        list.setName("configurations");
        return list;
    }

    private static class Model extends AbstractMutableListModel<BackupConfiguration> {
        private static final Logger LOG = LoggerFactory.getLogger(Model.class);

        private final ConfigurationManager manager;

        public Model(ConfigurationManager manager) {
            this.manager = manager;
        }

        @Override
        public BackupConfiguration remove(int i) {
            BackupConfiguration config = getElementAt(i);
            try {
                manager.removeBackupConfiguration(config);
                fireIntervalRemoved(this, i, i);
            } catch (ConfigurationException e) {
                config = null;
                LOG.error(e.getMessage(), e);
            }
            return config;
        }

        @Override
        public void add(int index, BackupConfiguration backupConfiguration) {
            try {
                manager.addBackupConfiguration(backupConfiguration);
                index = getSize();
                fireIntervalAdded(this, index, index);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }

        @Override
        public BackupConfiguration set(int index, BackupConfiguration backupConfiguration) {
            try {
                BackupConfiguration oldConfig = manager.setBackupConfiguration(index, backupConfiguration);
                fireContentsChanged(this, index, index);
                return oldConfig;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                return null;
            }
        }

        @Override
        public int getSize() {
            try {
                return manager.getBackupConfigurations().size();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                return 0;
            }
        }

        @Override
        public BackupConfiguration getElementAt(int index) {
            try {
                return manager.getBackupConfigurations().get(index);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                return null;
            }
        }
    }
}
