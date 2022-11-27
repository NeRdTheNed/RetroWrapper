package com.zero.retrowrapper.emulator;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.zero.retrowrapper.emulator.registry.EmulatorRegistry;
import com.zero.retrowrapper.emulator.registry.IHandler;

import net.minecraft.launchwrapper.LogWrapper;

public final class SocketEmulator {
    private static final Pattern spacePattern = Pattern.compile(" ");
    private static final Pattern contentLengthPattern = Pattern.compile("Content-Length: ", Pattern.LITERAL);
    private final Socket socket;

    public SocketEmulator(Socket socket) {
        this.socket = socket;
    }

    public void parseIncoming() {
        InputStream is = null;
        OutputStream os = null;
        InputStreamReader ir = null;
        BufferedReader br = null;

        try {
            is = socket.getInputStream();
            os = socket.getOutputStream();
            ir = new InputStreamReader(is);
            br = new BufferedReader(ir);
            int length = -1;
            String get = "";
            int limit = 0;

            while (limit < 20) {
                final String line = br.readLine();

                if (limit == 0) {
                    get = spacePattern.split(line)[1];
                } else if (line.startsWith("Content-Length: ")) {
                    try {
                        length = Integer.parseInt(contentLengthPattern.matcher(line).replaceAll(""));
                    } catch (final NumberFormatException e) {
                        LogWrapper.severe("Content-Length was not a number (header: " + line + ")", e);
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
            }

            os.flush();
            socket.close();
        } catch (final IOException e) {
            LogWrapper.warning("Error in SocketEmulator when parsing incoming data for RetroWrapper local server: " + ExceptionUtils.getStackTrace(e));
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(ir);
            IOUtils.closeQuietly(br);
            IOUtils.closeQuietly(socket);
        }
    }
}
