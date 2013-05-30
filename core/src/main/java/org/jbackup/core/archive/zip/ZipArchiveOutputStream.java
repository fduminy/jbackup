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
package org.jbackup.core.archive.zip;

import org.apache.commons.io.IOUtils;
import org.jbackup.core.archive.ArchiveOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class ZipArchiveOutputStream implements ArchiveOutputStream {
    final private ZipOutputStream output;

    ZipArchiveOutputStream(OutputStream output) {
        this.output = new ZipOutputStream(output);
    }

    @Override
    public void addEntry(String name, InputStream input) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        output.putNextEntry(entry);
        IOUtils.copy(input, output);
    }

    @Override
    public void close() throws IOException {
        output.close();
    }
}
