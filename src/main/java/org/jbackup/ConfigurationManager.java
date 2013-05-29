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
package org.jbackup;

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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Manager for JBackup's configurations.
 */
public class ConfigurationManager {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationManager.class);
    private static final String FILE_EXTENSION = "xml";
    private static final FilenameFilter XML_FILE_FILTER = FileFilterUtils.suffixFileFilter(FILE_EXTENSION);

    private final Map<String, BackupConfiguration> configurations = new HashMap<String, BackupConfiguration>();
    private final File configurationDir;

    public ConfigurationManager(File configurationDir) {
        this.configurationDir = configurationDir;
    }

    public Collection<BackupConfiguration> getBackupConfigurations() throws Exception {
        if (configurations.isEmpty()) {
            loadAllConfigurations();
        }
        return configurations.values();
    }

    void loadAllConfigurations() throws Exception {
        configurations.clear();
        for (File configFile : configurationDir.listFiles(XML_FILE_FILTER)) {
            try {
                BackupConfiguration config = loadBackupConfiguration(configFile);
                if (FilenameUtils.getBaseName(configFile.getAbsolutePath()).equals(config.getName())) {
                    configurations.put(config.getName(), config);
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
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
        if (configurations.containsKey(config.getName())) {
            throw new DuplicateNameException("There is already a configuration with name '" + config.getName() + "'");
        }

        configurations.put(config.getName(), config);

        saveBackupConfiguration(config);
    }

    public void removeBackupConfiguration(BackupConfiguration config) {
        BackupConfiguration conf = configurations.remove(config.getName());
        if (conf != null) {
            configFileFor(config).delete();
        }
    }

    File configFileFor(BackupConfiguration config) {
        return configFileFor(config.getName());
    }

    private File configFileFor(String configName) {
        return new File(configurationDir, configName + "." + FILE_EXTENSION);
    }

    public File saveRenamedBackupConfiguration(String oldName, BackupConfiguration config) throws Exception {
        configFileFor(oldName).delete();
        return saveBackupConfiguration(config);
    }
}
