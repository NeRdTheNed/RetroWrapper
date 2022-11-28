package com.zero.retrowrapper.injector;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.JPanel;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.lwjgl.Sys;

import com.zero.retrowrapper.emulator.RetroEmulator;
import com.zero.retrowrapper.hack.HackThread;
import com.zero.retrowrapper.util.FileUtil;
import com.zero.retrowrapper.util.MetadataUtil;
import com.zero.retrowrapper.util.NetworkUtil;
import com.zero.retrowrapper.util.SwingUtil;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LogWrapper;

public final class RetroTweakInjectorTarget implements IClassTransformer {
    /**
     *
     * THIS IS MODIFIED VERSION OF ALPHAVANILLATWEAKINJECTOR
     *   ALL RIGHTS TO MOJANG
     *
     */

    public static String username;
    public static String sessionId;

    public static String serverIP;
    public static String serverPort;

    public static boolean connectedToClassicServer = false;

    public static boolean showClassiCubeUserDefaultSkin = false;

    public static Field minecraftField;
    public static Applet applet;

    public static int localServerPort;

    private static final Pattern tokenPattern = Pattern.compile("token:", Pattern.LITERAL);
    private static final Pattern colonPattern = Pattern.compile(":");

    public byte[] transform(final String name, final String transformedName, final byte[] bytes) {
        return bytes;
    }

    // TODO can the throws be removed?
    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, IOException {
        LogWrapper.info(
            "\n******************************" +
            "\n*     old mojang servers     *" +
            "\n*       emulator by 000      *" +
            "\n******************************"
        );
        LogWrapper.info("RetroWrapper version " + MetadataUtil.VERSION);

        try {
            final String lwjglVersion = Sys.getVersion();
            LogWrapper.info("LWJGL version " + lwjglVersion);

            if (MetadataUtil.compareSemver(lwjglVersion, "3.0.0") >= 0) {
                LogWrapper.info("Somehow, you're using LWJGL " + lwjglVersion + " despite this method calling a LWJGL 2 method. Consider me impressed.");
            } else if (SystemUtils.IS_OS_MAC && (MetadataUtil.compareSemver(lwjglVersion, "2.9.3") < 0)) {
                LogWrapper.warning("Warning: LWJGL 2.9.3 or higher is recommended on newer versions of MacOS.");
            }
        } catch (final Exception e) {
            LogWrapper.warning("There's something wrong with LWJGL: " + ExceptionUtils.getStackTrace(e));
        }

        try {
            final File defaultMinecraftDir = new File(FileUtil.defaultMinecraftDirectory());
            final File defaultCacheDir = new File(defaultMinecraftDir, "retrowrapper/cache");
            final File currentCacheDir = new File(Launch.minecraftHome, "retrowrapper/cache");

            if (defaultMinecraftDir.exists()) {
                defaultCacheDir.mkdirs();
                SwingUtil.checkAndDisplayUpdate(defaultCacheDir);
            } else {
                currentCacheDir.mkdirs();
                SwingUtil.checkAndDisplayUpdate(currentCacheDir);
            }
        } catch (final Exception e) {
            LogWrapper.warning("Update check failed: " + ExceptionUtils.getStackTrace(e));
        }

        final ServerSocket server = new ServerSocket(0);
        localServerPort = server.getLocalPort();
        new RetroEmulator(server).start();

        try {
            Class<?> clazz;
            boolean veryOld = false;

            try {
                clazz = getaClass("net.minecraft.client.MinecraftApplet");
            } catch (final ClassNotFoundException e) {
                try {
                    veryOld = true;
                    clazz = getaClass("com.mojang.minecraft.MinecraftApplet");
                } catch (final ClassNotFoundException ex) {
                    throw new ClassNotFoundException("Could not find MinecraftApplet!", ex);
                }
            }

            final Map<String, String> params = new HashMap<String, String>();
            username = args.length > 0 ? args[0] : "Player" + (System.currentTimeMillis() % 1000);
            sessionId = args.length > 1 ? args[1] : "-";

            if (sessionId.startsWith("token:")) {
                sessionId = tokenPattern.matcher(sessionId).replaceAll("");
                sessionId = colonPattern.split(sessionId)[0];
            }

            params.put("username", username);
            params.put("sessionid", sessionId);
            params.put("haspaid", "true");
            params.put("stand-alone", "true");
            params.put("fullscreen", "false");
            // Experimental
            serverIP = System.getProperties().getProperty("retrowrapper.experimental.classicServerIP");
            serverPort = System.getProperties().getProperty("retrowrapper.experimental.classicServerPort");
            showClassiCubeUserDefaultSkin = System.getProperties().getProperty("retrowrapper.showClassiCubeDefaultSkin") != null;

            if (serverIP != null) {
                if (serverPort == null) {
                    serverPort = "25565";
                }

                final String serverID = DigestUtils.shaHex((serverIP + ":" + serverPort).getBytes());

                if (NetworkUtil.joinServer(sessionId, username, serverID)) {
                    connectedToClassicServer = true;
                    params.put("server", serverIP);
                    params.put("port", serverPort);
                    final String mppass = NetworkUtil.getBetacraftMPPass(username, serverIP, serverPort);

                    if (mppass != null) {
                        params.put("mppass", mppass);
                    } else {
                        params.put("mppass", "0");
                    }
                }
            }

            final Constructor<?> constructor = clazz.getConstructor();
            final Applet object = (Applet) constructor.newInstance();
            final LauncherFake fakeLauncher = new LauncherFake(params, object);
            object.setStub(fakeLauncher);
            object.setSize(854, 480);
            object.init();

            for (final Field field : clazz.getDeclaredFields()) {
                final String name = field.getType().getName();

                if (!name.contains("awt") && !name.contains("java") && !field.getType().equals(Long.TYPE)) {
                    LogWrapper.fine("Found likely Minecraft candidate: " + field);
                    minecraftField = field;
                    final Field fileField = getWorkingDirField(name);

                    if (veryOld) {
                        field.setAccessible(true);
                        final Object mcObj = field.get(object);
                        LogWrapper.fine(mcObj.toString());
                        Field appletField = null;

                        for (final Field f : mcObj.getClass().getDeclaredFields()) {
                            if (f.getType().equals(Boolean.TYPE) && Modifier.isPublic(f.getModifiers())) {
                                appletField = f;
                                break;
                            }
                        }

                        if (appletField != null) {
                            LogWrapper.fine("Applet mode: " + appletField.get(mcObj));
                            appletField.set(mcObj, false);
                        }
                    }

                    if (fileField != null) {
                        LogWrapper.fine("Found File, changing to " + Launch.minecraftHome);
                        fileField.setAccessible(true);
                        fileField.set(null, Launch.minecraftHome);
                        break;
                    }
                }
            }

            applet = object;
            startMinecraft(fakeLauncher, object, args);

            if (System.getProperties().getProperty("retrowrapper.hack") != null) {
                new HackThread().start();
            }
        } catch (final Exception e) {
            LogWrapper.severe("Fatal error while starting RetroWrapper: " + ExceptionUtils.getStackTrace(e));
            System.exit(1);
        }
    }

