package com.zero.retrowrapper.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.lwjgl.opengl.Display;

public class SwingUtil {
    public static void addJButtonCentered(Container container, JButton component) {
        component.setHorizontalAlignment(SwingConstants.CENTER);
        component.setVerticalAlignment(SwingConstants.CENTER);
        addJComponentCentered(container, component);
    }

    public static void addJComponentCentered(Container container, JComponent component) {
        component.setAlignmentX(Component.CENTER_ALIGNMENT);
        component.setAlignmentY(Component.CENTER_ALIGNMENT);
        container.add(component);
    }

    public static void addJLabelCentered(Container container, JLabel component) {
        component.setHorizontalAlignment(SwingConstants.CENTER);
        component.setVerticalAlignment(SwingConstants.CENTER);
        addJComponentCentered(container, component);
    }

    public static void addJTextFieldCentered(Container container, JTextField component) {
        component.setHorizontalAlignment(SwingConstants.CENTER);
        addJComponentCentered(container, component);
    }

    public static void loadIconsOnFrames() {
        final List<File> iconList = new ArrayList<File>();
        final File[] files = { FileUtil.tryFindResourceFile("icons/icon_16x16.png"), FileUtil.tryFindResourceFile("icons/icon_32x32.png") };
        CollectionUtil.addNonNullToCollection(iconList, files);

        if (!iconList.isEmpty()) {
            System.out.println("Loading current icons for window from: " + iconList);
            // TODO Refactor
            final List<ByteBuffer> iconsAsByteBufferArrayList = new ArrayList<ByteBuffer>();

            for (final File icon : iconList) {
                try {
                    final ByteBuffer loadedIcon = FileUtil.loadIcon(icon);
                    iconsAsByteBufferArrayList.add(loadedIcon);
                } catch (final IOException e) {
                    // TODO Better error handling
                    e.printStackTrace();
                }
            }

            Display.setIcon(iconsAsByteBufferArrayList.toArray(new ByteBuffer[0]));
            final java.awt.Frame[] frames = java.awt.Frame.getFrames();

            if (frames != null) {
                final List<BufferedImage> bufferedImageList = new ArrayList<BufferedImage>();

                for (final File icon : iconList) {
                    try {
                        final BufferedImage iconImage = ImageIO.read(icon);
                        bufferedImageList.add(iconImage);
                    } catch (final IOException e) {
                        // TODO Better error handling
                        e.printStackTrace();
                    }
                }

                if (!bufferedImageList.isEmpty()) {
                    for (final Frame frame : frames) {
                        frame.setIconImages(bufferedImageList);
                    }
                }
            }
        } else {
            System.out.println("Could not find any icon files!");
        }
    }

    private SwingUtil() {
        // As this is a helper class, there should be no reason to instantiate an instance of it.
    }
}
