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

import fr.duminy.jbackup.core.ConfigurationManager;
import fr.duminy.jbackup.core.JBackup;
import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * The main class of the application.
 */
public class Application extends JPanel {
    public static void main(String[] args) throws Exception {
        JFrame frame = new JFrame();

        final Application[] application = new Application[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    application[0] = new Application();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        frame.setContentPane(application[0]);
        frame.pack();
        frame.validate();
//        fr.duminy.components.swing.SwingUtilities.centerInScreen(frame); //TODO
        frame.setVisible(true);
        frame.setTitle("JBackup"); //TODO add version from maven's pom.xml
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private final JBackup jBackup;
    private final ConfigurationManager manager;

    private final ConfigurationManagerPanel managerPanel;
    private final TaskManagerPanel taskManagerPanel;

    public Application() throws Exception {
        super(new BorderLayout());

        jBackup = new JBackup();
        manager = new ConfigurationManager(new File(System.getProperty("user.home"), ".jbackup"));

        taskManagerPanel = new TaskManagerPanel(jBackup);
        add(taskManagerPanel, BorderLayout.SOUTH);

        managerPanel = new ConfigurationManagerPanel(manager, taskManagerPanel, this, new ZipArchiveFactory());
        add(managerPanel, BorderLayout.CENTER);
    }
}
