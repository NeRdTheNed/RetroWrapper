package com.zero.retrowrapper.emulator.registry;

import java.io.IOException;
import java.io.OutputStream;

public abstract class EmulatorHandler implements IHandler {
    private static final byte[] HTTP_HEADERS =
        ("HTTP/1.1 200 OK\n"
         + "Date: Mon, 27 Jul 2009 12:28:53 GMT\n"
         + "Server: Apache/2.2.14 (Win32)\n"
         + "Last-Modified: Wed, 22 Jul 2009 19:15:56 GMT\n"
         + "Content-Length: 10485760\n"
         + "Content-Type: audio/ogg\n"
         + "Connection: Closed\r\n\n").getBytes();

    protected String url;

    protected EmulatorHandler(String url) {
        this.url = url;
    }

    public void sendHeaders(OutputStream os) throws IOException {
        os.write(HTTP_HEADERS);
    }

    public String getUrl() {
        return url;
    }
}
