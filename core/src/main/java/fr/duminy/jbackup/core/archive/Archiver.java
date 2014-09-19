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

import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * A very basic command line tool for the zip format {(thanks to {@link ZipArchiveFactory}.
 */
public class Archiver {
    public static void main(String[] args) throws IOException, ArchiveException {
        final String operation = args[0];
        final Path archive = Paths.get(args[1]);
        final ZipArchiveFactory factory = ZipArchiveFactory.INSTANCE;

        switch (operation) {
            case "-c":
                final ArchiveParameters archiveParameters = new ArchiveParameters(archive, true);
                for (int i = 2; i < args.length; i++) {
                    archiveParameters.addSource(Paths.get(args[i]));
                }
                List<SourceWithPath> collectedFiles = new ArrayList<>();
                new FileCollector().collectFiles(collectedFiles, archiveParameters, null, null);
                new Compressor(factory).compress(archiveParameters, collectedFiles, null, null);
                break;
            case "-d":
                Path directory = null;

                if (args.length > 2) {
                    directory = Paths.get(args[2]);
                }

                new Decompressor(factory).decompress(archive, directory, null, null);
                break;
            default:
                throw new IOException("unsupported operation: " + operation);
        }
    }
}
