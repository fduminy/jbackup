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

import fr.duminy.components.swing.listpanel.AbstractUserItemAction;
import fr.duminy.jbackup.core.BackupConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.KeyEvent;

public class BackupAction extends AbstractUserItemAction<BackupConfiguration, Messages> {
    private static final Logger LOG = LoggerFactory.getLogger(BackupAction.class);

    private final BackupConfigurationActions configActions;

    /**
     * @param configActions
     */
    public BackupAction(BackupConfigurationActions configActions) {
        super(KeyEvent.VK_CONTROL + KeyEvent.VK_B, "backup.png", Messages.class);
        this.configActions = configActions;
    }

    @Override
    protected String getShortDescription(Messages bundle) {
        return bundle.backup();
    }

    @Override
    public void executeAction(BackupConfiguration config) {
        try {
            configActions.backup(config);
        } catch (DuplicateTaskException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    public void updateState(int[] selectedItems, int listSize) {
        setEnabled(selectedItems.length > 0);
    }
}