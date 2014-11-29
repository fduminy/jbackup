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

import fr.duminy.jbackup.core.BackupConfiguration;
import fr.duminy.jbackup.core.JBackup;
import fr.duminy.jbackup.core.archive.ProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A status bar displaying the current status of the task manager.
 */
public class TaskManagerStatusBar extends JPanel implements BackupConfigurationActions, ProgressListener {
    private static final Logger LOG = LoggerFactory.getLogger(TaskManagerStatusBar.class);

    private final JLabel label = new JLabel();
    private final JBackup jBackup;
    private final JButton button = new JButton("Show tasks");
    private AtomicInteger taskCount = new AtomicInteger(0);
    private JPopupMenu popup;
    private TaskManagerPanel popupPanel;

    TaskManagerStatusBar(final JBackup jBackup) {
        super(new BorderLayout());
        this.jBackup = jBackup;

        add(label, BorderLayout.CENTER);
        updateLabel(0);

        button.setName("showPopup");
        add(button, BorderLayout.EAST);

        popupPanel = new TaskManagerPanel(jBackup);

        popup = new JPopupMenu();
        popup.setInvoker(button);
        popup.setLayout(new BorderLayout());
        popup.add(popupPanel, BorderLayout.CENTER);

        button.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!popup.isShowing()) {
                    LOG.info("popup is not shown -> showing it");
                    Point location = button.getLocationOnScreen();
                    location.move((int) location.getX(), (int) (location.getY() - popup.getPreferredSize().getHeight()));
                    popup.setLocation(location);
                    popup.setVisible(true);
                }
            }
        });
    }

    @Override
    public void taskStarted(String configurationName) {
        updateLabel(taskCount.incrementAndGet());
    }

    @Override
    public void totalSizeComputed(String configurationName, long totalSize) {
    }

    @Override
    public void progress(String configurationName, long totalReadBytes) {
    }

    @Override
    public void taskFinished(String configurationName, Throwable error) {
        updateLabel(taskCount.decrementAndGet());
    }

    private void updateLabel(final int nbRunningTasks) {
        Utils.runInEventDispatchThread(new Runnable() {
            @Override
            public void run() {
                final String text;
                if (nbRunningTasks == 0) {
                    text = "No task are running";
                } else if (nbRunningTasks == 1) {
                    text = "1 task is running";
                } else {
                    text = nbRunningTasks + " tasks are running";
                }
                LOG.info("updateLabel: setText(\"{}\")", text);
                label.setText(text);
            }
        });
    }

    @Override
    public void backup(BackupConfiguration config) throws DuplicateTaskException {
        jBackup.backup(config);
    }

    @Override
    public void restore(BackupConfiguration config, Path archive, Path targetDirectory) throws DuplicateTaskException {
        jBackup.restore(config, archive, targetDirectory);
    }
}
