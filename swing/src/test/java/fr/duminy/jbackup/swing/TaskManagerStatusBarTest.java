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

import com.google.common.base.Supplier;
import fr.duminy.components.swing.AbstractSwingTest;
import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.JBackup;
import fr.duminy.jbackup.core.TestUtils;
import org.fest.swing.fixture.JButtonFixture;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

import static fr.duminy.jbackup.core.ConfigurationManagerTest.createConfiguration;
import static fr.duminy.jbackup.core.matchers.Matchers.propertyChangeEventWithNewValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for class {@link fr.duminy.jbackup.swing.TaskManagerStatusBar}.
 */
public class TaskManagerStatusBarTest extends AbstractSwingTest {
    private TaskManagerStatusBar statusBar;
    private JBackup jBackup;

    @Override
    public void onSetUp() {
        super.onSetUp();

        try {
            statusBar = buildAndShowWindow(new Supplier<TaskManagerStatusBar>() {
                @Override
                public TaskManagerStatusBar get() {
                    jBackup = mock(JBackup.class);
                    return new TaskManagerStatusBar(jBackup);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testTaskManagerLabel_init() {
        window.label().requireText("No task are running");
    }

    @Test
    public void testTaskManagerLabel_oneBackupTask() throws IOException, DuplicateTaskException, InterruptedException {
        testTaskManagerLabel(true, false);
    }

    @Test
    public void testTaskManagerLabel_oneRestoreTask() throws IOException, DuplicateTaskException, InterruptedException {
        testTaskManagerLabel(false, true);
    }

    @Test
    public void testTaskManagerLabel_oneRestoreTask_oneBackupTask() throws IOException, DuplicateTaskException, InterruptedException {
        testTaskManagerLabel(true, true);
    }

    private void testTaskManagerLabel(boolean backup, boolean restore) throws IOException, InterruptedException, DuplicateTaskException {
        // prepare test
        boolean expect2Tasks = backup && restore;
        final int nbExpectedTasks = expect2Tasks ? 2 : 1;
        final BackupConfiguration backupConfig = createConfiguration("backupConfig");
        final BackupConfiguration restoreConfig = createConfiguration("restoreConfig");
        final CountDownLatch beginCount = new CountDownLatch(nbExpectedTasks);
        final CountDownLatch endCount = new CountDownLatch((int) beginCount.getCount());
        when(jBackup.backup(any(BackupConfiguration.class))).thenAnswer(
                simulateTaskInNewThread(backupConfig, beginCount, endCount));
        when(jBackup.restore(any(BackupConfiguration.class), any(Path.class), any(Path.class))).thenAnswer(
                simulateTaskInNewThread(restoreConfig, beginCount, endCount));

        PropertyChangeListener listener = mock(PropertyChangeListener.class);
        window.label().component().addPropertyChangeListener("text", listener);

        // test
        if (backup) {
            TaskManagerComponentTest.runBackup(statusBar, backupConfig);
        }
        if (restore) {
            TaskManagerComponentTest.runRestore(statusBar, null, null, restoreConfig);
        }
        beginCount.await();
        endCount.await();
        robot().waitForIdle();

        // verify
        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener, times(1)).propertyChange(propertyChangeEventWithNewValue("1 task is running"));
        if (expect2Tasks) {
            inOrder.verify(listener, times(1)).propertyChange(propertyChangeEventWithNewValue("2 tasks are running"));
        }
        inOrder.verify(listener, times(1)).propertyChange(propertyChangeEventWithNewValue("No task are running"));
        inOrder.verifyNoMoreInteractions();
    }

    private Answer<Object> simulateTaskInNewThread(final BackupConfiguration backupConfig, final CountDownLatch beginCount, final CountDownLatch endCount) {
        return new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                new Thread() {
                    @Override
                    public void run() {
                        beginCount.countDown();
                        statusBar.taskStarted(backupConfig.getName());
                        TestUtils.sleep(100);
                        statusBar.taskFinished(backupConfig.getName(), null);
                        endCount.countDown();
                    }
                }.start();
                return null;
            }
        };
    }

    @Test
    public void testShowPopupButton() {
        // show popup
        final JButtonFixture button = window.button("showPopup");
        button.click();

        JPopupMenu component = robot().findActivePopupMenu();
        assertThat(component).as("popup after first click").isNotNull();
        assertThat(component.getComponentCount()).as("expectedComponentCount").isEqualTo(1);
        assertThat(component.getComponent(0)).isInstanceOf(TaskManagerPanel.class);
        assertThat(component.getComponent(0).getSize()).isEqualTo(sizeMinusInsets(component));

        // hide popup
        window.click(); // click outside of the popup

        JPopupMenu popupMenu = robot().findActivePopupMenu();
        final boolean noPopup = popupMenu == null;
        final boolean showing = noPopup ? false : popupMenu.isShowing();
        assertThat(noPopup || !showing).as("popup after second click (popup=" + !noPopup + ", showing=" + showing + ')').isTrue();
    }

    @Test
    public void testShowPopupButton_showAfterClickOutside() {
        // show popup
        final JButtonFixture button = window.button("showPopup");
        button.click();
        assertThat(robot().findActivePopupMenu()).isNotNull();

        // click outside
        window.click();
        assertThat(robot().findActivePopupMenu()).isNull();

        // show popup again
        button.click();
        assertThat(robot().findActivePopupMenu()).isNotNull();
    }

    private Dimension sizeMinusInsets(JPopupMenu component) {
        Insets insets = component.getInsets();
        Dimension expectedSize = component.getSize();
        expectedSize.setSize(component.getWidth() - insets.left - insets.right, component.getHeight() - insets.bottom - insets.top);
        return expectedSize;
    }

}
