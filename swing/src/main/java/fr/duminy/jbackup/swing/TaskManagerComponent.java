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
import fr.duminy.jbackup.core.archive.ProgressListener;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Abstract component displaying the current state of the task manager.
 */
abstract class TaskManagerComponent<T extends ProgressListener> extends JPanel implements BackupConfigurationActions {
    private final JBackup jBackup;
    private final Map<String, T> taskPanels = new HashMap<>();

    TaskManagerComponent(LayoutManager layout, JBackup jBackup) {
        super(layout);
        this.jBackup = jBackup;
    }

    @Override
    public final void backup(BackupConfiguration config) throws DuplicateTaskException {
        checkNoTaskIsAlreadyRunningFor(config);
        final T progressListener = createProgressListener(config);
        taskPanels.put(config.getName(), progressListener);
        jBackup.addProgressListener(config.getName(), progressListener);
        Future<Void> task = jBackup.backup(config);
        associate(progressListener, task);
    }

    @Override
    public final void restore(BackupConfiguration config, Path archive, Path targetDirectory) throws DuplicateTaskException {
        checkNoTaskIsAlreadyRunningFor(config);
        final T progressListener = createProgressListener(config);
        taskPanels.put(config.getName(), progressListener);
        jBackup.addProgressListener(config.getName(), progressListener);
        Future<Void> task = jBackup.restore(config, archive, targetDirectory);
        associate(progressListener, task);
    }

    abstract protected void associate(T progressListener, Future<Void> task);

    abstract protected T createProgressListener(BackupConfiguration config);

    abstract protected boolean removeIfFinished(T progressListener, BackupConfiguration config);

    private void checkNoTaskIsAlreadyRunningFor(BackupConfiguration config) throws DuplicateTaskException {
        T pPanel = taskPanels.get(config.getName());
        if (pPanel != null) {
            if (removeIfFinished(pPanel, config)) {
                taskPanels.remove(config.getName());
            } else {
                throw new DuplicateTaskException(config.getName());
            }
        }
    }
}
