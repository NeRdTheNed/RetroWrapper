package com.zero.retrowrapper.util;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class ByteUtils {
    public static String readLine(InputStream dis) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        while (true) {
            final int b = dis.read();

            if (b == 0x0a) {
                break;
            }

            bos.write(b);
        }

        return new String(bos.toByteArray());
    }

    public static String readString(DataInputStream dis) throws IOException {
        final int len = dis.readUnsignedShort();
        System.out.println(len);
        final byte[] bytes = new byte[len];
        dis.read(bytes);
        return new String(bytes);
    }

    private ByteUtils() {
        // As this is a helper class, there should be no reason to instantiate an instance of it.
    }
}
