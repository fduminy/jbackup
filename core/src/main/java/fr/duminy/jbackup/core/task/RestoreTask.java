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
package fr.duminy.jbackup.core.task;

import com.google.common.base.Supplier;
import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.archive.ArchiveFactory;
import fr.duminy.jbackup.core.archive.Decompressor;
import fr.duminy.jbackup.core.archive.ProgressListener;
import fr.duminy.jbackup.core.util.FileDeleter;

import java.nio.file.Path;

public class RestoreTask extends Task {
    private final Path archive;
    private final Path targetDirectory;
    private final Supplier<FileDeleter> deleterSupplier;

    public RestoreTask(BackupConfiguration config, Path archive, Path targetDirectory,
                       Supplier<FileDeleter> deleterSupplier, ProgressListener listener) {
        super(listener, config);
        this.archive = archive;
        this.deleterSupplier = deleterSupplier;
        this.targetDirectory = targetDirectory;
    }

    @Override
    protected void execute() throws Exception {
        FileDeleter deleter = deleterSupplier.get();
        try {
            deleter.registerDirectory(targetDirectory);

            ArchiveFactory factory = config.getArchiveFactory();
            createDecompressor(factory).decompress(archive, targetDirectory, listener);
        } catch (Exception e) {
            deleter.deleteAll();
            throw e;
        }
    }

    Decompressor createDecompressor(ArchiveFactory factory) {
        return new Decompressor(factory);
    }
}