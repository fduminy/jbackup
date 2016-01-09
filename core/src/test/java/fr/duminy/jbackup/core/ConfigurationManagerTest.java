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
package fr.duminy.jbackup.core;

import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactory;
import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactoryTest;
import fr.duminy.jbackup.core.util.LogRule;
import org.apache.commons.beanutils.PropertyUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InvalidNameException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.*;

import static fr.duminy.jbackup.core.TestUtils.createFile;
import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for class {@link fr.duminy.jbackup.core.ConfigurationManager}.
 */
public class ConfigurationManagerTest {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationManagerTest.class);

    private static final String TARGET_DIRECTORY = "aDirectory";
    private static final String CONFIG1 = "configOne";
    private static final String CONFIG2 = "configTwo";
    private static final String CONFIG_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<backupConfiguration xmlVersion=\"1\">\n" +
            "    <archiveFactory>" + ZipArchiveFactory.class.getName() + "</archiveFactory>\n" +
            "    <name>" + CONFIG1 + "</name>\n" +
            "    <relativeEntries>true</relativeEntries>\n" +
            "    <sources>\n" +
            generateSourceXml("        ", "aDirFilter", "aFileFilter", "aSource") +
            generateSourceXml("        ", null, null, "aSource2") +
            generateSourceXml("        ", "anotherDirFilter", "anotherFileFilter", "anotherSource") +
            "    </sources>\n" +
            "    <targetDirectory>" + TARGET_DIRECTORY + "</targetDirectory>\n" +
            "    <verify>false</verify>\n" +
            "</backupConfiguration>\n";
    private static final String CONFIG_XML2 = CONFIG_XML.replace("<name>" + CONFIG1 + "</name>", "<name>" + CONFIG2 + "</name>");

    private static String generateSourceXml(String indent, String dirFilter, String fileFilter, String sourceDirectory) {
        return indent + "<source>\n" +
                ((dirFilter == null) ? "" : indent + "    <dirFilter>" + dirFilter + "</dirFilter>\n") +
                ((fileFilter == null) ? "" : indent + "    <fileFilter>" + fileFilter + "</fileFilter>\n") +
                ((sourceDirectory == null) ? "" : indent + "    <path>" + Paths.get(sourceDirectory).toAbsolutePath() + "</path>\n") +
                indent + "</source>\n";
    }

    private Path configDir;
    private ConfigurationManager manager;

    @Rule
    public final LogRule logRule = new LogRule();

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        configDir = tempFolder.newFolder().toPath();
        manager = new ConfigurationManager(configDir);
    }

    @Test(expected = NullPointerException.class)
    public void testInit_nullDirectory() throws Exception {
        new ConfigurationManager(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInit_notWritableDirectory() throws Exception {
        Path dir = tempFolder.newFolder().toPath();
        dir.toFile().setWritable(false);
        new ConfigurationManager(dir);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInit_notADirectory() throws Exception {
        Path dir = tempFolder.newFile().toPath();
        new ConfigurationManager(dir);
    }

    @Test
    public void testInit_nonExistingDirectory() throws Exception {
        Path dir = tempFolder.newFolder().toPath();
        Files.delete(dir);
        assertThat(Files.exists(dir)).as("directory exists").isFalse();

        new ConfigurationManager(dir);

        assertThat(Files.exists(dir)).as("directory exists").isTrue();
    }

    @Test
    public void testGetBackupConfigurations() throws Exception {
        writeConfigFile(true); //config1=true
        writeConfigFile(false); //config1=false => config2

        Collection<BackupConfiguration> configs = manager.getBackupConfigurations();

        assertThat(configs).extracting("name").containsOnlyOnce(CONFIG1, CONFIG2);
    }

    @Test
    public void testRemoveBackupConfiguration_sameName() throws Exception {
        testRemoveBackupConfiguration(false, false);
    }

    @Test
    public void testRemoveBackupConfiguration_sameInstance() throws Exception {
        testRemoveBackupConfiguration(true, false);
    }

    @Test
    public void testRemoveBackupConfiguration_unknownName() throws Exception {
        testRemoveBackupConfiguration(false, true);
    }

    private void testRemoveBackupConfiguration(boolean sameInstance, boolean unknownName) throws Exception {
        BackupConfiguration config = createConfiguration();
        manager.addBackupConfiguration(config);

        BackupConfiguration configToRemove = config;
        if (!sameInstance) {
            configToRemove = createConfiguration();
            if (unknownName) {
                configToRemove.setName("unknownName");
            }
        }
        manager.removeBackupConfiguration(configToRemove);

        if (unknownName) {
            ConfigurationManagerAssert.assertThat(manager).hasBackupConfigurations(config); //TODO replace by hasOnlyOnceBackupConfigurations(config);

            existsRW(configFileFor(config));
            assertThat(Files.exists(configFileFor(configToRemove))).as("configFileToRemove exists").isFalse();
        } else {
            ConfigurationManagerAssert.assertThat(manager).hasNoBackupConfigurations();

            assertThat(Files.exists(configFileFor(config))).as("configFile exists").isFalse();
            assertThat(Files.exists(configFileFor(configToRemove))).as("configFileToRemove exists").isFalse();
        }
    }

    @Test
    public void testAddBackupConfiguration() throws Exception {
        BackupConfiguration expectedConfiguration = createConfiguration();

        manager.addBackupConfiguration(expectedConfiguration);

        Collection<BackupConfiguration> configs = manager.getBackupConfigurations();
        assertThat(configs).hasSize(1);
        BackupConfiguration actualConfiguration = configs.iterator().next();
        BackupConfigurationAssert.assertThat(actualConfiguration).isEqualTo(expectedConfiguration);
        existsRW(configFileFor(expectedConfiguration));
    }

    @Test
    public void testSetBackupConfiguration() throws Exception {
        BackupConfiguration oldConfig = createConfiguration("oldName");
        BackupConfiguration newConfig = createConfiguration("newName");
        manager.addBackupConfiguration(oldConfig);

        manager.setBackupConfiguration(0, newConfig);

        Collection<BackupConfiguration> configs = manager.getBackupConfigurations();
        assertThat(configs).hasSize(1);
        BackupConfiguration actualConfiguration = configs.iterator().next();
        BackupConfigurationAssert.assertThat(actualConfiguration).isNotSameAs(oldConfig);
        BackupConfigurationAssert.assertThat(actualConfiguration).isSameAs(newConfig);
        existsRW(configFileFor(oldConfig), false);
        existsRW(configFileFor(newConfig));
    }

    private void existsRW(Path configFile) {
        existsRW(configFile, true);
    }

    private void existsRW(Path configFile, boolean existsRW) {
        assertThat(Files.exists(configFile)).as("configFile exists").isEqualTo(existsRW);
        assertThat(Files.isReadable(configFile)).as("configFile is readable").isEqualTo(existsRW);
        assertThat(Files.isWritable(configFile)).as("configFile is writable").isEqualTo(existsRW);
    }

    @Test(expected = DuplicateNameException.class)
    public void testAddBackupConfiguration_duplicateName() throws Exception {
        manager.addBackupConfiguration(createConfiguration());
        manager.addBackupConfiguration(createConfiguration());
    }

    @Test
    public void testAddBackupConfiguration_invalidNames() throws Exception {
        String[] names = {null, "", " ", "a.b", "a b"};

        Set<String> failingNames = new HashSet<>();
        for (String name : names) {
            try {
                manager.addBackupConfiguration(createConfiguration(name));
                failingNames.add(name);
            } catch (InvalidNameException ine) {
                // ok
            }
        }

        assertThat(failingNames).as("failingNames").isEmpty();
    }

    @Test
    public void testLoadAllConfigurations() throws Exception {
        // prepare mock
        Path validXmlConfigFile1 = writeConfigFile(true); //config1=true
        Path validXmlConfigFile2 = writeConfigFile(false); //config1=false => config2

        ConfigurationManager mock = spy(manager);
        doCallRealMethod().when(mock).loadAllConfigurations();

        // test
        mock.loadAllConfigurations();

        // verify
        verify(mock, times(1)).loadBackupConfiguration(eq(validXmlConfigFile1));
        verify(mock, times(1)).loadBackupConfiguration(eq(validXmlConfigFile2));
        verify(mock, times(1)).loadAllConfigurations();
        verifyNoMoreInteractions(mock);
        assertThat(mock.getBackupConfigurations()).extracting("name").as("fully valid config files").containsOnlyOnce(CONFIG1, CONFIG2);
    }

    @Test
    public void testLoadAllConfigurations_nonXmlFile() throws Exception {
        // prepare mock
        Path notAnXmlFile = configDir.resolve("notAnXmlFile");
        createFile(notAnXmlFile, "");

        ConfigurationManager mock = spy(manager);
        doCallRealMethod().when(mock).loadAllConfigurations();

        // test
        mock.loadAllConfigurations();

        // verify
        verify(mock, times(1)).loadAllConfigurations();
        verify(mock, never()).loadBackupConfiguration(eq(notAnXmlFile));
        verifyNoMoreInteractions(mock);
        assertThat(mock.getBackupConfigurations()).isEmpty();
    }

    @Test
    public void testLoadAllConfigurations_wrongXmlConfigFile() throws Exception {
        // prepare mock
        final Path wrongXmlConfigFile = configDir.resolve("wrongXmlConfigFile.xml");
        createFile(wrongXmlConfigFile, "<root></root>");

        ConfigurationManager mock = spy(manager);
        doCallRealMethod().when(mock).loadAllConfigurations();
        final List<Exception> errors = new ArrayList<>();
        doAnswer(invocation -> {
            Path file = (Path) invocation.getArguments()[0];
            if (file.equals(wrongXmlConfigFile)) {
                try {
                    return invocation.callRealMethod();
                } catch (Exception e) {
                    errors.add(e);
                    throw e;
                }
            } else {
                return invocation.callRealMethod();
            }
        }).when(mock).loadBackupConfiguration(any(Path.class));

        // test
        LOG.warn("**************************************************************");
        LOG.warn("***** The following logged ERROR is expected by the test *****");
        LOG.warn("**************************************************************");
        mock.loadAllConfigurations();

        // verify
        verify(mock, times(1)).loadBackupConfiguration(eq(wrongXmlConfigFile));
        verify(mock, times(1)).loadAllConfigurations();
        verifyNoMoreInteractions(mock);
        assertThat(errors).as("number of invalid xml files").hasSize(1);
        ConfigurationManagerAssert.assertThat(mock).hasNoBackupConfigurations();
        LOG.warn("**************************************************************");
        LOG.warn("*************** End of expected ERROR in test ***************");
        LOG.warn("**************************************************************");
    }


    @Test
    public void testLoadAllConfigurations_wrongConfigName() throws Exception {
        // prepare mock
        final Path wrongConfigName = configDir.resolve("config4.xml");
        String otherTargetDirectory = TARGET_DIRECTORY + '2';
        createFile(wrongConfigName, CONFIG_XML2.replace(TARGET_DIRECTORY, otherTargetDirectory));

        ConfigurationManager mock = spy(manager);
        doCallRealMethod().when(mock).loadAllConfigurations();

        // test
        mock.loadAllConfigurations();

        // verify
        verify(mock, times(1)).loadBackupConfiguration(eq(wrongConfigName));
        verify(mock, times(1)).loadAllConfigurations();
        verifyNoMoreInteractions(mock);
        ConfigurationManagerAssert.assertThat(mock).hasNoBackupConfigurations();
    }

    @Test
    public void testLoadBackupConfiguration() throws Exception {
        BackupConfiguration expectedConfiguration = createConfiguration();

        Path input = tempFolder.newFile().toPath();
        createFile(input, CONFIG_XML);
        BackupConfiguration actualConfiguration = manager.loadBackupConfiguration(input);

        assertAreEquals(expectedConfiguration, actualConfiguration);
    }

    @Test
    public void testSaveRenamedBackupConfiguration() throws Exception {
        BackupConfiguration config = createConfiguration();

        String oldName = config.getName();
        Path output1 = manager.saveBackupConfiguration(config);
        assertThat(Files.exists(output1)).as("output1 exists").isTrue();

        config.setName("NewName");

        Path output2 = manager.saveRenamedBackupConfiguration(oldName, config);
        assertThat(Files.exists(output1)).as("output1 exists").isFalse();
        assertThat(Files.exists(output2)).as("output2 exists").isTrue();
    }

    @Test
    public void testSaveBackupConfiguration_newConfig() throws Exception {
        testSaveBackupConfiguration(createConfiguration());
    }

    @Test
    public void testSaveBackupConfiguration_updateConfig() throws Exception {
        BackupConfiguration config = createConfiguration();

        Path output = testSaveBackupConfiguration(config);
        FileTime t0 = Files.getLastModifiedTime(output);

        sleep(1000);

        output = testSaveBackupConfiguration(config);
        FileTime t1 = Files.getLastModifiedTime(output);

        assertThat(t1.compareTo(t0) >= 0).as("t1 > t0").isTrue();
    }

    private Path testSaveBackupConfiguration(BackupConfiguration config) throws Exception {
        Path output = manager.saveBackupConfiguration(config);

        assertThat(output.getParent()).isEqualTo(configDir);
        Assertions.assertThat(output.toFile()).hasContent(CONFIG_XML);

        return output;
    }

    @Test
    public void testGetLatestArchive_noArchive() throws IOException {
        BackupConfiguration config = createConfiguration("config", tempFolder.newFolder().toPath());

        Path configFile = ConfigurationManager.getLatestArchive(config);

        assertThat(configFile).isNull();
    }

    @Test
    public void testGetLatestArchive_oneArchive() throws Exception {
        initAndGetLatestArchive(1);
    }

    @Test
    public void testGetLatestArchive_twoArchives() throws Exception {
        initAndGetLatestArchive(2);
    }

    private void initAndGetLatestArchive(int nbConfigurations) throws Exception {
        BackupConfiguration config = createConfiguration("config", tempFolder.newFolder().toPath());
        Path[] files = new Path[nbConfigurations];
        for (int i = 0; i < nbConfigurations; i++) {
            Path file = Paths.get(config.getTargetDirectory()).resolve("file" + i);
            Files.copy(ZipArchiveFactoryTest.getArchive(), file);
            Thread.sleep(1000);
            files[i] = file;
        }

        Path configFile = ConfigurationManager.getLatestArchive(config);

        assertThat(configFile).isEqualTo(files[files.length - 1]);
    }

    public static BackupConfiguration createConfiguration() {
        return createConfiguration(CONFIG1);
    }

    public static BackupConfiguration createConfiguration(String configName) {
        return createConfiguration(configName, null);
    }

    public static BackupConfiguration createConfiguration(String configName, Path targetDirectory) {
        final BackupConfiguration config = new BackupConfiguration();

        config.setName(configName);
        config.setArchiveFactory(ZipArchiveFactory.INSTANCE);
        Path targetPath = (targetDirectory == null) ? Paths.get(TARGET_DIRECTORY) : targetDirectory.toAbsolutePath();
        config.setTargetDirectory(targetPath.toString());
        config.addSource(Paths.get("aSource").toAbsolutePath(), "aDirFilter", "aFileFilter");
        config.addSource(Paths.get("aSource2").toAbsolutePath());
        config.addSource(Paths.get("anotherSource").toAbsolutePath(), "anotherDirFilter", "anotherFileFilter");

        return config;
    }

    private Path configFileFor(BackupConfiguration config) {
        return manager.configFileFor(config);
    }

    private Path writeConfigFile(boolean config1) throws IOException {
        Path configFile = configDir.resolve((config1 ? CONFIG1 : CONFIG2) + ".xml");
        String configXML = config1 ? CONFIG_XML : CONFIG_XML2;
        createFile(configFile, configXML);
        assertThat(Files.size(configFile)).as("config file size").isEqualTo(configXML.getBytes().length);
        return configFile;
    }

    public static void assertAreEquals(BackupConfiguration expected, BackupConfiguration actual) {
        Assertions.assertThat(describe(actual)).isEqualTo(describe(expected));
    }

    private static Map describe(BackupConfiguration config) {
        Map properties;
        try {
            properties = PropertyUtils.describe(config);
        } catch (Exception e) {
            LOG.error("unable to extract properties from configuration", e);
            properties = Collections.EMPTY_MAP;
        }

        properties.remove("class");
        properties.put("archiveFactory", config.getArchiveFactory().getClass().getName());
        List<BackupConfiguration.Source> sources = (List<BackupConfiguration.Source>) properties.remove("sources");
        properties.put("sources.size", sources.size());
        for (int i = 0; i < sources.size(); i++) {
            Map sourceProperties = null;
            try {
                sourceProperties = PropertyUtils.describe(sources.get(i));
            } catch (Exception e) {
                LOG.error("unable to extract source #" + i, e);
            }
            sourceProperties.remove("class");
            properties.put("sources[" + i + "]", sourceProperties);
        }
        return properties;
    }
}
