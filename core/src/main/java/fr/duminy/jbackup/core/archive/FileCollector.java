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

import fr.duminy.jbackup.core.Cancellable;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;

import static java.nio.file.FileVisitResult.*;

/**
 * Class collecting files in a directory. Files are filtered with a directory filter and a file filter.
 */
public class FileCollector {
    private static final Logger LOG = LoggerFactory.getLogger(FileCollector.class);

    public long collect(final Collection<Path> results, final Path startDirectory, final IOFileFilter directoryFilter,
                        final IOFileFilter fileFilter, final Cancellable cancellable) throws IOException {
        final long[] totalSize = {0L};

        SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                super.preVisitDirectory(dir, attrs);
                if ((directoryFilter == null) || startDirectory.equals(dir) || directoryFilter.accept(dir.toFile())) {
                    return CONTINUE;
                } else {
                    return SKIP_SUBTREE;
                }
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if ((cancellable != null) && cancellable.isCancelled()) {
                    return TERMINATE;
                }

                super.visitFile(file, attrs);

                if (!Files.isSymbolicLink(file)) {
                    if ((fileFilter == null) || fileFilter.accept(file.toFile())) {
                        LOG.trace("visitFile {}", file.toAbsolutePath());
                        results.add(file);
                        totalSize[0] += Files.size(file);
                    }
                }

                return CONTINUE;
            }
        };
        Files.walkFileTree(startDirectory, visitor);

        return totalSize[0];
    }
}
