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
package fr.duminy.jbackup.core;

import fr.duminy.jbackup.core.archive.ArchiveFactory;
import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactory;
import fr.duminy.jbackup.core.util.LogRule;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ArchiveFactoryXmlAdapterTest {
    @Rule
    public final LogRule logRule = new LogRule();

    @Test
    public void testUnmarshal() throws Exception {
        ArchiveFactory actualFactory = new BackupConfiguration.ArchiveFactoryXmlAdapter().unmarshal(ZipArchiveFactory.class);

        assertThat(actualFactory).isSameAs(ZipArchiveFactory.INSTANCE);
    }
}