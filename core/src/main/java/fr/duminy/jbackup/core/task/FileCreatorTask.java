/**
 * JBackup is a software managing backups.
 *
 * Copyright (C) 2013-2016 Fabien DUMINY (fabien [dot] duminy [at] webmails [dot] com)
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

import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.Cancellable;
import fr.duminy.jbackup.core.util.FileDeleter;

import java.util.Objects;
import java.util.function.Supplier;

public abstract class FileCreatorTask extends AbstractTask {
    private final Supplier<FileDeleter> deleterSupplier;

    public FileCreatorTask(BackupConfiguration config, Supplier<FileDeleter> deleterSupplier,
                           TaskListener listener, Cancellable cancellable) {
        super(listener, config, cancellable);
        Objects.requireNonNull(deleterSupplier, "deleterSupplier is null");
        this.deleterSupplier = deleterSupplier;
    }

    @Override
    protected final void execute() throws Exception {
        FileDeleter deleter = deleterSupplier.get();
        boolean taskComplete = false;
        try {
            executeTask(deleter);
            taskComplete = !isCancelled();
        } finally {
            if (!taskComplete) {
                deleter.deleteAll();
            }
        }
    }

    abstract protected void executeTask(FileDeleter deleter) throws Exception;
}
