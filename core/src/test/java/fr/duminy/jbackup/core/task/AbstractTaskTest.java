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
package fr.duminy.jbackup.core.task;

import fr.duminy.jbackup.core.archive.ProgressListener;
import fr.duminy.jbackup.core.util.LogRule;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.mock;

@RunWith(Theories.class)
abstract public class AbstractTaskTest {
    @DataPoint
    public static final ProgressListener WITH_LISTENER = mock(ProgressListener.class);

    @DataPoint
    public static final ProgressListener WITHOUT_LISTENER = null;

    @Rule
    public final LogRule logRule = new LogRule();

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public final ExpectedException thrown = ExpectedException.none();
}