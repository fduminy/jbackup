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
import fr.duminy.jbackup.core.JBackup;
import fr.duminy.jbackup.core.JBackupImpl;
import fr.duminy.jbackup.core.util.LogRule;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.edt.GuiTask;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static fr.duminy.jbackup.core.JBackup.TerminationListener;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static org.assertj.swing.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ApplicationTest extends AbstractSwingTest {
    @Rule
    public final LogRule logRule = new LogRule();

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Test
    public void testGetBackupIcon() throws IOException, URISyntaxException {
        String path = Application.class.getResource("backup.png").toURI().getPath();
        BufferedImage expectedImage = ImageIO.read(Files.newInputStream(Paths.get(path)));

        ImageIcon actualImageIcon = Application.getBackupIcon();

        assertThat(actualImageIcon).as("backupIcon").isNotNull();
        assertThat(toBufferedImage(actualImageIcon)).as("backupIcon").isEqualTo(expectedImage);
    }

    @Test
    public void testGetVersion() throws IOException {
        final String actualVersion = Application.getVersion();
        assertThat(actualVersion).as("version").isNotNull();
        assertThat(actualVersion.trim()).as("version").isNotEmpty();
    }

    @Test
    public void testApplicationIcon() {
        startApplication(null);

        assertThatImageAreEquals("imageIcon", window.target().getIconImage(), Application.getBackupIcon());
    }

    @Test
    public void testApplicationTitle() {
        startApplication(null);

        assertThat(window.target().getTitle()).as("application title").isEqualTo("JBackup " + Application.getVersion());
    }

    @Test
    public void testCallShutDownOnFrameClosure_realJBackup() throws InterruptedException {
        testCallShutDownOnFrameClosure(false);
    }

    @Test
    public void testCallShutDownOnFrameClosure_mockJBackup() throws InterruptedException {
        testCallShutDownOnFrameClosure(true);
    }

    private void testCallShutDownOnFrameClosure(boolean mockJBackup) throws InterruptedException {
        final JBackup jBackup;
        if (mockJBackup) {
            jBackup = Mockito.mock(JBackup.class);
            exit.none();
        } else {
            jBackup = spy(new JBackupImpl());
            exit.expectSystemExitWithStatus(0);
        }
        exit.checkAssertionAfterwards(() -> {
            ArgumentCaptor<TerminationListener> argument = ArgumentCaptor.forClass(TerminationListener.class);
            verify(jBackup, times(1)).shutdown(argument.capture());
            assertThat(argument.getValue()).as("TerminationListener").isNotNull();

            window.requireNotVisible();
        });

        startApplication(jBackup);
        window.requireVisible();

        JFrame frame = (JFrame) window.target();
        assertThat(frame.getDefaultCloseOperation()).isEqualTo(JFrame.DO_NOTHING_ON_CLOSE);
        window.close();
    }

    private void startApplication(final JBackup jBackup) {
        window.close();
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                if (jBackup == null) {
                    new Application();
                } else {
                    new Application() {
                        @Override
                        JBackup createJBackup() {
                            return jBackup;
                        }
                    };
                }

            }
        });
        window = new FrameFixture(getRobot(), "jbackup");
    }

    //TODO move this to assert assertj
    private static void assertThatImageAreEquals(String description, Image actualImage, ImageIcon expectedImage) {
        assertThat(actualImage).as(description).isNotNull();
        assertThat(toBufferedImage(actualImage)).as(description).isEqualTo(toBufferedImage(expectedImage));
    }

    //TODO move this to assert assertj
    private static BufferedImage toBufferedImage(ImageIcon imageIcon) {
        return toBufferedImage(imageIcon.getImage());
    }

    //TODO move this to assert assertj
    private static BufferedImage toBufferedImage(Image image) {
        BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), TYPE_INT_ARGB);
        Graphics2D g = bufferedImage.createGraphics();
        try {
            g.drawImage(image, 0, 0, null);
        } finally {
            g.dispose();
        }
        return bufferedImage;
    }
}
