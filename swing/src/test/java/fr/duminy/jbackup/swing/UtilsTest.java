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
package fr.duminy.jbackup.swing;

import fr.duminy.jbackup.core.util.LogRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Utils}.
 */
public class UtilsTest {
    @Rule
    public final LogRule logRule = new LogRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testMaxInteger() {
        assertThat(Utils.MAX_INTEGER.intValue()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void testToInteger_negativeMaxValue() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("maxValue < 0");

        Utils.toInteger(0, -1);
    }

    @Test
    public void testToInteger_negativeValue() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("value < 0");

        Utils.toInteger(-1, 100);
    }

    @Test
    public void testToInteger_valueGreaterThanMaxValue() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("value > maxValue");

        Utils.toInteger(200, 100);
    }

    @Test
    public void testToInteger_intValues() throws Exception {
        int result = Utils.toInteger(100, 200);

        assertThat(result).isEqualTo(100);
    }

    @Test
    public void testToInteger_intValue_longMaxValue() throws Exception {
        int result = Utils.toInteger(100, Integer.MAX_VALUE * 100L);

        assertThat(result).isEqualTo(1);
    }

    @Test
    public void testToInteger_longValue_longMaxValue() throws Exception {
        int result = Utils.toInteger(Integer.MAX_VALUE + 1L, 1000L * (Integer.MAX_VALUE + 1L));

        assertThat(result).isEqualTo(Integer.MAX_VALUE / 1000);
    }

    @Test
    public void testPercent_decimals() throws Exception {
        double result = Utils.percent(1L, 200L);

        assertThat(result).isEqualTo(0.5d);
    }

    @Test
    public void testPercent_noDecimals() throws Exception {
        double result = Utils.percent(10L, 1000L);

        assertThat(result).isEqualTo(1d);
    }
}
