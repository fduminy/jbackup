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
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.JPanelFixture;
import org.fest.swing.fixture.JProgressBarFixture;
import org.junit.Test;

import javax.swing.border.TitledBorder;

import static java.lang.String.format;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for class {@link ProgressPanel}.
 */
public class ProgressPanelTest extends AbstractSwingTest {
    private static final String TITLE = "MyTask";
    private ProgressPanel panel;

    @Override
    public void onSetUp() {
        super.onSetUp();

        try {
            panel = buildAndShowWindow(new Supplier<ProgressPanel>() {
                @Override
                public ProgressPanel get() {
                    return new ProgressPanel(TITLE);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testInit() throws Exception {
        checkState(TaskState.NOT_STARTED, null, null, null);
    }

    @Test
    public void testTaskStarted() {
        GuiActionRunner.execute(new GuiTask() {
            protected void executeInEDT() {
                panel.taskStarted();
            }
        });

        checkState(TaskState.STARTED, null, null, null);
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
                panel.taskStarted();
                panel.totalSizeComputed(maxValue);
            }
        });

        checkState(TaskState.TOTAL_SIZE_COMPUTED, 0L, maxValue, null);
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
                panel.taskStarted();
                panel.totalSizeComputed(maxValue);
                panel.progress(value);
            }
        });

        checkState(TaskState.PROGRESS, value, maxValue, null);
    }

    @Test
    public void testTaskFinished_withoutError() {
        testTaskFinished(null);
    }

    @Test
    public void testTaskFinished_withError() {
        testTaskFinished(new Exception("Something went wrong"));
    }

    @Test
    public void testTaskFinished_withErrorAndNullMessage() {
        testTaskFinished(new Exception());
    }

    private void testTaskFinished(final Throwable error) {
        GuiActionRunner.execute(new GuiTask() {
            protected void executeInEDT() {
                panel.taskStarted();
                panel.totalSizeComputed(10);
                panel.progress(0);
                panel.taskFinished(error);
            }
        });

        checkState(TaskState.FINISHED, null, null, error);
    }

    public static void assertThatPanelHasTitle(ProgressPanel panel, String expectedTitle) {
        assertThat(((TitledBorder) panel.getBorder()).getTitle()).isEqualTo(expectedTitle);
    }

    private void checkState(TaskState taskState, Long value, Long maxValue, Throwable error) {
        assertThat(panel.getBorder()).isExactlyInstanceOf(TitledBorder.class);
        assertThatPanelHasTitle(panel, TITLE);

        JPanelFixture progressPanel = new JPanelFixture(robot(), panel);
        JProgressBarFixture progressBar = progressPanel.progressBar();
        String expectedMessage;
        switch (taskState) {
            case NOT_STARTED:
                progressBar.requireIndeterminate().requireText("Not started");
                assertThat(panel.isFinished()).as("isFinished").isFalse();
                break;

            case STARTED:
                progressBar.requireIndeterminate().requireText("Estimating total size");
                assertThat(panel.isFinished()).as("isFinished").isFalse();
                break;

            case TOTAL_SIZE_COMPUTED:
            case PROGRESS:
                int iValue;
                int iMaxValue;
                if (maxValue > Integer.MAX_VALUE) {
                    iValue = MathUtils.toInteger(value, maxValue);
                    iMaxValue = Integer.MAX_VALUE;
                } else {
                    iValue = value.intValue();
                    iMaxValue = maxValue.intValue();
                }

                expectedMessage = format("%s/%s written (%1.2f %%)", byteCountToDisplaySize(value),
                        byteCountToDisplaySize(maxValue), MathUtils.percent(value, maxValue));
                progressBar.requireDeterminate().requireValue(iValue).requireText(expectedMessage);
                assertThat(progressBar.component().getMinimum()).isEqualTo(0);
                assertThat(progressBar.component().getMaximum()).isEqualTo(iMaxValue);
                assertThat(panel.isFinished()).as("isFinished").isFalse();
                break;

            case FINISHED:
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
    }

    private static enum TaskState {
        NOT_STARTED,
        STARTED,
        TOTAL_SIZE_COMPUTED,
        PROGRESS,
        FINISHED;
    }

}
