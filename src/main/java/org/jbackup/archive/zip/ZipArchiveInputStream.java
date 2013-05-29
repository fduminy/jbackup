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
package org.jbackup.archive.zip;

import org.jbackup.archive.ArchiveInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class ZipArchiveInputStream implements ArchiveInputStream {
    final private ZipInputStream input;

    ZipArchiveInputStream(InputStream input) {
        this.input = new ZipInputStream(input);
    }

    private static class ZipBackupEntry extends Entry {
        private final ZipInputStream zipInput;

        private ZipBackupEntry(ZipInputStream zipInput, String name) {
            super(name);
            this.zipInput = zipInput;
        }

        @Override
        public void close() throws IOException {
            zipInput.closeEntry();
        }

        @Override
        public InputStream getInput() {
            return zipInput;
        }
    }

    @Override
    public Entry getNextEntry() throws IOException {
        final ZipEntry entry = input.getNextEntry();
        ZipBackupEntry zipEntry = null;
        if (entry != null) {
            zipEntry = new ZipBackupEntry(input, entry.getName());
        }
        return zipEntry;
    }

    @Override
    public void close() throws IOException {
        input.close();
    }
}
