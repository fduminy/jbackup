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
package fr.duminy.jbackup.swing;

import fr.duminy.jbackup.core.archive.ArchiveFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Renders {@link fr.duminy.jbackup.core.archive.ArchiveFactory}.
 */
public class ArchiveFactoryRenderer extends DefaultListCellRenderer {
    public static final ArchiveFactoryRenderer INSTANCE = new ArchiveFactoryRenderer();

    private ArchiveFactoryRenderer() {
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        value = (value instanceof ArchiveFactory) ? ((ArchiveFactory) value).getExtension() : "";
        return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    }
}
