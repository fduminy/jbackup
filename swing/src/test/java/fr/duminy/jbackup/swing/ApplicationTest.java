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

import fr.duminy.components.swing.AbstractSwingTest;
import org.assertj.core.api.Assertions;
import org.fest.assertions.ImageAssert;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.FrameFixture;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static org.fest.assertions.Assertions.assertThat;

public class ApplicationTest extends AbstractSwingTest {
    @Test
    public void testGetBackupIcon() throws IOException, URISyntaxException {
        String path = Application.class.getResource("backup.png").toURI().getPath();
        BufferedImage expectedImage = ImageAssert.read(path);

        ImageIcon actualImageIcon = Application.getBackupIcon();

        Assertions.assertThat(actualImageIcon).as("backupIcon").isNotNull();
        assertThat(toBufferedImage(actualImageIcon)).as("backupIcon").isEqualTo(expectedImage);
    }

    @Test
    public void testApplicationIcon() {
        startApplication();

        assertThatImageAreEquals("imageIcon", window.component().getIconImage(), Application.getBackupIcon());
    }

    private void startApplication() {
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                new Application();
            }
        });
        window = new FrameFixture(robot(), "jbackup");
    }

    //TODO move this to assert fest-assert/assertj
    private static void assertThatImageAreEquals(String description, Image actualImage, ImageIcon expectedImage) {
        assertThat(actualImage).as(description).isNotNull();
        assertThat(toBufferedImage(actualImage)).as(description).isEqualTo(toBufferedImage(expectedImage));
    }

    //TODO move this to assert fest-assert/assertj
    private static BufferedImage toBufferedImage(ImageIcon imageIcon) {
        return toBufferedImage(imageIcon.getImage());
    }

    //TODO move this to assert fest-assert/assertj
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
