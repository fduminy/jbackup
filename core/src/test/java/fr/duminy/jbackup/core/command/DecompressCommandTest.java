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
import fr.duminy.jbackup.core.Cancellable;
import fr.duminy.jbackup.core.archive.ArchiveException;
import fr.duminy.jbackup.core.archive.ArchiveFactory;
import fr.duminy.jbackup.core.archive.ArchiveParameters;
import fr.duminy.jbackup.core.archive.Decompressor;
import fr.duminy.jbackup.core.task.TaskListener;
import fr.duminy.jbackup.core.util.FileDeleter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.*;

public class DecompressCommandTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private ArchiveFactory factory;

    @Mock
    private Path archive;

    @Mock
    private Path targetDirectory;

    @Mock
    private Cancellable cancellable;

    @Mock
    private ArchiveParameters archiveParameters;

    @Mock
    private TaskListener listener;

    @Mock
    private Decompressor decompressor;

    @Mock
    private FileDeleter fileDeleter;

    private DecompressCommand command;
    private JBackupContext context;
    private Decompressor createdDecompressor;
    private ArchiveFactory usedFactory;
    private boolean revertCalled;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        MutableJBackupContext ctx = new MutableJBackupContext();
        ctx.setFactory(factory);
        ctx.setArchivePath(archive);
        ctx.setTargetDirectory(targetDirectory);
        ctx.setCancellable(cancellable);
        ctx.setArchiveParameters(archiveParameters);
        ctx.setListener(listener);
        ctx.setFileDeleter(fileDeleter);
        context = spy(ctx);

        when(archiveParameters.getArchive()).thenReturn(archive);

        command = new DecompressCommand() {
            @Override
            Decompressor createDecompressor(ArchiveFactory factory) {
                createdDecompressor = super.createDecompressor(factory);
                usedFactory = factory;
                return decompressor;
            }

            @Override
            public void revert(JBackupContext context) {
                revertCalled = true;
                super.revert(context);
            }
        };
    }

    @Test
    public void testExecute() throws Exception {
        command.execute(context);

        InOrder inOrder = inOrder(decompressor, fileDeleter);
        inOrder.verify(fileDeleter).registerDirectory(eq(targetDirectory));
        inOrder.verify(decompressor).decompress(eq(archive), eq(targetDirectory), eq(listener), eq(cancellable));
        inOrder.verifyNoMoreInteractions();
        assertThat(createdDecompressor).as("result of createDecompressor").isNotNull();
        assertThat(usedFactory).as("factory used by createDecompressor").isSameAs(factory);
        assertThat(revertCalled).as("revert() called").isFalse();
    }

    @Test
    public void testExecute_withError() throws Exception {
        ArchiveException exception = new ArchiveException(new Exception("unexpected error"));
        doThrow(exception).when(decompressor).decompress(any(), any(), any(), any());
        thrown.expect(CommandException.class);
        thrown.expectCause(equalTo(exception));
        thrown.expectMessage(exception.getMessage());

        try {
            command.execute(context);
        } finally {
            verify(fileDeleter).registerDirectory(eq(targetDirectory));
            assertThat(revertCalled).as("revert() called").isFalse();
            verifyNoMoreInteractions(fileDeleter);
        }
    }

    @Test
    public void testRevert() throws Exception {
        command.revert(context);

        verify(fileDeleter).deleteAll();
        verifyNoMoreInteractions(fileDeleter);
    }
}