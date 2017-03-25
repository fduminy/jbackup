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
package fr.duminy.jbackup.core.task;

import fr.duminy.components.chain.CommandException;
import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.Cancellable;
import fr.duminy.jbackup.core.command.DecompressCommand;
import fr.duminy.jbackup.core.command.MutableJBackupContext;
import fr.duminy.jbackup.core.util.FileDeleter;

import java.nio.file.Path;
import java.util.function.Supplier;

public class RestoreTask extends FileCreatorTask {
    private final Path archive;
    private final Path targetDirectory;

    public RestoreTask(BackupConfiguration config, Path archive, Path targetDirectory,
                       Supplier<FileDeleter> deleterSupplier, TaskListener listener, Cancellable cancellable) {
        super(config, deleterSupplier, listener, cancellable);
        this.archive = archive;
        this.targetDirectory = targetDirectory;
    }

    @Override
    protected void executeTask(FileDeleter deleter) throws TaskException {
        deleter.registerDirectory(targetDirectory);

        MutableJBackupContext context = new MutableJBackupContext();
        context.setFactory(config.getArchiveFactory());
        context.setFileDeleter(deleter);
        context.setListener(listener);
        context.setTargetDirectory(targetDirectory);
        context.setArchivePath(archive);
        context.setCancellable(cancellable);

        try {
            createDecompressCommand().execute(context);
        } catch (CommandException e) {
            throw new TaskException(e);
        }
    }

    DecompressCommand createDecompressCommand() {
        return new DecompressCommand();
    }
}
