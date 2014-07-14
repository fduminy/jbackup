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

import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import fr.duminy.components.swing.AbstractSwingTest;
import fr.duminy.components.swing.TestUtilities;
import fr.duminy.components.swing.form.JFormPaneFixture;
import fr.duminy.components.swing.listpanel.ListPanelFixture;
import fr.duminy.components.swing.path.JPath;
import fr.duminy.components.swing.path.JPathFixture;
import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.ConfigurationManager;
import fr.duminy.jbackup.core.ConfigurationManagerTest;
import fr.duminy.jbackup.core.archive.ArchiveFactory;
import fr.duminy.jbackup.core.archive.ArchiveInputStream;
import fr.duminy.jbackup.core.archive.ArchiveOutputStream;
import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactoryTest;
import org.apache.commons.io.IOUtils;
import org.fest.assertions.Assertions;
import org.fest.swing.core.Robot;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.JButtonFixture;
import org.fest.swing.fixture.JComboBoxFixture;
import org.fest.swing.fixture.JListFixture;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.duminy.jbackup.core.ConfigurationManagerTest.ZIP_ARCHIVE_FACTORY;
import static fr.duminy.jbackup.swing.ConfigurationManagerPanel.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for class {@link ConfigurationManagerPanel}.
 */
@RunWith(Theories.class)
public class ConfigurationManagerPanelTest extends AbstractSwingTest {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationManagerPanelTest.class);

    @DataPoint
    public static final int INIT_NO_CONFIG = 0;
    @DataPoint
    public static final int INIT_1_CONFIG = 1;
    @DataPoint
    public static final int INIT_2_CONFIGS = 2;
    @DataPoint
    public static final int INIT_3_CONFIGS = 3;

    private static final List<BackupConfiguration.Source> NO_SOURCE = Collections.emptyList();

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
    private static final String CONFIG_MANAGER_PANEL_NAME = "configurations";

    private ConfigurationManagerPanel panel;
    private File configDir;
    private ConfigurationManager manager;
    private BackupConfigurationActions configActions;

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public final TestName testName = new TestName();

    @Override
    protected void onSetUp() {
        super.onSetUp();
        robot().settings().delayBetweenEvents(100);
        robot().settings().timeoutToBeVisible(60000);
    }

    @Theory
    public void testInit(int nbConfigurations) {
        List<BackupConfiguration> expectedConfigs = init(nbConfigurations);
        assertFormValues(expectedConfigs);
    }

    @Theory
    public void testAdd_defaultValues(int nbConfigurations) throws Exception {
        traceParameters("nbConfigurations", nbConfigurations);
                
        List<BackupConfiguration> expectedConfigs = init(nbConfigurations);
        BackupConfiguration config = new BackupConfiguration();
        config.setName(DEFAULT_CONFIG_NAME);
        expectedConfigs.add(config);

        ListPanelFixture<BackupConfiguration, JList> configurationList = new ListPanelFixture<>(robot(), CONFIG_MANAGER_PANEL_NAME);
        configurationList.addButton().click();
        assertConfigurationFormValues(DEFAULT_CONFIG_NAME, NO_SOURCE, "", null);

        new JFormPaneFixture(robot(), BackupConfiguration.class).okButton().click();

        assertFormValues(expectedConfigs);
        assertThat(manager.getBackupConfigurations()).usingElementComparator(COMPARATOR).containsOnlyOnce(Iterables.toArray(expectedConfigs, BackupConfiguration.class));
    }

    /**
     * Especially test the interactions while adding sources and their rendering in the list.
     */
    @Theory
    public void testAdd_customValues(int nbConfigurations) throws Exception {
        traceParameters("nbConfigurations", nbConfigurations);

        List<BackupConfiguration> expectedConfigs = init(nbConfigurations);

        ListPanelFixture<BackupConfiguration, JList> configurationList = new ListPanelFixture<>(robot(), CONFIG_MANAGER_PANEL_NAME);
        configurationList.addButton().click();

        robot().waitForIdle();
        final BackupConfiguration expectedConfig = fillConfigurationForm();
        expectedConfigs.add(expectedConfig);

        assertConfigurationFormValues(expectedConfig);
        new JFormPaneFixture(robot(), BackupConfiguration.class).okButton().click();

        assertFormValues(expectedConfigs);
        assertThat(manager.getBackupConfigurations()).usingElementComparator(COMPARATOR).containsOnlyOnce(Iterables.toArray(expectedConfigs, BackupConfiguration.class));
    }

    @Theory
    public void testRemove(int nbConfigurations) throws Exception {
        assumeTrue(nbConfigurations > 0);

        List<BackupConfiguration> expectedConfigs = init(nbConfigurations);
        BackupConfiguration config = manager.getBackupConfigurations().get(0);
        for (BackupConfiguration c : expectedConfigs) {
            if (c.getName().equals(config.getName())) {
                expectedConfigs.remove(c);
                break;
            }
        }

        ListPanelFixture<BackupConfiguration, JList> configurationList = new ListPanelFixture<>(robot(), CONFIG_MANAGER_PANEL_NAME);
        configurationList.list().selectItem(config.getName());
        configurationList.removeButton().click();

        assertFormValues(expectedConfigs);
        assertThat(manager.getBackupConfigurations()).usingElementComparator(COMPARATOR).containsOnlyOnce(Iterables.toArray(expectedConfigs, BackupConfiguration.class));
    }

    @Theory
    public void testBackup(int nbConfigurations) throws Exception {
        assumeTrue(nbConfigurations > 0);
        List<BackupConfiguration> configs = init(nbConfigurations);

        BackupConfiguration expectedConfig = configs.get(nbConfigurations - 1);
        ListPanelFixture<BackupConfiguration, JList> configurationList = new ListPanelFixture<>(robot(), CONFIG_MANAGER_PANEL_NAME);
        configurationList.list().selectItem(expectedConfig.getName());
        robot().waitForIdle();
        configurationList.userButton(BACKUP_BUTTON_NAME).requireToolTip(Messages.BACKUP_MESSAGE).click();
        robot().waitForIdle();
        
        ArgumentCaptor<BackupConfiguration> actualConfig = ArgumentCaptor.forClass(BackupConfiguration.class);
        verify(configActions, times(1)).backup(actualConfig.capture());
        verifyNoMoreInteractions(configActions);

        ConfigurationManagerTest.assertAreEquals(expectedConfig, actualConfig.getValue());
    }

    @Test
    public void testBackup_noSelection() throws Exception {
        init(1);

        ListPanelFixture<BackupConfiguration, JList> configurationList = new ListPanelFixture<>(robot(), CONFIG_MANAGER_PANEL_NAME);
        configurationList.list().selectItem(0).clearSelection();
        configurationList.userButton(BACKUP_BUTTON_NAME).requireDisabled();
    }

    @Test
    public void testBackup_multipleSelection() throws Exception {
        init(3);

        ListPanelFixture<BackupConfiguration, JList> configurationList = new ListPanelFixture<>(robot(), CONFIG_MANAGER_PANEL_NAME);
        configurationList.list().selectItems(0, 2);
        configurationList.userButton(BACKUP_BUTTON_NAME).requireEnabled();
    }

    @Theory
    public void testRestore_cancel(int nbConfigurations) throws Exception {
        testRestore(nbConfigurations, false);
    }

    @Theory
    public void testRestore_ok(int nbConfigurations) throws Exception {
        testRestore(nbConfigurations, true);
    }

    private void testRestore(int nbConfigurations, boolean ok) throws Exception {
        assumeTrue(nbConfigurations > 0);
        List<BackupConfiguration> configs = init(nbConfigurations);

        BackupConfiguration expectedConfig = configs.get(nbConfigurations - 1);
        File expectedArchive = createArchive(expectedConfig);
        final File expectedTargetDirectory = tempFolder.newFile("targetDir");
        ListPanelFixture<BackupConfiguration, JList> configurationList = new ListPanelFixture<>(robot(), CONFIG_MANAGER_PANEL_NAME);
        configurationList.list().selectItem(expectedConfig.getName());
        configurationList.userButton(RESTORE_BUTTON_NAME).requireToolTip(Messages.RESTORE_MESSAGE).click();

        JFormPaneFixture fixture = new JFormPaneFixture(robot(), RestoreAction.RestoreParameters.class);
        fixture.requireInDialog(true).requireModeCreate();
//TODO        fixture.requireQuestionMessage();
//TODO        fixture.requireTitle("Restore backup '" + expectedConfig.getName() + "'");
        fixture.path("archive").requireSelectedPath(expectedArchive.toPath());
        final JPathFixture pathFixture = fixture.path("targetDirectory");
        pathFixture.requireSelectedPath(null);

        GuiActionRunner.execute(new GuiQuery<Object>() {
            protected Object executeInEDT() {
                pathFixture.selectPath(expectedTargetDirectory.toPath());
                return null;
            }
        });

        if (ok) {
            fixture.okButton().click();

            ArgumentCaptor<BackupConfiguration> actualConfig = ArgumentCaptor.forClass(BackupConfiguration.class);
            verify(configActions, times(1)).restore(actualConfig.capture(), eq(expectedArchive), eq(expectedTargetDirectory));
            verifyNoMoreInteractions(configActions);

            ConfigurationManagerTest.assertAreEquals(expectedConfig, actualConfig.getValue());
        } else {
            fixture.cancelButton().click();

            verify(configActions, never()).restore(any(BackupConfiguration.class), any(File.class), any(File.class));
            verifyNoMoreInteractions(configActions);
        }
    }

    @Test
    public void testRestore_noSelection() throws Exception {
        init(1);

        ListPanelFixture<BackupConfiguration, JList> configurationList = new ListPanelFixture<>(robot(), CONFIG_MANAGER_PANEL_NAME);
        configurationList.list().selectItem(0).clearSelection();
        configurationList.userButton(RESTORE_BUTTON_NAME).requireDisabled();
    }

    @Test
    public void testRestore_multipleSelection() throws Exception {
        init(3);

        ListPanelFixture<BackupConfiguration, JList> configurationList = new ListPanelFixture<>(robot(), CONFIG_MANAGER_PANEL_NAME);
        configurationList.list().selectItems(0, 2);
        configurationList.userButton(RESTORE_BUTTON_NAME).requireDisabled();
    }

    private File createArchive(BackupConfiguration config) throws IOException {
        File result = new File(config.getTargetDirectory(), "archive.zip");
        IOUtils.copy(ZipArchiveFactoryTest.getArchive(), new FileOutputStream(result));
        return result;
    }

    private List<BackupConfiguration> init(int nbConfigurations) {
        try {
            configDir = tempFolder.newFolder();

            // add configurations in the directory
            ConfigurationManager tmpManager = new ConfigurationManager(configDir);
            List<BackupConfiguration> configs = createConfigurations(nbConfigurations);
            for (BackupConfiguration config : configs) {
                tmpManager.addBackupConfiguration(config);
            }

            manager = new ConfigurationManager(configDir);
            configActions = mock(BackupConfigurationActions.class);

            final ConfigurationManager mgr = manager;
            panel = buildAndShowWindow(new Supplier<ConfigurationManagerPanel>() {
                @Override
                public ConfigurationManagerPanel get() {
                    try {
                        final ConfigurationManagerPanel panel = new ConfigurationManagerPanel(mgr, configActions, (JComponent) getFrame().getContentPane(), ARCHIVE_FACTORIES);
                        panel.setName(CONFIG_MANAGER_PANEL_NAME);
                        return panel;                        
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            return configs;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<BackupConfiguration> createConfigurations(int nbConfigurations) {
        List<BackupConfiguration> configs = new ArrayList<>();
        for (int i = 0; i < nbConfigurations; i++) {
            configs.add(createConfiguration(i));
        }
        return configs;
    }

    private BackupConfiguration createConfiguration(int i) {
        Path targetDirectory = tempFolder.newFolder("archiveDirectory").toPath();
        return ConfigurationManagerTest.createConfiguration("name" + i, targetDirectory);
    }

    private void assertFormValues(List<BackupConfiguration> configs) {
        JListFixture jl = window.list(CONFIG_MANAGER_PANEL_NAME).requireVisible().requireEnabled();
        String[] renderedConfigs = new String[configs.size()];
        for (int i = 0; i < renderedConfigs.length; i++) {
            renderedConfigs[i] = configs.get(i).getName();
        }

        String[] actualContents = jl.contents();
        Arrays.sort(actualContents); //TODO enable sort directly in the ListModel
        Arrays.sort(renderedConfigs);
        Assertions.assertThat(actualContents).as(CONFIG_MANAGER_PANEL_NAME).isEqualTo(renderedConfigs);
    }

    private BackupConfiguration fillConfigurationForm() {
        return fillConfigurationForm(robot());
    }

    public static BackupConfiguration fillConfigurationForm(Robot robot) {
        JFormPaneFixture configForm = new JFormPaneFixture(robot, BackupConfiguration.class);
        final BackupConfiguration expectedConfig = ConfigurationManagerTest.createConfiguration();

        //TODO support dirFilter and fileFilter
        for (BackupConfiguration.Source source : expectedConfig.getSources()) {
            source.setDirFilter(null);
            source.setFileFilter(null);
        }

        configForm.textBox("name").deleteText().enterText(expectedConfig.getName());
        JButtonFixture addSource = configForm.listPanel().addButton();
        for (final BackupConfiguration.Source source : expectedConfig.getSources()) {
            addSource.click();
            robot.waitForIdle();

            JFormPaneFixture sourceForm = new JFormPaneFixture(robot, BackupConfiguration.Source.class);
            final JPathFixture sourcePath = sourceForm.path();
            sourcePath.requireSelectionMode(JPath.SelectionMode.FILES_AND_DIRECTORIES);
            GuiActionRunner.execute(new GuiTask() {
                protected void executeInEDT() {
                    sourcePath.selectPath(source.getSourceDirectory().toPath());
                }
            });

            sourceForm.okButton().click();
        }
        configForm.textBox("targetDirectory").enterText(expectedConfig.getTargetDirectory());
        configForm.comboBox("archiveFactory").selectItem("zip");
        return expectedConfig;
    }

    private void traceParameters(Object... parameters) {
        //TODO move this to a utility class
        StringBuilder sb = new StringBuilder("***\n*** Starting test ");
        sb.append(testName.getMethodName()).append('(');
        for (int i = 0; i < parameters.length; i += 2) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(parameters[i]).append('=').append(parameters[i + 1]);
        }
        sb.append(")\n***\n");
        LOG.debug(sb.toString());
    }

    private void dumpComponents(String context) {
        //TODO move this to a utility class
        LOG.debug("{} :\n{}", context, TestUtilities.dumpComponents(robot()));
    }

//    private static GenericTypeMatcher<JDialog> withTitle(final String title) {
//        return new GenericTypeMatcher<JDialog>(JDialog.class) {
//            @Override
//            protected boolean isMatching(JDialog component) {
//                return component.getTitle().equals(title);
//            }
//
//            @Override
//            public String toString() {
//                return "<Dialog with title '" + title + "'>";
//            }
//        };
//    }

    private void assertConfigurationFormValues(BackupConfiguration config) {
        assertConfigurationFormValues(config.getName(), config.getSources(), config.getTargetDirectory(), config.getArchiveFactory());
    }

    private void assertConfigurationFormValues(String name, List<BackupConfiguration.Source> sources, String targetDirectory, ArchiveFactory selectedArchiveFactory) {
        JFormPaneFixture formPane = new JFormPaneFixture(robot(), BackupConfiguration.class);
        formPane.textBox("name").requireVisible().requireEnabled().requireEditable().requireText(name);

        ListPanelFixture<BackupConfiguration.Source, JList<BackupConfiguration.Source>> listPanel = formPane.listPanel();
        listPanel.requireVisible()/*.requireEnabled()*/;
        String[] renderedSources = new String[sources.size()];
        for (int i = 0; i < renderedSources.length; i++) {
            renderedSources[i] = sources.get(i).getSourceDirectory().getAbsolutePath();
        }
        Assertions.assertThat(listPanel.list().contents()).as("sources").isEqualTo(renderedSources);

        formPane.textBox("targetDirectory").requireVisible().requireEnabled().requireEditable().requireText(targetDirectory);

        JComboBoxFixture cb = formPane.comboBox("archiveFactory").requireVisible().requireEnabled().requireNotEditable();
        String[] expectedExtensions = new String[ARCHIVE_FACTORIES.length + 1];
        expectedExtensions[0] = "";
        for (int i = 0; i < ARCHIVE_FACTORIES.length; i++) {
            expectedExtensions[i + 1] = ARCHIVE_FACTORIES[i].getExtension();
        }
        Arrays.sort(expectedExtensions);
        Assertions.assertThat(cb.contents()).as("archive factories").isEqualTo(expectedExtensions);

        if (selectedArchiveFactory == null) {
            cb.requireNoSelection();
        } else {
            cb.requireSelection(selectedArchiveFactory.getExtension());
        }
    }
}
