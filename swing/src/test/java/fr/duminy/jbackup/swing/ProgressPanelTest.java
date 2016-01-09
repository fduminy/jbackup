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
package fr.duminy.jbackup.swing;

import fr.duminy.components.swing.AbstractSwingTest;
import fr.duminy.jbackup.core.util.LogRule;
import org.assertj.swing.core.ComponentLookupScope;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.edt.GuiQuery;
import org.assertj.swing.edt.GuiTask;
import org.assertj.swing.exception.ComponentLookupException;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JPanelFixture;
import org.assertj.swing.fixture.JProgressBarFixture;
import org.junit.Rule;
import org.junit.Test;

import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for class {@link ProgressPanel}.
 */
public class ProgressPanelTest extends AbstractSwingTest {
    private static final String TITLE = "MyTask";
    private static final String CONFIGURATION_NAME = "NotChecked";

    private ProgressPanel panel;

    @Rule
    public final LogRule logRule = new LogRule();

    @Override
    public void onSetUp() {
        super.onSetUp();

        try {
            panel = buildAndShowWindow(() -> new ProgressPanel(TITLE));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testInit() throws Exception {
        // check state before call to setTask(Future).
        checkState(TaskState.NOT_STARTED, null, null, null, null);

        final TestableTask task = new TestableTask();
        panel.setTask(task);

        // check state after call to setTask(Future).
        checkState(TaskState.TASK_DEFINED, null, null, null, task);
    }

    @Test
    public void testCancel() throws Exception {
        final TestableTask task = new TestableTask();
        panel.setTask(task);

        final long actualValue = 1;
        final long maxValue = 10L;
        GuiActionRunner.execute(new GuiTask() {
            protected void executeInEDT() {
                panel.taskStarted(CONFIGURATION_NAME);
                panel.totalSizeComputed(CONFIGURATION_NAME, maxValue);
                panel.progress(CONFIGURATION_NAME, actualValue);
            }
        });

        checkState(TaskState.PROGRESS, actualValue, maxValue, null, task);
    }

    @Test
    public void testTaskStarted() {
        GuiActionRunner.execute(new GuiTask() {
            protected void executeInEDT() {
                panel.taskStarted(CONFIGURATION_NAME);
            }
        });

        checkState(TaskState.STARTED, null, null, null, null);
    }

    @Test
    public void testTotalSizeComputed_bigSize() throws Exception {
        testTotalSizeComputed(Integer.MAX_VALUE + 123L);
    }

    @Test
    public void testTotalSizeComputed_smallSize() throws Exception {
        testTotalSizeComputed(123);
    }

    private void testTotalSizeComputed(final long maxValue) throws Exception {
        GuiActionRunner.execute(new GuiTask() {
            protected void executeInEDT() {
                panel.taskStarted(CONFIGURATION_NAME);
                panel.totalSizeComputed(CONFIGURATION_NAME, maxValue);
            }
        });

        checkState(TaskState.TOTAL_SIZE_COMPUTED, 0L, maxValue, null, null);
    }

    @Test
    public void testProgress_bigSize() throws Exception {
        testProgress(Integer.MAX_VALUE + 1L, 1000L * (Integer.MAX_VALUE + 1L));
    }

    @Test
    public void testProgress_smallSize() throws Exception {
        testProgress(10, 123);
    }

    private void testProgress(final long value, final long maxValue) throws Exception {
        GuiActionRunner.execute(new GuiTask() {
            protected void executeInEDT() {
                panel.taskStarted(CONFIGURATION_NAME);
                panel.totalSizeComputed(CONFIGURATION_NAME, maxValue);
                panel.progress(CONFIGURATION_NAME, value);
            }
        });

        checkState(TaskState.PROGRESS, value, maxValue, null, null);
    }

    @Test
    public void testTaskFinished_withoutError() {
        testTaskFinished(null, false);
    }

    @Test
    public void testTaskFinished_withError_afterTotalSizeComputed() {
        testTaskFinished(new Exception("Something went wrong"), false);
    }

    @Test
    public void testTaskFinished_withError_beforeTotalSizeComputed() {
        testTaskFinished(new Exception("Something went wrong"), true);
    }

    @Test
    public void testTaskFinished_withErrorAndNullMessage() {
        testTaskFinished(new Exception(), false);
    }

    private void testTaskFinished(final Throwable error, final boolean beforeTotalSizeComputed) {
        final TestableTask task = GuiActionRunner.execute(new GuiQuery<TestableTask>() {
            protected TestableTask executeInEDT() {
                final TestableTask task = new TestableTask();
                panel.setTask(task);
                panel.taskStarted(CONFIGURATION_NAME);
                if ((error == null) || ((error != null) && !beforeTotalSizeComputed)) {
                    panel.totalSizeComputed(CONFIGURATION_NAME, 10);
                    panel.progress(CONFIGURATION_NAME, 0);
                }
                panel.taskFinished(CONFIGURATION_NAME, error);
                if (error != null) {
                    task.setException(error);
                }
                return task;
            }
        });

        checkState(TaskState.FINISHED, null, null, error, task);
    }

    public static void assertThatPanelHasTitle(ProgressPanel panel, String expectedTitle) {
        assertThat(((TitledBorder) panel.getBorder()).getTitle()).isEqualTo(expectedTitle);
    }

    private void checkState(TaskState taskState, Long value, Long maxValue, Throwable error, Future<?> task) {
        assertThat(panel.getBorder()).isExactlyInstanceOf(TitledBorder.class);
        assertThatPanelHasTitle(panel, TITLE);

        JPanelFixture progressPanel = new JPanelFixture(getRobot(), panel);
        getRobot().settings().componentLookupScope(ComponentLookupScope.ALL);
        JProgressBarFixture progressBar = progressPanel.progressBar();
        getRobot().settings().componentLookupScope(ComponentLookupScope.SHOWING_ONLY);
        String expectedMessage;
        final boolean taskInProgress;
        switch (taskState) {
            case NOT_STARTED:
                taskInProgress = false;
                progressBar.requireIndeterminate().requireText("Not started");
                assertThat(panel.isFinished()).as("isFinished").isFalse();
                break;

            case TASK_DEFINED:
                taskInProgress = false;
                break;

            case STARTED:
                taskInProgress = (task != null);
                progressBar.requireIndeterminate().requireText("Estimating total size");
                assertThat(panel.isFinished()).as("isFinished").isFalse();
                break;

            case TOTAL_SIZE_COMPUTED:
            case PROGRESS:
                taskInProgress = (task != null);
                int iValue;
                int iMaxValue;
                if (maxValue > Integer.MAX_VALUE) {
                    iValue = Utils.toInteger(value, maxValue);
                    iMaxValue = Integer.MAX_VALUE;
                } else {
                    iValue = value.intValue();
                    iMaxValue = maxValue.intValue();
                }

                expectedMessage = format("%s/%s written (%1.2f %%)", byteCountToDisplaySize(value),
                        byteCountToDisplaySize(maxValue), Utils.percent(value, maxValue));
                progressBar.requireDeterminate().requireValue(iValue).requireText(expectedMessage);
                assertThat(progressBar.target().getMinimum()).isEqualTo(0);
                assertThat(progressBar.target().getMaximum()).isEqualTo(iMaxValue);
                assertThat(panel.isFinished()).as("isFinished").isFalse();
                break;

            case FINISHED:
            default:
                taskInProgress = false;
                if (error == null) {
                    progressBar.requireDeterminate().requireText("Finished");
                } else {
                    expectedMessage = error.getMessage();
                    if (expectedMessage == null) {
                        expectedMessage = error.getClass().getSimpleName();
                    }
                    progressBar.requireDeterminate().requireText("Error : " + expectedMessage);
                }
                assertThat(panel.isFinished()).as("isFinished").isTrue();
                break;
        }

        Container parent = null;
        if (!TaskState.FINISHED.equals(taskState)) {
            // checks progress panel is visible
            parent = panel.getParent();
            assertThat(parent).isNotNull();
            assertThat(parent.getComponents()).contains(panel);
        }

        if (taskInProgress) {
            assertThat(panel.isFinished()).isFalse();
            assertThat(task.isCancelled()).as("task cancelled").isFalse();

            // cancel the task
            JButtonFixture cancelButton = progressPanel.button();
            cancelButton.requireText("").requireToolTip("Cancel the task");
            assertThat(cancelButton.target().getIcon()).isNotNull();
            cancelButton.click();

            // checks progress panel is not visible
            assertThat(panel.isFinished()).isTrue();
            assertThat(panel.getParent()).isNull();
            assertThat(parent.getComponents()).doesNotContain(panel);
            assertThat(task.isCancelled()).as("task cancelled").isTrue();
            assertThat(((TestableTask) task).getMayInterruptIfRunning()).as("MayInterruptIfRunning").isFalse();
        } else {
            // check no cancel button is visible if the task has been defined
            requireCancelButton(progressPanel, taskState == TaskState.TASK_DEFINED);
        }
    }

    private void requireCancelButton(JPanelFixture progressPanel, boolean taskDefined) {
        boolean buttonVisible;
        try {
            progressPanel.button();
            buttonVisible = true;
        } catch (ComponentLookupException e) {
            // ok
            buttonVisible = false;
        }

        assertThat(buttonVisible).as("cancel button visible").isEqualTo(taskDefined);
    }

    private static enum TaskState {
        NOT_STARTED,
        TASK_DEFINED,
        STARTED,
        TOTAL_SIZE_COMPUTED,
        PROGRESS,
        FINISHED;
    }

    private static class TestableTask implements Future<Object> {
        private TaskState state = TaskState.NOT_STARTED;
        private boolean cancelled = false;
        private Boolean mayInterruptIfRunning;

        public void setException(Throwable error) {
//            completeExceptionally(error);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelled = true;
            this.mayInterruptIfRunning = mayInterruptIfRunning;
            return true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        public Boolean getMayInterruptIfRunning() {
            return mayInterruptIfRunning;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public Object get() throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }
    }
}
