package com.zero.retrowrapper.emulator.registry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.zero.retrowrapper.emulator.RetroEmulator;
import com.zero.retrowrapper.emulator.registry.handlers.ListmapsHandler;
import com.zero.retrowrapper.emulator.registry.handlers.LoadHandler;
import com.zero.retrowrapper.emulator.registry.handlers.ResourcesHandler;
import com.zero.retrowrapper.emulator.registry.handlers.SaveHandler;
import com.zero.retrowrapper.emulator.registry.handlers.SingleResponseHandler;
import com.zero.retrowrapper.emulator.registry.handlers.SkinOrCapeHandler;

import net.minecraft.launchwrapper.Launch;

public final class EmulatorRegistry {
    private EmulatorRegistry() {
        // Empty private constructor to hide default constructor
    }

    private static final List<IHandler> handlers;
    private static final int smallestSize = 16;

    private static void moveInvalidFiles(File directory, String[] ext) {
        final String toPrint;

        if (ext != null) {
            final StringBuilder builder = new StringBuilder();

            for (final String s : ext) {
                builder.append(" " + s);
            }

            toPrint = "Scanning for invalid files of type(s)" + builder.toString() + " to move in " + directory;
        } else {
            toPrint = "Scanning for invalid files to move in " + directory;
        }

        System.out.println(toPrint);
        directory.mkdirs();

        if (directory.isDirectory()) {
            for (final File file : FileUtils.listFiles(directory, ext, true)) {
                try {
                    if (Files.size(file.toPath()) < smallestSize) {
                        final String baseFile = directory.getParent() + "/_invalidFiles/" + file.getAbsolutePath().replace(directory.getAbsolutePath(), "");
                        File newDir = new File(baseFile);

                        for (int count = 0; newDir.exists(); count++) {
                            newDir = new File(baseFile + "_" + count);
                        }

                        newDir.getParentFile().mkdirs();
                        FileUtils.moveFile(file, newDir);
                        System.out.println("Moved file " + file + " to " + newDir + ", too small to be a valid file.");
                    }
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static {
        moveInvalidFiles(RetroEmulator.getInstance().getCacheDirectory(), null);
        moveInvalidFiles(new File(Launch.minecraftHome, "resources"), new String[] { "ogg" });
        moveInvalidFiles(new File(Launch.minecraftHome + "/assets/virtual/legacy/"), new String[] { "ogg" });
        handlers = new ArrayList<IHandler>();
        handlers.add(new SingleResponseHandler("login/session.jsp", "ok"));
        handlers.add(new SingleResponseHandler("session?name=", "ok"));
        handlers.add(new SingleResponseHandler("/game/?n=", "0"));
        handlers.add(new SingleResponseHandler("haspaid.jsp", "true"));
        handlers.add(new SaveHandler());
        handlers.add(new LoadHandler());
        handlers.add(new ListmapsHandler());
        handlers.add(new ResourcesHandler("/resources/"));
        handlers.add(new ResourcesHandler("/MinecraftResources/"));
        handlers.add(new SkinOrCapeHandler("/skin/", false));
        handlers.add(new SkinOrCapeHandler("/MinecraftSkins/", false));
        handlers.add(new SkinOrCapeHandler("/cloak/get.jsp?user=", true));
        handlers.add(new SkinOrCapeHandler("/MinecraftCloaks/", true));
    }

    public static IHandler getHandlerByUrl(String url) {
        for (final IHandler handler : handlers) {
            if (url.contains(handler.getUrl())) {
                return handler;
            }
        }

        return null;
    }
}
