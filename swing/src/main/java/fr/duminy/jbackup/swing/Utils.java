/**
 * JBackup is a software managing backups.
 *
 * Copyright (C) 2013-2017 Fabien DUMINY (fabien [dot] duminy [at] webmails [dot] com)
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

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;

import static java.math.BigInteger.valueOf;

/**
 * Some Math utilities.
 */
public class Utils {
    public static final BigInteger MAX_INTEGER = valueOf(Integer.MAX_VALUE);

    private Utils() {
    }

    /**
     * Converts a <code>long</code> value to <code>int</code> value.
     * The value must be in the interval [0;<code>maxValue</code>].
     * If the <code>maxValue</code> <= {@link Integer#MAX_VALUE}, the returned value is the same as <code>(int)value</code>.
     *
     * @param value    The <code>long</code> to convert.
     * @param maxValue The maximal possible value.
     * @return The value being converted to int.
     * @throws IllegalArgumentException if <code>value</code> or <code>maxValue</code> are negatives.
     * @throws IllegalArgumentException if <code>value</code> > <code>maxValue</code>.
     */
    public static int toInteger(long value, long maxValue) {
        if (value < 0) {
            throw new IllegalArgumentException("value < 0");
        }
        if (maxValue < 0) {
            throw new IllegalArgumentException("maxValue < 0");
        }
        if (value > maxValue) {
            throw new IllegalArgumentException("value > maxValue");
        }

        if (maxValue <= Integer.MAX_VALUE) {
            return (int) value;
        }

        BigInteger b = MAX_INTEGER.multiply(valueOf(value)).divide(valueOf(maxValue));
        return b.intValue();
    }

    /**
     * Converts the result of <code>value</code> / <code>maxValue</code> as a percent and with a maximum of
     * <code>nbDecimals</code> decimals.
     *
     * @param value    The value to convert.
     * @param maxValue The maximum of possible values (corresponding to '100 %').
     */
    public static double percent(long value, long maxValue) {
        return 100d * value / maxValue;
    }

    //TODO move this method to swing-components
    public static void runInEventDispatchThread(Runnable runnable) {
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                runnable.run();
            } else {
                SwingUtilities.invokeAndWait(runnable);
            }
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
