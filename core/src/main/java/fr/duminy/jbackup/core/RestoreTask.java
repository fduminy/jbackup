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
package fr.duminy.jbackup.core;

import fr.duminy.jbackup.core.archive.ArchiveFactory;
import fr.duminy.jbackup.core.archive.Decompressor;
import fr.duminy.jbackup.core.archive.ProgressListener;
import fr.duminy.jbackup.core.util.FileDeleter;

import java.nio.file.Path;

class RestoreTask extends Task {
    private final Path archive;
    private final Path targetDirectory;

    RestoreTask(JBackup jbackup, BackupConfiguration config, Path archive, Path targetDirectory, ProgressListener listener) {
        super(jbackup, listener, config);
        this.archive = archive;
        this.targetDirectory = targetDirectory;
    }

    @Override
    protected void execute() throws Exception {
        FileDeleter deleter = jbackup.createFileDeleter();
        try {
            deleter.registerDirectory(targetDirectory);

            ArchiveFactory factory = config.getArchiveFactory();
            new Decompressor(factory).decompress(archive, targetDirectory, listener);
        } catch (Exception e) {
            deleter.deleteAll();
            throw e;
        }
    }
}
