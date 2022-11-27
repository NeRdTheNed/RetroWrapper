package com.zero.retrowrapper.emulator.registry;

import java.io.IOException;
import java.io.OutputStream;

public interface IHandler {
    String getUrl();

    void sendHeaders(OutputStream os, String get) throws IOException;

    void handle(OutputStream os, String get, byte[] data) throws IOException;
}
