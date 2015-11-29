/**
 * JBackup is a software managing backups.
 *
 * Copyright (C) 2013-2015 Fabien DUMINY (fabien [dot] duminy [at] webmails [dot] com)
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
import fr.duminy.jbackup.core.archive.SourceWithPath;

import java.beans.PropertyChangeEvent;
import java.nio.file.Path;

public class Matchers extends org.mockito.Matchers {
    public static SourceWithPath eq(Path value) {
        return argThat(new SourceWithPathMatcher(value));
    }

    public static ArchiveParameters eq(ArchiveParameters parameters, Path expectedArchive) {
        return argThat(new ArchiveParametersMatcher(parameters, expectedArchive));
    }

    public static <T> PropertyChangeEvent propertyChangeEventWithNewValue(T value) {
        return argThat(new PropertyChangeEventMatcher<T>(value));
    }
}
