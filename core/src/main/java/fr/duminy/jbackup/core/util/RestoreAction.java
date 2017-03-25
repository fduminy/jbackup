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
package fr.duminy.jbackup.core.util;

import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.JBackup;

import java.nio.file.Path;
import java.util.concurrent.Future;

/**
 * Class calling {@link fr.duminy.jbackup.core.JBackup#restore(fr.duminy.jbackup.core.BackupConfiguration, java.nio.file.Path, java.nio.file.Path)} on a {@link fr.duminy.jbackup.core.JBackup}.
 */
public class RestoreAction extends JBackupAction {
    private final Path archive;
    private final Path targetDirectory;

    public RestoreAction(BackupConfiguration config, Path archive, Path targetDirectory) {
        super(config);
        this.archive = archive;
        this.targetDirectory = targetDirectory;
    }

    @Override
    public Future<Void> executeAction(JBackup jBackup) {
        return jBackup.restore(config, archive, targetDirectory);
    }
}
