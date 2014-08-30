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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static fr.duminy.jbackup.core.archive.Archiver.ArchiverException;

/**
 * Test for the class {@link Archiver}.
 */
public class ArchiverTest extends AbstractArchivingTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testCompress_relativeFilesMustThrowAnException() throws Throwable {
        ArchiveFactory mockFactory = createMockArchiveFactory(Mockito.mock(ArchiveOutputStream.class));
        Path relativeFile = Paths.get("testCompressRelativeFile.tmp");
        Files.write(relativeFile, "A".getBytes());

        try {
            thrown.expect(IllegalArgumentException.class);
            thrown.expectMessage("The file '" + relativeFile.toString() + "' is relative.");

            final ArchiveParameters archiveParameters = new ArchiveParameters(createArchivePath(), false);
            archiveParameters.addSource(relativeFile);
            compress(mockFactory, archiveParameters, Collections.<Path>emptyList(), null, true);
        } finally {
            Files.delete(relativeFile);
        }
    }

    @Override
    protected void decompress(ArchiveFactory mockFactory, Path archive, Path targetDirectory, ProgressListener listener) throws ArchiverException {
        Archiver archiver = new Archiver(mockFactory);
        if (listener == null) {
            archiver.decompress(archive, targetDirectory);
        } else {
            archiver.decompress(archive, targetDirectory, listener);
        }
    }

    @Override
    protected void compress(ArchiveFactory mockFactory, ArchiveParameters archiveParameters, List<Path> expectedFiles, ProgressListener listener, boolean errorIsExpected) throws ArchiverException {
        Archiver archiver = new Archiver(mockFactory);
        if (listener == null) {
            archiver.compress(archiveParameters);
        } else {
            archiver.compress(archiveParameters, listener);
        }
    }
}
