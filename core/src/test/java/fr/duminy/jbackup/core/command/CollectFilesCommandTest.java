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
package fr.duminy.jbackup.core.command;

import fr.duminy.components.chain.CommandException;
import fr.duminy.jbackup.core.Cancellable;
import fr.duminy.jbackup.core.archive.ArchiveException;
import fr.duminy.jbackup.core.archive.ArchiveParameters;
import fr.duminy.jbackup.core.archive.FileCollector;
import fr.duminy.jbackup.core.archive.SourceWithPath;
import fr.duminy.jbackup.core.task.TaskListener;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.*;

public class CollectFilesCommandTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private ArchiveParameters archiveParameters;

    @Mock
    private TaskListener listener;

    @Mock
    private Cancellable cancellable;

    @Mock
    private FileCollector fileCollector;

    private CollectFilesCommand command;
    private JBackupContext context;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        MutableJBackupContext ctx = new MutableJBackupContext();
        ctx.setArchiveParameters(archiveParameters);
        ctx.setListener(listener);
        ctx.setCancellable(cancellable);
        context = ctx;

        command = new CollectFilesCommand(fileCollector);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecute() throws Exception {
        command.execute(context);

        ArgumentCaptor<List<SourceWithPath>> collectedFilesArg = ArgumentCaptor.forClass(List.class);
        verify(fileCollector)
            .collectFiles(collectedFilesArg.capture(), eq(archiveParameters), eq(listener), eq(cancellable));
        final List<SourceWithPath> collectedFiles = collectedFilesArg.getValue();
        assertThat(collectedFiles).isNotNull();
        assertThat(context.getCollectedFiles()).isSameAs(collectedFiles);
    }

    @Test
    public void testExecute_withError() throws Exception {
        ArchiveException exception = new ArchiveException(new Exception("unexpected error"));
        doThrow(exception).when(fileCollector).collectFiles(any(), any(), any(), any());
        thrown.expect(CommandException.class);
        thrown.expectCause(equalTo(exception));
        thrown.expectMessage(exception.getMessage());

        command.execute(context);
    }

    @Test
    public void testRevert() throws Exception {
        command.revert(context);

        verifyNoMoreInteractions(archiveParameters, listener, cancellable, fileCollector);
    }
}