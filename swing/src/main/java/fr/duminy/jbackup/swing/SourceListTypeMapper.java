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
import fr.duminy.components.swing.form.StringPathTypeMapper;
import fr.duminy.components.swing.list.DefaultMutableListModel;
import fr.duminy.components.swing.listpanel.ListPanel;
import fr.duminy.components.swing.listpanel.SimpleItemManager;
import fr.duminy.components.swing.path.JPath;
import fr.duminy.components.swing.path.JPathBuilder;
import org.apache.commons.lang3.builder.Builder;
import org.formbuilder.FormBuilder;
import org.formbuilder.TypeMapper;
import org.formbuilder.mapping.change.ChangeHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

import static fr.duminy.components.swing.listpanel.SimpleItemManager.ContainerType.DIALOG;
import static fr.duminy.jbackup.core.BackupConfiguration.Source;

/**
 * A {@link org.formbuilder.TypeMapper} for {@link fr.duminy.jbackup.core.archive.ArchiveFactory}.
 * TODO : create a generic version from this in swing-components and (try to) associate it to list (and other collections ?) properties by default.
 */
public class SourceListTypeMapper implements TypeMapper<ListPanel<Source, JList<Source>>, Object> {
    private static final Builder<JPath> SHOW_HIDDEN_FILES_BUILDER = new JPathBuilder().fileHidingEnabled(false);
    private final JComponent parent;

    public SourceListTypeMapper(JComponent parent) {
        this.parent = parent;
    }

    @Override
    public void handleChanges(@Nonnull ListPanel listPanel, @Nonnull ChangeHandler changeHandler) {
        //TODO implement this.
    }

    @Nonnull
    @Override
    public ListPanel<Source, JList<Source>> createEditorComponent() {
        JList<Source> list = new JList(new DefaultMutableListModel<Source>());
        list.setName("sources");
        list.setCellRenderer(SourceRenderer.INSTANCE);

        DefaultFormBuilder sourceFormBuilder = new DefaultFormBuilder<Source>(Source.class) {
            @Override
            protected void configureBuilder(FormBuilder<Source> builder) {
                super.configureBuilder(builder);
                builder.useForProperty("sourceDirectory", new StringPathTypeMapper(SHOW_HIDDEN_FILES_BUILDER));
            }
        };
        SimpleItemManager<Source> sourceProvider = new SimpleItemManager<>(Source.class, sourceFormBuilder, parent, "Sources", DIALOG);
        return new ListPanel<>(list, sourceProvider);
    }

    @Nullable
    @Override
    public Object getValue(@Nonnull ListPanel<Source, JList<Source>> listPanel) {
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
    public void setValue(@Nonnull ListPanel<Source, JList<Source>> listPanel, @Nullable Object o) {
        DefaultListModel<Source> model = getModel(listPanel);
        model.clear();
        if (o != null) {
            for (Source source : (List<Source>) o) {
                model.addElement(source);
            }
        }
    }

    private DefaultListModel<Source> getModel(ListPanel<Source, JList<Source>> listPanel) {
        JList<Source> list = listPanel.getListComponent();
        return (DefaultListModel<Source>) list.getModel();
    }
}