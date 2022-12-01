package com.zero.retrowrapper.emulator.registry.handlers;

import java.io.IOException;
import java.io.OutputStream;

import com.zero.retrowrapper.emulator.registry.EmulatorHandler;

public final class SingleResponseHandler extends EmulatorHandler {
    private final byte[] toWrite;

    public SingleResponseHandler(String url, String toWrite) {
        super(url);
        this.toWrite = toWrite.getBytes();
    }

    public void handle(OutputStream os, String get, byte[] data) throws IOException {
        os.write(toWrite);
    }
}
