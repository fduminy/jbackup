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

import fr.duminy.jbackup.core.ConfigurationManager;
import fr.duminy.jbackup.core.JBackup;
import fr.duminy.jbackup.core.JBackupImpl;
import fr.duminy.jbackup.core.archive.zip.ZipArchiveFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
        LOGGER.info("*** Application startup ***");
        JFrame frame = new JFrame();

        frame.setName("jbackup");
        final JBackup jBackup = createJBackup();
        frame.setContentPane(createApplicationPanel(jBackup));
        frame.pack();
        frame.validate();
//        fr.duminy.components.swing.SwingUtilities.centerInScreen(frame); //TODO
        frame.setIconImage(new ImageIcon(Application.class.getResource("backup.png")).getImage());
        frame.setVisible(true);
        String title = "JBackup " + getVersion();
        frame.setTitle(title);
        frame.setSize(SwingUtilities.computeStringWidth(frame.getFontMetrics(frame.getFont()), title) + 200, frame.getHeight());
        frame.addWindowListener(new WindowAdapter() {
            private boolean alreadyCalled = false;

            @Override
            public void windowClosing(WindowEvent e) {
                if (!alreadyCalled) {
                    alreadyCalled = true;
                    LOGGER.info("The user has requested a shutdown. Waiting end of tasks ...");
                    try {
                        jBackup.shutdown(() -> {
                            LOGGER.info("*** Application shutdown ***");
                            System.exit(0);
                        });
                    } catch (InterruptedException e1) {
                        throw new RuntimeException(e1);
                    }
                }
            }
        });
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }

    JBackup createJBackup() {
        return new JBackupImpl();
    }

    private ApplicationPanel createApplicationPanel(final JBackup jBackup) throws Exception {
        final ApplicationPanel[] application = new ApplicationPanel[1];
        Utils.runInEventDispatchThread(() -> {
            try {
                application[0] = new ApplicationPanel(jBackup);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return application[0];
    }

    static class ApplicationPanel extends JPanel {
        private final ConfigurationManager manager;

        private final ConfigurationManagerPanel managerPanel;
        private final TaskManagerStatusBar taskManagerStatusBar;

        public ApplicationPanel(JBackup jBackup) throws Exception {
            super(new BorderLayout());

            manager = new ConfigurationManager(Paths.get(System.getProperty("user.home"), ".jbackup"));

            taskManagerStatusBar = new TaskManagerStatusBar(jBackup);
            add(taskManagerStatusBar, BorderLayout.SOUTH);

            managerPanel = new ConfigurationManagerPanel(manager, taskManagerStatusBar, this, ZipArchiveFactory.INSTANCE);
            add(managerPanel, BorderLayout.CENTER);
        }
    }
}
