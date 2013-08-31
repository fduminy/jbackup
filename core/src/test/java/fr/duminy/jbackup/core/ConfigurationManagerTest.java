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
package fr.duminy.jbackup.core;

import fr.duminy.jbackup.core.archive.ArchiveFactory;
import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactory;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InvalidNameException;
import java.io.File;
import java.io.IOException;
import java.util.*;

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
            "<backupConfiguration>\n" +
            "    <archiveFactory>" + ZipArchiveFactory.class.getName() + "</archiveFactory>\n" +
            "    <name>" + CONFIG1 + "</name>\n" +
            "    <sources>\n" +
            generateSourceXml("        ", "aDirFilter", "aFileFilter", "aSource") +
            generateSourceXml("        ", null, null, "aSource2") +
            generateSourceXml("        ", "anotherDirFilter", "anotherFileFilter", "anotherSource") +
            "    </sources>\n" +
            "    <targetDirectory>" + TARGET_DIRECTORY + "</targetDirectory>\n" +
            "</backupConfiguration>\n";
    private static final String CONFIG_XML2 = CONFIG_XML.replace("<name>" + CONFIG1 + "</name>", "<name>" + CONFIG2 + "</name>");

    private static String generateSourceXml(String indent, String dirFilter, String fileFilter, String sourceDirectory) {
        return indent + "<source>\n" +
                ((dirFilter == null) ? "" : indent + "    <dirFilter>" + dirFilter + "</dirFilter>\n") +
                ((fileFilter == null) ? "" : indent + "    <fileFilter>" + fileFilter + "</fileFilter>\n") +
                ((sourceDirectory == null) ? "" : indent + "    <sourceDirectory>" + toAbsolutePath(sourceDirectory) + "</sourceDirectory>\n") +
                indent + "</source>\n";
    }

    public static final ArchiveFactory ZIP_ARCHIVE_FACTORY = new ZipArchiveFactory();

    private File configDir;
    private ConfigurationManager manager;

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        configDir = tempFolder.newFolder();
        manager = new ConfigurationManager(configDir);
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

            assertThat(configFileFor(config)).exists().canRead().canWrite();
            assertThat(configFileFor(configToRemove)).doesNotExist();
        } else {
            ConfigurationManagerAssert.assertThat(manager).hasNoBackupConfigurations();

            assertThat(configFileFor(config)).doesNotExist();
            assertThat(configFileFor(configToRemove)).doesNotExist();
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
        assertThat(configFileFor(expectedConfiguration)).exists().canRead().canWrite();
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
        File validXmlConfigFile1 = writeConfigFile(true); //config1=true
        File validXmlConfigFile2 = writeConfigFile(false); //config1=false => config2

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
        File notAnXmlFile = new File(configDir, "notAnXmlFile");
        FileUtils.write(notAnXmlFile, "");

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
        final File wrongXmlConfigFile = new File(configDir, "wrongXmlConfigFile.xml");
        FileUtils.write(wrongXmlConfigFile, "<root></root>");

        ConfigurationManager mock = spy(manager);
        doCallRealMethod().when(mock).loadAllConfigurations();
        final List<Exception> errors = new ArrayList<>();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                File file = (File) invocation.getArguments()[0];
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
            }
        }).when(mock).loadBackupConfiguration(any(File.class));

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
        final File wrongConfigName = new File(configDir, "config4.xml");
        String otherTargetDirectory = TARGET_DIRECTORY + '2';
        FileUtils.write(wrongConfigName, CONFIG_XML2.replace(TARGET_DIRECTORY, otherTargetDirectory));

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

        File input = tempFolder.newFile();
        FileUtils.write(input, CONFIG_XML);
        BackupConfiguration actualConfiguration = manager.loadBackupConfiguration(input);

        assertAreEquals(expectedConfiguration, actualConfiguration);
    }

    @Test
    public void testSaveRenamedBackupConfiguration() throws Exception {
        BackupConfiguration config = createConfiguration();

        String oldName = config.getName();
        File output1 = manager.saveBackupConfiguration(config);
        assertThat(output1).exists();

        config.setName("NewName");

        File output2 = manager.saveRenamedBackupConfiguration(oldName, config);
        assertThat(output1).doesNotExist();
        assertThat(output2).exists();
    }

    @Test
    public void testSaveBackupConfiguration_newConfig() throws Exception {
        testSaveBackupConfiguration(createConfiguration());
    }

    @Test
    public void testSaveBackupConfiguration_updateConfig() throws Exception {
        BackupConfiguration config = createConfiguration();

        File output = testSaveBackupConfiguration(config);
        long t0 = output.lastModified();

        sleep(1000);

        output = testSaveBackupConfiguration(config);
        long t1 = output.lastModified();

        assertThat(t1).isGreaterThan(t0);
    }

    private File testSaveBackupConfiguration(BackupConfiguration config) throws Exception {
        File output = manager.saveBackupConfiguration(config);

        assertThat(output.getParentFile()).isEqualTo(configDir);
        Assertions.assertThat(output).hasContent(CONFIG_XML);

        return output;
    }

    public static BackupConfiguration createConfiguration() {
        return createConfiguration(CONFIG1);
    }

    public static BackupConfiguration createConfiguration(String configName) {
        final BackupConfiguration config = new BackupConfiguration();

        config.setName(configName);
        config.setArchiveFactory(ZIP_ARCHIVE_FACTORY);
        config.setTargetDirectory(TARGET_DIRECTORY);
        config.addSource(toAbsolutePath("aSource"), "aDirFilter", "aFileFilter");
        config.addSource(toAbsolutePath("aSource2"));
        config.addSource(toAbsolutePath("anotherSource"), "anotherDirFilter", "anotherFileFilter");

        return config;
    }

    public static String toAbsolutePath(String file) {
        return new File(file).getAbsolutePath();
    }

    private File configFileFor(BackupConfiguration config) {
        return manager.configFileFor(config);
    }

    private File writeConfigFile(boolean config1) throws IOException {
        File configFile = new File(configDir, (config1 ? CONFIG1 : CONFIG2) + ".xml");
        FileUtils.write(configFile, (config1 ? CONFIG_XML : CONFIG_XML2));
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
