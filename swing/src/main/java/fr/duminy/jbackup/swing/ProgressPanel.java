/**
 * JBackup is a software managing backups.
 *
 * Copyright (C) 2013-2015 Fabien DUMINY (fabien [dot] duminy [at] webmails [dot] com)
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

import fr.duminy.components.swing.Bundle;
import fr.duminy.jbackup.core.archive.ProgressListener;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Future;

import static fr.duminy.jbackup.swing.Utils.toInteger;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;

/**
 * Implements {@link fr.duminy.jbackup.core.archive.ProgressListener} by displaying the progression of a task.
 */
public class ProgressPanel extends JPanel implements ProgressListener {
    private final JProgressBar progressBar = new JProgressBar();
    private long totalSize;
    private boolean finished = false;
    private JButton cancelButton;
    private Future<?> task;

    public ProgressPanel(String title) {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(title));
        progressBar.setName("progress");
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(true);
        progressBar.setString("Not started");
        add(progressBar, BorderLayout.CENTER);
    }

    public void setTask(final Future<?> task) {
        this.task = task;
        if ((task != null) && (cancelButton == null)) {
            Utils.runInEventDispatchThread(() -> {
                ImageIcon icon = new ImageIcon(ProgressPanel.class.getResource("cancel.png"));
                cancelButton = new JButton(icon);
                cancelButton.setMargin(new Insets(0, 0, 0, 0));
                cancelButton.setToolTipText(Bundle.getBundle(Messages.class).cancelTaskTooltip());
                add(cancelButton, BorderLayout.EAST);
                revalidate();
                cancelButton.addActionListener(e -> {
                    if (task.cancel(false)) {
                        removeFromParent();
                    }
                });
            });
        }
    }

    @Override
    public void taskStarted(String configurationName) {
        progressBar.setString("Estimating total size");
    }

    @Override
    public void totalSizeComputed(String configurationName, long totalSize) {
        progressBar.setIndeterminate(false);
        progressBar.setMinimum(0);

        this.totalSize = totalSize;
        if (totalSize > Integer.MAX_VALUE) {
            progressBar.setMaximum(Integer.MAX_VALUE);
        } else {
            progressBar.setMaximum((int) totalSize);
        }
        progressBar.setValue(0);
        setText(0);
    }

    @Override
    public void progress(String configurationName, long totalReadBytes) {
        int value;
        if (totalSize > Integer.MAX_VALUE) {
            value = toInteger(totalReadBytes, totalSize);
        } else {
            value = (int) totalReadBytes;
        }
        progressBar.setValue(value);
        setText(totalReadBytes);
    }

    @Override
    public void taskFinished(String configurationName, Throwable error) {
        progressBar.setIndeterminate(false);
        if (error == null) {
            progressBar.setString("Finished");
        } else {
            String errorMessage = error.getMessage();
            if (errorMessage == null) {
                errorMessage = error.getClass().getSimpleName();
            }
            progressBar.setString("Error : " + errorMessage);
        }
        if (cancelButton != null) {
            remove(cancelButton);
            revalidate();
        }
        removeFromParent();
        finished = true;
    }

    private void setText(long totalReadBytes) {
        String message = String.format("%s/%s written (%1.2f %%)", byteCountToDisplaySize(totalReadBytes),
                byteCountToDisplaySize(totalSize), Utils.percent(totalReadBytes, totalSize));
        progressBar.setString(message);
    }

    private void removeFromParent() {
        final Container parent = getParent();
        parent.remove(this);
        parent.revalidate();
    }

    public boolean isFinished() {
        return finished || ((task != null) && (task.isCancelled() || task.isDone()));
    }
}
