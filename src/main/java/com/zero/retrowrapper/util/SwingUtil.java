package com.zero.retrowrapper.util;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.lwjgl.opengl.Display;

import net.minecraft.launchwrapper.LogWrapper;

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
        CollectionUtil.addNonNullToCollection(iconList, FileUtil.tryFindResourceFile("icons/icon_16x16.png"), FileUtil.tryFindResourceFile("icons/icon_32x32.png"));

        if (!iconList.isEmpty()) {
            LogWrapper.fine("Loading current icons for window from: " + iconList);
            // TODO Refactor
            final List<ByteBuffer> iconsAsByteBufferArrayList = new ArrayList<ByteBuffer>();

            for (final File icon : iconList) {
                try {
                    final ByteBuffer loadedIcon = FileUtil.loadIcon(icon);
                    iconsAsByteBufferArrayList.add(loadedIcon);
                } catch (final IOException e) {
                    // TODO Better error handling
                    LogWrapper.warning("Issue loading icon " + icon + ": " + ExceptionUtils.getStackTrace(e));
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
                        LogWrapper.warning("Issue reading icon " + icon + ": " + ExceptionUtils.getStackTrace(e));
                    }
                }

                if (!bufferedImageList.isEmpty()) {
                    for (final Frame frame : frames) {
                        try {
                            final Method setIconImages = Frame.class.getMethod("setIconImages", List.class);
                            setIconImages.invoke(frame, bufferedImageList);
                        } catch (final Exception ignored) {
                            LogWrapper.warning("Are you running RetroWrapper on Java 5?: " + ExceptionUtils.getStackTrace(ignored));
                            frame.setIconImage(bufferedImageList.get(0));
                        }
                    }
                }
            }
        } else {
            LogWrapper.warning("Could not find any icon files!");
        }
    }

    public static void setupMacOSProperties(Logger logger, String title) {
        try {
            System.setProperty("apple.awt.application.name", title);
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", title);
            System.setProperty("apple.laf.useScreenMenuBar", "true");

            // TODO Proper dark mode
            if (System.getProperty("apple.awt.application.appearance") == null) {
                System.setProperty("apple.awt.application.appearance", "system");
            }
        } catch (final Exception ignored) {
            logger.log(Level.WARNING, "An Exception was thrown while trying to set system properties: " + ExceptionUtils.getStackTrace(ignored));
        }
    }

    public static void showExceptionHandler(final Logger logger, final String context, final Exception toShow) {
        final String exceptText = ExceptionUtils.getStackTrace(toShow);
        logger.log(Level.FINE, "Displaying exception handler with context \"{0}\" and stacktrace \"{1}\"", new Object[] { context, exceptText });
        final String dialogTitle = "RetroWrapper error report: " + context;
        final String issueTitle = toShow.getClass().getSimpleName() + " thrown when running RetroWrapper " + MetadataUtil.VERSION;
        final String githubIssueTitle = issueTitle + " (modify to add context)";
        final String baseIssueBody = "RetroWrapper version: " + MetadataUtil.VERSION + "\nOS: " + SystemUtils.OS_NAME + "\nJava version: " + SystemUtils.JAVA_VERSION + "\nInternal reason: " + context;
        final String displayIssueBody = baseIssueBody + "\nStacktrace:\n" + exceptText;
        final String githubIssueBody = baseIssueBody + "\n\nAdd some context about what you were doing when this error occurred!\n\nStacktrace (don't modify):\n```java\n" + exceptText + "```";
        final JFrame errorFrame = new JFrame();
        errorFrame.setResizable(true);
        errorFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        errorFrame.setVisible(false);

        try {
            final JTextPane textPane = new JTextPane();
            textPane.setEditable(false);
            textPane.setBorder(null);
            textPane.setContentType("text/html");
            textPane.setText("<html>" +
                             "<p>Please report this issue on GitHub (the link will autofill this information for you):</p><br>" +
                             "<a href=\"https://github.com/NeRdTheNed/RetroWrapper/issues/new?title=" + URLEncoder.encode(githubIssueTitle, "UTF-8") + "&body=" + URLEncoder.encode(githubIssueBody, "UTF-8") + "\">Create an issue on Github!</a><br>" +
                             "<p>" + escapeHtml4(dialogTitle).replace("\n", "<br>") + "</p><br>" +
                             "<br><p>" + escapeHtml4(issueTitle).replace("\n", "<br>") + "</p><br>" +
                             "<br><p>" + escapeHtml4(displayIssueBody).replace("\n", "<br>") + "</p><br>" +
                             "</html>");
            textPane.addHyperlinkListener(new HyperlinkListener() {
                public void hyperlinkUpdate(HyperlinkEvent event) {
                    if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        try {
                            final Class<?> desktopClass = Class.forName("java.awt.Desktop");
                            final Method getDesktop = desktopClass.getMethod("getDesktop");
                            final Object desktopObject = getDesktop.invoke(null);
                            final Method browse = desktopClass.getMethod("browse", URI.class);
                            browse.invoke(desktopObject, event.getURL().toURI());
                        } catch (final Exception ignored) {
                            if ((ignored instanceof NoSuchMethodException) || (ignored instanceof ClassNotFoundException)) {
                                logger.log(Level.WARNING, "Are you running RetroWrapper on Java 5?");
                            }

                            logger.log(Level.WARNING, "Could not open link from hyperlinkUpdate", ignored);
                            JOptionPane.showMessageDialog(textPane, "Your platform doesn't let Java open links.\nPlease browse to https://github.com/NeRdTheNed/RetroWrapper/issues/new to create an issue.\nPlease copy-paste the error report into the issue.\nThanks for putting up with this!", "Sorry", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            });
            textPane.setCaretPosition(0);
            final JScrollPane jsp = new JScrollPane(textPane);
            jsp.setPreferredSize(new Dimension(720, 420));
            jsp.setBorder(null);
            JOptionPane.showMessageDialog(errorFrame, jsp, dialogTitle, JOptionPane.ERROR_MESSAGE);
        } catch (final Exception ignored) {
            logger.log(Level.WARNING, "An Exception was thrown while trying to display the exception handler", ignored);
            JOptionPane.showMessageDialog(errorFrame, "Please report this issue on GitHub!\nhttps://github.com/NeRdTheNed/RetroWrapper/issues/new\nPlease take a screenshot of this message for the issue.\n" + dialogTitle + "\n" + issueTitle + "\n" + displayIssueBody, "(Backup handler) " + dialogTitle, JOptionPane.ERROR_MESSAGE);
        }

        errorFrame.dispose();
    }

    private SwingUtil() {
        // As this is a helper class, there should be no reason to instantiate an instance of it.
    }
}
