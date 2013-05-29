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

import org.apache.commons.io.HexDump;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.jbackup.archive.ArchiveInputStream.Entry;
import static org.junit.Assert.*;

abstract public class ArchiveFactoryTest<T extends ArchiveFactory> {
    private static final Logger LOG = LoggerFactory.getLogger(ArchiveFactoryTest.class);
    private static final String[] RESOURCES = {"file1.txt", "file2.txt"};

    private final String archiveResource;
    private final T factory;

    protected ArchiveFactoryTest(String archiveResource, T factory) {
        this.archiveResource = archiveResource;
        this.factory = factory;
    }

    @Test
    public final void testGetExtension() {
        String extension = factory.getExtension();
        assertNotNull(extension);
        assertFalse("extension is blank", extension.trim().isEmpty());
    }

    @Test
    public final void testCreateArchiveInputStream() throws IOException {
        testCreateArchiveInputStream(getClass().getResourceAsStream(archiveResource));
    }

    @Test
    public final void testCreateArchiveOutputStream() throws Exception {
        ByteArrayOutputStream archive = new ByteArrayOutputStream(128);
        ArchiveOutputStream output = factory.create(archive);
        assertNotNull(output);

        try {
            for (String resource : RESOURCES) {
                InputStream input = ArchiveFactoryTest.class.getResourceAsStream(resource);
                assertNotNull(input);
                try {
                    output.addEntry(resource, input);
                } finally {
                    input.close();
                }
            }
        } finally {
            output.close(); // this also test the close() method
        }

        // archives can't be compared byte by byte, so let compare them by size.
//        boolean equals = contentEquals(new ByteArrayInputStream(archive.toByteArray()), archiveResource, true);
        int resourceSize = IOUtils.toByteArray(getResourceAsStream(archiveResource, true)).length;
        boolean equals = (archive.toByteArray().length == resourceSize);

        if (!equals) {
            LOG.info("Dump of generated archive :\n{}", dump(archive.toByteArray()));
            LOG.info("Dump of reference archive :\n{}", dump(getClass().getResourceAsStream(archiveResource)));
        }
        assertTrue("size are different", equals);

        // test validity of created archive
        testCreateArchiveInputStream(new ByteArrayInputStream(archive.toByteArray()));
    }

    private void testCreateArchiveInputStream(InputStream archive) throws IOException {
        ArchiveInputStream input = null;

        try {
            input = factory.create(archive);
            assertNotNull(input);

            Entry actualEntry = input.getNextEntry();
            Set<String> actualEntries = new HashSet<String>();
            Set<String> expectedEntries = new HashSet<String>(Arrays.asList(RESOURCES));
            while (actualEntry != null) {
                final String name = actualEntry.getName();
                assertNotNull("entry name is null", name);

                assertFalse("resource '" + name + "' duplicated in archive", actualEntries.contains(name));
                assertTrue("unexpected resource '" + name + "' in archive", expectedEntries.contains(name));

                actualEntries.add(name);
                expectedEntries.remove(name);

                assertTrue("wrong content for resource '" + name + "'", contentEquals(actualEntry.getInput(), name, false));

                actualEntry = input.getNextEntry();
            }
            assertTrue("files not found in archive: " + expectedEntries, expectedEntries.isEmpty());
        } finally {
            closeQuietly(archive);
            closeQuietly(input);
        }
    }

    private String dump(InputStream input) throws IOException {
        byte[] buffer = new byte[10240];
        int read = IOUtils.read(input, buffer);
        byte[] bytes = new byte[read];
        System.arraycopy(buffer, 0, bytes, 0, bytes.length);
        return dump(bytes);
    }

    private String dump(byte[] bytes) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(128);
        HexDump.dump(bytes, 0, stream, 0);
        return new String(stream.toByteArray());
    }

    private boolean contentEquals(final InputStream stream, String resource, boolean specificClass) throws IOException {
        InputStream resourceStream = null;
        try {
            resourceStream = getResourceAsStream(resource, specificClass);
            return IOUtils.contentEquals(resourceStream, stream);
        } finally {
            IOUtils.closeQuietly(resourceStream);
        }
    }

    private InputStream getResourceAsStream(String resource, boolean specificClass) {
        Class<?> clazz = specificClass ? getClass() : ArchiveFactoryTest.class;
        return clazz.getResourceAsStream(resource);
    }
}
