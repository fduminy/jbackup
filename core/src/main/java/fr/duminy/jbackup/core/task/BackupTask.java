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
package fr.duminy.jbackup.core.task;

import fr.duminy.components.chain.Command;
import fr.duminy.components.chain.CommandException;
import fr.duminy.components.chain.CommandListener;
import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.Cancellable;
import fr.duminy.jbackup.core.archive.*;
import fr.duminy.jbackup.core.command.*;
import fr.duminy.jbackup.core.util.FileDeleter;
import fr.duminy.jbackup.core.util.InputStreamComparator;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Objects;
import java.util.function.Supplier;

public class BackupTask extends FileCreatorTask {
    private static final Logger LOG = LoggerFactory.getLogger(BackupTask.class);

    public static class VerificationFailedException extends CommandException {
        public VerificationFailedException(String message) {
            super(message);
        }
    }

    public BackupTask(BackupConfiguration config, Supplier<FileDeleter> deleterSupplier,
                      TaskListener listener, Cancellable cancellable) {
        super(config, deleterSupplier, listener, cancellable);
    }

    @Override
    protected void executeTask(FileDeleter deleter) throws TaskException {
        Path target = Paths.get(config.getTargetDirectory());
        try {
            Files.createDirectories(target);
        } catch (IOException e) {
            throw new TaskException(e);
        }

        String archiveName = generateName(config.getName(), config.getArchiveFactory());

        Path archive = target.resolve(archiveName);

        final ArchiveParameters archiveParameters = new ArchiveParameters(archive, config.isRelativeEntries());
        for (BackupConfiguration.Source filter : config.getSources()) {
            IOFileFilter dirFilter = config.createIOFileFilter("_dir", filter.getDirFilter());
            IOFileFilter fileFilter = config.createIOFileFilter("_file", filter.getFileFilter());
            Path source = Paths.get(filter.getPath());
            archiveParameters.addSource(source, dirFilter, fileFilter);
        }

        MutableJBackupContext context = new MutableJBackupContext();
        context.setFileDeleter(deleter);
        context.setArchiveParameters(archiveParameters);
        context.setFactory(config.getArchiveFactory());
        context.setListener(listener);
        context.setCancellable(cancellable);
        context.setArchivePath(archiveParameters.getArchive());

        JBackupCommand[] commands;
        if (config.isVerify()) {
            commands = new JBackupCommand[] {
                createCollectFilesCommand(),
                createCompressCommand(),
                createVerifyArchiveCommand(),
            };
        } else {
            commands = new JBackupCommand[] {
                createCollectFilesCommand(),
                createCompressCommand()
            };
        }
        CommandListener<JBackupContext> listener = new CommandListener<JBackupContext>() {
            @Override
            public void commandStarted(Command command, JBackupContext jBackupContext) {
                LOG.info("Started command {}", command.getClass().getSimpleName());
            }

            @Override
            public void commandFinished(Command command, JBackupContext jBackupContext, Exception e) {
                LOG.info("Finished command {} {}", command.getClass().getSimpleName(), e);
            }
        };
        JBackupChain chain = new JBackupChain(listener, commands);
        try {
            chain.execute(context);
        } catch (CommandException e) {
            throw new TaskException(e);
        }
    }

    CollectFilesCommand createCollectFilesCommand() {
        return new CollectFilesCommand(new FileCollector());
    }

    CompressCommand createCompressCommand() {
        return new CompressCommand() {
            @Override
            protected Compressor createCompressor(ArchiveFactory factory) {
                return new Compressor(factory);
            }
        };
    }

    VerifyArchiveCommand createVerifyArchiveCommand() {
        return new VerifyArchiveCommand(new ArchiveVerifier(new InputStreamComparator()));
    }

    protected String generateName(String configName, ArchiveFactory factory) {
        Objects.requireNonNull(factory, "ArchiveFactory is null");

        Calendar date = Calendar.getInstance();
        return String.format("%1$s_%2$tY_%2$tm_%2$td_%2$tH_%2$tM_%2$tS.%3$s", configName, date, factory.getExtension());
    }
}
