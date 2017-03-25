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
import fr.duminy.jbackup.core.archive.ArchiveException;
import fr.duminy.jbackup.core.archive.ArchiveFactory;
import fr.duminy.jbackup.core.archive.Decompressor;

public class DecompressCommand implements JBackupCommand {
    @Override
    public void execute(JBackupContext context) throws CommandException {
        context.getFileDeleter().registerDirectory(context.getTargetDirectory());
        try {
            createDecompressor(context.getFactory())
                .decompress(context.getArchivePath(), context.getTargetDirectory(), context.getListener(),
                            context.getCancellable());
        } catch (ArchiveException e) {
            throw new CommandException(e);
        }
    }

    @Override
    public void revert(JBackupContext context) {
        context.getFileDeleter().deleteAll();
    }

    Decompressor createDecompressor(ArchiveFactory factory) {
        return new Decompressor(factory);
    }
}
