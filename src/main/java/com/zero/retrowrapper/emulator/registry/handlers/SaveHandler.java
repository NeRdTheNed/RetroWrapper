package com.zero.retrowrapper.emulator.registry.handlers;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.zero.retrowrapper.emulator.RetroEmulator;
import com.zero.retrowrapper.emulator.registry.EmulatorHandler;

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
        FileOutputStream fos1 = null;

        try {
            fos1 = new FileOutputStream(fileMap);
            fos1.write(level);
        } catch (final Exception e) {
            // TODO Better error handling
            LogWrapper.warning("Error when trying to save level: " + ExceptionUtils.getStackTrace(e));
        } finally {
            IOUtils.closeQuietly(fos1);
        }

        FileOutputStream fos2 = null;

        try {
            fos2 = new FileOutputStream(fileMapMeta);
            fos2.write(levelName.getBytes());
        } catch (final Exception e) {
            // TODO Better error handling
            LogWrapper.warning("Error when trying to save level metadata: " + ExceptionUtils.getStackTrace(e));
        } finally {
            IOUtils.closeQuietly(fos2);
        }
    }
}
