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
import fr.duminy.components.swing.list.AbstractMutableListModel;
import fr.duminy.components.swing.listpanel.ListPanel;
import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.ConfigurationManager;
import org.apache.commons.beanutils.BeanComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.Comparator;

/**
 * Panel representing a {@link fr.duminy.jbackup.core.ConfigurationManager}.
 */
public class ConfigurationManagerPanel extends ListPanel<JList<BackupConfiguration>, BackupConfiguration> {
    static final Comparator<BackupConfiguration> COMPARATOR = new BeanComparator("name");
    public static final String DEFAULT_CONFIG_NAME = "newConfiguration";

    public ConfigurationManagerPanel(final ConfigurationManager manager, final BackupConfigurationActions configActions) throws Exception {
        super(createList(manager), new Supplier<BackupConfiguration>() {
            @Override
            public BackupConfiguration get() {
                BackupConfiguration config = new BackupConfiguration();
                config.setName(DEFAULT_CONFIG_NAME);
                return config;
            }
        });

        addUserButton("backupButton", new BackupAction(configActions));
        addUserButton("restoreButton", new RestoreAction(this, configActions));
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
            } catch (IOException e) {
                config = null;
                LOG.error(e.getMessage(), e);
            }
            return config;
        }

        @Override
        public void add(int i, BackupConfiguration backupConfiguration) {
            try {
                manager.addBackupConfiguration(backupConfiguration);
                i = getSize();
                fireIntervalAdded(this, i, i);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
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
