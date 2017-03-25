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
package fr.duminy.jbackup.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * Compare 2 {@link java.io.InputStream} byte by byte until a difference is found or end of stream is reached.
 */
public class InputStreamComparator {

    private static final int BUFFER_SIZE = 1024;

    public boolean equals(Path inputPath, InputStream inputStream) throws IOException {
        try (InputStream input1 = Files.newInputStream(inputPath)) {
            return equals(input1, inputStream);
        }
    }

    public boolean equals(InputStream inputStream1, InputStream inputStream2) throws IOException {
        try {
            byte[] buffer1 = new byte[BUFFER_SIZE];
            byte[] buffer2 = new byte[BUFFER_SIZE];

            int nbRead1;
            int nbRead2;

            do {
                nbRead1 = inputStream1.read(buffer1);
                nbRead2 = inputStream2.read(buffer2);
                if (nbRead1 == nbRead2) {
                    for (int i = 0; i < nbRead1; i++) {
                        if (buffer1[i] != buffer2[i]) {
                            return false;
                        }
                    }
                }
            } while ((nbRead1 == nbRead2) && (nbRead1 > 0) && (nbRead2 > 0));

            return (nbRead1 == nbRead2);
        } finally {
            closeQuietly(inputStream1);
            closeQuietly(inputStream2);
        }
    }
}
