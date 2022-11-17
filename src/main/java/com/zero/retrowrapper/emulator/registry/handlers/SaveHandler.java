package com.zero.retrowrapper.emulator.registry.handlers;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.zero.retrowrapper.emulator.RetroEmulator;
import com.zero.retrowrapper.emulator.registry.EmulatorHandler;
import com.zero.retrowrapper.util.ByteUtil;

public final class SaveHandler extends EmulatorHandler {
    public SaveHandler() {
        super("/level/save.html");
    }

    @Override
    public void handle(OutputStream os, String get, byte[] data) throws IOException {
        final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
        ByteUtil.readString(dis);
        ByteUtil.readString(dis);
        final String levelName = ByteUtil.readString(dis);
        final byte id = dis.readByte();
        final int levelLength = dis.readInt();
        System.out.println(levelLength + ";" + data.length);
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
            e.printStackTrace();
        } finally {
            if (fos1 != null) {
                try {
                    fos1.close();
                } catch (final IOException ee) {
                    // TODO Better error handling
                    ee.printStackTrace();
                }
            }
        }

        FileOutputStream fos2 = null;

        try {
            fos2 = new FileOutputStream(fileMapMeta);
            fos2.write(levelName.getBytes());
        } catch (final Exception e) {
            // TODO Better error handling
            e.printStackTrace();
        } finally {
            if (fos2 != null) {
                try {
                    fos2.close();
                } catch (final IOException ee) {
                    // TODO Better error handling
                    ee.printStackTrace();
                }
            }
        }
    }
}
