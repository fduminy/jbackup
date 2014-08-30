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

import fr.duminy.jbackup.core.TestUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;

/**
 * Tests for {@link FileCollector}.
 */
public class FileCollectorTest {
    private static final String FILE1 = "File1.txt";
    private static final String FILE1_DIR = "Directory1/";
    private static final String FILE2 = "File2.txt";
    private static final String FILE2_DIR = "Directory2/";
    private static final String[] FILES = {FILE1_DIR + FILE1, FILE2_DIR + FILE2};

    private Path directory;
    private Path[] expectedFiles;

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        directory = tempFolder.newFolder().toPath();

        expectedFiles = new Path[FILES.length];
        int i = 0;
        for (String file : FILES) {
            expectedFiles[i++] = createFile(file);
        }
        Arrays.sort(expectedFiles);
    }

    @Test
    public void testCollect_all_withSymbolicLinkToFile() throws Exception {
        addSymbolicLink(FILE2_DIR, FILE2);
        testCollect(expectedFiles, null, null);
    }

    @Test
    public void testCollect_all_withSymbolicLinkToDir() throws Exception {
        addSymbolicLink(null, FILE1_DIR);
        testCollect(expectedFiles, null, null);
    }

    @Test
    public void testCollect_all() throws Exception {
        testCollect(expectedFiles, null, null);
    }

    @Test
    public void testCollect_file1() throws Exception {
        Path[] files = {expectedFiles[0]};
        testCollect(files, trueFileFilter(), FileFilterUtils.nameFileFilter(FILE1));
    }

    @Test
    public void testCollect_file2() throws Exception {
        Path[] files = {expectedFiles[1]};
        testCollect(files, trueFileFilter(), FileFilterUtils.nameFileFilter(FILE2));
    }

    private void testCollect(Path[] expectedFiles, IOFileFilter directoryFilter, IOFileFilter fileFilter) throws Exception {
        List<Path> files = new ArrayList<>();
        new FileCollector().collect(files, directory, directoryFilter, fileFilter);

        Collections.sort(files);
        Assertions.assertThat(files.toArray()).as("collected files").isEqualTo(expectedFiles);
    }

    private Path createFile(String file) throws IOException {
        Path f = directory.resolve(file);
        return TestUtils.createFile(f, "one line");
    }

    private void addSymbolicLink(String parentDirName, String targetName) throws IOException {
        Path parentDir = (parentDirName == null) ? directory : directory.resolve(parentDirName);
        Path targetPath = parentDir.resolve(targetName);
        Path linkPath = parentDir.resolve("LinkTo" + targetName);

        /*expectedFiles = Arrays.copyOf(expectedFiles, expectedFiles.length + 1);
        expectedFiles[expectedFiles.length - 1] = */
        Files.createSymbolicLink(linkPath, targetPath);
    }
}
