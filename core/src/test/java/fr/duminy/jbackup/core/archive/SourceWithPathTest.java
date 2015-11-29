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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceWithPathTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private String relativePath;
    private Path source;
    private Path path;

    @Before
    public void setUp() throws IOException {
        Path relPath = Paths.get("sourceFolder", "subDir", "file");
        this.relativePath = relPath.toString();
        final Path rootPath = temporaryFolder.getRoot().toPath();
        this.source = rootPath.resolve(relPath.getName(0).toString());
        this.path = source.getParent().resolve(relativePath);

        Files.createDirectories(rootPath.resolve(relPath.getParent()));
    }

    @Test
    public void testConstructor_relativeSource() throws Exception {
        thrown.expectMessage("source parameter must be absolute");

        new SourceWithPath(source.relativize(path), path);
    }

    @Test
    public void testConstructor_absoluteSource() throws Exception {
        new SourceWithPath(source, path);
    }

    @Test
    public void testGetRelativePath() throws Exception {
        SourceWithPath swp = new SourceWithPath(source, path);

        String actual = swp.getRelativePath();

        assertThat(actual).isEqualTo(relativePath);
    }

    @Test
    public void testGetAbsolutePath() throws Exception {
        SourceWithPath swp = new SourceWithPath(source, path);

        String actual = swp.getAbsolutePath();

        assertThat(actual).isEqualTo(path.toString());
    }
}