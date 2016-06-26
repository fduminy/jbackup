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
package fr.duminy.jbackup.core.archive;

import fr.duminy.jbackup.core.util.LogRule;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static fr.duminy.jbackup.core.archive.ArchiveInputStream.Entry;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

abstract public class ArchiveFactoryTest<T extends ArchiveFactory> {
    private static final Logger LOG = LoggerFactory.getLogger(ArchiveFactoryTest.class);
    private static final String[] RESOURCES = {"file1.txt", "file2.txt"};

    @Rule
    public final LogRule logRule = new LogRule();

    private final String archiveResource;
    private final T factory;

    protected ArchiveFactoryTest(String archiveResource, T factory) {
        this.archiveResource = archiveResource;
        this.factory = factory;
    }

    @Test
    public final void testGetExtension() {
        String extension = factory.getExtension();
        assertThat(extension).isNotNull();
        assertThat(extension.trim()).isNotEmpty(); //TODO add assertions for blank CharSequence
    }

    @Test
    public final void testCreateArchiveInputStream() throws Exception {
        testCreateArchiveInputStream(getArchiveResource(getClass(), archiveResource));
    }

    protected static InputStream getArchiveResource(Class<? extends ArchiveFactoryTest> clazz, String archiveResource) {
        return clazz.getResourceAsStream(archiveResource);
    }

    @Test
    public final void testCreateArchiveOutputStream() throws Exception {
        ByteArrayOutputStream archive = new ByteArrayOutputStream(128);

        try (ArchiveOutputStream output = factory.create(archive)) {
            assertThat(output).isNotNull();

            for (String resource : RESOURCES) {
                try (InputStream input = getTestFile(resource)) {
                    assertThat(input).isNotNull();
                    output.addEntry(resource, input);
                }
            }
        }

        // archives can't be compared byte by byte, so let compare them by size.
//        assertThat(archive.toByteArray()).isEqualTo(IOUtils.toByteArray(getResourceAsStream(archiveResource, true)));
        int resourceSize = IOUtils.toByteArray(getResourceAsStream(archiveResource, true)).length;
        assertThat(archive.toByteArray().length).as("archive size").isEqualTo(resourceSize);

        // test validity of created archive
        testCreateArchiveInputStream(new ByteArrayInputStream(archive.toByteArray()));
    }

    private InputStream getTestFile(String resource) {
        return ArchiveFactoryTest.class.getResourceAsStream(resource);
    }

    private void testCreateArchiveInputStream(InputStream archive) throws Exception {
        ArchiveInputStream input = null;

        try {
            input = factory.create(archive);
            assertThat(input).isNotNull();

            Map<String, byte[]> actualContent = asMap(input);

            Set<String> expectedEntries = new HashSet<>(Arrays.asList(RESOURCES));
            assertThat(actualContent.keySet()).as("entry names").isEqualTo(expectedEntries);

            for (String name : actualContent.keySet()) {
                byte[] expectedContent = IOUtils.toByteArray(getTestFile(name));
                assertThat(actualContent.get(name)).as("content for entry " + name).isEqualTo(expectedContent);
            }
        } finally {
            closeQuietly(archive);
            closeQuietly(input);
        }
    }

    private static Map<String, byte[]> asMap(ArchiveInputStream input) throws IOException {
        Map<String, byte[]> actualEntries = new HashMap<>();

        Entry actualEntry = input.getNextEntry();
        while (actualEntry != null) {
            final String name = actualEntry.getName();
            assertNotNull("entry name is null", name);
            assertThat(actualEntries.keySet()).as("resource '" + name + "' duplicated in archive").doesNotContain(name);

            actualEntries.put(name, IOUtils.toByteArray(actualEntry.getInput()));

            actualEntry = input.getNextEntry();
        }

        return actualEntries;
    }

    private InputStream getResourceAsStream(String resource, boolean specificClass) {
        Class<?> clazz = specificClass ? getClass() : ArchiveFactoryTest.class;
        return clazz.getResourceAsStream(resource);
    }
}
