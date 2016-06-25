/**
 * JBackup is a software managing backups.
 *
 * Copyright (C) 2013-2016 Fabien DUMINY (fabien [dot] duminy [at] webmails [dot] com)
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
package fr.duminy.jbackup.core.command;

import fr.duminy.jbackup.core.Cancellable;
import fr.duminy.jbackup.core.archive.ArchiveFactory;
import fr.duminy.jbackup.core.archive.ArchiveParameters;
import fr.duminy.jbackup.core.archive.SourceWithPath;
import fr.duminy.jbackup.core.task.TaskListener;
import fr.duminy.jbackup.core.util.FileDeleter;
import fr.duminy.jbackup.core.util.InputStreamComparator;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

public class MutableJBackupContext implements JBackupContext {
    private List<SourceWithPath> collectedFiles;
    private ArchiveParameters archiveParameters;
    private TaskListener listener;
    private Cancellable cancellable;
    private InputStreamComparator comparator;
    private ArchiveFactory factory;
    private InputStream archive;
    private Path archivePath;
    private Path targetDirectory;
    private FileDeleter fileDeleter;

    @Override
    public List<SourceWithPath> getCollectedFiles() {
        return collectedFiles;
    }

    @Override
    public ArchiveParameters getArchiveParameters() {
        return archiveParameters;
    }

    @Override
    public TaskListener getListener() {
        return listener;
    }

    @Override
    public Cancellable getCancellable() {
        return cancellable;
    }

    public void setCollectedFiles(List<SourceWithPath> collectedFiles) {
        this.collectedFiles = collectedFiles;
    }

    public void setArchiveParameters(ArchiveParameters archiveParameters) {
        this.archiveParameters = archiveParameters;
    }

    public void setListener(TaskListener listener) {
        this.listener = listener;
    }

    public void setCancellable(Cancellable cancellable) {
        this.cancellable = cancellable;
    }

    @Override
    public InputStreamComparator getComparator() {
        return comparator;
    }

    public void setComparator(InputStreamComparator comparator) {
        this.comparator = comparator;
    }

    @Override
    public ArchiveFactory getFactory() {
        return factory;
    }

    public void setFactory(ArchiveFactory factory) {
        this.factory = factory;
    }

    @Override
    public InputStream getArchive() {
        return archive;
    }

    public void setArchive(InputStream archive) {
        this.archive = archive;
    }

    @Override
    public Path getArchivePath() {
        return archivePath;
    }

    public void setArchivePath(Path archivePath) {
        this.archivePath = archivePath;
    }

    @Override
    public Path getTargetDirectory() {
        return targetDirectory;
    }

    public void setTargetDirectory(Path targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    @Override
    public FileDeleter getFileDeleter() {
        return fileDeleter;
    }

    public void setFileDeleter(FileDeleter fileDeleter) {
        this.fileDeleter = fileDeleter;
    }
}
