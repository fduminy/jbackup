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
package fr.duminy.jbackup.core.matchers;

import fr.duminy.jbackup.core.archive.SourceWithPath;
import org.mockito.ArgumentMatcher;

import java.nio.file.Path;

class SourceWithPathMatcher implements ArgumentMatcher<SourceWithPath> {
    private final Path path;

    SourceWithPathMatcher(Path path) {
        this.path = path;
    }

    @Override
    public boolean matches(Object o) {
        SourceWithPath swp = (SourceWithPath) o;
        return swp.getPath().equals(path);
    }
}
