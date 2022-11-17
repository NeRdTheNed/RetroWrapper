package com.zero.retrowrapper.emulator;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import net.minecraft.launchwrapper.Launch;

public final class RetroEmulator extends Thread {
    private static RetroEmulator instance;

    private File directory;
    private File mapsDirectory;
    private File cacheDirectory;

    public void run() {
        // TODO Is this threadsafe, and does it need to be?
        instance = this;
        System.out.println("Old servers emulator is running!");
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
                    e.printStackTrace();
                }
            }
        } catch (final Exception e) {
            // TODO Better error handling
            e.printStackTrace();
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (final IOException ee) {
                    // TODO Better error handling
                    ee.printStackTrace();
                }
            }
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
