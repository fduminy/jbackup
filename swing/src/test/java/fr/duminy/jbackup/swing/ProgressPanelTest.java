/**
 * JBackup is a software managing backups.
 *
 * Copyright (C) 2013-2013 Fabien DUMINY (fabien [dot] duminy [at] webmails [dot] com)
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
import org.fest.swing.edt.GuiQuery;
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
        GuiActionRunner.execute(new GuiQuery<Object>() {
            protected Object executeInEDT() {
                panel.taskStarted();
                return null;
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
        GuiActionRunner.execute(new GuiQuery<Object>() {
            protected Object executeInEDT() {
                panel.taskStarted();
                panel.totalSizeComputed(maxValue);
                return null;
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
        GuiActionRunner.execute(new GuiQuery<Object>() {
            protected Object executeInEDT() {
                panel.taskStarted();
                panel.totalSizeComputed(maxValue);
                panel.progress(value);
                return null;
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

    private void testTaskFinished(final Throwable error) {
        GuiActionRunner.execute(new GuiQuery<Object>() {
            protected Object executeInEDT() {
                panel.taskStarted();
                panel.totalSizeComputed(10);
                panel.progress(0);
                panel.taskFinished(error);
                return null;
            }
        });

        checkState(TaskState.FINISHED, null, null, error);
    }

    private void checkState(TaskState taskState, Long value, Long maxValue, Throwable error) {
        assertThat(panel.getBorder()).isExactlyInstanceOf(TitledBorder.class);
        assertThat(((TitledBorder) panel.getBorder()).getTitle()).isEqualTo(TITLE);

        JProgressBarFixture f = window.progressBar("progress");
        switch (taskState) {
            case NOT_STARTED:
                f.requireIndeterminate().requireText("Not started");
                break;

            case STARTED:
                f.requireIndeterminate().requireText("Estimating total size");
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

                String expectedMessage = format("%s/%s written (%1.2f %%)", byteCountToDisplaySize(value),
                        byteCountToDisplaySize(maxValue), MathUtils.percent(value, maxValue));
                f.requireDeterminate().requireValue(iValue).requireText(expectedMessage);
                assertThat(f.component().getMinimum()).isEqualTo(0);
                assertThat(f.component().getMaximum()).isEqualTo(iMaxValue);
                break;

            case FINISHED:
                if (error == null) {
                    f.requireDeterminate().requireText("Finished");
                } else {
                    f.requireDeterminate().requireText("Error : " + error.getMessage());
                }
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
