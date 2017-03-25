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
package fr.duminy.jbackup.core.archive.zip;

import fr.duminy.jbackup.core.archive.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;

class ZipArchiveInputStream implements fr.duminy.jbackup.core.archive.ArchiveInputStream {
    private final ArchiveInputStream input;

    ZipArchiveInputStream(InputStream input) throws ArchiveException {
        try {
            this.input = new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.ZIP, input);
        } catch (org.apache.commons.compress.archivers.ArchiveException e) {
            throw new ArchiveException(e);
        }
    }

    private static class ZipBackupEntry extends Entry {
        private final ArchiveInputStream zipInput;

        private ZipBackupEntry(ArchiveInputStream zipInput, ZipEntry entry) {
            super(entry.getName(), entry.getCompressedSize());
            this.zipInput = zipInput;
        }

        @Override
        public void close() throws IOException {
            zipInput.close();
        }

        @Override
        public InputStream getInput() {
            return zipInput;
        }
    }

    @Override
    public Entry getNextEntry() throws IOException {
        ZipEntry entry = (ZipEntry) input.getNextEntry();
        ZipBackupEntry zipEntry = null;
        if (entry != null) {
            zipEntry = new ZipBackupEntry(input, entry);
        }
        return zipEntry;
    }

    @Override
    public void close() throws IOException {
        input.close();
    }
}
