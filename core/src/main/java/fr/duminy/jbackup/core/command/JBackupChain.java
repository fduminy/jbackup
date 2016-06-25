/**
 * JBackup is a software managing backups.
 * <p>
 * Copyright (C) 2013-2016 Fabien DUMINY (fabien [dot] duminy [at] webmails [dot] com)
 * <p>
 * JBackup is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * <p>
 * JBackup is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 */
package fr.duminy.jbackup.core.command;

import fr.duminy.components.chain.Command;
import fr.duminy.components.chain.CommandListener;
import fr.duminy.components.chain.SimpleChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JBackupChain extends SimpleChain<JBackupContext, JBackupCommand> {
    public JBackupChain(CommandListener listener, JBackupCommand... commands) {
        super(ErrorLogger.INSTANCE, listener, commands);
    }

    private static class ErrorLogger implements SimpleChain.ListenerErrorLogger<JBackupContext> {
        private static final ErrorLogger INSTANCE = new ErrorLogger();

        private ErrorLogger() {
        }

        @Override
        public void logError(String method, Command command, JBackupContext context, Exception errorInListener) {
            final Logger logger = LoggerFactory.getLogger(getClass().getName());
            if (logger.isErrorEnabled()) {
                logger.error(String.format("Error in %s(%s, %s)", method, command, context), errorInListener);
            }
        }
    }
}
