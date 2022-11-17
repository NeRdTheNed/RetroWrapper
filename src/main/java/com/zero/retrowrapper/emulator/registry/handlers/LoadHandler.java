package com.zero.retrowrapper.emulator.registry.handlers;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

import com.zero.retrowrapper.emulator.RetroEmulator;
import com.zero.retrowrapper.emulator.registry.EmulatorHandler;

public final class LoadHandler extends EmulatorHandler {
    public LoadHandler() {
        super("/level/load.html?id=");
    }

    public void handle(OutputStream os, String get, byte[] data) throws IOException {
        final String id = get.replace("/level/load.html?id=", "").split("&")[0];
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(new File(RetroEmulator.getInstance().getMapsDirectory(), "map" + id + ".mclevel"));
            final byte[] bytes = IOUtils.toByteArray(fis);
            final DataOutputStream dis = new DataOutputStream(os);
            dis.writeUTF("ok");
            dis.write(bytes);
        } catch (final Exception e) {
            // TODO Better error handling
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (final IOException ee) {
                    // TODO Better error handling
                    ee.printStackTrace();
                }
            }
        }
    }
}
