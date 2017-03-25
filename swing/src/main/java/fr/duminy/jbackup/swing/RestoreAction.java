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

import fr.duminy.components.swing.form.DefaultFormBuilder;
import fr.duminy.components.swing.form.FormBuilder;
import fr.duminy.components.swing.form.JFormPane;
import fr.duminy.components.swing.listpanel.AbstractUserItemAction;
import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.ConfigurationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.nio.file.Path;

public class RestoreAction extends AbstractUserItemAction<BackupConfiguration, Messages> {
    private static final Logger LOG = LoggerFactory.getLogger(RestoreAction.class);

    private final JComponent parent;
    private final transient BackupConfigurationActions configActions;

    public RestoreAction(JComponent parent, BackupConfigurationActions configActions) {
        super(KeyEvent.VK_CONTROL + KeyEvent.VK_R, "restore.png", Messages.class);
        this.parent = parent;
        this.configActions = configActions;
    }

    @Override
    protected String getShortDescription(Messages bundle) {
        return bundle.restore();
    }

    @Override
    public void executeAction(BackupConfiguration config) {
        try {
            FormBuilder<RestoreParameters> builder = new DefaultFormBuilder<>(RestoreParameters.class);
            RestoreParameters params = new RestoreParameters();
            params.setArchive(ConfigurationManager.getLatestArchive(config));
            String title = getBundle().restoreDialogTitle(config.getName());
            if ((params = JFormPane.showFormDialog(parent, builder, params, title, JFormPane.Mode.CREATE)) != null) {
                configActions.restore(config, params.getArchive(), params.getTargetDirectory());
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    public void updateState(int[] selectedItems, int listSize) {
        setEnabled(selectedItems.length == 1);
    }

    public static class RestoreParameters {
        private Path archive;
        private Path targetDirectory;

        public Path getArchive() {
            return archive;
        }

        public void setArchive(Path archive) {
            this.archive = archive;
        }

        public Path getTargetDirectory() {
            return targetDirectory;
        }

        public void setTargetDirectory(Path targetDirectory) {
            this.targetDirectory = targetDirectory;
        }
    }
}