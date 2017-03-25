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

import java.awt.*;
import java.util.concurrent.Future;

/**
 * This is the panel that show current tasks.
 */
public class TaskManagerPanel extends TaskManagerComponent<ProgressPanel> {
    public TaskManagerPanel(JBackup jBackup) {
        super(new GridLayout(1, 1), jBackup);
    }

    @Override
    protected void associate(ProgressPanel progressListener, Future task) {
        progressListener.setTask(task);
    }

    @Override
    protected ProgressPanel createProgressListener(BackupConfiguration config) {
        ProgressPanel pPanel = new ProgressPanel(config.getName());
        pPanel.setName(config.getName());

        GridLayout layout = (GridLayout) getLayout();
        if (getComponentCount() > 0) {
            layout.setRows(layout.getRows() + 1);
        }

        return pPanel;
    }
}
