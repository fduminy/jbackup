/**
 * JBackup is a software managing backups.
 * <p/>
 * Copyright (C) 2013-2015 Fabien DUMINY (fabien [dot] duminy [at] webmails [dot] com)
 * <p/>
 * JBackup is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * <p/>
 * JBackup is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 */
package fr.duminy.jbackup.core.util;

import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(Theories.class)
public class InputStreamComparatorTest {
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testEquals() throws IOException {
        Path inputPath = Files.createTempFile("", "");
        Files.write(inputPath, new byte[]{1, 2, 3});
        InputStream inputStream = createMockInputStreamThrowing(null);
        InputStreamComparator comparator = Mockito.mock(InputStreamComparator.class);
        when(comparator.equals(eq(inputPath), eq(inputStream))).thenCallRealMethod();

        comparator.equals(inputPath, inputStream);

        verify(comparator, times(1)).equals(any(FileInputStream.class), eq(inputStream));
    }

    @Test
    public void testEquals_sameContent() throws IOException {
        assertEquals(createBuffer(5, false), createBuffer(5, false), true);
    }

    @Test
    public void testEquals_differentSize() throws IOException {
        assertEquals(createBuffer(5, false), createBuffer(6, false), false);
    }

    @Test
    public void testEquals_sameSize_differentContent() throws IOException {
        assertEquals(createBuffer(5, false), createBuffer(5, true), false);
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @Theory
    public void testEquals_closeOnFailure(boolean input1Fail, boolean input2Fail) throws IOException {
        assumeTrue(input1Fail || input2Fail);
        System.out.println("testEquals_closeOnFailure(" + input1Fail + "," + input2Fail + ")");

        InputStream input1 = createMockInputStreamThrowing(input1Fail ? IOException.class : null);
        InputStream input2 = createMockInputStreamThrowing(input2Fail ? IOException.class : null);

        IOException erreur = null;
        try {
            new InputStreamComparator().equals(input1, input2);
        } catch (IOException ioe) {
            erreur = ioe;
        }

        verify(input1, times(1)).close();
        verify(input2, times(1)).close();
        assertThat(erreur).as("error").isNotNull();
    }

    @SuppressWarnings("unchecked")
    private InputStream createMockInputStreamThrowing(Class<IOException> throwableClass) throws IOException {
        InputStream input = mock(InputStream.class);
        when(input.read(any(byte[].class))).thenCallRealMethod();
        when(input.read(any(byte[].class), anyInt(), anyInt())).thenCallRealMethod();
        if (throwableClass != null) {
            when(input.read()).thenThrow(throwableClass);
        } else {
            when(input.read()).thenReturn(1, 2, -1);
        }
        return input;
    }

    private void assertEquals(byte[] buffer1, byte[] buffer2, boolean expected) throws IOException {
        InputStream input1 = new ByteArrayInputStream(buffer1);
        InputStream input2 = new ByteArrayInputStream(buffer2);

        InputStreamComparator comparator = new InputStreamComparator();
        boolean actual = comparator.equals(input1, input2);

        assertThat(actual).isEqualTo(expected);
    }

    private byte[] createBuffer(int size, boolean reverse) {
        byte[] bytes = new byte[size];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (reverse ? (bytes.length - 1 - i) : i);
        }
        return bytes;
    }
}