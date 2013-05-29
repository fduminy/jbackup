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
package org.jbackup.archive;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.jbackup.archive.zip.ZipArchiveFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.io.FileUtils.openInputStream;
import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * A high level class that can (de)compress files in a format managed by the provided {@link ArchiveFactory}.
 * The {@link #main(String[])} method provides a very basic command line tool for the zip format {(thanks to {@link ZipArchiveFactory}.
 */
public class Archiver {
    private static final Logger LOG = LoggerFactory.getLogger(Archiver.class);

    private final ArchiveFactory factory;

    public static void main(String[] args) throws IOException {
        final String operation = args[0];
        final File archive = new File(args[1]);
        final ZipArchiveFactory factory = new ZipArchiveFactory();

        if ("-c".equals(operation)) {
            new Archiver(factory).compress(toFiles(args, 2), archive);
        } else if ("-d".equals(operation)) {
            File directory = null;

            if (args.length > 2) {
                directory = new File(args[2]);
            }

            new Archiver(factory).decompress(archive, directory);
        }
    }

    private static File[] toFiles(String[] files, int fromIndex) {
        File[] result = new File[files.length - (fromIndex + 1)];
        for (int i = fromIndex; i < files.length; i++) {
            result[i - fromIndex] = new File(files[i]);
        }
        return result;
    }

    private static class RelativeFile extends File {
        private final String relativePath;

        public RelativeFile(File baseDirectory, String relativePath) {
            super(baseDirectory, relativePath);

            if (relativePath.startsWith(File.separator)) {
                relativePath = relativePath.substring(File.separator.length());
            }
            this.relativePath = relativePath;
        }
    }

    public Archiver(ArchiveFactory factory) {
        this.factory = factory;
    }

    public void compress(File[] files, final File archive) throws IOException {
        files = filterFiles(files);

        final String name = archive.getAbsolutePath();
        FileOutputStream fos = FileUtils.openOutputStream(archive);
        ArchiveOutputStream output = factory.create(fos);
        final long[] processedSize = {0L};
        try {
            LOG.info("Backup '{}': creating archive {}", name, archive);
            for (File file : files) {
                if (!file.isDirectory()) {
                    LOG.info("Backup '{}': compressing file {}", name, file.getAbsolutePath());
                    FileInputStream fis = openInputStream(file);
                    CountingInputStream cis = new CountingInputStream(fis) {
                        @Override
                        protected synchronized void afterRead(int n) {
                            super.afterRead(n);
                            processedSize[0] += getByteCount();
                            LOG.info("Backup '{}': processed {} bytes", new Object[]{name, processedSize[0]});
                        }
                    };
                    try {
                        String path = (file instanceof RelativeFile) ? ((RelativeFile) file).relativePath : file.getAbsolutePath();
                        LOG.info("Backup '{}': adding entry {}", new Object[]{name, path});
                        output.addEntry(path, cis);
                    } finally {
                        IOUtils.closeQuietly(cis);
                        IOUtils.closeQuietly(fis);
                    }
                }
            }
            LOG.info("Backup '{}': archive {} created ({})", new Object[]{name, archive, FileUtils.byteCountToDisplaySize(archive.length())});
        } finally {
            IOUtils.closeQuietly(output);
            IOUtils.closeQuietly(fos);
        }
    }

    public void decompress(File archive, File directory) throws IOException {
        directory = (directory == null) ? new File(".") : directory;

        InputStream archiveStream = new FileInputStream(archive);
        ArchiveInputStream input = null;

        try {
            input = factory.create(archiveStream);

            ArchiveInputStream.Entry entry = input.getNextEntry();
            while (entry != null) {
                InputStream entryStream = entry.getInput();
                try {
                    decompress(entry.getName(), entryStream, directory);
                } finally {
                    entry.close();
                }
                entry = input.getNextEntry();
            }
        } finally {
            closeQuietly(archiveStream);
            closeQuietly(input);
        }
    }

    private void decompress(String name, InputStream stream, File outputDirectory) throws IOException {
        FileOutputStream output = new FileOutputStream(new File(outputDirectory, name));
        try {
            IOUtils.copy(stream, output);
        } finally {
            IOUtils.closeQuietly(output);
        }
    }

    private File[] filterFiles(File[] files) throws IOException {
        List<File> onlyFiles = new ArrayList<File>();
        FileCollector collector = new FileCollector();
        for (File file : files) {
            if (file.isDirectory()) {
                List<File> fileList = new ArrayList<File>();
                collector.collect(fileList, file);
                for (int j = 0; j < fileList.size(); j++) {
                    File f = fileList.get(j);
                    String relativePath = f.getAbsolutePath().substring(file.getAbsolutePath().length());
                    fileList.set(j, new RelativeFile(file, relativePath));
                }
                onlyFiles.addAll(fileList);
            } else {
                onlyFiles.add(file);
            }
        }
        return onlyFiles.toArray(new File[onlyFiles.size()]);
    }
}
