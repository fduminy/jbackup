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

import fr.duminy.components.swing.form.StringPathTypeMapper;
import fr.duminy.jbackup.core.archive.ArchiveFactory;
import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactory;
import fr.duminy.jbackup.core.filter.JexlFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.StringUtils;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.nio.file.Path;
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

    private boolean relativeEntries = true;

    public void addSource(Path sourceDirectory) {
        addSource(sourceDirectory, null, null);
    }

    public void addSource(Path sourceDirectory, String dirFilter, String fileFilter) {
        Source source = new Source();
        source.setPath(StringPathTypeMapper.toString(sourceDirectory));
        source.setDirFilter(dirFilter);
        source.setFileFilter(fileFilter);
        sources.add(source);
    }

    public IOFileFilter createIOFileFilter(String nameSuffix, String filter) {
        return StringUtils.isBlank(filter) ? trueFileFilter() : new JexlFileFilter(name + nameSuffix, filter);
    }

    public boolean isRelativeEntries() {
        return relativeEntries;
    }

    public void setRelativeEntries(boolean relativeEntries) {
        this.relativeEntries = relativeEntries;
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
            return ZipArchiveFactory.class.equals(v) ? ZipArchiveFactory.INSTANCE : v.newInstance();
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
        private String path;
        private String dirFilter;
        private String fileFilter;

        public void setPath(String path) {
            this.path = path;
        }

        public void setDirFilter(String dirFilter) {
            this.dirFilter = dirFilter;
        }

        public void setFileFilter(String fileFilter) {
            this.fileFilter = fileFilter;
        }

        public String getPath() {
            return path;
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
                    "path=" + path +
                    ", dirFilter='" + dirFilter + '\'' +
                    ", fileFilter='" + fileFilter + '\'' +
                    '}';
        }
    }
}
