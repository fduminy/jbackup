/**
 * JBackup is a software managing backups.
 *
 * Copyright (C) 2013-2013 Fabien DUMINY (fabien [dot] duminy [at] webmails [dot] com)
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

import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.archive.ArchiveFactory;
import org.formbuilder.Form;
import org.formbuilder.FormBuilder;

import javax.swing.*;
import java.awt.*;

import static org.formbuilder.FormBuilder.map;
import static org.formbuilder.mapping.form.FormFactories.REPLICATING;

/**
 * Panel representing a {@link fr.duminy.jbackup.core.BackupConfiguration}.
 */
public class BackupConfigurationPanel extends JPanel {
    private final Form<BackupConfiguration> form;

    public BackupConfigurationPanel(ArchiveFactory... factories) {
        FormBuilder<BackupConfiguration> builder = map(BackupConfiguration.class).formsOf(REPLICATING);

        builder.useForProperty("sources", new SourceListTypeMapper(this));
        builder.useForProperty("archiveFactory", new ArchiveFactoryTypeMapper(factories));

        form = builder.buildForm();

        setLayout(new BorderLayout());
        add(form.asComponent(), BorderLayout.CENTER);
    }

    public void setConfiguration(BackupConfiguration config) {
        form.setValue(config);
    }

    public BackupConfiguration getConfiguration() {
        return form.getValue();
    }
}
