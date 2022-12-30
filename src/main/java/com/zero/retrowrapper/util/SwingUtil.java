package com.zero.retrowrapper.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.lwjgl.opengl.Display;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

import net.minecraft.launchwrapper.LogWrapper;

public final class SwingUtil {
    private static final Pattern newlinePattern = Pattern.compile("\n", Pattern.LITERAL);

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
        final Collection<File> iconList = new ArrayList<File>();
        CollectionUtil.addNonNullToCollection(iconList, FileUtil.tryFindResourceFile("icons" + File.separator + "icon_16x16.png"), FileUtil.tryFindResourceFile("icons" + File.separator + "icon_32x32.png"));

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
            final Frame[] frames = Frame.getFrames();

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
                        } catch (final Exception e) {
                            LogWrapper.warning("Are you running RetroWrapper on Java 5?: " + ExceptionUtils.getStackTrace(e));
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
        } catch (final Exception e) {
            logger.log(Level.WARNING, "An Exception was thrown while trying to set system properties", e);
        }
    }

    // TODO This is very inefficient, but it's only called if the exception handler is shown.
    private static String escapeHtml(String input) {
        return input.replace("&", "&amp;").replace("\"", "&quot;").replace("'", "&#39;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static void showExceptionHandler(final Logger logger, final String context, final Exception toShow) {
        final String exceptText = ExceptionUtils.getStackTrace(toShow);
        logger.log(Level.FINE, "Displaying exception handler with context \"{0}\" and stacktrace \"{1}\"", new Object[] {context, exceptText});
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
                             "<p><i>Bad things, man.... bad things.</i></p><br>" +
                             "<p>Please report this issue on GitHub (the link will autofill this information for you):</p><br>" +
                             "<a href=\"https://github.com/NeRdTheNed/RetroWrapper/issues/new?title=" + URLEncoder.encode(githubIssueTitle, "UTF-8") + "&body=" + URLEncoder.encode(githubIssueBody, "UTF-8") + "\">Create an issue on Github!</a><br>" +
                             "<p>" + newlinePattern.matcher(escapeHtml(dialogTitle)).replaceAll("<br>") + "</p><br>" +
                             "<br><p>" + newlinePattern.matcher(escapeHtml(issueTitle)).replaceAll("<br>") + "</p><br>" +
                             "<br><p>" + newlinePattern.matcher(escapeHtml(displayIssueBody)).replaceAll("<br>") + "</p><br>" +
                             "</html>");
            textPane.addHyperlinkListener(new NavigateToHyperlinkListener(logger, textPane));
            textPane.setCaretPosition(0);
            final JScrollPane jsp = new JScrollPane(textPane);
            final int pwidth = jsp.getPreferredSize().width;
            jsp.setPreferredSize(new Dimension(Math.max(pwidth, 720), 420));
            jsp.setBorder(null);
            JOptionPane.showMessageDialog(errorFrame, jsp, dialogTitle, JOptionPane.ERROR_MESSAGE);
        } catch (final Exception e) {
            logger.log(Level.WARNING, "An Exception was thrown while trying to display the exception handler", e);
            JOptionPane.showMessageDialog(errorFrame, "Please report this issue on GitHub!\nhttps://github.com/NeRdTheNed/RetroWrapper/issues/new\nPlease take a screenshot of this message for the issue.\n" + dialogTitle + "\n" + issueTitle + "\n" + displayIssueBody, "(Backup handler) " + dialogTitle, JOptionPane.ERROR_MESSAGE);
        }

        errorFrame.dispose();
    }

    private static String getReleaseTagFromGithubJson(JsonValue json) {
        if ((json != null) && json.isObject()) {
            final JsonValue tag = json.asObject().get("tag_name");

            if (tag.isString()) {
                return tag.asString();
            }
        }

        return null;
    }

    public static void checkAndDisplayUpdate(File cacheDirectory) throws IOException {
        // Check for a new release, and inform the user if there is one
        final Logger tempLogger = Logger.getLogger(SwingUtil.class.getName());

        if (MetadataUtil.IS_RELEASE) {
            InputStream rateAPIStream = null;
            int remainingRateLimit = 0;

            try {
                // Try to check the current remaining requests we can make.
                final URLConnection urlConnection = new URL("https://api.github.com/rate_limit").openConnection();
                // https://docs.github.com/en/rest/overview/resources-in-the-rest-api#user-agent-required
                urlConnection.setRequestProperty("User-Agent", "RetroWrapper-NeRd-Fork");
                final HttpURLConnection httpConnection = urlConnection instanceof HttpURLConnection ? (HttpURLConnection) urlConnection : null;
                //httpConnection.setRequestMethod("HEAD"); // Not supported?
                rateAPIStream = urlConnection.getInputStream();
                final String ratelimit = urlConnection.getHeaderField("x-ratelimit-remaining");

                if (ratelimit != null) {
                    try {
                        remainingRateLimit = Integer.parseInt(ratelimit);
                    } catch (final Exception e) {
                        tempLogger.log(Level.WARNING, "Unknown error when trying to parse the vaule of the remaining rate limit", e);
                    }
                }

                if (httpConnection != null) {
                    httpConnection.disconnect();
                }
            } catch (final Exception e) {
                try {
                    final Class<?> logWrapper = Class.forName("net.minecraft.launchwrapper.LogWrapper");
                    final Method warning = logWrapper.getMethod("warning", String.class, Object[].class);
                    warning.invoke(null, "Could not connect to rate limit API: " + ExceptionUtils.getStackTrace(e), new Object[0]);
                } catch (final Exception ignored) {
                    // LaunchWrapper isn't available
                    tempLogger.log(Level.WARNING, "Could not connect to rate limit API", e);
                }
            } finally {
                IOUtils.closeQuietly(rateAPIStream);
            }

            tempLogger.log(Level.FINE, "GitHub rate limit remaining {0}", Integer.valueOf(remainingRateLimit));

            if (remainingRateLimit != 0) {
                cacheDirectory.mkdirs();
                // Cached version response from GitHub
                final File cachedGithubResponseFile = new File(cacheDirectory, "versioncheck.json");
                // Cached ETag for the version response from GitHub
                final File eTagFile = new File(cacheDirectory, "versionchecketag.txt");
                // The returned latest release tag
                String latestRelease = null;
                InputStream connectionInputStream = null;

                try {
                    final URLConnection urlConnection = new URL("https://api.github.com/repos/NeRdTheNed/RetroWrapper/releases/latest").openConnection();
                    // https://docs.github.com/en/rest/overview/resources-in-the-rest-api#user-agent-required
                    urlConnection.setRequestProperty("User-Agent", "RetroWrapper-NeRd-Fork");
                    HttpURLConnection httpConnection = null;

                    if (urlConnection instanceof HttpURLConnection) {
                        httpConnection = (HttpURLConnection) urlConnection;
                    }

                    if (cachedGithubResponseFile.isFile()) {
                        final JsonValue json = FileUtil.readFileAsJsonOrNull(cachedGithubResponseFile);
                        latestRelease = getReleaseTagFromGithubJson(json);

                        if (latestRelease == null) {
                            // Invalid version file
                            cachedGithubResponseFile.delete();

                            if (eTagFile.isFile()) {
                                eTagFile.delete();
                            }
                        } else if (eTagFile.isFile()) {
                            final String etag = FileUtils.readFileToString(eTagFile);
                            // Use the cached ETag to prevent excessive requests.
                            // Setting the "If-None-Match" to the previously returned ETag means that
                            // if the data hasn't changed, GitHub will return a response code of return 304
                            // and not count the request towards the API rate limit.
                            urlConnection.setRequestProperty("If-None-Match", etag);
                        }
                    }

                    connectionInputStream = urlConnection.getInputStream();
                    final int responseCode = httpConnection != null ? httpConnection.getResponseCode() : HttpURLConnection.HTTP_OK;

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // The cached file is out of date, or we don't have a cached file.
                        final String jsonString = IOUtils.toString(connectionInputStream);
                        final JsonValue json = Json.parse(jsonString);
                        latestRelease = getReleaseTagFromGithubJson(json);

                        if (latestRelease != null) {
                            // Cache the version file
                            FileUtils.writeStringToFile(cachedGithubResponseFile, jsonString);
                            // Cache the ETag to send in future requests.
                            final String newETag = urlConnection.getHeaderField("ETag");

                            if (newETag != null) {
                                FileUtils.writeStringToFile(eTagFile, newETag);
                            }
                        }
                    }

                    if (httpConnection != null) {
                        httpConnection.disconnect();
                    }
                } catch (final Exception e) {
                    try {
                        final Class<?> logWrapper = Class.forName("net.minecraft.launchwrapper.LogWrapper");
                        final Method warning = logWrapper.getMethod("warning", String.class, Object[].class);
                        warning.invoke(null, "Could not complete update check: " + ExceptionUtils.getStackTrace(e), new Object[0]);
                    } catch (final Exception ignored) {
                        // LaunchWrapper isn't available
                        tempLogger.log(Level.WARNING, "Could not complete update check", e);
                    }
                } finally {
                    IOUtils.closeQuietly(connectionInputStream);
                }

                if ((latestRelease != null) && !latestRelease.equals(MetadataUtil.VERSION)) {
                    tempLogger.log(Level.INFO, "Found new update {0}", latestRelease);
                    final JFrame updateFrame = new JFrame();
                    updateFrame.setResizable(true);
                    updateFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    updateFrame.setVisible(false);

                    try {
                        final JTextPane textPane = new JTextPane();
                        textPane.setEditable(false);
                        textPane.setBorder(null);
                        textPane.setContentType("text/html");
                        textPane.setText("<html>" +
                                         "<p>A new version of RetroWrapper (" + latestRelease + ") has been released!</p><br>" +
                                         "<p><a href=\"https://github.com/NeRdTheNed/RetroWrapper/releases\">Click here to open the releases page!</a></p><br>" +
                                         "</html>");
                        textPane.addHyperlinkListener(new NavigateToHyperlinkListener(tempLogger, textPane));
                        textPane.setCaretPosition(0);
                        textPane.setOpaque(false);
                        textPane.setHighlighter(null);
                        textPane.setSelectedTextColor(new Color(0, 0, 0, 0));
                        textPane.setBackground(new Color(0, 0, 0, 0));
                        JOptionPane.showMessageDialog(updateFrame, textPane, "Update available!", JOptionPane.INFORMATION_MESSAGE);
                    } catch (final Exception e) {
                        tempLogger.log(Level.WARNING, "An Exception was thrown while trying to display the update notifier", e);
                        JOptionPane.showMessageDialog(null, "A new version of RetroWrapper (" + latestRelease + ") has been released!\nYou can download it from https://github.com/NeRdTheNed/RetroWrapper/releases", "Update available!", JOptionPane.INFORMATION_MESSAGE);
                    }

                    updateFrame.dispose();
                }
            } else {
                try {
                    final Class<?> logWrapper = Class.forName("net.minecraft.launchwrapper.LogWrapper");
                    final Method warning = logWrapper.getMethod("warning", String.class, Object[].class);
                    warning.invoke(null, "GitHub API rate limit exceeded, can't check for update!", new Object[0]);
                } catch (final Exception ignored) {
                    // LaunchWrapper isn't available
                    tempLogger.log(Level.WARNING, "GitHub API rate limit exceeded, unable to check for update!");
                }
            }
        } else {
            tempLogger.log(Level.INFO, "Update checking not enabled for RetroWrapper snapshots.");
            JOptionPane.showMessageDialog(null, "The update checker doesn't work on snapshot versions of RetroWrapper!\nPlease check for the latest release manually!", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private static JPanel makeScrollerPanel(String[] wrapArray, JComponent[] additionalComponents, String... preface) {
        final JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.PAGE_AXIS));

        for (final String line : preface) {
            final JLabel lineLabel = new JLabel(line);
            lineLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
            addJLabelCentered(messagePanel, lineLabel);
        }

        if (additionalComponents != null) {
            for (final JComponent comp : additionalComponents) {
                addJComponentCentered(messagePanel, comp);
            }
        }

        final JList diagList = new JList(wrapArray);
        diagList.setSelectionModel(new NoSelectionModel());
        diagList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        final JScrollPane jsp = new JScrollPane(diagList);
        final int pwidth = jsp.getPreferredSize().width;
        jsp.setPreferredSize(new Dimension(Math.max(pwidth, 500), jsp.getPreferredSize().height));
        addJComponentCentered(messagePanel, jsp);
        return messagePanel;
    }

    public static void showMessageScroller(int messageType, String title, String[] wrapArray, JComponent[] additionalComponents, String... preface) {
        final JPanel messagePanel = makeScrollerPanel(wrapArray, additionalComponents, preface);
        JOptionPane.showMessageDialog(null, messagePanel, title, messageType);
    }

    public static int showOptionScroller(int option, String title, String[] wrapArray, JComponent[] additionalComponents, String... preface) {
        final JPanel messagePanel = makeScrollerPanel(wrapArray, additionalComponents, preface);
        return JOptionPane.showConfirmDialog(null, messagePanel, title, option);
    }

    private SwingUtil() {
        // As this is a helper class, there should be no reason to instantiate an instance of it.
    }

    private static final class NoSelectionModel extends DefaultListSelectionModel {
        // Eclipse complains unless I add this
        private static final long serialVersionUID = 1L;

        NoSelectionModel() {
            // Empty constructor
        }

        @Override
        public void addSelectionInterval(int index0, int index1) {
            super.addSelectionInterval(-1, -1);
        }

        @Override
        public void setSelectionInterval(int index0, int index1) {
            super.setSelectionInterval(-1, -1);
        }
    }

    private static final class NavigateToHyperlinkListener implements HyperlinkListener {
        private final Logger logger;
        private final JTextPane textPane;

        NavigateToHyperlinkListener(Logger logger, JTextPane textPane) {
            this.logger = logger;
            this.textPane = textPane;
        }

        public void hyperlinkUpdate(HyperlinkEvent event) {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    final Class<?> desktopClass = Class.forName("java.awt.Desktop");
                    final Method getDesktop = desktopClass.getMethod("getDesktop");
                    final Object desktopObject = getDesktop.invoke(null);
                    final Method browse = desktopClass.getMethod("browse", URI.class);
                    browse.invoke(desktopObject, event.getURL().toURI());
                } catch (final Exception e) {
                    if ((e instanceof NoSuchMethodException) || (e instanceof ClassNotFoundException)) {
                        logger.log(Level.WARNING, "Are you running RetroWrapper on Java 5?");
                    }

                    logger.log(Level.WARNING, "Could not open link from hyperlinkUpdate", e);
                    JOptionPane.showMessageDialog(textPane, "Your platform doesn't let Java open links!\nPlease browse to " + event.getURL() + " manually.\nThanks for putting up with this!", "Sorry", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
    }
}
