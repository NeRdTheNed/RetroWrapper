package com.zero.retrowrapper.emulator.registry.handlers;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import com.zero.retrowrapper.emulator.RetroEmulator;
import com.zero.retrowrapper.emulator.registry.EmulatorHandler;
import com.zero.retrowrapper.util.FileUtil;

import net.minecraft.launchwrapper.LogWrapper;

public final class SaveHandler extends EmulatorHandler {
    public SaveHandler() {
        super("/level/save.html");
    }

    public void handle(OutputStream os, String get, byte[] data) throws IOException {
        final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
        // Username, not used
        dis.readUTF();
        // Auth token, not used
        dis.readUTF();
        final String levelName = dis.readUTF();
        final byte id = dis.readByte();
        final int levelLength = dis.readInt();
        LogWrapper.fine(levelLength + ";" + data.length);
        final byte[] level = new byte[levelLength];
        dis.readFully(level);
        os.write("ok\n".getBytes());
        dis.close();
        final File fileMap = new File(RetroEmulator.getInstance().getMapsDirectory(), "map" + id + ".mclevel");
        final File fileMapMeta = new File(RetroEmulator.getInstance().getMapsDirectory(), "map" + id + ".txt");

        if (!FileUtil.bytesToFile(level, fileMap)) {
            LogWrapper.warning("Error when trying to save level");
        }

        if (!FileUtil.bytesToFile(levelName.getBytes(), fileMapMeta)) {
            LogWrapper.warning("Error when trying to save level metadata");
        }
    }
}
