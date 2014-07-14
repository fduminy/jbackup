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
package fr.duminy.jbackup.swing;

import fr.duminy.components.swing.form.DefaultFormBuilder;
import fr.duminy.components.swing.form.JFormPane;
import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.archive.ArchiveFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Panel representing a {@link fr.duminy.jbackup.core.BackupConfiguration}.
 */
public class BackupConfigurationPanel extends JPanel {
    private final JFormPane<BackupConfiguration> form;

    public BackupConfigurationPanel(final ArchiveFactory... factories) {
        DefaultFormBuilder<BackupConfiguration> formBuilder = new DefaultFormBuilder<BackupConfiguration>(BackupConfiguration.class) {
            @Override
            protected void configureBuilder(org.formbuilder.FormBuilder<BackupConfiguration> builder) {
                super.configureBuilder(builder);
                builder.useForProperty("sources", new SourceListTypeMapper(BackupConfigurationPanel.this));
                builder.useForProperty("archiveFactory", new ArchiveFactoryTypeMapper(factories));
            }
        };
        setLayout(new BorderLayout());
        form = new JFormPane<>(formBuilder, "Configuration", JFormPane.Mode.CREATE);
        add(form, BorderLayout.CENTER);
    }

    public void setConfiguration(BackupConfiguration config) {
        form.setValue(config);
    }

    public BackupConfiguration getConfiguration() {
        return form.getValue();
    }
}
