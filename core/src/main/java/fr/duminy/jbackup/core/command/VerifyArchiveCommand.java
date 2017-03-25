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
package fr.duminy.jbackup.core.command;

import fr.duminy.components.chain.CommandException;
import fr.duminy.jbackup.core.archive.ArchiveVerifier;
import fr.duminy.jbackup.core.task.BackupTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Objects;

public class VerifyArchiveCommand implements JBackupCommand {
    private static final Logger LOG = LoggerFactory.getLogger(VerifyArchiveCommand.class);

    private final ArchiveVerifier verifier;

    public VerifyArchiveCommand(ArchiveVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    public void execute(JBackupContext context) throws CommandException {
        Objects.requireNonNull(context.getArchivePath(), "archivePath");
        Objects.requireNonNull(context.getArchivePath().getFileName(), "archivePath.fileName");
        String archive = context.getArchivePath().getFileName().toString();
        LOG.info("Verifing archive {}", archive);
        try (InputStream archiveInputStream = context.getArchive()) {
            final boolean valid = verifier
                .verify(context.getFactory(), archiveInputStream, context.getCollectedFiles());
            if (valid) {
                LOG.info("Archive {} valid", archive);
            } else {
                LOG.error("Archive {} corrupted", archive);
            }
            if (!valid) {
                throw new BackupTask.VerificationFailedException("Archive verification failed");
            }
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            throw new CommandException(e);
        }
    }

    @Override
    public void revert(JBackupContext context) {
        context.getFileDeleter().deleteAll();
    }
}
