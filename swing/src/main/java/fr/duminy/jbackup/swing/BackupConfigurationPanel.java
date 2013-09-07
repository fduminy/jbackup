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

import com.google.common.base.Supplier;
import fr.duminy.components.swing.list.ListPanel;
import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.archive.ArchiveFactory;
import org.formbuilder.Form;
import org.formbuilder.FormBuilder;
import org.formbuilder.TypeMapper;
import org.formbuilder.mapping.change.ChangeHandler;
import org.formbuilder.mapping.typemapper.impl.ReferenceToComboboxMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import static fr.duminy.jbackup.core.BackupConfiguration.Source;
import static org.formbuilder.FormBuilder.map;
import static org.formbuilder.mapping.form.FormFactories.REPLICATING;

/**
 * Panel representing a {@link fr.duminy.jbackup.core.BackupConfiguration}.
 */
public class BackupConfigurationPanel extends JPanel {
    private final Form<BackupConfiguration> form;

    public BackupConfigurationPanel(ArchiveFactory... factories) {
        final List<ArchiveFactory> sortedFactories = new ArrayList<>(factories.length + 1);
        for (ArchiveFactory f : factories) {
            sortedFactories.add(f);
        }
        Collections.sort(sortedFactories, new Comparator<ArchiveFactory>() {
            @Override
            public int compare(ArchiveFactory o1, ArchiveFactory o2) {
                return o1.getExtension().compareTo(o2.getExtension());
            }
        });
        sortedFactories.add(0, null);

        FormBuilder<BackupConfiguration> builder = map(BackupConfiguration.class).formsOf(REPLICATING);

        builder.useForProperty("sources", new TypeMapper<ListPanel<JList<Source>, Source>, Object>() {
            @Override
            public void handleChanges(@Nonnull ListPanel listPanel, @Nonnull ChangeHandler changeHandler) {
                //TODO implement this.
            }

            @Nonnull
            @Override
            public ListPanel<JList<Source>, Source> createEditorComponent() {
                JList<Source> list = new JList(new DefaultListModel<Source>());
                list.setName("sources");
                list.setCellRenderer(SourceRenderer.INSTANCE);

                Supplier<Source> sourceProvider = new Supplier<Source>() {
                    @Override
                    public Source get() {
                        Source source = null;
                        JFileChooser chooser = new JFileChooser();
                        if (chooser.showOpenDialog(BackupConfigurationPanel.this) == JFileChooser.APPROVE_OPTION) {
                            source = new Source();
                            source.setSourceDirectory(chooser.getSelectedFile());
                        }
                        return source;
                    }
                };
                return new ListPanel<>(list, sourceProvider);
            }

            @Nullable
            @Override
            public Object getValue(@Nonnull ListPanel<JList<Source>, Source> listPanel) {
                List<Source> result = new ArrayList<>();

                DefaultListModel<Source> model = getModel(listPanel);
                for (int i = 0; i < model.getSize(); i++) {
                    result.add(model.getElementAt(i));
                }

                return result;
            }

            @Nonnull
            @Override
            public Class getValueClass() {
                return List.class;
            }

            @Override
            public void setValue(@Nonnull ListPanel<JList<Source>, Source> listPanel, @Nullable Object o) {
                DefaultListModel<Source> model = getModel(listPanel);
                model.clear();
                for (Source source : (List<Source>) o) {
                    model.addElement(source);
                }
            }

            private DefaultListModel<Source> getModel(ListPanel<JList<Source>, Source> listPanel) {
                JList<Source> list = listPanel.getListComponent();
                return (DefaultListModel<Source>) list.getModel();
            }
        }).useForProperty("archiveFactory", new ReferenceToComboboxMapper() {
            @Nonnull
            @Override
            public JComboBox createEditorComponent() {
                JComboBox result = super.createEditorComponent();
                result.setRenderer(ArchiveFactoryRenderer.INSTANCE);
                return result;
            }

            @Nonnull
            @Override
            protected Collection getSuitableData() {
                return sortedFactories;
            }

            @Override
            public void handleChanges(@Nonnull JComponent jComponent, @Nonnull ChangeHandler changeHandler) {
                //TODO implement this.
            }

            @Nullable
            @Override
            public Object getValue(@Nonnull JComponent jComponent) {
                Object item = ((JComboBox) jComponent).getSelectedItem();
                return (item instanceof ArchiveFactory) ? (ArchiveFactory) item : null;
            }

            @Nonnull
            @Override
            public Class getValueClass() {
                return ArchiveFactory.class;
            }

            @Override
            public void setValue(@Nonnull JComponent jComponent, @Nullable Object o) {
                ((JComboBox) jComponent).setSelectedItem((ArchiveFactory) o);
            }
        });

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
