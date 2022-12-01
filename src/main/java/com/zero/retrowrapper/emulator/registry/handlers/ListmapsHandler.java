package com.zero.retrowrapper.emulator.registry.handlers;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;

import com.zero.retrowrapper.emulator.RetroEmulator;
import com.zero.retrowrapper.emulator.registry.EmulatorHandler;

public final class ListmapsHandler extends EmulatorHandler {
    public ListmapsHandler() {
        super("/listmaps.jsp");
    }

    public void handle(OutputStream os, String get, byte[] data) throws IOException {
        for (int i = 0; i < 5; i++) {
            final File file = new File(RetroEmulator.getInstance().getMapsDirectory(), "map" + i + ".txt");
            final String name = file.exists() ? FileUtils.readFileToString(file) + ";" : "-;";
            os.write(name.getBytes());
        }
    }
}
