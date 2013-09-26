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
import fr.duminy.components.swing.AbstractSwingTest;
import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.archive.ArchiveFactory;
import fr.duminy.jbackup.core.archive.ArchiveInputStream;
import fr.duminy.jbackup.core.archive.ArchiveOutputStream;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.fixture.JComboBoxFixture;
import org.fest.swing.fixture.JFileChooserFixture;
import org.fest.swing.fixture.JListFixture;
import org.junit.Test;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.duminy.jbackup.core.BackupConfiguration.Source;
import static fr.duminy.jbackup.core.ConfigurationManagerTest.*;
import static org.fest.assertions.Assertions.assertThat;

/**
 * Tests for class {@link BackupConfigurationPanel}.
 */
public class BackupConfigurationPanelTest extends AbstractSwingTest {
    private static final List<Source> NO_SOURCE = Collections.emptyList();

    private static final ArchiveFactory FAKE_ARCHIVE_FACTORY = new ArchiveFactory() {
        @Override
        public String getExtension() {
            return "fake";
        }

        @Override
        public ArchiveInputStream create(InputStream input) throws IOException {
            return null;
        }

        @Override
        public ArchiveOutputStream create(OutputStream output) throws IOException {
            return null;
        }
    };

    /**
     * This array must not be sorted in ascending order on the extension property
     */
    private static final ArchiveFactory[] ARCHIVE_FACTORIES = {ZIP_ARCHIVE_FACTORY, FAKE_ARCHIVE_FACTORY};

    private BackupConfigurationPanel panel;

    @Override
    public void onSetUp() {
        super.onSetUp();

        try {
            panel = buildAndShowWindow(new Supplier<BackupConfigurationPanel>() {
                @Override
                public BackupConfigurationPanel get() {
                    return new BackupConfigurationPanel(ARCHIVE_FACTORIES);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testInitialState() throws Exception {
        assertFormValues("", NO_SOURCE, "", null);
    }

    @Test
    public void testSetConfiguration() throws Exception {
        final BackupConfiguration expectedConfiguration = createConfiguration();

        GuiActionRunner.execute(new GuiQuery<Object>() {
            protected Object executeInEDT() {
                panel.setConfiguration(expectedConfiguration);
                return null;
            }
        });

        assertFormValues(expectedConfiguration);
    }

    /**
     * Especially test the interactions while adding sources and their rendering in the list.
     */
    @Test
    public void testFillForm() {
        final BackupConfiguration expectedConfig = fillForm();

        assertFormValues(expectedConfig);
    }

    @Test
    public void testGetConfiguration() throws Exception {
        final BackupConfiguration expectedConfig = fillForm();

        // test
        BackupConfiguration actualConfig = panel.getConfiguration();

        // assertions
        assertAreEquals(expectedConfig, actualConfig);
    }

    private BackupConfiguration fillForm() {
        final BackupConfiguration expectedConfig = createConfiguration();

        //TODO support dirFilter and fileFilter
        for (Source source : expectedConfig.getSources()) {
            source.setDirFilter(null);
            source.setFileFilter(null);
        }

        window.textBox("name").enterText(expectedConfig.getName());
        for (Source source : expectedConfig.getSources()) {
            window.button("addButton").click();
            JFileChooserFixture jfc = window.fileChooser();

            //TODO also support multiple files/directories in listpanel
            assertThat(jfc.component().isMultiSelectionEnabled()).as("multiSelectionEnabled").isFalse(); //TODO add that to fest assert
            assertThat(jfc.component().getFileSelectionMode()).as("fileSelectionMode").isEqualTo(JFileChooser.FILES_AND_DIRECTORIES); // TODO add that to fest assert

            jfc.selectFile(source.getSourceDirectory()).approve();
        }
        window.textBox("targetDirectory").enterText(expectedConfig.getTargetDirectory());
        window.comboBox("archiveFactory").selectItem("zip");
        return expectedConfig;
    }

    private void assertFormValues(BackupConfiguration config) {
        assertFormValues(config.getName(), config.getSources(), config.getTargetDirectory(), config.getArchiveFactory());
    }

    private void assertFormValues(String name, List<Source> sources, String targetDirectory, ArchiveFactory selectedArchiveFactory) {
        window.textBox("name").requireVisible().requireEnabled().requireEditable().requireText(name);

        JListFixture jl = window.list("sources").requireVisible()/*.requireEnabled()*/;
        String[] renderedSources = new String[sources.size()];
        for (int i = 0; i < renderedSources.length; i++) {
            renderedSources[i] = sources.get(i).getSourceDirectory().getAbsolutePath();
        }
        assertThat(jl.contents()).as("sources").isEqualTo(renderedSources);

        window.textBox("targetDirectory").requireVisible().requireEnabled().requireEditable().requireText(targetDirectory);

        JComboBoxFixture cb = window.comboBox("archiveFactory").requireVisible().requireEnabled().requireNotEditable();
        String[] expectedExtensions = new String[ARCHIVE_FACTORIES.length + 1];
        expectedExtensions[0] = "";
        for (int i = 0; i < ARCHIVE_FACTORIES.length; i++) {
            expectedExtensions[i + 1] = ARCHIVE_FACTORIES[i].getExtension();
        }
        Arrays.sort(expectedExtensions);
        assertThat(cb.contents()).as("archive factories").isEqualTo(expectedExtensions);

        if (selectedArchiveFactory == null) {
            cb.requireNoSelection();
        } else {
            cb.requireSelection(selectedArchiveFactory.getExtension());
        }
    }
}
