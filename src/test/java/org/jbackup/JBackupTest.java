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
package org.jbackup;

import org.jbackup.archive.AbstractArchivingTest;
import org.jbackup.archive.ArchiveFactory;
import org.jbackup.archive.ProgressListener;
import org.junit.Before;

import java.io.File;

/**
 * Tests for class {@link JBackup}.
 */
public class JBackupTest extends AbstractArchivingTest {
    private JBackup jbackup;

    public JBackupTest() {
        super(true);
    }

    @Before
    public void setUp() throws Exception {
        jbackup = new JBackup();
    }

    @Override
    protected void decompress(ArchiveFactory mockFactory, File archive, File directory, ProgressListener listener) throws Exception {
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
    protected void compress(ArchiveFactory mockFactory, File sourceDirectory, File[] files, File archive, ProgressListener listener) throws Exception {
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
}
