package com.zero.retrowrapper.installer;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.util.Arrays;
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

import org.apache.commons.io.FileUtils;

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

    private static DefaultListModel model;
    private static JList list;

    private static JFrame frame;

    private static boolean refreshList(String givenDirectory, final Logger installerLogger) {
        int versionCount = 0;
        int wrappedVersionCount = 0;
        model.removeAllElements();

        if (givenDirectory.length() != 0) {
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
                            Scanner s = null;

                            try {
                                s = new Scanner(json).useDelimiter("\\A");
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
                            } finally {
                                if (s != null) {
                                    s.close();
                                }
                            }
                        }
                    }
                }
            }
        }

        // button visibility

        if (givenDirectory.length() == 0) {
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

        model = new DefaultListModel();
        list = new JList(model);
        frame = new JFrame("RetroWrapper - NeRd Fork");
        frame.setPreferredSize(new Dimension(654, 420));
        frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setResizable(true);
        // Installer label
        final JLabel installerLabel = new JLabel("RetroWrapper Installer");
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
            public void actionPerformed(ActionEvent e) {
                final Object[] versionListO = list.getSelectedValues();
                final String[] versionList = new String[versionListO.length];

                for (int i = 0; i < versionListO.length; i++) {
                    final String tempVer = (String)versionListO[i];
                    versionList[i] = tempVer;
                }

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

                    Reader s = null;

                    try {
                        s = new FileReader(new File(versions, version + File.separator + version + ".json"));
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
                        FileOutputStream fos = null;

                        try {
                            fos = new FileOutputStream(new File(wrapDir, versionWrapped + ".json"));
                            FileUtils.copyFile(new File(versions, version + File.separator + version + ".jar"), new File(wrapDir, versionWrapped + ".jar"));
                            fos.write(versionJson.toString().getBytes());
                            final File jar = new File(Installer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                            FileUtils.copyFile(jar, new File(libDir, "retrowrapper-installer.jar"));
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
                        } finally {
                            if (fos != null) {
                                try {
                                    fos.close();
                                } catch (final IOException ee) {
                                    // TODO Better error handling
                                    ee.printStackTrace();
                                }
                            }
                        }
                    } catch (final IOException ee) {
                        // TODO better logging
                        final LogRecord logRecord = new LogRecord(Level.SEVERE, "An IOException was thrown while trying to wrap version {0}");
                        logRecord.setParameters(new Object[] { version });
                        logRecord.setThrown(ee);
                        installerLogger.log(logRecord);
                    } finally {
                        if (s != null) {
                            try {
                                s.close();
                            } catch (final IOException ee) {
                                // TODO Better error handling
                                ee.printStackTrace();
                            }
                        }
                    }
                }

                JOptionPane.showMessageDialog(null, (versionList.length > 1 ? "Successfully wrapped versions\n" : "Successfully wrapped version\n") + finalVersions.toString(), "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshList(workingDirectory, installerLogger);
            }
        });
        SwingUtil.addJButtonCentered(frame, install);
        // Uninstall button
        uninstall = new JButton("Uninstall ALL versions"); //uninstaller code
        // TODO Refactor
        uninstall.addActionListener(new ActionListener() {
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
        installerLogger.log(Level.INFO, "Logger initialized.");
        SwingUtil.setupMacOSProperties(installerLogger, "RetroWrapper Installer");
        setupDebugKeyCombos(installerLogger);
        installerLogger.log(Level.INFO, "Starting RetroWrapper installer");

        try {
            new Installer(installerLogger);
        } catch (final Exception e) {
            if (frame != null) {
                frame.dispose();
            }

            final String context = "An Exception was thrown while running the RetroWrapper installer";
            installerLogger.log(Level.SEVERE, context, e);
            SwingUtil.showExceptionHandler(installerLogger, context, e);
            // Swing doesn't want the JVM to exit and I can't figure out why :(
            System.exit(1);
        }
    }

    private static void setupDebugKeyCombos(final Logger logger) {
        try {
            final KeyboardFocusManager keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            keyboardFocusManager.addKeyEventDispatcher(new KeyEventDispatcher() {
                boolean f3Pressed = false;
                boolean cPressed = false;
                boolean hPressed = false;
                boolean qPressed = false;
                boolean tPressed = false;
                boolean openDebug = false;
                public boolean dispatchKeyEvent(KeyEvent e) {
                    switch (e.getID()) {
                    case KeyEvent.KEY_PRESSED:
                        switch (e.getKeyCode()) {
                        case KeyEvent.VK_C:
                            cPressed = true;
                            break;

                        case KeyEvent.VK_H:
                            hPressed = true;
                            break;

                        case KeyEvent.VK_Q:
                            qPressed = true;
                            break;

                        case KeyEvent.VK_T:
                            tPressed = true;
                            break;

                        case KeyEvent.VK_F3:
                            f3Pressed = true;
                            break;

                        default:
                            break;
                        }

                        break;

                    case KeyEvent.KEY_RELEASED:
                        switch (e.getKeyCode()) {
                        case KeyEvent.VK_C:
                            cPressed = false;
                            openDebug = false;
                            break;

                        case KeyEvent.VK_H:
                            hPressed = false;
                            openDebug = false;
                            break;

                        case KeyEvent.VK_Q:
                            qPressed = false;
                            openDebug = false;
                            break;

                        case KeyEvent.VK_T:
                            tPressed = false;
                            openDebug = false;
                            break;

                        case KeyEvent.VK_F3:
                            f3Pressed = false;
                            openDebug = false;
                            break;

                        default:
                            break;
                        }

                        break;

                    default:
                        break;
                    }

                    if (f3Pressed && cPressed && !openDebug) {
                        openDebug = true;
                        JOptionPane.showMessageDialog(null, "Debug F3 + C: Test exception handler (not a real crash)", "Debug", JOptionPane.QUESTION_MESSAGE);
                        SwingUtil.showExceptionHandler(logger, "Debug exception handler test (not a real crash)", new Exception("Debug exception (not a real exception)"));
                    }

                    if (f3Pressed && hPressed && !openDebug) {
                        openDebug = true;
                        final StringBuilder tempDirBuilder = new StringBuilder();

                        for (final File tempDir : directories) {
                            tempDirBuilder.append("\n" + tempDir);
                        }

                        final StringBuilder wrappableBuilder = new StringBuilder();

                        for (final Object wrappable : model.toArray()) {
                            wrappableBuilder.append("\n" + wrappable);
                        }

                        final StringBuilder selectedBuilder = new StringBuilder();

                        for (final Object selectedO : list.getSelectedValues()) {
                            final String selected = (String)selectedO;
                            selectedBuilder.append("\n" + selected);
                        }

                        final String toShow = "\nWorking directory " + workingDirectory +
                                              "\nDirectory " + directory +
                                              "\nVersions folder " + versions +
                                              "\nCurrently selected Minecraft versions in list " + selectedBuilder.toString() +
                                              "\nWrappable versions of Minecraft in versions folder " + wrappableBuilder.toString() +
                                              "\nAll verions of Minecraft in versions folder " + tempDirBuilder.toString();
                        final JTextPane textPane = new JTextPane();
                        textPane.setText("Debug F3 + H: Show variable info: " + toShow);
                        textPane.setCaretPosition(0);
                        final JScrollPane jsp = new JScrollPane(textPane);
                        jsp.setPreferredSize(new Dimension(654, 420));
                        JOptionPane.showMessageDialog(null, jsp, "Debug", JOptionPane.QUESTION_MESSAGE);
                    }

                    if (f3Pressed && qPressed && !openDebug) {
                        openDebug = true;
                        JOptionPane.showMessageDialog(null, "Debug F3 + Q: Show key combos. Combos:\nF3 + C: Test exception handler (not a real crash)\nF3 + H: Show variable info\nF3 + Q: Show key combos\nF3 + T: Reload folders", "Debug", JOptionPane.QUESTION_MESSAGE);
                    }

                    if (f3Pressed && tPressed && !openDebug) {
                        openDebug = true;
                        JOptionPane.showMessageDialog(null, "Debug F3 + T: Reloading folders", "Debug", JOptionPane.QUESTION_MESSAGE);
                        refreshList(workingDirectory, logger);
                    }

                    return false;
                }
            });
        } catch (final Exception ignored) {
            logger.log(Level.WARNING, "Could not add KeyEventDispatcher, debug key combinations will not work", ignored);
        }
    }

}
