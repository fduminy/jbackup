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
package fr.duminy.jbackup.core;

import fr.duminy.jbackup.core.archive.*;
import fr.duminy.jbackup.core.util.FileDeleter;
import org.apache.commons.io.filefilter.IOFileFilter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

class BackupTask extends Task {
    BackupTask(JBackup jbackup, BackupConfiguration config, ProgressListener listener) {
        super(jbackup, listener, config);
    }

    @Override
    protected void execute() throws Exception {
        ArchiveFactory factory = config.getArchiveFactory();

        Path target = Paths.get(config.getTargetDirectory());
        Files.createDirectories(target);

        String archiveName = jbackup.generateName(config.getName(), config.getArchiveFactory());

        Path archive = target.resolve(archiveName);

        final ArchiveParameters archiveParameters = new ArchiveParameters(archive, config.isRelativeEntries());
        for (BackupConfiguration.Source filter : config.getSources()) {
            IOFileFilter dirFilter = config.createIOFileFilter("_dir", filter.getDirFilter());
            IOFileFilter fileFilter = config.createIOFileFilter("_file", filter.getFileFilter());
            Path source = Paths.get(filter.getPath());
            archiveParameters.addSource(source, dirFilter, fileFilter);
        }

        FileDeleter deleter = jbackup.createFileDeleter();
        try {
            deleter.registerFile(archiveParameters.getArchive());

            List<SourceWithPath> collectedFiles = new ArrayList<>();
            new FileCollector().collectFiles(collectedFiles, archiveParameters, listener, null);
            compress(factory, archiveParameters, collectedFiles);
        } catch (Exception e) {
            deleter.deleteAll();
            throw e;
        }
    }

    void compress(ArchiveFactory factory, ArchiveParameters archiveParameters, List<SourceWithPath> collectedFiles) throws ArchiveException {
        new Compressor(factory).compress(archiveParameters, collectedFiles, listener);
    }
}
