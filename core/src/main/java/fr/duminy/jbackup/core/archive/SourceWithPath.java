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
package fr.duminy.jbackup.core.archive;

import java.nio.file.Files;
import java.nio.file.Path;

public final class SourceWithPath {
    private final Path source;
    private final Path path;

    public SourceWithPath(Path source, Path path) {
        if (!source.isAbsolute()) {
            throw new IllegalArgumentException("source parameter must be absolute");
        }
        this.source = source;
        this.path = path;
    }

    public final Path getSource() {
        return source;
    }

    public final Path getPath() {
        return path;
    }

    public final String getRelativePath() {
        String relativePath;
        if (Files.isDirectory(source)) {
            final Path sourceParent = source.getParent();
            if (sourceParent == null) {
                relativePath = source.relativize(getPath()).toString();
            } else {
                relativePath = sourceParent.relativize(getPath()).toString();
            }
        } else {
            relativePath = String.valueOf(getPath().getFileName());
        }
        return relativePath;
    }

    public String getAbsolutePath() {
        return getPath().toString();
    }
}
