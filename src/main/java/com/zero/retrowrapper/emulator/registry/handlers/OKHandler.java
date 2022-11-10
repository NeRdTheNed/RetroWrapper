package com.zero.retrowrapper.emulator.registry.handlers;

import java.io.IOException;
import java.io.OutputStream;

import com.zero.retrowrapper.emulator.registry.EmulatorHandler;

public final class OKHandler extends EmulatorHandler {
    private final String toWrite;

    public OKHandler(String url, String toWrite) {
        super(url);
        this.toWrite = toWrite;
    }

    @Override
    public void handle(OutputStream os, String get, byte[] data) throws IOException {
        os.write(toWrite.getBytes());
    }
}
