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
package fr.duminy.jbackup.core;

import fr.duminy.jbackup.core.archive.ArchiveFactory;
import fr.duminy.jbackup.core.filter.JexlFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.StringUtils;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;

/**
 * Configuration of a backup.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class BackupConfiguration {
    private String name;

    private List<Source> sources = new ArrayList<>();

    private String targetDirectory;

    private ArchiveFactory archiveFactory;

    public void addSource(String sourceDirectory) {
        addSource(sourceDirectory, null, null);
    }

    public void addSource(String sourceDirectory, String dirFilter, String fileFilter) {
        Source source = new Source();
        source.setSourceDirectory(new File(sourceDirectory));
        source.setDirFilter(dirFilter);
        source.setFileFilter(fileFilter);
        sources.add(source);
    }

    public IOFileFilter createIOFileFilter(String nameSuffix, String filter) {
        return StringUtils.isBlank(filter) ? trueFileFilter() : new JexlFileFilter(name + nameSuffix, filter);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getTargetDirectory() {
        return targetDirectory;
    }

    public void setTargetDirectory(String targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    @XmlElementWrapper(name = "sources")
    @XmlElement(name = "source")
    public List<Source> getSources() {
        return sources;
    }

    public void setSources(List<Source> sources) {
        this.sources = sources;
    }

    @XmlJavaTypeAdapter(value = ArchiveFactoryXmlAdapter.class)
    public ArchiveFactory getArchiveFactory() {
        return archiveFactory;
    }

    public static class ArchiveFactoryXmlAdapter extends XmlAdapter<Class<? extends ArchiveFactory>, ArchiveFactory> {
        @Override
        public ArchiveFactory unmarshal(Class<? extends ArchiveFactory> v) throws Exception {
            return v.newInstance();
        }

        @Override
        public Class<? extends ArchiveFactory> marshal(ArchiveFactory v) throws Exception {
            return (v == null) ? null : v.getClass();
        }
    }

    public void setArchiveFactory(ArchiveFactory archiveFactory) {
        this.archiveFactory = archiveFactory;
    }

    @Override
    public String toString() {
        return "BackupConfiguration{" +
                "name='" + name + '\'' +
                ", sources=" + sources +
                ", targetDirectory='" + targetDirectory + '\'' +
                ", archiveFactory=" + archiveFactory +
                '}';
    }

    public static class Source {
        private File sourceDirectory;
        private String dirFilter;
        private String fileFilter;

        public void setSourceDirectory(File sourceDirectory) {
            this.sourceDirectory = sourceDirectory;
        }

        public void setDirFilter(String dirFilter) {
            this.dirFilter = dirFilter;
        }

        public void setFileFilter(String fileFilter) {
            this.fileFilter = fileFilter;
        }

        public File getSourceDirectory() {
            return sourceDirectory;
        }

        public String getDirFilter() {
            return dirFilter;
        }

        public String getFileFilter() {
            return fileFilter;
        }

        @Override
        public String toString() {
            return "Source{" +
                    "sourceDirectory=" + sourceDirectory +
                    ", dirFilter='" + dirFilter + '\'' +
                    ", fileFilter='" + fileFilter + '\'' +
                    '}';
        }
    }
}
