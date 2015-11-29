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
package fr.duminy.jbackup.core.archive;

import fr.duminy.jbackup.core.util.LogRule;
import org.junit.Rule;
import org.junit.Test;

import static fr.duminy.jbackup.core.archive.ArchiveDSL.Entries;
import static fr.duminy.jbackup.core.archive.ArchiveDSL.withoutFilter;
import static org.fest.assertions.Assertions.assertThat;

public class ArchiveDSLTest {
    @Rule
    public final LogRule logRule = new LogRule();

    @Test
    public void testEntries_SourceFile() {
        final String file1 = "file1";
        Entries entries = withoutFilter().andSourceFile(file1).acceptAll().entries();
        assertEntries(entries, file1, 1);
    }

    @Test
    public void testEntries_SourceDir_withOneFile() {
        final String file1 = "file1";
        Entries entries = withoutFilter().andSourceDir("sourceDir").accept(file1).entries();
        assertEntries(entries, file1, 1);
    }

    private void assertEntries(Entries actualEntries, Object... expectedEntries) {
        int nbExpectedEntries = expectedEntries.length / 2;
        assertThat(actualEntries.size()).isEqualTo(nbExpectedEntries);

        assertEntry(actualEntries.firstEntry(), expectedEntries, 0);

        final ArchiveInputStream.Entry[] nextEntries = actualEntries.nextEntries();
        assertThat(nextEntries.length).isEqualTo(nbExpectedEntries); // last entry is null
        for (int index = 2; index < expectedEntries.length; index += 2) {
            assertEntry(nextEntries[(index / 2) - 1], expectedEntries, index);
        }
        assertThat(nextEntries[nextEntries.length - 1]).isNull();
    }

    private void assertEntry(ArchiveInputStream.Entry entry, Object[] expectedEntries, int index) {
        assertThat(entry.getName()).isEqualTo((String) expectedEntries[index]);
        assertThat(entry.getCompressedSize()).isEqualTo((Integer) expectedEntries[index + 1]);
    }
}