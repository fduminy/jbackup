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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InvalidNameException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manager for JBackup's configurations.
 */
public class ConfigurationManager {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationManager.class);
    private static final String FILE_EXTENSION = "xml";
    private static final FilenameFilter XML_FILE_FILTER = FileFilterUtils.suffixFileFilter(FILE_EXTENSION);

    private final List<BackupConfiguration> configurations = new ArrayList<>();
    private final File configurationDir;

    public ConfigurationManager(File configurationDir) {
        this.configurationDir = configurationDir;
    }

    public List<BackupConfiguration> getBackupConfigurations() throws Exception {
        if (configurations.isEmpty()) {
            loadAllConfigurations();
        }

        return Collections.unmodifiableList(configurations);
    }

    void loadAllConfigurations() throws Exception {
        configurations.clear();
        for (File configFile : configurationDir.listFiles(XML_FILE_FILTER)) {
            try {
                BackupConfiguration config = loadBackupConfiguration(configFile);
                if (FilenameUtils.getBaseName(configFile.getAbsolutePath()).equals(config.getName())) {
                    doAddBackupConfiguration(config);
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    public static File getLatestArchive(BackupConfiguration configuration) {
        File result = null;
        for (File file : new File(configuration.getTargetDirectory()).listFiles()) {
            if (result == null) {
                result = file;
            } else if (file.lastModified() > result.lastModified()) {
                result = file;
            }
        }
        return result;
    }

    BackupConfiguration loadBackupConfiguration(File input) throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(BackupConfiguration.class);

        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        return (BackupConfiguration) jaxbUnmarshaller.unmarshal(input);
    }

    public File saveBackupConfiguration(BackupConfiguration config) throws Exception {
        File output = configFileFor(config);

        JAXBContext jaxbContext = JAXBContext.newInstance(BackupConfiguration.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        // output pretty printed
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        jaxbMarshaller.marshal(config, output);

        return output;
    }

    public void addBackupConfiguration(BackupConfiguration config) throws Exception {
        doAddBackupConfiguration(config);

        saveBackupConfiguration(config);
    }

    public void removeBackupConfiguration(BackupConfiguration config) throws IOException {
        int index = indexOf(config);
        if (index >= 0) {
            configurations.remove(index);
            Files.delete(configFileFor(config).toPath());
        }
    }

    File configFileFor(BackupConfiguration config) {
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

    private File configFileFor(String configName) {
        return new File(configurationDir, configName + "." + FILE_EXTENSION);
    }

    private void doAddBackupConfiguration(BackupConfiguration config) throws Exception {
        if (config.getName() == null) {
            throw new InvalidNameException("configuration has a null name");
        }
        if (config.getName().isEmpty()) {
            throw new InvalidNameException("configuration has an empty name");
        }
        for (int i = 0; i < config.getName().length(); i++) {
            if (!Character.isJavaIdentifierPart(config.getName().charAt(i))) {
                throw new InvalidNameException("configuration has an invalid name : '" + config.getName() + "'");
            }
        }

        if (indexOf(config) >= 0) {
            throw new DuplicateNameException("There is already a configuration with name '" + config.getName() + "'");
        }

        configurations.add(config);
    }

    public File saveRenamedBackupConfiguration(String oldName, BackupConfiguration config) throws Exception {
        Files.delete(configFileFor(oldName).toPath());
        return saveBackupConfiguration(config);
    }
}
