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
package fr.duminy.jbackup.core.matchers;

import fr.duminy.jbackup.core.archive.ArchiveParameters;
import org.mockito.ArgumentMatcher;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;

class ArchiveParametersMatcher implements ArgumentMatcher<ArchiveParameters> {
    private final ArchiveParameters parameters;
    private final Path expectedArchive;

    ArchiveParametersMatcher(ArchiveParameters parameters, Path expectedArchive) {
        this.parameters = parameters;
        this.expectedArchive = expectedArchive;
    }

    @Override
    public boolean matches(Object o) {
        ArchiveParameters params = (ArchiveParameters) o;
        final boolean b = parameters.isRelativeEntries() == params.isRelativeEntries();
        final boolean b1 = expectedArchive.equals(params.getArchive());
        final boolean b2 = sameSources(parameters.getSources(), params.getSources());
        return b && b1 && b2;
    }

    private boolean sameSources(Collection<ArchiveParameters.Source> sources1, Collection<ArchiveParameters.Source> sources2) {
        if (sources1.size() != sources2.size()) {
            return false;
        }

        Iterator<ArchiveParameters.Source> iterator2 = sources2.iterator();
        for (ArchiveParameters.Source source1 : sources1) {
            ArchiveParameters.Source source2 = iterator2.next();
            if (!source1.getDirFilter().equals(source2.getDirFilter())) {
                return false;
            }
            if (!source1.getFileFilter().equals(source2.getFileFilter())) {
                return false;
            }
            if (!source1.getPath().equals(source2.getPath())) {
                return false;
            }
        }

        return true;
    }
}
