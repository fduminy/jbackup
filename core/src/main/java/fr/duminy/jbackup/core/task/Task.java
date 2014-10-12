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

import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.Cancellable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

abstract class Task implements Callable<Void> {
    private static Logger LOG = LoggerFactory.getLogger(Task.class);

    protected final TaskListener listener;
    protected final BackupConfiguration config;
    protected final Cancellable cancellable;

    Task(TaskListener listener, BackupConfiguration config, Cancellable cancellable) {
        this.listener = listener;
        this.config = config;
        this.cancellable = cancellable;
    }

    @Override
    public final Void call() throws Exception {
        Throwable error = null;
        try {
            if (listener != null) {
                listener.taskStarted();
            }
            execute();
        } catch (Exception e) {
            LOG.error("Error in " + Task.this.getClass().getSimpleName() + " for configuration '" + config.getName() + "'", e);
            error = e;
            throw e;
        } finally {
            if (listener != null) {
                listener.taskFinished(error);
            }
        }

        return null;
    }

    protected final boolean isCancelled() {
        return (cancellable != null) && cancellable.isCancelled();
    }

    abstract protected void execute() throws Exception;
}
