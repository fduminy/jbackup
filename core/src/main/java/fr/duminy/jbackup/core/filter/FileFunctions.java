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

import org.apache.commons.jexl2.JexlContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Functions that can be used by {@link JexlFileFilter}.
 */
public class FileFunctions {
    private static final MavenTargetRecognizer RECOGNIZER = new MavenTargetRecognizer();

    public static final String FILE = "file";

    private final JexlContext context;

    public FileFunctions(JexlContext context) {
        this.context = context;
    }

    public boolean namePrefix(String prefix) {
        return getFile().getName().startsWith(prefix);
    }

    public boolean mavenTarget() throws IOException {
        Path path = getFile().toPath();
        return RECOGNIZER.couldBeMavenTargetDirectory(path) && (RECOGNIZER.getMavenProjectFile(path.getParent()) != null);
    }

    private File getFile() {
        return (File) context.get(FILE);
    }
}
