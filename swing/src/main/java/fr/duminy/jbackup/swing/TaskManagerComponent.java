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
import fr.duminy.jbackup.core.archive.ProgressListener;
import fr.duminy.jbackup.core.util.BackupAction;
import fr.duminy.jbackup.core.util.JBackupAction;
import fr.duminy.jbackup.core.util.RestoreAction;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Abstract component displaying the current state of the task manager.
 */
abstract class TaskManagerComponent<T extends JComponent & ProgressListener> extends JPanel implements BackupConfigurationActions {
    private final transient JBackup jBackup;
    private final Map<String, T> taskPanels = new HashMap<>();
    private final JLabel emptyLabel = new JLabel("No task are running");

    TaskManagerComponent(LayoutManager layout, JBackup jBackup) {
        super(layout);
        this.jBackup = jBackup;
        add(emptyLabel);
    }

    @Override
    public final void backup(BackupConfiguration config) throws DuplicateTaskException {
        executeAction(new BackupAction(config), config);
    }

    @Override
    public final void restore(BackupConfiguration config, Path archive, Path targetDirectory) throws DuplicateTaskException {
        executeAction(new RestoreAction(config, archive, targetDirectory), config);
    }

    protected abstract void associate(T progressListener, Future<Void> task);

    protected abstract T createProgressListener(BackupConfiguration config);

    private void executeAction(JBackupAction action, BackupConfiguration config) throws DuplicateTaskException {
        if (taskPanels.containsKey(config.getName())) {
            throw new DuplicateTaskException(config.getName());
        }

        final T progressListener = createProgressListener(config);
        taskPanels.put(config.getName(), progressListener);
        add(progressListener);
        remove(emptyLabel);
        revalidate();

        jBackup.addProgressListener(config.getName(), progressListener);
        jBackup.addProgressListener(config.getName(), new ProgressListener() {
            @Override
            public void taskStarted(String configurationName) {
                // nothing to do
            }

            @Override
            public void totalSizeComputed(String configurationName, long totalSize) {
                // nothing to do
            }

            @Override
            public void progress(String configurationName, long totalReadBytes) {
                // nothing to do
            }

            @Override
            public void taskFinished(String configurationName, Throwable error) {
                taskPanels.remove(configurationName);
                remove(progressListener);
                jBackup.removeProgressListener(configurationName, progressListener);
                jBackup.removeProgressListener(configurationName, this);

                if (taskPanels.isEmpty()) {
                    add(emptyLabel);
                }
                revalidate();
            }
        });
        associate(progressListener, action.executeAction(jBackup));
    }
}
