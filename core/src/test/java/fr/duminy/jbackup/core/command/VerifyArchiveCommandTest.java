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
package fr.duminy.jbackup.core.command;

import fr.duminy.components.chain.CommandException;
import fr.duminy.jbackup.core.archive.ArchiveException;
import fr.duminy.jbackup.core.archive.ArchiveFactory;
import fr.duminy.jbackup.core.archive.ArchiveVerifier;
import fr.duminy.jbackup.core.archive.SourceWithPath;
import fr.duminy.jbackup.core.task.BackupTask;
import fr.duminy.jbackup.core.util.FileDeleter;
import fr.duminy.jbackup.core.util.InputStreamComparator;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.InputStream;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class VerifyArchiveCommandTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private InputStreamComparator comparator;

    @Mock
    private ArchiveFactory factory;

    @Mock
    private InputStream archive;

    @Mock
    private List<SourceWithPath> collectedFiles;

    @Mock
    private ArchiveVerifier verifier;

    @Mock
    private FileDeleter fileDeleter;

    private VerifyArchiveCommand command;
    private JBackupContext context;
    private MutableInt count;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        count = new MutableInt();
        MutableJBackupContext ctx = new MutableJBackupContext() {
            @Override
            public InputStream getArchive() {
                count.increment();
                return super.getArchive();
            }
        };
        ctx.setCollectedFiles(collectedFiles);
        ctx.setComparator(comparator);
        ctx.setFactory(factory);
        ctx.setArchive(archive);
        ctx.setArchivePath(tempFolder.newFile().toPath());
        ctx.setFileDeleter(fileDeleter);
        context = spy(ctx);

        command = new VerifyArchiveCommand(verifier);
    }

    @Test
    public void testExecute() throws Exception {
        when(verifier.verify(any(), any(), any())).thenReturn(true);

        command.execute(context);

        verify(context, times(1)).getArchive();
        verify(verifier).verify(eq(factory), eq(archive), eq(collectedFiles));
        verify(archive).close();
        verifyNoMoreInteractions(verifier, archive);
    }

    @Test
    public void testExecute_withFailure() throws Exception {
        when(verifier.verify(any(), any(), any())).thenReturn(false);
        thrown.expect(BackupTask.VerificationFailedException.class);

        try {
            command.execute(context);
        } finally {
            verify(archive).close();
        }
    }

    @Test
    public void testExecute_withError() throws Exception {
        ArchiveException exception = new ArchiveException(new Exception("unexpected error"));
        when(verifier.verify(any(), any(), any())).thenThrow(exception);
        thrown.expect(CommandException.class);
        thrown.expectCause(equalTo(exception));
        thrown.expectMessage(exception.getMessage());

        try {
            command.execute(context);
        } finally {
            verify(archive).close();
        }
    }

    @Test
    public void testRevert() throws Exception {
        command.revert(context);

        verify(fileDeleter).deleteAll();
        verifyNoMoreInteractions(fileDeleter);
    }
}