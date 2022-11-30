package com.zero.retrowrapper.emulator.registry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.zero.retrowrapper.emulator.RetroEmulator;
import com.zero.retrowrapper.emulator.registry.handlers.JoinServerHandler;
import com.zero.retrowrapper.emulator.registry.handlers.ListmapsHandler;
import com.zero.retrowrapper.emulator.registry.handlers.LoadHandler;
import com.zero.retrowrapper.emulator.registry.handlers.ResourcesHandler;
import com.zero.retrowrapper.emulator.registry.handlers.ResourcesHandler.ResourcesFormat;
import com.zero.retrowrapper.emulator.registry.handlers.SaveHandler;
import com.zero.retrowrapper.emulator.registry.handlers.SingleResponseHandler;
import com.zero.retrowrapper.emulator.registry.handlers.SkinOrCapeHandler;
import com.zero.retrowrapper.util.FileUtil;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LogWrapper;

public final class EmulatorRegistry {
    private EmulatorRegistry() {
        // Empty private constructor to hide default constructor
    }

    private static final List<IHandler> handlers;
    private static final int smallestSize = 16;

    private static void moveInvalidFiles(File directory, String... ext) {
        final String toPrint;

        if ((ext != null) && (ext.length != 0)) {
            final StringBuilder builder = new StringBuilder();

            for (final String s : ext) {
                builder.append(" ").append(s);
            }

            toPrint = "Scanning for invalid files of type(s)" + builder + " to move in " + directory;
        } else {
            toPrint = "Scanning for invalid files to move in " + directory;
        }

        LogWrapper.info(toPrint);
        directory.mkdirs();

        if (directory.isDirectory()) {
            for (final File file : FileUtil.findFiles(directory, ext)) {
                if (file.length() < smallestSize) {
                    final String baseFile = directory.getParent() + "/_invalidFiles/" + file.getAbsolutePath().replace(directory.getAbsolutePath(), "");
                    File newDir = new File(baseFile);

                    for (int count = 0; newDir.exists(); count++) {
                        newDir = new File(baseFile + "_" + count);
                    }

                    newDir.getParentFile().mkdirs();

                    if (file.renameTo(newDir)) {
                        LogWrapper.warning("Moved file " + file + " to " + newDir + ", too small to be a valid file.");
                    } else {
                        LogWrapper.warning("Problem moving " + file + " to " + newDir);
                    }
                }
            }
        }
    }

    static {
        moveInvalidFiles(RetroEmulator.getInstance().getCacheDirectory());
        moveInvalidFiles(new File(Launch.minecraftHome, "resources"), "ogg");
        moveInvalidFiles(new File(Launch.minecraftHome + "/assets/virtual/legacy/"), "ogg");
        handlers = new ArrayList<IHandler>();
        handlers.add(new JoinServerHandler("joinserver.jsp"));
        handlers.add(new SingleResponseHandler("login/session.jsp", "ok".getBytes()));
        handlers.add(new SingleResponseHandler("session?name=", "ok".getBytes()));
        handlers.add(new SingleResponseHandler("/game/?n=", "0".getBytes()));
        handlers.add(new SingleResponseHandler("haspaid.jsp", "true".getBytes()));
        handlers.add(new SaveHandler());
        handlers.add(new LoadHandler());
        handlers.add(new ListmapsHandler());
        handlers.add(new ResourcesHandler("/resources/", ResourcesFormat.CLASSIC, "legacy", "https://launchermeta.mojang.com/v1/packages/770572e819335b6c0a053f8378ad88eda189fc14/legacy.json"));
        handlers.add(new ResourcesHandler("/MinecraftResources/", ResourcesFormat.AWS, "pre-1.6", "https://launchermeta.mojang.com/v1/packages/3d8e55480977e32acd9844e545177e69a52f594b/pre-1.6.json"));
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
