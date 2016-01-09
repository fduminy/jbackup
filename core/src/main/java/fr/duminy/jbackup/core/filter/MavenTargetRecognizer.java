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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.file.Path;
import java.util.Optional;

import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.Files.*;

/**
 * A class used to find the project file in a directory
 * and to identify the target directory of a <a href="https://maven.apache.org/">Maven</a> project.
 */
class MavenTargetRecognizer {
    final static String MAVEN2_PROJECT_FILE = "pom.xml";
    final static String MAVEN2_TARGET_DIR = "target";

    public boolean couldBeMavenTargetDirectory(Path directory) {
        return (directory != null) && exists(directory) && isDirectory(directory) && directory.endsWith(MAVEN2_TARGET_DIR);
    }

    public Optional<Path> getMavenProjectFile(Path directory) throws IOException {
        if (directory == null) {
            return Optional.empty();
        }
        if (!exists(directory)) {
            return Optional.empty();
        }
        if (!isDirectory(directory)) {
            return Optional.empty();
        }
        Path projectPath = directory.resolve(MAVEN2_PROJECT_FILE);
        if (!exists(projectPath)) {
            return Optional.empty();
        }

        CharBuffer charBuffer = ByteBuffer.allocate(2048).asCharBuffer();
        try (BufferedReader reader = newBufferedReader(projectPath, defaultCharset());) {
            reader.read(charBuffer);
        }

        return isMavenProjectContent(charBuffer.rewind().toString()) ? Optional.of(projectPath) : Optional.empty();
    }

    // TODO replacement of following (not working) regular expression : buffer.matches("(.*)<project (.*)<modelVersion>(.*)")
    private boolean isMavenProjectContent(String buffer) {
        String tag = "<project";
        int index = buffer.indexOf(tag);
        if (index < 0) {
            tag += ' ';
            index = buffer.indexOf(tag);
            if (index < 0) {
                return false;
            }
        }
        index = buffer.indexOf("<modelVersion>", index + tag.length());
        if (index < 0) {
            return false;
        }
        return true;
    }
}
