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
package fr.duminy.jbackup.core.filter;

import fr.duminy.jbackup.core.util.LogRule;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static fr.duminy.jbackup.core.filter.MavenTargetRecognizer.MAVEN2_TARGET_DIR;
import static org.assertj.core.api.Assertions.assertThat;

public class JexlFileFilterTest {

    @Rule
    public final LogRule logRule = new LogRule();

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    /**
     * Method copied from {@link org.apache.commons.io.filefilter.RegexFileFilterTestCase#assertFiltering(org.apache.commons.io.filefilter.IOFileFilter, java.io.File, boolean)}.
     * TODO check potential license issue.
     *
     * @param filter
     * @param file
     * @param expected
     * @throws Exception
     */
    public void assertFiltering(final IOFileFilter filter, final File file, final boolean expected) throws Exception {
        // Note. This only tests the (File, String) version if the parent of the File passed in is not null
        assertThat(filter.accept(file)).
                as("Source(File) " + filter.getClass().getName() + " not " + expected + " for " + file).isEqualTo(expected);

        if (file != null && file.getParentFile() != null) {
            assertThat(filter.accept(file.getParentFile(), file.getName())).
                    as("Source(File, String) " + filter.getClass().getName() + " not " + expected + " for " + file).isEqualTo(expected);
        }
    }

    @Test
    public void testNamePrefix() throws Exception {
        IOFileFilter filter = new JexlFileFilter("template", "namePrefix('.')");
        assertFiltering(filter, new File("."), true);
        assertFiltering(filter, new File(".m2"), true);
        assertFiltering(filter, new File(".a.b"), true);
        assertFiltering(filter, new File("readme.txt"), false);
        assertFiltering(filter, new File("file."), false);
    }

    @Test
    public void testMavenTarget() throws Exception {
        testMavenTarget(false, false);
        testMavenTarget(false, true);
        testMavenTarget(true, false);
        testMavenTarget(true, true);
    }

    private void testMavenTarget(boolean projectFile, boolean targetDir) throws Exception {
        Path projectDir = tempFolder.newFolder(Boolean.toString(projectFile) + Boolean.toString(targetDir)).toPath();
        if (projectFile) {
            Path template = MavenTargetRecognizerTest.MavenProjectFileTestCase.M2_NOMINAL.getProjectFile();
            Files.copy(template, projectDir.resolve(template.getFileName()));
        }
        Path targetPath = projectDir.resolve(targetDir ? MAVEN2_TARGET_DIR : "notATargetDirectory");
        Files.createDirectory(targetPath);

        IOFileFilter filter = new JexlFileFilter("template", "mavenTarget()");
        assertFiltering(filter, targetPath.toFile(), projectFile && targetDir);
    }

    @Test
    public void testEqualOperator() throws Exception {
        IOFileFilter filter = new JexlFileFilter("template", "file.name=='Abc'");
        assertFiltering(filter, new File("z"), false);
        assertFiltering(filter, new File("abc"), false);
        assertFiltering(filter, new File("Abc"), true);
        assertFiltering(filter, new File("bc"), false);
    }
}
