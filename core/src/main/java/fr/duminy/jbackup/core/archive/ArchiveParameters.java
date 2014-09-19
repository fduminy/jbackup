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

import org.apache.commons.io.filefilter.IOFileFilter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;

public class ArchiveParameters {
    private final Collection<Source> sources = new ArrayList<>();
    private final Path archive;
    private boolean relativeEntries;

    public ArchiveParameters(Path archive, boolean relativeEntries) {
        this.archive = archive;
        this.relativeEntries = relativeEntries;
    }

    public Collection<Source> getSources() {
        return sources;
    }

    public Path getArchive() {
        return archive;
    }

    public void addSource(Path source, IOFileFilter dirFilter, IOFileFilter fileFilter) {
        sources.add(new Source(source, dirFilter, fileFilter));
    }

    public void addSource(Path source) {
        addSource(source, null, null);
    }

    public boolean isRelativeEntries() {
        return relativeEntries;
    }

    @Override
    public String toString() {
        return "ArchiveParameters{" +
                "sources=" + sources +
                ", archive=" + archive +
                ", relativeEntries=" + relativeEntries +
                '}';
    }

    public static final class Source {
        private final Path source;
        private final IOFileFilter dirFilter;
        private final IOFileFilter fileFilter;

        private Source(Path source, IOFileFilter dirFilter, IOFileFilter fileFilter) {
            this.source = source;
            this.dirFilter = (dirFilter == null) ? trueFileFilter() : dirFilter;
            this.fileFilter = (fileFilter == null) ? trueFileFilter() : fileFilter;
        }

        public Path getSource() {
            return source;
        }

        public IOFileFilter getDirFilter() {
            return dirFilter;
        }

        public IOFileFilter getFileFilter() {
            return fileFilter;
        }

        @Override
        public String toString() {
            return "Source{" +
                    "source=" + source +
                    ", dirFilter=" + dirFilter +
                    ", fileFilter=" + fileFilter +
                    '}';
        }
    }
}
