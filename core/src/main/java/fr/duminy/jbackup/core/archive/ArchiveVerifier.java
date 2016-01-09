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
package fr.duminy.jbackup.core.archive;

import fr.duminy.jbackup.core.util.InputStreamComparator;

import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;

/**
 * Verify an archive by comparing its archived files with ones from source directory.
 */
public class ArchiveVerifier {
    private final InputStreamComparator comparator;

    public ArchiveVerifier(InputStreamComparator comparator) {
        this.comparator = comparator;
    }

    public boolean verify(ArchiveFactory factory, InputStream archive, List<SourceWithPath> sourceFiles) throws Exception {
        boolean result = true;
        try (ArchiveInputStream archiveInputStream = factory.create(archive)) {
            ArchiveInputStream.Entry entry;

            while ((entry = archiveInputStream.getNextEntry()) != null) {
                SourceWithPath swp = findPath(sourceFiles, entry.getName());
                result &= (swp != null) && comparator.equals(Paths.get(swp.getAbsolutePath()), entry.getInput());
            }
        }

        return result;
    }

    private SourceWithPath findPath(List<SourceWithPath> sourceFiles, String entryName) {
        for (SourceWithPath swp : sourceFiles) {
            if (swp.getRelativePath().equals(entryName)) {
                return swp;
            }
        }
        return null;
    }
}
