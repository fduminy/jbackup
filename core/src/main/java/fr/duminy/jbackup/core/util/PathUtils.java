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
package fr.duminy.jbackup.core.util;

import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PathUtils {
    public static void setReadable(Path path, final boolean readable) throws IOException {
        if (readable) {
            setReadableImpl(path, readable);
            if (Files.isDirectory(path)) {
                for (Path p : Files.newDirectoryStream(path)) {
                    setReadable(p, readable);
                }
            }
            return;
        }

        final List<Path> paths = new ArrayList<>();
        SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                paths.add(dir);
                return super.preVisitDirectory(dir, attrs);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                paths.add(file);
                return super.visitFile(file, attrs);
            }
        };

        paths.add(path);

        Files.walkFileTree(path, visitor);

        Collections.reverse(paths);
        for (Path p : paths) {
            setReadableImpl(p, readable);
        }
    }

    private static void setReadableImpl(Path path, boolean readable) throws IOException {
        LoggerFactory.getLogger(PathUtils.class).debug("setReadableImpl({}, {})", path, readable);
        boolean success = path.toFile().setReadable(readable);
        if (!success) {
            throw new IOException("Unable to set readable state to " + readable + " for path " + path);
        }
    }
}
