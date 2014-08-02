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
package fr.duminy.jbackup.core;

import fr.duminy.components.swing.form.StringPathTypeMapper;
import fr.duminy.jbackup.core.archive.*;
import org.apache.commons.lang.mutable.MutableObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for class {@link fr.duminy.jbackup.core.JBackup}.
 */
public class JBackupTest extends AbstractArchivingTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void test_NullArchiveFactory() throws Throwable {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("ArchiveFactory is null");
        Path archive = tempFolder.newFolder().toPath().resolve("archiveFile");

        final ArchiveParameters archiveParameters = new ArchiveParameters(archive);
        compress(null, archiveParameters, Collections.<Path>emptyList(), mock(ProgressListener.class), false);
    }

    @Override
    protected void decompress(ArchiveFactory mockFactory, Path archive, Path directory, ProgressListener listener, boolean errorIsExpected) throws Throwable {
        JBackup jbackup = new JBackup();
        BackupConfiguration config = new BackupConfiguration();
        config.setName("testDecompress");
        config.setTargetDirectory(StringPathTypeMapper.toString(archive.getParent().toAbsolutePath()));
        config.setArchiveFactory(mockFactory);

        Future<Void> future;
        if (listener == null) {
            future = jbackup.restore(config, archive, directory);
        } else {
            future = jbackup.restore(config, archive, directory, listener);
        }

        waitResult(future);
        jbackup.shutdown();
    }

    @Override
    protected void compress(ArchiveFactory mockFactory, ArchiveParameters archiveParameters, List<Path> expectedFiles, ProgressListener listener, boolean errorIsExpected) throws Throwable {
        final MutableObject actualFilesWrapper = new MutableObject();
        JBackup jbackup = new JBackup() {
            @Override
            Archiver createArchiver(ArchiveFactory factory) {
                return new Archiver(factory) {
                    @Override
                    protected void compress(ArchiveParameters archiveParameters, ProgressListener listener, Collection<Path> files) throws IOException {
                        actualFilesWrapper.setValue(files);

                        // now compress files in the order given by expectedFiles
                        // (otherwise the test will fail on some platforms)
                        super.compress(archiveParameters, listener, files);
                    }
                };
            }
        };

        BackupConfiguration config = new BackupConfiguration();
        config.setName("testCompress");
        config.setTargetDirectory(StringPathTypeMapper.toString(archiveParameters.getArchive().getParent().toAbsolutePath()));
        for (ArchiveParameters.Source source : archiveParameters.getSources()) {
            config.addSource(Paths.get(StringPathTypeMapper.toString(source.getSource().toAbsolutePath())));
        }
        config.setArchiveFactory(mockFactory);

        Future<Void> future;
        if (listener == null) {
            future = jbackup.backup(config);
        } else {
            future = jbackup.backup(config, listener);
        }

        waitResult(future);

        // ensure that actual files are as expected
        if (!errorIsExpected) {
            assertThat(actualFilesWrapper.getValue()).isEqualTo(expectedFiles);
        }

        jbackup.shutdown();
    }

    protected void waitResult(Future<Void> future) throws Throwable {
        assertThat(future).isNotNull();
        try {
            future.get(); // block until finished and maybe throw an Exception if task has thrown one.
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }
}
