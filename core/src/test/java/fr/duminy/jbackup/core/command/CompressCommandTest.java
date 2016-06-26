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
import fr.duminy.jbackup.core.archive.*;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.*;

public class CompressCommandTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private ArchiveFactory factory;

    @Mock
    private List<SourceWithPath> collectedFiles;

    @Mock
    private ArchiveParameters archiveParameters;

    @Mock
    private Cancellable cancellable;

    @Mock
    private TaskListener listener;

    @Mock
    private Compressor mockCompressor;

    @Mock
    private FileDeleter fileDeleter;

    @Mock
    private Path archive;

    private CompressCommand command;
    private JBackupContext context;
    private Compressor realCompressor;
    private ArchiveFactory usedFactory;
    private boolean revertCalled;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        MutableJBackupContext ctx = new MutableJBackupContext();
        ctx.setFactory(factory);
        ctx.setCollectedFiles(collectedFiles);
        ctx.setArchiveParameters(archiveParameters);
        ctx.setCancellable(cancellable);
        ctx.setListener(listener);
        ctx.setFileDeleter(fileDeleter);
        context = spy(ctx);

        when(archiveParameters.getArchive()).thenReturn(archive);

        command = new CompressCommand() {
            @Override
            protected Compressor createCompressor(ArchiveFactory factory) {
                realCompressor = super.createCompressor(factory);
                usedFactory = factory;
                return mockCompressor;
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

        InOrder inOrder = inOrder(mockCompressor, fileDeleter);
        inOrder.verify(fileDeleter).registerFile(eq(archive));
        inOrder.verify(mockCompressor)
               .compress(eq(archiveParameters), eq(collectedFiles), eq(listener), eq(cancellable));
        inOrder.verifyNoMoreInteractions();
        assertThat(realCompressor).as("result of createCompressor").isNotNull();
        assertThat(usedFactory).as("factory used by createCompressor").isSameAs(factory);
        assertThat(revertCalled).as("revert() called").isFalse();
    }

    @Test
    public void testExecute_withError() throws Exception {
        ArchiveException exception = new ArchiveException(new Exception("unexpected error"));
        doThrow(exception).when(mockCompressor).compress(any(), any(), any(), any());
        thrown.expect(CommandException.class);
        thrown.expectCause(equalTo(exception));
        thrown.expectMessage(exception.getMessage());

        try {
            command.execute(context);
        } finally {
            verify(fileDeleter).registerFile(eq(archive));
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