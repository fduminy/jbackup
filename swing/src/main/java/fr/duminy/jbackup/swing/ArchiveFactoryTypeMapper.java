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
package fr.duminy.jbackup.swing;

import fr.duminy.jbackup.core.archive.ArchiveFactory;
import org.formbuilder.mapping.change.ChangeHandler;
import org.formbuilder.mapping.typemapper.impl.ReferenceToComboboxMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static java.util.Collections.addAll;
import static java.util.Collections.sort;

/**
 * A {@link org.formbuilder.TypeMapper} for {@link fr.duminy.jbackup.core.archive.ArchiveFactory}.
 */
public class ArchiveFactoryTypeMapper extends ReferenceToComboboxMapper<ArchiveFactory> {
    private final List<ArchiveFactory> sortedFactories;

    public ArchiveFactoryTypeMapper(ArchiveFactory[] factories) {
        sortedFactories = new ArrayList<>(factories.length + 1);
        addAll(sortedFactories, factories);
        sort(sortedFactories, ArchiveFactoryComparator.INSTANCE);
        sortedFactories.add(0, null);
    }

    private static class ArchiveFactoryComparator implements Comparator<ArchiveFactory> {
        private static final ArchiveFactoryComparator INSTANCE = new ArchiveFactoryComparator();

        private ArchiveFactoryComparator() {
        }

        @Override
        public int compare(ArchiveFactory o1, ArchiveFactory o2) {
            return o1.getExtension().compareTo(o2.getExtension());
        }
    }

    @Nonnull
    @Override
    public JComboBox createEditorComponent() {
        JComboBox result = super.createEditorComponent();
        result.setRenderer(ArchiveFactoryRenderer.INSTANCE);
        return result;
    }

    @Nonnull
    @Override
    protected Collection<ArchiveFactory> getSuitableData() {
        return sortedFactories;
    }

    @Override
    public void handleChanges(@Nonnull JComboBox jComponent, @Nonnull ChangeHandler changeHandler) {
        //TODO implement this.
    }

    @Nullable
    @Override
    public ArchiveFactory getValue(@Nonnull JComboBox jComponent) {
        Object item = jComponent.getSelectedItem();
        return (item instanceof ArchiveFactory) ? (ArchiveFactory) item : null;
    }

    @Nonnull
    @Override
    public Class<ArchiveFactory> getValueClass() {
        return ArchiveFactory.class;
    }

    @Override
    public void setValue(@Nonnull JComboBox jComponent, @Nullable ArchiveFactory o) {
        jComponent.setSelectedItem(o);
    }
}