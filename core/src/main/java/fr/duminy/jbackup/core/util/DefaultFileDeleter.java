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

import org.apache.commons.io.FileDeleteStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link fr.duminy.jbackup.core.util.FileDeleter} interface .
 */
public class DefaultFileDeleter implements FileDeleter {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultFileDeleter.class);

    private List<Path> registeredFiles;
    private List<Path> registeredDirectories;

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerFile(Path file) {
        if (Files.exists(file)) {
            throw new IllegalStateException(String.format("The file '%s' already exists.", file));
        }
        if (registeredFiles == null) {
            registeredFiles = new ArrayList<>();
        }
        registeredFiles.add(file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerDirectory(Path directory) {
        if (Files.exists(directory)) {
            throw new IllegalStateException(String.format("The directory '%s' already exists.", directory));
        }
        if (registeredDirectories == null) {
            registeredDirectories = new ArrayList<>();
        }
        registeredDirectories.add(directory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteAll() {
        FileDeleteStrategy deleteStrategy = FileDeleteStrategy.FORCE;
        deleteFiles(deleteStrategy, registeredFiles, true);
        deleteFiles(deleteStrategy, registeredDirectories, false);
    }

    private void deleteFiles(FileDeleteStrategy deleteStrategy, List<Path> paths, boolean expectFiles) {
        if (paths != null) {
            for (Path path : paths) {
                boolean deleted;

                try {
                    PathUtils.setReadable(path, true);
                    deleted = deleteFile(deleteStrategy, expectFiles, path);
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                    deleted = false;
                }

                if (deleted) {
                    LOG.info("{} : deleted", path);
                } else {
                    LOG.error("{} : NOT DELETED", path);
                }
            }
        }
    }

    private boolean deleteFile(FileDeleteStrategy deleteStrategy, boolean expectFiles, Path path) {
        boolean deleted;
        if ((expectFiles && Files.isRegularFile(path)) || (!expectFiles && Files.isDirectory(path))) {
            deleted = deleteStrategy.deleteQuietly(path.toFile());
        } else {
            LOG.error("Wrong path type. Expected: {} Actual: {} {}",
                      new Object[] { expectFiles ? "file" : "directory",
                          Files.isRegularFile(path) ? "file" : "",
                          Files.isDirectory(path) ? "directory" : "" });
            deleted = false;
        }
        return deleted;
    }

    List<Path> getRegisteredFiles() {
        return registeredFiles;
    }

    List<Path> getRegisteredDirectories() {
        return registeredDirectories;
    }
}
