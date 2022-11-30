package com.zero.retrowrapper.emulator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.zero.retrowrapper.emulator.registry.EmulatorRegistry;
import com.zero.retrowrapper.emulator.registry.IHandler;

import net.minecraft.launchwrapper.LogWrapper;

public final class SocketEmulator implements Runnable {
    private static final Pattern spacePattern = Pattern.compile(" ");
    private static final Pattern contentLengthPattern = Pattern.compile("Content-Length: ", Pattern.LITERAL);
    private final Socket socket;

    private static final byte[] HTTP_404_HEADERS =
        ("HTTP/1.1 404 Not Found\n"
         + "Date: Mon, 27 Jul 2009 12:28:53 GMT\n"
         + "Server: Apache/2.2.14 (Win32)\n"
         + "Content-Length: 0\n"
         + "Connection: Closed\r\n\n").getBytes();

    public SocketEmulator(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        InputStream is = null;
        OutputStream os = null;

        try {
            is = socket.getInputStream();
            os = socket.getOutputStream();
            int length = -1;
            String get = "";
            int limit = 0;

            while (limit < 20) {
                final String line = readLine(is).trim();

                if (limit == 0) {
                    get = URLDecoder.decode(spacePattern.split(line)[1], "UTF-8");
                } else if (line.startsWith("Content-Length: ")) {
                    try {
                        length = Integer.parseInt(contentLengthPattern.matcher(line).replaceAll(""));
                    } catch (final NumberFormatException e) {
                        LogWrapper.severe("Content-Length was not a number (header: " + line + "): " + ExceptionUtils.getStackTrace(e));
                    }
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
                    LogWrapper.info("Request: " + get);
                    handler.sendHeaders(os, get);
                    handler.handle(os, get, data);
                } catch (final Exception e) {
                    LogWrapper.warning("Exception in handling URL: " + ExceptionUtils.getStackTrace(e));
                }
            } else {
                LogWrapper.warning("No handler for URL: " + get);
                os.write(HTTP_404_HEADERS);
            }

            os.flush();
            socket.close();
        } catch (final Exception e) {
            LogWrapper.warning("Error in SocketEmulator when parsing incoming data for RetroWrapper local server: " + ExceptionUtils.getStackTrace(e));
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(socket);
        }
    }

    private static String readLine(InputStream is) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        while (true) {
            final int b = is.read();

            if (b == '\n') {
                break;
            }

            bos.write(b);
        }

        return bos.toString();
    }
}
