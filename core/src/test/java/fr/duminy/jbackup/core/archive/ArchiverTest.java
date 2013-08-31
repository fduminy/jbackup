/**
 * JBackup is a software managing backups.
 *
 * Copyright (C) 2013-2013 Fabien DUMINY (fabien [dot] duminy [at] webmails [dot] com)
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

import java.io.File;
import java.io.IOException;

/**
 * Test for the class {@link Archiver}.
 */
public class ArchiverTest extends AbstractArchivingTest {
    public ArchiverTest() {
        super(false);
    }

    @Override
    protected void decompress(ArchiveFactory mockFactory, File archive, File directory, ProgressListener listener) throws IOException {
        Archiver archiver = new Archiver(mockFactory);
        if (listener == null) {
            archiver.decompress(archive, directory);
        } else {
            archiver.decompress(archive, directory, listener);
        }
    }

    @Override
    protected void compress(ArchiveFactory mockFactory, File sourceDirectory, File[] files, File archive, ProgressListener listener) throws IOException {
        Archiver archiver = new Archiver(mockFactory);
        if (listener == null) {
            archiver.compress(files, archive);
        } else {
            archiver.compress(files, archive, listener);
        }
    }
}