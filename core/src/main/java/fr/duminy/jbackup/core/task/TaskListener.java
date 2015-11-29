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
package fr.duminy.jbackup.core.task;

/**
 * Interface used to notify progress of a task.
 */
public interface TaskListener {
    /**
     * Notify that the task has just started (by computing the totalSize).
     */
    void taskStarted();

    /**
     * Notify that the total number of bytes has been computed. Note that this method must be called before {@link #progress(long)}.
     *
     * @param totalSize The total number of bytes to be read for (de)compression.
     */
    void totalSizeComputed(long totalSize);

    /**
     * Notify that some bytes have been read.
     *
     * @param totalReadBytes The total number of bytes read since the begin of (de)compression.
     */
    void progress(long totalReadBytes);

    /**
     * Notify that the task has finished and possibly failed.
     *
     * @param error If not null, provides details about the failure.
     */
    void taskFinished(Throwable error);
}
