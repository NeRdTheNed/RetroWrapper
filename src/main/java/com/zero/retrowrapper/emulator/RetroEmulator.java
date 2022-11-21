package com.zero.retrowrapper.emulator;

import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LogWrapper;

public final class RetroEmulator extends Thread {
    private static RetroEmulator instance;

    private File directory;
    private File mapsDirectory;
    private File cacheDirectory;

    public void run() {
        // TODO Is this threadsafe, and does it need to be?
        instance = this;
        LogWrapper.info("Old servers emulator is running!");
        directory = new File(Launch.minecraftHome, "retrowrapper");
        directory.mkdirs();
        mapsDirectory = new File(RetroEmulator.getInstance().getDirectory(), "maps");
        mapsDirectory.mkdir();
        cacheDirectory = new File(RetroEmulator.getInstance().getDirectory(), "cache");
        cacheDirectory.mkdir();
        ServerSocket server = null;

        try {
            server = new ServerSocket(EmulatorConfig.getInstance().getPort());

            while (true) {
                final Socket socket = server.accept();

                try {
                    new SocketEmulator(socket).parseIncoming();
                } catch (final Exception e) {
                    // TODO Better error handling
                    LogWrapper.warning("Error when parsing incoming data for RetroWrapper local server: " + ExceptionUtils.getStackTrace(e));
                }
            }
        } catch (final Exception e) {
            // TODO Better error handling
            LogWrapper.severe("Error when starting RetroWrapper local server! This is very bad.\n" + ExceptionUtils.getStackTrace(e));
        } finally {
            IOUtils.closeQuietly(server);
        }
    }

    public File getDirectory() {
        return directory;
    }

    public File getMapsDirectory() {
        return mapsDirectory;
    }

    public File getCacheDirectory() {
        return cacheDirectory;
    }

    public static RetroEmulator getInstance() {
        return instance;
    }
}
