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
package fr.duminy.jbackup.core.archive.zip;

import fr.duminy.jbackup.core.archive.ArchiveException;
import fr.duminy.jbackup.core.archive.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class ZipArchiveOutputStream implements ArchiveOutputStream {
    private final org.apache.commons.compress.archivers.ArchiveOutputStream output;

    ZipArchiveOutputStream(OutputStream output) throws ArchiveException {
        try {
            this.output = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP, output);
        } catch (org.apache.commons.compress.archivers.ArchiveException e) {
            throw new ArchiveException(e);
        }

    }

    @Override
    public void addEntry(String name, InputStream input) throws IOException {
        output.putArchiveEntry(new ZipArchiveEntry(name));
        IOUtils.copy(input, output);
        output.closeArchiveEntry();
    }

    @Override
    public void close() throws IOException {
        output.close();
    }
}
