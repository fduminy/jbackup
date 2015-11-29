/**
 * JBackup is a software managing backups.
 *
 * Copyright (C) 2013-2015 Fabien DUMINY (fabien [dot] duminy [at] webmails [dot] com)
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

import fr.duminy.jbackup.core.archive.ProgressListener;

import javax.swing.*;
import java.nio.file.Path;
import java.util.concurrent.Future;

/**
 * Represents JBackup features.
 */
public interface JBackup {
    Future<Void> backup(BackupConfiguration config);

    Future<Void> restore(BackupConfiguration config, Path archive, Path targetDirectory);

    /**
     * Add a listener for all configurations.
     *
     * @param listener
     */
    void addProgressListener(ProgressListener listener);

    /**
     * Add a listener for a given configuration.
     *
     * @param configurationName Name of the configuration to listen.
     * @param listener
     */
    void addProgressListener(String configurationName, ProgressListener listener);

    /**
     * Remove a listener for all configurations.
     *
     * @param listener
     */
    void removeProgressListener(ProgressListener listener);

    /**
     * Remove a listener for a given configuration.
     * @param configurationName Name of the configuration to stop listening.
     * @param listener
     */
    void removeProgressListener(String configurationName, ProgressListener listener);

    Timer shutdown(final TerminationListener listener) throws InterruptedException;

    public static interface TerminationListener {
        void terminated();
    }
}
