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
    private static final String FILE1 = "file1.txt";
    private static final String FILE2 = "file2.txt";
    private static final String[] FILES = {"directory1/" + FILE1, "directory2/" + FILE2};

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
            Path f = directory.resolve(file);
            Files.createDirectories(f.getParent());
            Files.write(f, "one line".getBytes());

            expectedFiles[i++] = f;
        }
        Arrays.sort(expectedFiles);
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
}
