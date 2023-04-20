package com.zero.retrowrapper.emulator;

import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LogWrapper;

public final class RetroEmulator implements Runnable {
    private final ServerSocket server;
    private final boolean multithreaded;

    private static RetroEmulator instance;

    private File directory;
    private File mapsDirectory;
    private File cacheDirectory;

    public RetroEmulator(ServerSocket server, boolean multithreaded) {
        this.server = server;
        this.multithreaded = multithreaded;
    }

    public void run() {
        // TODO Is this threadsafe, and does it need to be?
        instance = this;
        LogWrapper.info("Old servers emulator is running!");
        directory = new File(Launch.minecraftHome, "retrowrapper");
        directory.mkdirs();
        mapsDirectory = new File(directory, "maps");
        mapsDirectory.mkdirs();
        cacheDirectory = new File(directory, "cache");
        cacheDirectory.mkdirs();

        try {
            final ExecutorService threadPool = multithreaded ? Executors.newCachedThreadPool() : Executors.newSingleThreadExecutor();

            while (true) {
                try {
                    final Socket socket = server.accept();
                    final Runnable emulator = new SocketEmulator(socket);
                    threadPool.execute(emulator);
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
