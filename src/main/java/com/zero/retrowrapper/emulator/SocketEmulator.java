package com.zero.retrowrapper.emulator;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.zero.retrowrapper.emulator.registry.EmulatorRegistry;
import com.zero.retrowrapper.emulator.registry.IHandler;
import com.zero.retrowrapper.util.ByteUtil;

public final class SocketEmulator {
    private final Socket socket;

    public SocketEmulator(Socket socket) {
        this.socket = socket;
    }

    public void parseIncoming() throws IOException {
        final InputStream is = socket.getInputStream();
        final OutputStream os = socket.getOutputStream();
        final DataInputStream dis = new DataInputStream(is);
        int length = -1;
        String get = "";
        int limit = 0;

        while (limit < 20) {
            final String line = ByteUtil.readLine(dis).trim();

            if (limit == 0) {
                get = line.split(" ")[1];
            } else if (line.startsWith("Content-Length: ")) {
                length = Integer.parseInt(line.replace("Content-Length: ", ""));
            } else if (line.length() < 2) {
                break;
            }

            limit++;
        }

        byte[] data = null;

        if (length != -1) {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final byte[] buffer = new byte[8192];
            int read = 0;
            int buffered = 0;

            while (read < length) {
                final int readd = is.read(buffer, 0, Math.min(length - 8192, buffer.length));
                read += readd;
                bos.write(buffer, 0, readd);
                buffered += read;

                if (buffered > (1024 * 1024)) {
                    bos.flush();
                    buffered = 0;
                }
            }

            data = bos.toByteArray();
        }

        final IHandler handler = EmulatorRegistry.getHandlerByUrl(get);

        if (handler != null) {
            try {
                System.out.println("Request: " + get);
                handler.sendHeaders(os);
                handler.handle(os, get, data);
            } catch (final Exception e) {
                System.out.println("***************************************");
                System.out.println("   Exception in handling URL: " + get);
                System.out.println("***************************************");
                e.printStackTrace();
            }
        } else {
            System.out.println("***************************************");
            System.out.println("   No handler for URL: " + get);
            System.out.println("***************************************");
        }

        os.flush();
        socket.close();
    }
}
