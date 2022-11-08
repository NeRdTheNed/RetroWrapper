package com.zero.retrowrapper.injector;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;

import com.zero.retrowrapper.emulator.EmulatorConfig;
import com.zero.retrowrapper.emulator.RetroEmulator;
import com.zero.retrowrapper.hack.HackThread;
import com.zero.retrowrapper.util.SwingUtil;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

public final class RetroTweakInjectorTarget implements IClassTransformer {
    /**
     *
     * THIS IS MODIFIED VERSION OF ALPHAVANILLATWEAKINJECTOR
     *   ALL RIGHTS TO MOJANG
     *
     */

    @Override
    public byte[] transform(final String name, final String transformedName, final byte[] bytes) {
        return bytes;
    }

    // TODO can the throws be removed?
    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        System.out.println("******************************");
        System.out.println("*     old mojang servers     *");
        System.out.println("*       emulator by 000      *");
        System.out.println("******************************");
        new RetroEmulator().start();

        try {
            Class<?> clazz;
            boolean veryOld = false;

            try {
                clazz = getaClass("net.minecraft.client.MinecraftApplet");
            } catch (final ClassNotFoundException ex) {
                veryOld = true;
                clazz = getaClass("com.mojang.minecraft.MinecraftApplet");
            }

            final Map<String, String> params = new HashMap<String, String>();
            final String username = args.length > 0 ? args[0] : "Player" + (System.currentTimeMillis() % 1000);
            final String sessionId = args.length > 1 ? args[1] : "-";
            params.put("username", username);
            params.put("sessionid", sessionId);
            params.put("haspaid", "true");
            final Constructor<?> constructor = clazz.getConstructor();
            final Applet object = (Applet)constructor.newInstance();
            final LauncherFake fakeLauncher = new LauncherFake(params, object);
            object.setStub(fakeLauncher);
            object.setSize(854, 480);
            object.init();

            for (final Field field : clazz.getDeclaredFields()) {
                final String name = field.getType().getName();

                if (!name.contains("awt") && !name.contains("java") && !field.getType().equals(Long.TYPE)) {
                    System.out.println("Found likely Minecraft candidate: " + field);
                    EmulatorConfig.getInstance().minecraftField = field;
                    final Field fileField = getWorkingDirField(name);

                    if (veryOld) {
                        field.setAccessible(true);
                        final Object mcObj = field.get(object);
                        System.out.println(mcObj);
                        Field appletField = null;

                        for (final Field f : mcObj.getClass().getDeclaredFields()) {
                            if (f.getType().equals(Boolean.TYPE) && Modifier.isPublic(f.getModifiers())) {
                                appletField = f;
                                break;
                            }
                        }

                        if (appletField != null) {
                            System.out.println("Applet mode: " + appletField.get(mcObj));
                            appletField.set(mcObj, false);
                        }
                    }

                    if (fileField != null) {
                        System.out.println("Found File, changing to " + Launch.minecraftHome);
                        fileField.setAccessible(true);
                        fileField.set(null, Launch.minecraftHome);
                        break;
                    }
                }
            }

            EmulatorConfig.getInstance().applet = object;
            startMinecraft(fakeLauncher, object, args);

            if (System.getProperties().getProperty("retrowrapper.hack") != null) {
                new HackThread().start();
            }
        } catch (final Exception e) {
            e.printStackTrace();
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
        launcherFrameFake.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        fakeLauncher.setLayout(new BorderLayout());
        fakeLauncher.add(applet, BorderLayout.CENTER);
        fakeLauncher.validate();
        launcherFrameFake.removeAll();
        launcherFrameFake.setLayout(new BorderLayout());
        launcherFrameFake.add(fakeLauncher, BorderLayout.CENTER);
        launcherFrameFake.validate();
        applet.start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                applet.stop();
            }
        });
        SwingUtil.loadIconsOnFrames();
    }

    public static Class<?> getaClass(String name) throws ClassNotFoundException {
        return Launch.classLoader.findClass(name);
    }

    private static Field getWorkingDirField(String name) throws ClassNotFoundException {
        final Class<?> clazz = getaClass(name);

        for (final Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType().equals(java.io.File.class)) {
                return field;
            }
        }

        return null;
    }
}

