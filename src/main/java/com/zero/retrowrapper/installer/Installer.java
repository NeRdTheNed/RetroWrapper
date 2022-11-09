package com.zero.retrowrapper.installer;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.zero.retrowrapper.util.FileUtil;
import com.zero.retrowrapper.util.MetadataUtil;
import com.zero.retrowrapper.util.SwingUtil;

public final class Installer {

    private static final Random rand = new Random();

    // TODO Some of these variables should possibly be refactored to not be static
    private static String workingDirectory;
    private static File directory;
    private static File[] directories;
    private static File versions;
    private static JButton install;
    private static JButton uninstall;

    private static DefaultListModel<String> model = new DefaultListModel<String>();
    private static JList<String> list = new JList<String>(model);

    private static JFrame frame;

    private static boolean refreshList(String givenDirectory, final Logger installerLogger) {
        int versionCount = 0;
        int wrappedVersionCount = 0;
        model.removeAllElements();

        if (!givenDirectory.isEmpty()) {
            directory = new File(givenDirectory);
            directory.mkdirs();
            directories = null;
            versions = new File(directory, "versions");

            if (versions.exists()) {
                directories = versions.listFiles();
            }

            if ((directories != null) && (directories.length != 0)) {
                Arrays.sort(directories);

                // add items
                // TODO Refactor into separate method

                for (final File f : directories) {
                    if (f.isDirectory()) {
                        final File json = new File(f, f.getName() + ".json");
                        final File jar = new File(f, f.getName() + ".jar");

                        if (json.exists() && jar.exists() && !f.getName().contains("-wrapped")) {
                            try (Scanner s = new Scanner(json).useDelimiter("\\A")) {
                                final String content = s.next();

                                if (content.contains("old_") && !content.contains("retrowrapper")) {
                                    if (new File(versions, f.getName() + "-wrapped").exists()) {
                                        wrappedVersionCount++;
                                        model.addElement(f.getName() + " - already wrapped");
                                    } else {
                                        versionCount++;
                                        model.addElement(f.getName());
                                    }
                                }
                            } catch (final FileNotFoundException e) {
                                // TODO Better logging
                                installerLogger.log(Level.SEVERE, "A FileNotFoundException was thrown while trying to refresh the list of versions", e);
                            }
                        }
                    }
                }
            }
        }

        // button visibility

        if (givenDirectory.isEmpty()) {
            install.setEnabled(false);
            uninstall.setEnabled(false);
            JOptionPane.showMessageDialog(null, "No directory / minecraft directory detected!\n", "Error", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        if (!versions.exists()) {
            install.setEnabled(false);
            uninstall.setEnabled(false);
            JOptionPane.showMessageDialog(null, "No Minecraft versions folder found!", "Error", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        if ((versionCount == 0) && (wrappedVersionCount == 0)) {
            install.setEnabled(false);
            uninstall.setEnabled(true);
            JOptionPane.showMessageDialog(null, "No wrappable versions found!", "Error", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        if (versionCount == 0) {
            install.setEnabled(true);
            uninstall.setEnabled(true);
            JOptionPane.showMessageDialog(null, "All detected versions have already been wrapped!", "Info", JOptionPane.INFORMATION_MESSAGE);
        } else {
            install.setEnabled(true);
            uninstall.setEnabled(true);
        }

        return true;
    }

    // TODO Refactor parts into separate method
    // TODO The installer can take a very long time to start up when there are large amounts of instances
    private Installer(final Logger installerLogger) throws Exception {
        workingDirectory = FileUtil.defaultMinecraftDirectory();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (final Exception e) {
            installerLogger.log(Level.WARNING, "setLookAndFeel failed", e);
        }

        frame = new JFrame("Retrowrapper - NeRd Fork");
        frame.setPreferredSize(new Dimension(654, 420));
        frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setResizable(true);
        // Installer label
        final JLabel installerLabel = new JLabel("Retrowrapper Installer");
        installerLabel.setFont(installerLabel.getFont().deriveFont(20F).deriveFont(Font.BOLD));
        SwingUtil.addJLabelCentered(frame, installerLabel);
        // Version label
        final JLabel versionLabel = new JLabel(MetadataUtil.VERSION + " - " + MetadataUtil.INSTALLER_SPLASHES.get(rand.nextInt(MetadataUtil.INSTALLER_SPLASHES.size())));
        versionLabel.setFont(installerLabel.getFont().deriveFont(12F));
        SwingUtil.addJLabelCentered(frame, versionLabel);
        // Working directory text field
        final JTextField workDir = new JTextField(workingDirectory);
        workDir.setMaximumSize(new Dimension(300, 20));
        // TODO Refactor
        workDir.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String workDirPath = ((JTextField)e.getSource()).getText();
                final File minecraftDir = new File(workDirPath);

                if (minecraftDir.exists() && refreshList(workDirPath, installerLogger)) {
                    workingDirectory = workDirPath;
                } else {
                    if (!minecraftDir.exists()) {
                        JOptionPane.showMessageDialog(null, "No directory / minecraft directory detected!\n", "Error", JOptionPane.INFORMATION_MESSAGE);
                    }

                    ((JTextField)e.getSource()).setText(workingDirectory);
                    refreshList(workingDirectory, installerLogger);
                }
            }
        });
        SwingUtil.addJTextFieldCentered(frame, workDir);
        // List of versions that can be wrapper
        final JScrollPane scrollList = new JScrollPane(list);
        SwingUtil.addJComponentCentered(frame, scrollList);
        // Install button
        install = new JButton("Install"); //installation code
        // TODO Refactor
        install.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final List<String> versionList = list.getSelectedValuesList();
                final StringBuilder finalVersions = new StringBuilder();
                final File libsDir = new File(directory, "libraries" + File.separator + "com" + File.separator + "zero");

                if (libsDir.exists()) {
                    // Makes sure that the library gets reinstalled
                    // TODO Add version checking?
                    FileUtils.deleteQuietly(libsDir);
                }

                for (String version : versionList) {
                    if (version.contains("- already wrapped")) {
                        version = version.replace(" - already wrapped", "");
                        FileUtils.deleteQuietly(new File(directory, "versions" + File.separator + version + "-wrapped"));
                    }

                    try (Reader s = new FileReader(new File(versions, version + File.separator + version + ".json"))) {
                        finalVersions.append(version).append("\n");
                        final JsonObject versionJson = Json.parse(s).asObject();
                        final String versionWrapped = version + "-wrapped";
                        // Add the RetroWrapper library to the list of libraries. A library is a JSON object, and libraries are stored in an array of JSON objects.
                        final JsonObject retrowrapperLibraryJson = Json.object().add("name", "com.zero:retrowrapper:installer");
                        final JsonValue newLibraries = versionJson.get("libraries");
                        newLibraries.asArray().add(retrowrapperLibraryJson);
                        versionJson.set("libraries", newLibraries);

                        // Replace version ID with the wrapped version ID (e.g "c0.30-3" with "c0.30-3-wrapped")
                        if (!versionJson.getString("id", "null").equals(version)) {
                            JOptionPane.showMessageDialog(null, "The version ID " + versionJson.getString("id", "null") + " found in the JSON file " + version + File.separator + version + ".json" + "did not match the expected version ID " + version + ". Things will not go as planned!", "Error", JOptionPane.ERROR_MESSAGE);
                        }

                        versionJson.set("id",  versionWrapped);
                        // Replace any of Mojangs tweakers with RetroWrapper tweakers
                        String modifiedLaunchArgs = versionJson.getString("minecraftArguments", "null");

                        if (modifiedLaunchArgs.contains("VanillaTweaker")) {
                            modifiedLaunchArgs = modifiedLaunchArgs.replace("net.minecraft.launchwrapper.AlphaVanillaTweaker", "com.zero.retrowrapper.RetroTweaker");
                            modifiedLaunchArgs = modifiedLaunchArgs.replace("net.minecraft.launchwrapper.IndevVanillaTweaker", "com.zero.retrowrapper.RetroTweaker");
                        } else {
                            modifiedLaunchArgs = modifiedLaunchArgs.replace("--assetsDir ${game_assets}", "--assetsDir ${game_assets} --tweakClass com.zero.retrowrapper.RetroTweaker");
                        }

                        versionJson.set("minecraftArguments", modifiedLaunchArgs);
                        final File wrapDir = new File(versions, versionWrapped + File.separator);
                        wrapDir.mkdirs();
                        final File libDir = new File(directory, "libraries" + File.separator + "com" + File.separator + "zero" + File.separator + "retrowrapper" + File.separator + "installer");
                        libDir.mkdirs();

                        try (FileOutputStream fos = new FileOutputStream(new File(wrapDir, versionWrapped + ".json"))) {
                            Files.copy(new File(versions, version + File.separator + version + ".jar").toPath(), new File(wrapDir, versionWrapped + ".jar").toPath(), StandardCopyOption.REPLACE_EXISTING);
                            fos.write(versionJson.toString().getBytes());
                            final File jar = new File(Installer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                            Files.copy(jar.toPath(), new File(libDir, "retrowrapper-installer.jar").toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (final IOException ee) {
                            // TODO better logging
                            final LogRecord logRecord = new LogRecord(Level.SEVERE, "An IOException was thrown while trying to wrap version {0}");
                            logRecord.setParameters(new Object[] { version });
                            logRecord.setThrown(ee);
                            installerLogger.log(logRecord);
                        } catch (final URISyntaxException ee) {
                            // TODO better logging
                            final LogRecord logRecord = new LogRecord(Level.SEVERE, "An URISyntaxException was thrown while trying to wrap version {0}");
                            logRecord.setParameters(new Object[] { version });
                            logRecord.setThrown(ee);
                            installerLogger.log(logRecord);
                        }
                    } catch (final IOException ee) {
                        // TODO better logging
                        final LogRecord logRecord = new LogRecord(Level.SEVERE, "An IOException was thrown while trying to wrap version {0}");
                        logRecord.setParameters(new Object[] { version });
                        logRecord.setThrown(ee);
                        installerLogger.log(logRecord);
                    }
                }

                JOptionPane.showMessageDialog(null, (versionList.size() > 1 ? "Successfully wrapped versions\n" : "Successfully wrapped version\n") + finalVersions.toString(), "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshList(workingDirectory, installerLogger);
            }
        });
        SwingUtil.addJButtonCentered(frame, install);
        // Uninstall button
        uninstall = new JButton("Uninstall ALL versions"); //uninstaller code
        // TODO Refactor
        uninstall.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (final File f : directories) {
                    if (f.isDirectory() && f.getName().contains("-wrapped")) {
                        FileUtils.deleteQuietly(f);
                    }
                }

                final File libDir = new File(directory, "libraries" + File.separator + "com" + File.separator + "zero");

                if (libDir.exists()) {
                    FileUtils.deleteQuietly(libDir);
                }

                JOptionPane.showMessageDialog(null, "Successfully uninstalled wrapper", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshList(workingDirectory, installerLogger);
            }
        });
        SwingUtil.addJButtonCentered(frame, uninstall);
        // Copyright label
        final JLabel copyrightLabel = new JLabel("\u00a92018 000");
        copyrightLabel.setFont(copyrightLabel.getFont().deriveFont(12F));
        SwingUtil.addJLabelCentered(frame, copyrightLabel);
        refreshList(workingDirectory, installerLogger);
        frame.pack();
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        final Logger installerLogger;
        Logger temp;

        try {
            temp = Logger.getLogger(Installer.class.getName());
        } catch (final Exception notPossible) {
            temp = Logger.getAnonymousLogger();
            temp.log(Level.WARNING, "An Exception was thrown while trying to get the logger for the installer", notPossible);
        }

        installerLogger = temp;
        installerLogger.log(Level.INFO, "Starting RetroWrapper installer");

        try {
            new Installer(installerLogger);
        } catch (final Exception e) {
            if (frame != null) {
                frame.dispose();
            }

            final String context = "An Exception was thrown while running the RetroWrapper installer";
            installerLogger.log(Level.SEVERE, context, e);
            exceptionHandler(installerLogger, context, e);
            // Swing doesn't want the JVM to exit and I can't figure out why :(
            System.exit(1);
        }
    }

    private static void exceptionHandler(final Logger installerLogger, final String context, final Exception toShow) {
        final String exeptText = ExceptionUtils.getStackTrace(toShow);
        installerLogger.log(Level.FINE, "Displaying exception handler with context \"{0}\" and stacktrace \"{1}\"", new Object[] { context, exeptText });
        final String dialogTitle = "RetroWrapper error report: " + context;
        final String issueTitle = toShow.getClass().getSimpleName() + " thrown when installing RetroWrapper " + MetadataUtil.VERSION;
        final String githubIssueTitle = issueTitle + " (modify to add context)";
        final String baseIssueBody = "RetroWrapper version: " + MetadataUtil.VERSION + "\nOS: " + SystemUtils.OS_NAME + "\nJava version: " + SystemUtils.JAVA_VERSION + "\nInternal reason: " + context;
        final String displayIssueBody = baseIssueBody + "\nStacktrace:\n" + exeptText;
        final String githubIssueBody = baseIssueBody + "\n\nAdd some context about what you were doing when this error occurred!\n\nStacktrace (don't modify):\n```java\n" + exeptText + "```";
        final JFrame errorFrame = new JFrame();
        errorFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        errorFrame.setVisible(false);

        try {
            final JTextPane textPane = new JTextPane();
            textPane.setEditable(false);
            textPane.setBorder(null);
            textPane.setContentType("text/html");
            textPane.setText("<html><p>" + escapeHtml4(dialogTitle).replace("\n", "<br>") + "</p>" +
                             "<br><p>" + escapeHtml4(issueTitle).replace("\n", "<br>") + "</p>" +
                             "<br><p>" + escapeHtml4(displayIssueBody).replace("\n", "<br>") + "</p>" +
                             "<p>Please report this issue on GitHub (the link autofills this information for you):</p>" +
                             "<a href=\"https://github.com/NeRdTheNed/RetroWrapper/issues/new?title=" + URLEncoder.encode(githubIssueTitle, StandardCharsets.UTF_8.name()) + "&body=" + URLEncoder.encode(githubIssueBody, StandardCharsets.UTF_8.name()) + "\">Create an issue on Github!</a>"
                            );
            textPane.addHyperlinkListener(new HyperlinkListener() {
                @Override
                public void hyperlinkUpdate(HyperlinkEvent event) {
                    if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        try {
                            Desktop.getDesktop().browse(event.getURL().toURI());
                        } catch (final Exception ignored) {
                            installerLogger.log(Level.WARNING, "Could not open link from hyperlinkUpdate", ignored);
                            JOptionPane.showMessageDialog(textPane, "Your platform doesn't let Java open links.\nPlease browse to https://github.com/NeRdTheNed/RetroWrapper/issues/new to create an issue.\nPlease copy-paste the error report into the issue.\nThanks for putting up with this!", "Sorry", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            });
            final JScrollPane jsp = new JScrollPane(textPane);
            jsp.setBorder(null);
            JOptionPane.showMessageDialog(errorFrame, jsp, dialogTitle, JOptionPane.ERROR_MESSAGE);
        } catch (final Exception ignored) {
            installerLogger.log(Level.WARNING, "An Exception was thrown while trying to display the exception handler", ignored);
            JOptionPane.showMessageDialog(errorFrame, dialogTitle + "\n" + issueTitle + "\n" + displayIssueBody + "\nPlease report this issue on GitHub!\nhttps://github.com/NeRdTheNed/RetroWrapper/issues/new\nPlease take a screenshot of this message for the issue.", "(Backup handler) " + dialogTitle, JOptionPane.ERROR_MESSAGE);
        }

        errorFrame.dispose();
    }
}
