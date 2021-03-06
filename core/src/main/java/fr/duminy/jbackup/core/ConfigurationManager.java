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
package fr.duminy.jbackup.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manager for JBackup's configurations.
 */
public class ConfigurationManager {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationManager.class);
    private static final String FILE_EXTENSION = ".xml";
    private static final String XML_FILE_FILTER = '*' + FILE_EXTENSION;

    private final List<BackupConfiguration> configurations = new ArrayList<>();
    private final Path configurationDir;

    public ConfigurationManager(Path configurationDir) {
        if (configurationDir == null) {
            throw new NullPointerException("configurationDir is null");
        }
        if (!Files.isDirectory(configurationDir)) {
            if (Files.exists(configurationDir)) {
                throw new IllegalArgumentException("'" + configurationDir.toAbsolutePath() + "' is not a directory");
            } else {
                try {
                    Files.createDirectories(configurationDir);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Can't create directory '" + configurationDir.toAbsolutePath() + "'", e);
                }
            }
        }
        if (!Files.isWritable(configurationDir)) {
            throw new IllegalArgumentException("can't write into directory '" + configurationDir.toAbsolutePath() + "'");
        }
        this.configurationDir = configurationDir;
    }

    public List<BackupConfiguration> getBackupConfigurations() throws ConfigurationException {
        if (configurations.isEmpty()) {
            loadAllConfigurations();
        }

        return Collections.unmodifiableList(configurations);
    }

    void loadAllConfigurations() throws ConfigurationException {
        configurations.clear();
        try (DirectoryStream<Path> configFiles = Files.newDirectoryStream(configurationDir, XML_FILE_FILTER)) {
            for (Path configFile : configFiles) {
                loadConfiguration(configFile);
            }
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }

    private void loadConfiguration(Path configFile) {
        try {
            BackupConfiguration config = loadBackupConfiguration(configFile);
            doAddBackupConfiguration(configFile, config);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void doAddBackupConfiguration(Path configFile, BackupConfiguration config) throws ConfigurationException {
        String fileName = String.valueOf(configFile.getFileName());
        fileName = fileName.substring(0, fileName.length() - FILE_EXTENSION.length());
        if (fileName.equals(config.getName())) {
            doAddBackupConfiguration(config);
        }
    }

    public static Path getLatestArchive(BackupConfiguration configuration) throws IOException {
        Path result = null;
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get(configuration.getTargetDirectory()))) {
            for (Path path : paths) {
                if ((result == null) || (path.toFile().lastModified() > result.toFile().lastModified())) {
                    result = path;
                }
            }
        }
        return result;
    }

    BackupConfiguration loadBackupConfiguration(Path input) throws ConfigurationException {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(BackupConfiguration.class);

            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            return (BackupConfiguration) jaxbUnmarshaller.unmarshal(Files.newInputStream(input));
        } catch (IOException | JAXBException e) {
            throw new ConfigurationException(e);
        }
    }

    public Path saveBackupConfiguration(BackupConfiguration config) throws ConfigurationException {
        Path output = configFileFor(config);

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(BackupConfiguration.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            // output pretty printed
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            jaxbMarshaller.marshal(config, Files.newOutputStream(output));
        } catch (IOException | JAXBException e) {
            throw new ConfigurationException(e);
        }

        return output;
    }

    public void addBackupConfiguration(BackupConfiguration config) throws ConfigurationException {
        doAddBackupConfiguration(config);

        saveBackupConfiguration(config);
    }

    public BackupConfiguration setBackupConfiguration(int index, BackupConfiguration backupConfiguration)
        throws ConfigurationException {
        final BackupConfiguration oldConfig = configurations.set(index, backupConfiguration);
        deleteConfigFileFor(oldConfig);
        saveBackupConfiguration(backupConfiguration);
        return oldConfig;
    }

    public void removeBackupConfiguration(BackupConfiguration config) throws ConfigurationException {
        int index = indexOf(config);
        if (index >= 0) {
            configurations.remove(index);
            deleteConfigFileFor(config);
        }
    }

    private void deleteConfigFileFor(BackupConfiguration config) throws ConfigurationException {
        try {
            Files.delete(configFileFor(config));
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }

    Path configFileFor(BackupConfiguration config) {
        return configFileFor(config.getName());
    }

    private int indexOf(BackupConfiguration config) {
        int index = -1;
        for (int i = 0, configurationsSize = configurations.size(); i < configurationsSize; i++) {
            BackupConfiguration c = configurations.get(i);
            if (c.getName().equals(config.getName())) {
                index = i;
                break;
            }
        }
        return index;
    }

    private Path configFileFor(String configName) {
        return configurationDir.resolve(configName + FILE_EXTENSION);
    }

    private void doAddBackupConfiguration(BackupConfiguration config) throws ConfigurationException {
        if (config.getName() == null) {
            throw new ConfigurationException("configuration has a null name");
        }
        if (config.getName().isEmpty()) {
            throw new ConfigurationException("configuration has an empty name");
        }
        for (int i = 0; i < config.getName().length(); i++) {
            if (!Character.isJavaIdentifierPart(config.getName().charAt(i))) {
                throw new ConfigurationException("configuration has an invalid name : '" + config.getName() + "'");
            }
        }

        if (indexOf(config) >= 0) {
            throw new DuplicateNameException("There is already a configuration with name '" + config.getName() + "'");
        }

        configurations.add(config);
    }

    public Path saveRenamedBackupConfiguration(String oldName, BackupConfiguration config)
        throws ConfigurationException {
        try {
            Files.delete(configFileFor(oldName));
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
        return saveBackupConfiguration(config);
    }
}
