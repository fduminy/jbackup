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
package fr.duminy.jbackup.core.filter;

import fr.duminy.jbackup.core.util.LogRule;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.testtools.FileBasedTestCase;
import org.junit.Rule;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * .
 */
public class JexlFileFilterTest extends FileBasedTestCase {

    @Rule
    public final LogRule logRule = new LogRule();

    public JexlFileFilterTest(String name) {
        super(name);
    }

    /**
     * Method copied from {@link org.apache.commons.io.filefilter.RegexFileFilterTestCase#setUp()}.
     * TODO check potential license issue.
     */
    @Override
    public void setUp() {
        getTestDirectory().mkdirs();
    }

    /**
     * Method copied from {@link org.apache.commons.io.filefilter.RegexFileFilterTestCase#tearDown()}.
     * TODO check potential license issue.
     *
     * @throws Exception
     */
    @Override
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(getTestDirectory());
    }

    /**
     * Method copied from {@link org.apache.commons.io.filefilter.RegexFileFilterTestCase#assertFiltering(org.apache.commons.io.filefilter.IOFileFilter, java.io.File, boolean)}.
     * TODO check potential license issue.
     *
     * @param filter
     * @param file
     * @param expected
     * @throws Exception
     */
    public void assertFiltering(final IOFileFilter filter, final File file, final boolean expected) throws Exception {
        // Note. This only tests the (File, String) version if the parent of
        //       the File passed in is not null
        assertThat(filter.accept(file)).
                as("Source(File) " + filter.getClass().getName() + " not " + expected + " for " + file).isEqualTo(expected);

        if (file != null && file.getParentFile() != null) {
            assertThat(filter.accept(file.getParentFile(), file.getName())).
                    as("Source(File, String) " + filter.getClass().getName() + " not " + expected + " for " + file).isEqualTo(expected);
        }
    }

    public void testNamePrefix() throws Exception {
        IOFileFilter filter = new JexlFileFilter("template", "namePrefix('.')");
        assertFiltering(filter, new File("."), true);
        assertFiltering(filter, new File(".m2"), true);
        assertFiltering(filter, new File(".a.b"), true);
        assertFiltering(filter, new File("readme.txt"), false);
        assertFiltering(filter, new File("file."), false);
    }

    public void testEqualOperator() throws Exception {
        IOFileFilter filter = new JexlFileFilter("template", "file.name=='Abc'");
        assertFiltering(filter, new File("z"), false);
        assertFiltering(filter, new File("abc"), false);
        assertFiltering(filter, new File("Abc"), true);
        assertFiltering(filter, new File("bc"), false);
    }
}