    private static void startMinecraft(LauncherFake fakeLauncher, final Applet applet, String[] args) {
        final Frame launcherFrameFake = new Frame();
        launcherFrameFake.setTitle("Minecraft");
        launcherFrameFake.setBackground(Color.BLACK);
        final JPanel panel = new JPanel();
        launcherFrameFake.setLayout(new BorderLayout());
        panel.setPreferredSize(new Dimension(854, 480));
        launcherFrameFake.add(panel, BorderLayout.CENTER);
        launcherFrameFake.pack();
        launcherFrameFake.setLocationRelativeTo(null);
        launcherFrameFake.setVisible(true);
        launcherFrameFake.addWindowListener(new WindowClosingAdapter());
        fakeLauncher.setLayout(new BorderLayout());
        fakeLauncher.add(applet, BorderLayout.CENTER);
        fakeLauncher.validate();
        launcherFrameFake.removeAll();
        launcherFrameFake.setLayout(new BorderLayout());
        launcherFrameFake.add(fakeLauncher, BorderLayout.CENTER);
        launcherFrameFake.validate();
        applet.start();
        Runtime.getRuntime().addShutdownHook(new ShutdownAppletThread(applet));
        SwingUtil.loadIconsOnFrames();
    }

    public static Class<?> getaClass(String name) throws ClassNotFoundException {
        return Launch.classLoader.findClass(name);
    }

    private static Field getWorkingDirField(String name) throws ClassNotFoundException {
        final Class<?> clazz = getaClass(name);

        for (final Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType().equals(File.class)) {
                return field;
            }
        }

        return null;
    }
}

