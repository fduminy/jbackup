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

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

public class ArchiveParameters {
    private Collection<Path> files = Collections.emptyList();
    private final Path archive;

    public ArchiveParameters(Path archive) {
        this.archive = archive;
    }

    public Collection<Path> getFiles() {
        return files;
    }

    public Path getArchive() {
        return archive;
    }

    public void setFiles(Collection<Path> files) {
        this.files = files;
    }
}
