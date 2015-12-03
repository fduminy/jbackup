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

import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactory;
import fr.duminy.jbackup.core.util.InputStreamComparator;
import org.junit.Rule;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(Theories.class)
public class ArchiveVerifierTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Theory
    public void testVerify(boolean alterContent, boolean twoSources) throws Exception {
        // prepare
        System.out.println(format("--- testVerify(alterContent=%s, twoSources=%s) ---", alterContent, twoSources));
        final ZipArchiveFactory factory = ZipArchiveFactory.INSTANCE;
        ArchiveFactory spiedFactory = spy(factory);
        List<SourceWithPath> files = createFiles("files");
        if (twoSources) {
            files.addAll(createFiles("files2"));
        }
        Path alteredFile = Paths.get(files.get(1).getAbsolutePath());
        Path archive = zipFiles(factory, files);
        InputStreamComparator comparator = mock(InputStreamComparator.class);
        if (alterContent) {
            when(comparator.equals(not(eq(alteredFile)), any(InputStream.class))).thenReturn(true);
            when(comparator.equals(eq(alteredFile), any(InputStream.class))).thenReturn(false);
        } else {
            when(comparator.equals(any(Path.class), any(InputStream.class))).thenReturn(true);
        }
        ArchiveVerifier verifier = new ArchiveVerifier(comparator);

        // test
        final InputStream archiveInputStream = Files.newInputStream(archive);
        boolean actual = verifier.verify(spiedFactory, archiveInputStream, files);

        // verify
        for (SourceWithPath swp : files) {
            verify(comparator, times(1)).equals(eq(Paths.get(swp.getAbsolutePath())), any(InputStream.class));
        }
        verify(spiedFactory, times(1)).create(eq(archiveInputStream));
        verifyNoMoreInteractions(spiedFactory, comparator);
        assertThat(actual).as("result of verify").isEqualTo(!alterContent);
    }

    private Path zipFiles(ArchiveFactory factory, List<SourceWithPath> files) throws IOException, ArchiveException {
        Path archive = tempFolder.newFile("archive.zip").toPath();
        Compressor compressor = new Compressor(factory);
        ArchiveParameters archiveParameters = new ArchiveParameters(archive, true);
        compressor.compress(archiveParameters, files, null, null);
        return archive;
    }

    private List<SourceWithPath> createFiles(String name) throws IOException, ArchiveException {
        List<SourceWithPath> files = new ArrayList<>();
        Path rootDir = tempFolder.newFolder(name).toPath();
        files.add(new SourceWithPath(rootDir, writeFile(rootDir, "file1")));
        files.add(new SourceWithPath(rootDir, writeFile(rootDir.resolve("subDir"), "file2")));
        return files;
    }

    private Path writeFile(Path dir, String name) throws IOException {
        Files.createDirectories(dir);
        Path file = dir.resolve(name);
        Files.write(file, (name + "Content").getBytes());
        return file;
    }
}