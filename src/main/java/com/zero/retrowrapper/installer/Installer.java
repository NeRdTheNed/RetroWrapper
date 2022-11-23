package com.zero.retrowrapper.installer;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.PrettyPrint;
import com.zero.retrowrapper.util.FileUtil;
import com.zero.retrowrapper.util.MetadataUtil;
import com.zero.retrowrapper.util.SwingUtil;

public final class Installer {

    private static final Random rand = new Random();

    // TODO Some of these variables should possibly be refactored to not be static
    static String workingDirectory;
    static File directory;
    static File[] directories;
    static File versions;
    static JButton install;
    private static JButton uninstall;

    static DefaultListModel model;
    static JList list;
    static List<String> listInternal;

    private static JFrame frame;

    static boolean refreshList(String givenDirectory, final Logger installerLogger) {
        int versionCount = 0;
        int wrappedVersionCount = 0;
        int outdatedVersionsCount = 0;
        list.clearSelection();
        model.removeAllElements();
        listInternal.clear();
        final List<Pair<String, Integer>> outdatedVersionsList = new ArrayList<Pair<String, Integer>>();

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
                for (final File f : directories) {
                    if (f.isDirectory()) {
                        final File json = new File(f, f.getName() + ".json");
                        final File jar = new File(f, f.getName() + ".jar");

                        if (json.exists() && jar.exists() && !f.getName().contains("-wrapped")) {
                            final JsonObject versionJson = getVersionJson(f.getName(), installerLogger);

                            if (versionJson.getString("type", "").contains("old_") && (getRetroWrapperVersionFromInstance(versionJson, installerLogger) == null)) {
                                if (new File(versions, f.getName() + "-wrapped").exists()) {
                                    wrappedVersionCount++;
                                    final String verNotif;
                                    final JsonObject versionJsonWrapped = getVersionJson(f.getName() + "-wrapped", installerLogger);
                                    boolean outdated = false;

                                    if (versionJsonWrapped != null) {
                                        final String oldVersion = getRetroWrapperVersionFromInstance(versionJsonWrapped, installerLogger);

                                        if (!"installer".equals(oldVersion)) {
                                            String tempVerNotif;

                                            try {
                                                if (MetadataUtil.compareSemver(MetadataUtil.VERSION, oldVersion) > 0) {
                                                    outdated = true;
                                                    tempVerNotif = "(outdated RetroWrapper version " + oldVersion + "!)";
                                                } else if (!MetadataUtil.VERSION.equals(oldVersion) && MetadataUtil.isVersionSnapshot(oldVersion)) {
                                                    outdated = true;
                                                    tempVerNotif = "(possibly outdated RetroWrapper version " + oldVersion + "!)";
                                                } else {
                                                    tempVerNotif = "(RetroWrapper version " + oldVersion + ")";
                                                }
                                            } catch (final NumberFormatException e) {
                                                outdated = true;
                                                installerLogger.log(Level.WARNING, "Issue parsing version number for version " + f.getPath(), e);
                                                tempVerNotif = "(outdated RetroWrapper version " + oldVersion + "!)";
                                            }

                                            verNotif = tempVerNotif;
                                        } else {
                                            outdated = true;
                                            verNotif = "(outdated RetroWrapper version 1.6.4 or earlier!)";
                                        }
                                    } else {
                                        outdated = true;
                                        installerLogger.warning("Could not parse JSON file for instance " + f.getName() + "-wrapped");
                                        verNotif = "error parsing JSON file for wrapped instance?";
                                    }

                                    final String versionName = f.getName() + " - " + verNotif + " already wrapped";
                                    model.addElement(versionName);

                                    if (outdated) {
                                        outdatedVersionsCount++;
                                        outdatedVersionsList.add(Pair.of(versionName, model.size() - 1));
                                    }
                                } else {
                                    versionCount++;
                                    model.addElement(f.getName());
                                }

                                listInternal.add(f.getName());
                            }
                        }
                    }
                }
            }
        }

        // button visibility
        install.setEnabled(false);
        install.setText("Install");
        uninstall.setEnabled(wrappedVersionCount > 0);

        if (givenDirectory.length() == 0) {
            JOptionPane.showMessageDialog(null, "No directory / minecraft directory detected!", "Error", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        if (!versions.exists()) {
            JOptionPane.showMessageDialog(null, "No Minecraft versions folder found!", "Error", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        if ((versionCount == 0) && (wrappedVersionCount == 0)) {
            JOptionPane.showMessageDialog(null, "No wrappable versions found!", "Error", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        if (outdatedVersionsCount != 0) {
            final String[] versions = new String[outdatedVersionsList.size()];
            final int[] versionIndex = new int[outdatedVersionsList.size()];

            for (int i = 0; i < outdatedVersionsList.size(); ++i) {
                final Pair<String, Integer> pair = outdatedVersionsList.get(i);
                versions[i] = pair.getLeft();
                versionIndex[i] = pair.getRight();
            }

            final int diagRes = SwingUtil.showOptionScroller(JOptionPane.YES_NO_OPTION, "Info", versions, "Some instances use an outdated version of RetroWrapper!", "Would you like to update RetroWrapper for these instance?");

            if (diagRes == JOptionPane.YES_OPTION) {
                wrapInstances(installerLogger, versionIndex, versions);
            }
        }

        if ((versionCount == 0) && (outdatedVersionsCount == 0)) {
            JOptionPane.showMessageDialog(null, "All wrapped versions are up to date!", "Info", JOptionPane.INFORMATION_MESSAGE);
        }

        return true;
    }

    static JsonObject getVersionJson(String version, final Logger installerLogger) {
        JsonObject versionJson = null;
        Reader s = null;

        try {
            s = new FileReader(new File(versions, version + File.separator + version + ".json"));
            versionJson = Json.parse(s).asObject();
        } catch (final IOException ee) {
            // TODO better logging
            final LogRecord logRecord = new LogRecord(Level.SEVERE, "An IOException was thrown while trying to wrap version {0}");
            logRecord.setParameters(new Object[] { version });
            logRecord.setThrown(ee);
            installerLogger.log(logRecord);
        } finally {
            IOUtils.closeQuietly(s);
        }

        return versionJson;
    }

    // May return "installer" for older versions
    private static String getRetroWrapperVersionFromInstance(JsonObject versionJson, final Logger installerLogger) {
        String toReturn = null;
        final JsonArray libraries = versionJson.get("libraries").asArray();

        for (final JsonValue jsonValue : libraries) {
            final JsonObject library = jsonValue.asObject();
            final String libName = library.get("name").asString();

            if (libName.contains("com.zero:retrowrapper")) {
                final String[] libNameSplit = libName.split(":");
                toReturn = libNameSplit[libNameSplit.length - 1];
            }
        }

        return toReturn;
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

        try {
            final File defaultMinecraftDir = new File(FileUtil.defaultMinecraftDirectory());
            final File defaultCacheDir = new File(defaultMinecraftDir, "retrowrapper/cache");
            final File currentCacheDir = new File(workingDirectory, "retrowrapper/cache");

            if (defaultMinecraftDir.exists()) {
                defaultCacheDir.mkdirs();
                SwingUtil.checkAndDisplayUpdate(defaultCacheDir);
            } else {
                currentCacheDir.mkdirs();
                SwingUtil.checkAndDisplayUpdate(currentCacheDir);
            }
        } catch (final Exception e) {
            installerLogger.log(Level.WARNING, "Update check failed!", e);
        }

        model = new DefaultListModel();
        list = new JList(model);
        listInternal = new ArrayList<String>();
        frame = new JFrame("RetroWrapper - NeRd Fork");
        frame.setPreferredSize(new Dimension(654, 420));
        frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.PAGE_AXIS));
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setResizable(true);
        // Installer label
        final JLabel installerLabel = new JLabel("RetroWrapper Installer");
        installerLabel.setFont(installerLabel.getFont().deriveFont(20.0F).deriveFont(Font.BOLD));
        SwingUtil.addJLabelCentered(frame, installerLabel);
        // Version label
        final JLabel versionLabel = new JLabel(MetadataUtil.VERSION + " - " + MetadataUtil.INSTALLER_SPLASHES.get(rand.nextInt(MetadataUtil.INSTALLER_SPLASHES.size())));
        versionLabel.setFont(installerLabel.getFont().deriveFont(12.0F));
        SwingUtil.addJLabelCentered(frame, versionLabel);
        // Working directory text field
        final JTextField workDir = new JTextField(workingDirectory);
        workDir.setMaximumSize(new Dimension(300, 20));
        // TODO Refactor
        workDir.addActionListener(new SelectMinecraftDirectoryActionListener(installerLogger));
        SwingUtil.addJTextFieldCentered(frame, workDir);
        // List of versions that can be wrapper
        final JScrollPane scrollList = new JScrollPane(list);
        list.addListSelectionListener(new VersionSelectionListener());
        SwingUtil.addJComponentCentered(frame, scrollList);
        // Install button
        install = new JButton("Install"); //installation code
        // TODO Refactor
        install.addActionListener(new WrapInstanceListener(installerLogger));
        SwingUtil.addJButtonCentered(frame, install);
        // Uninstall button
        uninstall = new JButton("Uninstall ALL versions"); //uninstaller code
        // TODO Refactor
        uninstall.addActionListener(new UninstallListener(installerLogger));
        SwingUtil.addJButtonCentered(frame, uninstall);
        // Copyright label
        final JLabel copyrightLabel = new JLabel("©2018 000");
        copyrightLabel.setFont(copyrightLabel.getFont().deriveFont(12.0F));
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
            keyboardFocusManager.addKeyEventDispatcher(new DebugKeyDispatcher(logger));
        } catch (final Exception ignored) {
            logger.log(Level.WARNING, "Could not add DebugKeyDispatcher, debug key combinations will not work", ignored);
        }
    }

    static void wrapInstances(Logger installerLogger, int[] mapInd, String... versionsToWrap) {
        int rewrappedVersions = 0;
        final List<String> finalVersions = new ArrayList<String>();

        for (int i = 0; i < versionsToWrap.length; ++i) {
            String version = versionsToWrap[i];

            if (version.contains("already wrapped")) {
                rewrappedVersions++;
                version = listInternal.get(mapInd[i]);
                FileUtils.deleteQuietly(new File(directory, "versions" + File.separator + version + "-wrapped"));
            }

            try {
                finalVersions.add(version);
                final JsonObject versionJson = getVersionJson(version, installerLogger);

                if (versionJson != null) {
                    final String versionWrapped = version + "-wrapped";
                    // RetroWrapper adds and changes a few libraries.
                    // A library is a JSON object, and libraries are stored in an array of JSON objects.
                    final JsonArray libraries = versionJson.get("libraries").asArray();
                    final JsonArray newLibraries = Json.array();
                    final Iterator<JsonValue> libraryIterator = libraries.iterator();
                    // If the version already has Log4j API
                    boolean hasLogAPI = false;
                    // If the version already has Log4j core
                    boolean hasLogCore = false;

                    while (libraryIterator.hasNext()) {
                        final JsonObject library = libraryIterator.next().asObject();
                        JsonObject toAdd = library;
                        final String libName = library.get("name").asString();

                        if (libName.contains("net.minecraft:launchwrapper")) {
                            final String[] libNameSplit = libName.split(":");
                            final String libVersion = libNameSplit[libNameSplit.length - 1];

                            if ("1.5".equals(libVersion)) {
                                // Replace LaunchWrapper 1.5 with LaunchWrapper 1.12
                                toAdd = Json.object().add("name", "net.minecraft:launchwrapper:1.12")
                                        .add("downloads", Json.object()
                                             .add("artifact", Json.object()
                                                  .add("path", "net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar")
                                                  .add("url", "https://libraries.minecraft.net/net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar")
                                                  .add("sha1", "111e7bea9c968cdb3d06ef4632bf7ff0824d0f36")
                                                  .add("size", 32999)
                                                 )
                                            );
                            }
                        } else if (libName.contains("org.apache.logging.log4j:log4j-api")) {
                            hasLogAPI = true;
                        } else if (libName.contains("org.apache.logging.log4j:log4j-core")) {
                            hasLogCore = true;
                        }

                        newLibraries.add(toAdd);
                    }

                    if (!hasLogAPI) {
                        // Add Log4j API, LaunchWrapper 1.12 requires it.
                        // 2.3.2 is the latest (RCE patched) version to support Java 6.
                        final JsonObject log4jAPI = Json.object().add("name", "org.apache.logging.log4j:log4j-api:2.3.2")
                                                    .add("downloads", Json.object()
                                                         .add("artifact", Json.object()
                                                              .add("path", "org/apache/logging/log4j/log4j-api/2.3.2/log4j-api-2.3.2.jar")
                                                              .add("url", "https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-api/2.3.2/log4j-api-2.3.2.jar")
                                                              .add("sha1", "294fb56d66693b6f97f1a2f2c7acfb101859388a")
                                                              .add("size", 134942)
                                                             )
                                                        );
                        newLibraries.add(log4jAPI);
                    }

                    if (!hasLogCore) {
                        // Add Log4j core, LaunchWrapper 1.12 requires it.
                        // 2.3.2 is the latest (RCE patched) version to support Java 6.
                        final JsonObject log4jCore = Json.object().add("name", "org.apache.logging.log4j:log4j-core:2.3.2")
                                                     .add("downloads", Json.object()
                                                          .add("artifact", Json.object()
                                                               .add("path", "org/apache/logging/log4j/log4j-core/2.3.2/log4j-core-2.3.2.jar")
                                                               .add("url", "https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-core/2.3.2/log4j-core-2.3.2.jar")
                                                               .add("sha1", "fcd866619df2b131be0defc4f63b09b703649031")
                                                               .add("size", 833136)
                                                              )
                                                         );
                        newLibraries.add(log4jCore);
                    }

                    final String retroWrapperLibraryLocation = "com" + File.separator + "zero" + File.separator + "retrowrapper" + File.separator + MetadataUtil.VERSION;
                    final File jar = new File(Installer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                    final JsonObject retroWrapperArtifact = Json.object()
                                                            .add("path", retroWrapperLibraryLocation)
                                                            .add("size", jar.length());
                    InputStream input = null;

                    try {
                        input = new FileInputStream(jar);
                        final String hash = DigestUtils.shaHex(input);
                        retroWrapperArtifact.add("sha1", hash);
                    } finally {
                        IOUtils.closeQuietly(input);
                    }

                    if (MetadataUtil.IS_RELEASE) {
                        retroWrapperArtifact.add("url", "https://github.com/NeRdTheNed/RetroWrapper/releases/download/" + MetadataUtil.TAG + "/RetroWrapper-" + MetadataUtil.VERSION + ".jar");
                    }

                    // Add the RetroWrapper library to the list of libraries.
                    final JsonObject retrowrapperLibraryJson = Json.object().add("name", "com.zero:retrowrapper:" + MetadataUtil.VERSION);

                    if (!MetadataUtil.VERSION.endsWith(".local")) {
                        retrowrapperLibraryJson.add("downloads", Json.object().add("artifact", retroWrapperArtifact));
                    }

                    newLibraries.add(retrowrapperLibraryJson);
                    versionJson.set("libraries", newLibraries);

                    // Replace version ID with the wrapped version ID (e.g "c0.30-3" with "c0.30-3-wrapped")
                    if (!versionJson.getString("id", "null").equals(version)) {
                        JOptionPane.showMessageDialog(null, "The version ID " + versionJson.getString("id", "null") + " found in the JSON file " + version + File.separator + version + ".json did not match the expected version ID " + version + ". Things will not go as planned!", "Error", JOptionPane.ERROR_MESSAGE);
                    }

                    versionJson.set("id",  versionWrapped);
                    // Replace any of Mojang's tweakers with RetroWrapper tweakers
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
                    final File libDir = new File(directory, "libraries" + File.separator + retroWrapperLibraryLocation);
                    libDir.mkdirs();
                    FileOutputStream fos = null;

                    try {
                        fos = new FileOutputStream(new File(wrapDir, versionWrapped + ".json"));
                        FileUtils.copyFile(new File(versions, version + File.separator + version + ".jar"), new File(wrapDir, versionWrapped + ".jar"));
                        fos.write(versionJson.toString(PrettyPrint.indentWithSpaces(4)).getBytes());
                        final File libFile = new File(libDir, "retrowrapper-" + MetadataUtil.VERSION + ".jar");

                        if (libFile.isFile()) {
                            libFile.delete();
                        }

                        FileUtils.copyFile(jar, libFile);
                    } catch (final IOException ee) {
                        // TODO better logging
                        final LogRecord logRecord = new LogRecord(Level.SEVERE, "An IOException was thrown while trying to wrap version {0}");
                        logRecord.setParameters(new Object[] { version });
                        logRecord.setThrown(ee);
                        installerLogger.log(logRecord);
                    } finally {
                        IOUtils.closeQuietly(fos);
                    }
                } else {
                    installerLogger.severe("Error when getting version JSON!");
                }
            } catch (final IOException ee) {
                // TODO better logging
                final LogRecord logRecord = new LogRecord(Level.SEVERE, "An IOException was thrown while trying to wrap version {0}");
                logRecord.setParameters(new Object[] { version });
                logRecord.setThrown(ee);
                installerLogger.log(logRecord);
            } catch (final URISyntaxException e1) {
                // TODO better logging
                final LogRecord logRecord = new LogRecord(Level.SEVERE, "An URISyntaxException was thrown while trying to wrap version {0}");
                logRecord.setParameters(new Object[] { version });
                logRecord.setThrown(e1);
                installerLogger.log(logRecord);
            }
        }

        final String[] wrappedVersions = finalVersions.toArray(new String[0]);
        final String[] listDiag = (rewrappedVersions > 0) ? new String[] {
                                      "Please restart the Minecraft Launcher to refresh re-wrapped versions and instances!",
                                      (wrappedVersions.length > 1 ? "Successfully wrapped versions" : "Successfully wrapped version")
                                  } : new String[] {
                                      (wrappedVersions.length > 1 ? "Successfully wrapped versions" : "Successfully wrapped version")
                                  };
        SwingUtil.showMessageScroller(JOptionPane.INFORMATION_MESSAGE, "Success", wrappedVersions, listDiag);
        refreshList(workingDirectory, installerLogger);
    }

    private static class UninstallListener implements ActionListener {
        private final Logger installerLogger;

        public UninstallListener(Logger installerLogger) {
            this.installerLogger = installerLogger;
        }

        public void actionPerformed(ActionEvent e) {
            for (final File f : directories) {
                if (f.isDirectory() && f.getName().contains("-wrapped")) {
                    FileUtils.deleteQuietly(f);
                }
            }

            JOptionPane.showMessageDialog(null, "Successfully uninstalled wrapper", "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshList(workingDirectory, installerLogger);
        }
    }

    private static class SelectMinecraftDirectoryActionListener implements ActionListener {
        private final Logger installerLogger;

        public SelectMinecraftDirectoryActionListener(Logger installerLogger) {
            this.installerLogger = installerLogger;
        }

        public void actionPerformed(ActionEvent e) {
            final String workDirPath = ((JTextComponent) e.getSource()).getText();
            final File minecraftDir = new File(workDirPath);

            if (minecraftDir.exists() && refreshList(workDirPath, installerLogger)) {
                workingDirectory = workDirPath;
            } else {
                if (!minecraftDir.exists()) {
                    JOptionPane.showMessageDialog(null, "No directory / minecraft directory detected!\n", "Error", JOptionPane.INFORMATION_MESSAGE);
                }

                ((JTextComponent) e.getSource()).setText(workingDirectory);
                refreshList(workingDirectory, installerLogger);
            }
        }
    }

    private static class WrapInstanceListener implements ActionListener {
        private final Logger installerLogger;

        public WrapInstanceListener(Logger installerLogger) {
            this.installerLogger = installerLogger;
        }

        public void actionPerformed(ActionEvent e) {
            final Object[] versionListO = list.getSelectedValues();
            final String[] versionList = new String[versionListO.length];

            for (int i = 0; i < versionListO.length; i++) {
                final String tempVer = (String)versionListO[i];
                versionList[i] = tempVer;
            }

            wrapInstances(installerLogger, list.getSelectedIndices(), versionList);
        }
    }

    private static class VersionSelectionListener implements ListSelectionListener {
        public VersionSelectionListener() {
            // Empty constructor
        }

        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                final boolean areVersionsSelected = !list.isSelectionEmpty();
                install.setEnabled(areVersionsSelected);

                if (areVersionsSelected) {
                    final Object[] versionList = list.getSelectedValues();
                    String text = "Install";

                    for (final Object version : versionList) {
                        if (((String)version).contains("already wrapped")) {
                            text = "Re-install";
                            break;
                        }
                    }

                    install.setText(text);
                }
            }
        }
    }

    private static class DebugKeyDispatcher implements KeyEventDispatcher {
        private final Logger logger;
        boolean f3Pressed;
        boolean cPressed;
        boolean hPressed;
        boolean qPressed;
        boolean tPressed;
        boolean openDebug;

        public DebugKeyDispatcher(Logger logger) {
            this.logger = logger;
            f3Pressed = false;
            cPressed = false;
            hPressed = false;
            qPressed = false;
            tPressed = false;
            openDebug = false;
        }

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
                    tempDirBuilder.append("\n").append(tempDir);
                }

                final StringBuilder wrappableBuilder = new StringBuilder();

                for (final Object wrappable : model.toArray()) {
                    wrappableBuilder.append("\n").append(wrappable);
                }

                final StringBuilder selectedBuilder = new StringBuilder();

                for (final Object selectedO : list.getSelectedValues()) {
                    final String selected = (String)selectedO;
                    selectedBuilder.append("\n").append(selected);
                }

                final String toShow = "\nWorking directory " + workingDirectory +
                                      "\nDirectory " + directory +
                                      "\nVersions folder " + versions +
                                      "\nCurrently selected Minecraft versions in list " + selectedBuilder +
                                      "\nWrappable versions of Minecraft in versions folder " + wrappableBuilder +
                                      "\nAll versions of Minecraft in versions folder " + tempDirBuilder;
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
    }
}
