/**
 * JBackup is a software managing backups.
 *
 * Copyright (C) 2013-2013 Fabien DUMINY (fabien [dot] duminy [at] webmails [dot] com)
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

import fr.duminy.jbackup.core.archive.AbstractArchivingTest;
import fr.duminy.jbackup.core.archive.ArchiveFactory;
import fr.duminy.jbackup.core.archive.Archiver;
import fr.duminy.jbackup.core.archive.ProgressListener;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Tests for class {@link fr.duminy.jbackup.core.JBackup}.
 */
public class JBackupTest extends AbstractArchivingTest {
    public JBackupTest() {
        super(true);
    }

    @Override
    protected void decompress(ArchiveFactory mockFactory, File archive, File directory, ProgressListener listener) throws Exception {
        JBackup jbackup = new JBackup();
        File archiveDirectory = new File(archive.getParent());
        BackupConfiguration config = new BackupConfiguration();
        config.setName("testDecompress");
        config.setTargetDirectory(archiveDirectory.getAbsolutePath());
        config.setArchiveFactory(mockFactory);

        if (listener == null) {
            jbackup.restore(config, archive, directory);
        } else {
            jbackup.restore(config, archive, directory, listener);
        }

        jbackup.shutdown();
    }

    @Override
    protected void compress(ArchiveFactory mockFactory, File sourceDirectory, final File[] expectedFiles, File archive, ProgressListener listener) throws Exception {
        JBackup jbackup = new JBackup() {
            @Override
            Archiver createArchiver(ArchiveFactory factory) {
                return new Archiver(factory) {
                    @Override
                    public void compress(File[] actualFiles, File archive, ProgressListener listener) throws IOException {
                        // ensure that actual files are as expected
                        assertThat(asSet(actualFiles)).isEqualTo(asSet(expectedFiles));

                        // now compress files in the order given by expectedFiles
                        // (otherwise the test will fail on some platforms)
                        super.compress(actualFiles, archive, listener);
                    }
                };
            }
        };

        BackupConfiguration config = new BackupConfiguration();
        config.setName("testCompress");
        config.setTargetDirectory(archive.getParent());
        config.addSource(sourceDirectory.getAbsolutePath());
        config.setArchiveFactory(mockFactory);

        if (listener == null) {
            jbackup.backup(config);
        } else {
            jbackup.backup(config, listener);
        }

        jbackup.shutdown();
    }

    private static <T> Set<T> asSet(T[] items) {
        return new HashSet<T>(Arrays.asList(items));
    }
}
