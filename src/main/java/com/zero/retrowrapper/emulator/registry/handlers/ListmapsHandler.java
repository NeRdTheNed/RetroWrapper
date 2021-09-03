package com.zero.retrowrapper.emulator.registry.handlers;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Scanner;

import com.zero.retrowrapper.emulator.RetroEmulator;
import com.zero.retrowrapper.emulator.registry.EmulatorHandler;
import com.zero.retrowrapper.emulator.registry.IHandler;

public class ListmapsHandler extends EmulatorHandler implements IHandler {
    public ListmapsHandler() {
        super("/listmaps.jsp");
    }

    @Override
    public void handle(OutputStream os, String get, byte[] data) throws IOException {
        for (int i = 0; i < 5; i++) {
            final File file = new File(RetroEmulator.getInstance().getMapsDirectory(), "map" + i + ".txt");
            String name = "-;";

            if (file.exists()) {
                final Scanner tempScan = new Scanner(file);
                name = tempScan.nextLine() + ";";
                tempScan.close();
            }

            os.write(name.getBytes());
        }
    }
}
