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
package fr.duminy.jbackup.core.task;

import com.google.common.base.Supplier;
import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.Cancellable;
import fr.duminy.jbackup.core.archive.*;
import fr.duminy.jbackup.core.util.FileDeleter;
import org.apache.commons.io.filefilter.IOFileFilter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class BackupTask extends FileCreatorTask {
    public BackupTask(BackupConfiguration config, Supplier<FileDeleter> deleterSupplier,
                      TaskListener listener, Cancellable cancellable) {
        super(config, deleterSupplier, listener, cancellable);
    }

    @Override
    protected void executeTask(FileDeleter deleter) throws Exception {
        ArchiveFactory factory = config.getArchiveFactory();

        Path target = Paths.get(config.getTargetDirectory());
        Files.createDirectories(target);

        String archiveName = generateName(config.getName(), config.getArchiveFactory());

        Path archive = target.resolve(archiveName);

        final ArchiveParameters archiveParameters = new ArchiveParameters(archive, config.isRelativeEntries());
        for (BackupConfiguration.Source filter : config.getSources()) {
            IOFileFilter dirFilter = config.createIOFileFilter("_dir", filter.getDirFilter());
            IOFileFilter fileFilter = config.createIOFileFilter("_file", filter.getFileFilter());
            Path source = Paths.get(filter.getPath());
            archiveParameters.addSource(source, dirFilter, fileFilter);
        }

        deleter.registerFile(archiveParameters.getArchive());

        List<SourceWithPath> collectedFiles = new ArrayList<>();
        createFileCollector().collectFiles(collectedFiles, archiveParameters, listener, cancellable);
        compress(factory, archiveParameters, collectedFiles, cancellable);
    }

    protected void compress(ArchiveFactory factory, ArchiveParameters archiveParameters, List<SourceWithPath> collectedFiles, Cancellable cancellable) throws ArchiveException {
        createCompressor(factory).compress(archiveParameters, collectedFiles, listener, cancellable);
    }

    FileCollector createFileCollector() {
        return new FileCollector();
    }

    Compressor createCompressor(ArchiveFactory factory) {
        return new Compressor(factory);
    }


    protected String generateName(String configName, ArchiveFactory factory) {
        Objects.requireNonNull(factory, "ArchiveFactory is null");

        Calendar date = Calendar.getInstance();
        return String.format("%1$s_%2$tY_%2$tm_%2$td_%2$tH_%2$tM_%2$tS.%3$s", configName, date, factory.getExtension());
    }
}
