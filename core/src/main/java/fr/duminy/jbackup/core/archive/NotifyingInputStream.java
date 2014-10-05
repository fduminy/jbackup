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
package fr.duminy.jbackup.core.archive;

import fr.duminy.jbackup.core.task.TaskListener;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.lang3.mutable.MutableLong;

import java.io.InputStream;

class NotifyingInputStream extends CountingInputStream {
    static InputStream createCountingInputStream(final TaskListener listener, final MutableLong processedSize, final InputStream input) {
        InputStream result = input;

        if (listener != null) {
            result = new NotifyingInputStream(listener, processedSize, input);
        }

        return result;
    }

    private final TaskListener listener;
    private final MutableLong processedSize;

    /**
     * Constructs a new CountingInputStream.
     *
     * @param input the InputStream to delegate to
     */
    private NotifyingInputStream(final TaskListener listener, final MutableLong processedSize, final InputStream input) {
        super(input);
        this.listener = listener;
        this.processedSize = processedSize;
    }

    @Override
    protected synchronized void afterRead(int n) {
        super.afterRead(n);

        if (n > 0) {
            processedSize.add(n);
            listener.progress(processedSize.longValue());
        }
    }
}
