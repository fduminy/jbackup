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

import fr.duminy.jbackup.core.ConfigurationManager;
import fr.duminy.jbackup.core.JBackup;
import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * The main class of the application.
 */
public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws Exception {
        new Application();
    }

    static Properties getProperties() {
        Properties p = new Properties();
        try (InputStream propStream = Application.class.getResourceAsStream("jbackup.properties")) {
            p.load(propStream);
        } catch (IOException e) {
            LOGGER.error("Can't get application properties", e);
        }
        return p;
    }

    static String getVersion() {
        return getProperties().getProperty("application.version", "");
    }

    public static ImageIcon getBackupIcon() {
        // TODO put in a cache and share it with the "backup" button
        return new ImageIcon(Application.class.getResource("backup.png"));
    }

    Application() throws Exception {
        JFrame frame = new JFrame();

        frame.setName("jbackup");
        frame.setContentPane(createApplicationPanel());
        frame.pack();
        frame.validate();
//        fr.duminy.components.swing.SwingUtilities.centerInScreen(frame); //TODO
        frame.setIconImage(new ImageIcon(Application.class.getResource("backup.png")).getImage());
        frame.setVisible(true);
        String title = "JBackup " + getVersion();
        frame.setTitle(title);
        frame.setSize(SwingUtilities.computeStringWidth(frame.getFontMetrics(frame.getFont()), title) + 200, frame.getHeight());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private ApplicationPanel createApplicationPanel() throws Exception {
        final ApplicationPanel[] application = new ApplicationPanel[1];
        if (SwingUtilities.isEventDispatchThread()) {
            application[0] = new ApplicationPanel();
        } else {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    try {
                        application[0] = new ApplicationPanel();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        return application[0];
    }

    private static class ApplicationPanel extends JPanel {
        private final JBackup jBackup;
        private final ConfigurationManager manager;

        private final ConfigurationManagerPanel managerPanel;
        private final TaskManagerPanel taskManagerPanel;

        public ApplicationPanel() throws Exception {
            super(new BorderLayout());

            jBackup = new JBackup();
            manager = new ConfigurationManager(Paths.get(System.getProperty("user.home"), ".jbackup"));

            taskManagerPanel = new TaskManagerPanel(jBackup);
            add(taskManagerPanel, BorderLayout.SOUTH);

            managerPanel = new ConfigurationManagerPanel(manager, taskManagerPanel, this, ZipArchiveFactory.INSTANCE);
            add(managerPanel, BorderLayout.CENTER);
        }
    }
}
