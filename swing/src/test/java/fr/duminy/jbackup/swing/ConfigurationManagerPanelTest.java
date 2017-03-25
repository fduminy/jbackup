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

import com.google.common.collect.Iterables;
import fr.duminy.components.swing.AbstractSwingTest;
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
import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactory;
import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactoryTest;
import fr.duminy.jbackup.core.util.LogRule;
import org.assertj.core.api.Assertions;
import org.assertj.swing.core.Robot;
import org.assertj.swing.core.matcher.JLabelMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.edt.GuiTask;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JComboBoxFixture;
import org.assertj.swing.fixture.JListFixture;
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
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static fr.duminy.components.swing.listpanel.StandardListPanelFeature.EDITING;
import static fr.duminy.jbackup.swing.ConfigurationManagerPanel.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;
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
        public ArchiveInputStream create(InputStream input) {
            return null;
        }

        @Override
        public ArchiveOutputStream create(OutputStream output) {
            return null;
        }
    };

    /**
     * This array must not be sorted in ascending order on the extension property
     */
    private static final ArchiveFactory[] ARCHIVE_FACTORIES = {ZipArchiveFactory.INSTANCE, FAKE_ARCHIVE_FACTORY};
    private static final String CONFIG_MANAGER_PANEL_NAME = "configurations";

    private ConfigurationManagerPanel panel;
    private Path configDir;
    private ConfigurationManager manager;
    private BackupConfigurationActions configActions;

    @Rule
    public final LogRule logRule = new LogRule();

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public final TestName testName = new TestName();

    @Override
    protected void onSetUp() {
        super.onSetUp();
        getRobot().settings().delayBetweenEvents(100);
        getRobot().settings().timeoutToBeVisible(60000);
    }

    @Theory
    public void testInit(int nbConfigurations) {
        List<BackupConfiguration> expectedConfigs = init(nbConfigurations);
        assertFormValues(expectedConfigs);
        ListPanelFixture<BackupConfiguration, JList> configurationList = new ListPanelFixture<>(getRobot(), CONFIG_MANAGER_PANEL_NAME);
        configurationList.requireOnlyFeatures(EDITING);
    }

    @Test
    public void testUpdate() throws Exception {
        BackupConfiguration oldConfig = init(1).get(0);

        ListPanelFixture<BackupConfiguration, JList> configurationList = new ListPanelFixture<>(getRobot(), CONFIG_MANAGER_PANEL_NAME);
        configurationList.list().selectItem(0);
        configurationList.updateButton().click();

        JFormPaneFixture form = new JFormPaneFixture(getRobot(), BackupConfiguration.class);
        String newConfigName = "New config name";
        form.textBox("name").setText(newConfigName);
        form.okButton().click();

        assertThat(manager.getBackupConfigurations()).hasSize(1);
        BackupConfiguration updatedConfig = manager.getBackupConfigurations().get(0);
        assertThat(updatedConfig).isNotSameAs(oldConfig);
        assertThat(manager.getBackupConfigurations().get(0).getName()).isEqualTo(newConfigName);
    }

    @Test
    public void assertXmlVersionFieldNotDisplayed() throws Exception {
        // prepare test
        init(1);
        ListPanelFixture<BackupConfiguration, JList> configurationList = new ListPanelFixture<>(getRobot(), CONFIG_MANAGER_PANEL_NAME);

        // test
        configurationList.addButton().click();

        //assertions
        JPanel form = new JFormPaneFixture(getRobot(), BackupConfiguration.class).target();
        TreeSet<String> labels = getRobot().finder().findAll(form, JLabelMatcher.any()).stream()
                                           .filter(label -> !"List.cellRenderer".equals(label.getName()))
                                           .map(Component::getName).collect(Collectors.toCollection(TreeSet::new));
        assertThat(labels).containsExactly("archiveFactory", "name", "relativeEntries", "sources", "targetDirectory", "verify");
    }

    @Theory
    public void testAdd_defaultValues(int nbConfigurations) throws Exception {
        traceParameters("nbConfigurations", nbConfigurations);

        List<BackupConfiguration> expectedConfigs = init(nbConfigurations);
        BackupConfiguration config = new BackupConfiguration();
        config.setName(DEFAULT_CONFIG_NAME);
        expectedConfigs.add(config);

        ListPanelFixture<BackupConfiguration, JList> configurationList = new ListPanelFixture<>(getRobot(), CONFIG_MANAGER_PANEL_NAME);
        configurationList.addButton().click();
        assertConfigurationFormValues(DEFAULT_CONFIG_NAME, NO_SOURCE, null, null);

        new JFormPaneFixture(getRobot(), BackupConfiguration.class).okButton().click();

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

        ListPanelFixture<BackupConfiguration, JList> configurationList = new ListPanelFixture<>(getRobot(), CONFIG_MANAGER_PANEL_NAME);
        configurationList.addButton().click();

        getRobot().waitForIdle();
        final BackupConfiguration expectedConfig = fillConfigurationForm();
        expectedConfigs.add(expectedConfig);

        assertConfigurationFormValues(expectedConfig);
        new JFormPaneFixture(getRobot(), BackupConfiguration.class).okButton().click();

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

        ListPanelFixture<BackupConfiguration, JList> configurationList = new ListPanelFixture<>(getRobot(), CONFIG_MANAGER_PANEL_NAME);
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
        ListPanelFixture<BackupConfiguration, JList> configurationList = new ListPanelFixture<>(getRobot(), CONFIG_MANAGER_PANEL_NAME);
        configurationList.list().selectItem(expectedConfig.getName());
        getRobot().waitForIdle();
        configurationList.userButton(BACKUP_BUTTON_NAME).requireToolTip(Messages.BACKUP_MESSAGE).click();
        getRobot().waitForIdle();

        ArgumentCaptor<BackupConfiguration> actualConfig = ArgumentCaptor.forClass(BackupConfiguration.class);
        verify(configActions, times(1)).backup(actualConfig.capture());
        verifyNoMoreInteractions(configActions);

        ConfigurationManagerTest.assertAreEquals(expectedConfig, actualConfig.getValue());
    }

    @Test
    public void testBackup_noSelection() throws Exception {
        init(1);

        ListPanelFixture<BackupConfiguration, JList> configurationList = new ListPanelFixture<>(getRobot(), CONFIG_MANAGER_PANEL_NAME);
        configurationList.list().selectItem(0).clearSelection();
        configurationList.userButton(BACKUP_BUTTON_NAME).requireDisabled();
    }

    @Test
    public void testBackup_multipleSelection() throws Exception {
        init(3);

        ListPanelFixture<BackupConfiguration, JList> configurationList = new ListPanelFixture<>(getRobot(), CONFIG_MANAGER_PANEL_NAME);
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
        Path expectedArchive = createArchive(expectedConfig);
        final Path expectedTargetDirectory = tempFolder.newFile("targetDir").toPath();
        ListPanelFixture<BackupConfiguration, JList> configurationList = new ListPanelFixture<>(getRobot(), CONFIG_MANAGER_PANEL_NAME);
        configurationList.list().selectItem(expectedConfig.getName());
        configurationList.userButton(RESTORE_BUTTON_NAME).requireToolTip(Messages.RESTORE_MESSAGE).click();

        JFormPaneFixture fixture = new JFormPaneFixture(getRobot(), RestoreAction.RestoreParameters.class);
        fixture.requireInDialog(true).requireModeCreate();
//TODO        fixture.requireQuestionMessage();
//TODO        fixture.requireTitle("Restore backup '" + expectedConfig.getName() + "'");
        fixture.path("archive").requireSelectedPath(expectedArchive);
        final JPathFixture pathFixture = fixture.path("targetDirectory");
        pathFixture.requireSelectedPath(null);

        GuiActionRunner.execute(new GuiTask() {
            protected void executeInEDT() {
                pathFixture.selectPath(expectedTargetDirectory);
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

            verify(configActions, never()).restore(any(BackupConfiguration.class), any(Path.class), any(Path.class));
            verifyNoMoreInteractions(configActions);
        }
    }

    @Test
    public void testRestore_noSelection() throws Exception {
        init(1);

        ListPanelFixture<BackupConfiguration, JList> configurationList = new ListPanelFixture<>(getRobot(), CONFIG_MANAGER_PANEL_NAME);
        configurationList.list().selectItem(0).clearSelection();
        configurationList.userButton(RESTORE_BUTTON_NAME).requireDisabled();
    }

    @Test
    public void testRestore_multipleSelection() throws Exception {
        init(3);

        ListPanelFixture<BackupConfiguration, JList> configurationList = new ListPanelFixture<>(getRobot(), CONFIG_MANAGER_PANEL_NAME);
        configurationList.list().selectItems(0, 2);
        configurationList.userButton(RESTORE_BUTTON_NAME).requireDisabled();
    }

    private Path createArchive(BackupConfiguration config) throws IOException {
        return ZipArchiveFactoryTest.createArchive(Paths.get(config.getTargetDirectory()));
    }

    private List<BackupConfiguration> init(int nbConfigurations) {
        try {
            configDir = tempFolder.newFolder().toPath();

            // add configurations in the directory
            ConfigurationManager tmpManager = new ConfigurationManager(configDir);
            List<BackupConfiguration> configs = createConfigurations(nbConfigurations);
            for (BackupConfiguration config : configs) {
                tmpManager.addBackupConfiguration(config);
            }

            manager = new ConfigurationManager(configDir);
            configActions = mock(BackupConfigurationActions.class);

            final ConfigurationManager mgr = manager;
            panel = buildAndShowWindow(() -> {
                try {
                    final ConfigurationManagerPanel panel1 = new ConfigurationManagerPanel(mgr, configActions, (JComponent) getFrame().getContentPane(), ARCHIVE_FACTORIES);
                    panel1.setName(CONFIG_MANAGER_PANEL_NAME);
                    return panel1;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            return configs;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<BackupConfiguration> createConfigurations(int nbConfigurations) throws IOException {
        List<BackupConfiguration> configs = new ArrayList<>();
        Path targetDirectory = tempFolder.newFolder("archiveDirectory").toPath();
        for (int i = 0; i < nbConfigurations; i++) {
            configs.add(ConfigurationManagerTest.createConfiguration("name" + i, targetDirectory));
        }
        return configs;
    }

    private void assertFormValues(List<BackupConfiguration> configs) {
        JListFixture jl = window.list(CONFIG_MANAGER_PANEL_NAME).requireVisible().requireEnabled();

        //TODO enable sort directly in the ListModel (contents)
        Assertions.assertThat(sort(jl.contents())).as(CONFIG_MANAGER_PANEL_NAME).isEqualTo(renderedConfigs(configs));
    }

    private BackupConfiguration fillConfigurationForm() {
        return fillConfigurationForm(getRobot());
    }

    public static BackupConfiguration fillConfigurationForm(Robot robot) {
        final JFormPaneFixture configForm = new JFormPaneFixture(robot, BackupConfiguration.class);
        final BackupConfiguration expectedConfig = ConfigurationManagerTest.createConfiguration();

        //TODO support dirFilter and fileFilter
        for (BackupConfiguration.Source source : expectedConfig.getSources()) {
            source.setDirFilter(null);
            source.setFileFilter(null);
        }

        configForm.textBox("name").deleteText().enterText(expectedConfig.getName());
        final ListPanelFixture<Object, JComponent> sourceList = configForm.listPanel();
        sourceList.requireOnlyFeatures(EDITING);
        JButtonFixture addSource = sourceList.addButton();
        for (final BackupConfiguration.Source source : expectedConfig.getSources()) {
            addSource.click();
            robot.waitForIdle();

            JFormPaneFixture sourceForm = new JFormPaneFixture(robot, BackupConfiguration.Source.class);
            sourceForm.requireTitle("Source");
            final JPathFixture sourcePath = sourceForm.path();
            sourcePath.requireSelectionMode(JPath.SelectionMode.FILES_AND_DIRECTORIES);
            sourcePath.requireFileHidingEnabled(false);

            GuiActionRunner.execute(new GuiTask() {
                protected void executeInEDT() {
                    sourcePath.selectPath(Paths.get(source.getPath()));
                }
            });

            sourceForm.okButton().click();
        }

        final JPathFixture targetPath = configForm.path("targetDirectory");
        targetPath.requireSelectionMode(JPath.SelectionMode.DIRECTORIES_ONLY);
        targetPath.requireFileHidingEnabled(true);

        GuiActionRunner.execute(new GuiTask() {
            protected void executeInEDT() {
                targetPath.selectPath(Paths.get(expectedConfig.getTargetDirectory()));
            }
        });

        JComboBoxFixture combo = configForm.comboBox("archiveFactory");
        combo.selectItem("zip");
        assertThat(combo.target().getSelectedItem()).isSameAs(ZipArchiveFactory.INSTANCE);

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

    private void assertConfigurationFormValues(BackupConfiguration config) {
        assertConfigurationFormValues(config.getName(), config.getSources(), Paths.get(config.getTargetDirectory()), config.getArchiveFactory());
    }

    private void assertConfigurationFormValues(String name, List<BackupConfiguration.Source> sources, Path targetDirectory, ArchiveFactory selectedArchiveFactory) {
        JFormPaneFixture formPane = new JFormPaneFixture(getRobot(), BackupConfiguration.class);
        formPane.textBox("name").requireVisible().requireEnabled().requireEditable().requireText(name);

        ListPanelFixture<BackupConfiguration.Source, JList<BackupConfiguration.Source>> sourceList = formPane.listPanel();
        sourceList.requireOnlyFeatures(EDITING).requireVisible()/*.requireEnabled()*/;
        String[] renderedSources = new String[sources.size()];
        for (int i = 0; i < renderedSources.length; i++) {
            renderedSources[i] = sources.get(i).getPath();
        }
        Assertions.assertThat(sourceList.list().contents()).as("sources").isEqualTo(renderedSources);

        //TODO later : swap the following lines
        //formPane.path("targetDirectory").requireVisible().requireEnabled().requireEditable().requireSelectedPath(targetDirectory);
        formPane.path("targetDirectory").requireSelectedPath(targetDirectory).requireVisible().requireEnabled();

        JComboBoxFixture cb = formPane.comboBox("archiveFactory").requireVisible().requireEnabled().requireNotEditable();
        Assertions.assertThat(cb.contents()).as("archive factories").isEqualTo(expectedExtensions());

        if (selectedArchiveFactory == null) {
            cb.requireNoSelection();
        } else {
            cb.requireSelection(selectedArchiveFactory.getExtension());
        }
    }

    private String[] expectedExtensions() {
        String[] expectedExtensions = new String[ARCHIVE_FACTORIES.length + 1];
        expectedExtensions[0] = "";
        for (int i = 0; i < ARCHIVE_FACTORIES.length; i++) {
            expectedExtensions[i + 1] = ARCHIVE_FACTORIES[i].getExtension();
        }
        Arrays.sort(expectedExtensions);
        return expectedExtensions;
    }

    private String[] renderedConfigs(List<BackupConfiguration> configs) {
        String[] renderedConfigs = new String[configs.size()];
        for (int i = 0; i < renderedConfigs.length; i++) {
            renderedConfigs[i] = configs.get(i).getName();
        }
        return sort(renderedConfigs);
    }

    private static String[] sort(String[] strings) {
        Arrays.sort(strings);
        return strings;
    }
}
