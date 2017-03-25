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
package fr.duminy.jbackup.core.util;

import fr.duminy.jbackup.core.TestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DefaultFileDeleterTest {
    @Rule
    public final LogRule logRule = new LogRule();

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testRegisterFile() throws Exception {
        final Path path = tempFolder.newFile().toPath();
        Files.delete(path);
        assertFalse(Files.exists(path));

        testRegisterFile(path, null);
    }

    @Test
    public void testRegisterFile_fileExists() throws Exception {
        Path path = TestUtils.createFile(tempFolder.newFile().toPath(), 1);
        assertTrue(Files.exists(path));
        assertTrue(Files.isRegularFile(path));

        testRegisterFile(path, "The file '%s' already exists.");
    }

    private void testRegisterFile(Path path, String expectedError) throws Exception {
        DefaultFileDeleter deleter = new DefaultFileDeleter();

        if (expectedError != null) {
            thrown.expect(IllegalStateException.class);
            thrown.expectMessage(String.format(expectedError, path));
        }

        try {
            deleter.registerFile(path);
        } finally {
            assertThat(deleter.getRegisteredDirectories()).as("registeredDirectories").isNullOrEmpty();
            if (expectedError != null) {
                assertThat(deleter.getRegisteredFiles()).as("registeredFiles").isNullOrEmpty();
            } else {
                assertThat(deleter.getRegisteredFiles()).as("registeredFiles").containsExactly(path);
            }
        }
    }

    @Test
    public void testRegisterDirectory() throws Exception {
        final Path path = tempFolder.newFolder().toPath();
        Files.delete(path);
        assertFalse(Files.exists(path));

        testRegisterDirectory(path, null);
    }

    @Test
    public void testRegisterDirectory_directoryExists() throws Exception {
        Path path = tempFolder.newFolder().toPath();
        Files.createDirectories(path);
        assertTrue(Files.exists(path));
        assertTrue(Files.isDirectory(path));

        testRegisterDirectory(path, "The directory '%s' already exists.");
    }

    private void testRegisterDirectory(Path path, String expectedError) throws Exception {
        DefaultFileDeleter deleter = new DefaultFileDeleter();

        if (expectedError != null) {
            thrown.expect(IllegalStateException.class);
            thrown.expectMessage(String.format(expectedError, path));
        }

        try {
            deleter.registerDirectory(path);
        } finally {
            if (expectedError != null) {
                assertThat(deleter.getRegisteredDirectories()).as("registeredDirectories").isNullOrEmpty();
            } else {
                assertThat(deleter.getRegisteredDirectories()).as("registeredDirectories").containsExactly(path);
            }
            assertThat(deleter.getRegisteredFiles()).as("registeredFiles").isNullOrEmpty();
        }
    }

    @Test
    public void testDeleteAll_files_unprotected() throws Exception {
        testDeleteAll_files(false);
    }

    @Test
    public void testDeleteAll_files_protected() throws Exception {
        testDeleteAll_files(true);
    }

    private void testDeleteAll_files(boolean protect) throws Exception {
        DefaultFileDeleter deleter = new DefaultFileDeleter();

        Path file1 = getRootPath().resolve("file1");
        deleter.registerFile(file1);
        TestUtils.createFile(file1, 1);
        if (protect) {
            PathUtils.setReadable(file1, false);
        }

        Path file2 = getRootPath().resolve("file2");
        deleter.registerFile(file2);
        TestUtils.createFile(file2, 1);
        if (protect) {
            PathUtils.setReadable(file2, false);
        }

        deleter.deleteAll();

        assertThat(file1.toFile()).doesNotExist();
        assertThat(file2.toFile()).doesNotExist();
    }

    @Test
    public void testDeleteAll_directories_unprotected() throws Exception {
        testDeleteAll_directories(false);
    }

    @Test
    public void testDeleteAll_directories_protected() throws Exception {
        testDeleteAll_directories(true);
    }

    private void testDeleteAll_directories(boolean protect) throws Exception {
        DefaultFileDeleter deleter = new DefaultFileDeleter();

        Path dir1 = getRootPath().resolve("dir1");
        deleter.registerDirectory(dir1);
        Path file1 = TestUtils.createFile(dir1.resolve("file1"), 1);
        if (protect) {
            PathUtils.setReadable(dir1, false);
        }

        Path dir2 = getRootPath().resolve("dir2");
        deleter.registerDirectory(dir2);
        Path dir3 = dir2.resolve("dir3");
        Path file2 = TestUtils.createFile(dir3.resolve("file2"), 1);
        if (protect) {
            PathUtils.setReadable(dir2, false);
        }

        deleter.deleteAll();

        assertThat(dir1.toFile()).doesNotExist();
        assertThat(file1.toFile()).doesNotExist();
        assertThat(dir2.toFile()).doesNotExist();
        assertThat(file2.toFile()).doesNotExist();
    }

    private Path getRootPath() {
        return tempFolder.getRoot().toPath();
    }
}