/**
 * JBackup is a software managing backups.
 * <p/>
 * Copyright (C) 2013-2014 Fabien DUMINY (fabien [dot] duminy [at] webmails [dot] com)
 * <p/>
 * JBackup is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * <p/>
 * JBackup is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 */
package fr.duminy.jbackup.core.filter;

import org.junit.Before;
import org.junit.Rule;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static fr.duminy.jbackup.core.filter.MavenTargetRecognizer.MAVEN2_PROJECT_FILE;
import static fr.duminy.jbackup.core.filter.MavenTargetRecognizer.MAVEN2_TARGET_DIR;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MavenTargetRecognizer}.
 */
@RunWith(Theories.class)
public class MavenTargetRecognizerTest {
    @SuppressWarnings("unused")
    public enum MavenProjectFileTestCase {
        EMPTY_DIRECTORY("emptyDirectory", false),
        NULL_DIRECTORY(null, false),
        INVALID_DIRECTORY("anInvalidDirectory", false),
        PARAMETER_IS_A_FILE("parameterIsAFile.txt", false),
        M2_WRONG_MODEL_VERSION_TAG("m2_wrongModelVersionTag", false),
        M2_WRONG_PROJECT_FILENAME("m2_wrongProjectFileName", false),
        M2_WRONG_ROOT_TAG("m2_wrongRootTag", false),
        M2_NOMINAL("m2_nominal", true),
        M2_NOMINAL_WITH_HEADER("m2_nominalWithHeader", true);

        private final Path directory;
        private final Path projectFile;

        MavenProjectFileTestCase(String directoryName, boolean validProjectFile) {
            URL resource = (directoryName == null) ? null : getClass().getResource(directoryName);
            directory = ((directoryName == null) || (resource == null)) ? null : Paths.get(resource.getFile());
            projectFile = ((directory != null) && validProjectFile) ? directory.resolve(MAVEN2_PROJECT_FILE) : null;
        }

        public Path getProjectFile() {
            return projectFile;
        }
    }

    @SuppressWarnings("unused")
    public enum MavenTargetDirectoryTestCase {
        NULL_DIRECTORY(null, false),
        INVALID_DIRECTORY("anInvalidDirectory", false),
        PARAMETER_IS_A_FILE(MAVEN2_TARGET_DIR, false),
        M2_NOMINAL(MAVEN2_TARGET_DIR, true);

        private final Path path;
        private final boolean directory;

        MavenTargetDirectoryTestCase(String name, boolean directory) {
            this.path = (name == null) ? null : Paths.get(name);
            this.directory = directory;
        }
    }

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    private MavenTargetRecognizer recognizer;

    @Before
    public void setUp() {
        recognizer = new MavenTargetRecognizer();
    }

    @Theory
    public void testGetMavenProjectFile(MavenProjectFileTestCase tc) throws IOException {
        Path actualPath = recognizer.getMavenProjectFile(tc.directory);

        assertThat(actualPath).isEqualTo(tc.projectFile);
    }

    @Theory
    public void testCouldBeMavenTargetDirectory(MavenTargetDirectoryTestCase tc) throws IOException {
        Path path = (tc.path == null) ? null : tempFolder.getRoot().toPath().resolve(tc.path);
        if (path != null) {
            if (tc.directory) {
                Files.createDirectory(path);
            } else {
                Files.createFile(path);
            }
        }
        boolean valid = (tc == MavenTargetDirectoryTestCase.M2_NOMINAL);

        boolean actual = recognizer.couldBeMavenTargetDirectory(path);

        assertThat(actual).isEqualTo(valid);
    }
}