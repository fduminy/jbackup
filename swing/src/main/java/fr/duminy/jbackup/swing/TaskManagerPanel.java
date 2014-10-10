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

import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.JBackup;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * This is the panel that show current tasks.
 */
public class TaskManagerPanel extends JPanel implements BackupConfigurationActions {
    private final JBackup jBackup;
    private final Map<String, ProgressPanel> taskPanels = new HashMap<>();

    public TaskManagerPanel(JBackup jBackup) {
        super(new GridLayout(1, 1));
        this.jBackup = jBackup;
    }

    @Override
    public void backup(BackupConfiguration config) throws DuplicateTaskException {
        checkNoTaskIsAlreadyRunningFor(config);
        final ProgressPanel progressPanel = createProgressPanel(config);
        jBackup.addProgressListener(config.getName(), progressPanel);
        Future<Void> task = jBackup.backup(config);
        progressPanel.setTask(task);
    }

    @Override
    public void restore(BackupConfiguration config, Path archive, Path targetDirectory) throws DuplicateTaskException {
        checkNoTaskIsAlreadyRunningFor(config);
        final ProgressPanel progressPanel = createProgressPanel(config);
        jBackup.addProgressListener(config.getName(), progressPanel);
        Future<Void> task = jBackup.restore(config, archive, targetDirectory);
        progressPanel.setTask(task);
    }

    private ProgressPanel createProgressPanel(BackupConfiguration config) {
        ProgressPanel pPanel = new ProgressPanel(config.getName());
        pPanel.setName(config.getName());
        taskPanels.put(config.getName(), pPanel);

        GridLayout layout = (GridLayout) getLayout();
        if (getComponentCount() > 0) {
            layout.setRows(layout.getRows() + 1);
        }
        add(pPanel);
        revalidate();

        return pPanel;
    }

    private void checkNoTaskIsAlreadyRunningFor(BackupConfiguration config) throws DuplicateTaskException {
        ProgressPanel pPanel = taskPanels.get(config.getName());
        if (pPanel != null) {
            if (pPanel.isFinished()) {
                taskPanels.remove(config.getName());
                remove(pPanel);
            } else {
                throw new DuplicateTaskException(config.getName());
            }
        }
    }
}
