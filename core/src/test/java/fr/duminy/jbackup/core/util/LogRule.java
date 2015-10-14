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
package fr.duminy.jbackup.core.util;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.MDC;

/**
 * A rule that allows to write logs in a dedicated file for each test.
 * TODO : move this class to a library of utilities or to junit (or one of its extensions).
 */
public class LogRule extends TestWatcher {
    private static final String TEST_NAME = "TestName";

    @Override
    protected void starting(Description description) {
        MDC.put(TEST_NAME, description.getMethodName());
    }

    @Override
    protected void finished(Description description) {
        MDC.remove(TEST_NAME);
    }
}
