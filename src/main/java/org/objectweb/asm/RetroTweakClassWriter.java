package org.objectweb.asm;

import java.util.ArrayList;
import java.util.List;

import com.zero.retrowrapper.emulator.EmulatorConfig;

public final class RetroTweakClassWriter extends ClassWriter {
    private static final int CLASS = 7;
    private static final int FIELD = 9;
    private static final int METH = 10;
    private static final int IMETH = 11;
    private static final int STR = 8;
    private static final int INT = 3;
    private static final int FLOAT = 4;
    private static final int LONG = 5;
    private static final int DOUBLE = 6;
    private static final int NAME_TYPE = 12;
    private static final int UTF8 = 1;
    private static final int MTYPE = 16;
    private static final int HANDLE = 15;
    private static final int INDY = 18;
    private static final int HANDLE_BASE = 20;
    private static final int TYPE_NORMAL = 30;
    private final String className;

    public RetroTweakClassWriter(int a, String className) {
        super(a);
        this.className = className;
    }

    @Override
    public byte[] toByteArray() {
        final ClassWriter writer = new ClassWriter(0);
        final byte[] bytes = super.toByteArray();
        final List<Item> items = new ArrayList<>();

        for (final Item item : e) {
            Item next = item;

            while (next != null) {
                items.add(next);
                next = next.k;
            }
        }

        for (final Item item : items) {
            item.a = writer.c;
            writer.c++;

            if ((item.b == LONG) || (item.b == DOUBLE)) {
                writer.c++;
            }

            final int hash = item.j % writer.e.length;
            item.k = writer.e[hash];
            writer.e[hash] = item;
        }

        for (final Item item : items) {
            switch (item.b) {
            case UTF8:
                if (item.g.contains("random.splash") || item.g.contains("char.png")) {
                    if (EmulatorConfig.getInstance().mobClass == null) {
                        EmulatorConfig.getInstance().mobClass = className;
                    }
                }

                if (item.g.contains(".com")) {
                    System.out.println("Found URL!: " + item.g);
                    String finalstr = item.g;
                    // TODO optimize
                    finalstr = (item.g.contains("https://") || item.g.contains("http://") ? "http://" : "") + "127.0.0.1:" + EmulatorConfig.getInstance().getPort() + item.g.replace(finalstr.split(".com")[0] + ".com", "");
                    System.out.println("Replaced with: " + finalstr);
                    writer.d.putByte(UTF8).putUTF8(finalstr);
                } else if (item.g.contains(".net")) {
                    System.out.println("Found URL!: " + item.g);
                    String finalstr = item.g;

                    if ("minecraft.net".equals(finalstr)) {
                        finalstr = "127.0.0.1";
                    } else {
                        // TODO optimize
                        finalstr = (item.g.contains("https://") ? "https://" : "") + (item.g.contains("http://") ? "http://" : "") + "127.0.0.1:" + EmulatorConfig.getInstance().getPort() + item.g.replace(finalstr.split(".net")[0] + ".net", "");
                    }

                    System.out.println("Replaced with: " + finalstr);
                    writer.d.putByte(UTF8).putUTF8(finalstr);
                } else {
                    writer.d.putByte(UTF8).putUTF8(item.g);
                }

                break;

            case CLASS:
            case STR:
            case MTYPE:
                writer.d.putByte(item.b).putShort(writer.newUTF8(item.g));
                break;

            case INT:
            case FLOAT:
                writer.d.putByte(item.b).putInt(item.c);
                break;

            case LONG:
            case DOUBLE:
                writer.d.putByte(item.b).putLong(item.d);
                break;

            case FIELD:
            case METH:
            case IMETH:
                writer.d.putByte(item.b).putShort(writer.newClass(item.g)).putShort(writer.newNameType(item.h, item.i));
                break;

            case NAME_TYPE:
                writer.d.putByte(item.b).putShort(writer.newUTF8(item.g)).putShort(writer.newUTF8(item.h));
                break;

            case INDY:
                writer.d.putByte(INDY).putShort((int)item.d).putShort(writer.newNameType(item.g, item.h));
                break;
            }

            if ((item.b >= HANDLE_BASE) && (item.b < TYPE_NORMAL)) {
                final int tag = item.b - HANDLE_BASE;

                if (tag <= Opcodes.H_PUTSTATIC) {
                    writer.d.putByte(HANDLE).putByte(tag).putShort(writer.newField(item.g, item.h, item.i));
                } else {
                    writer.d.putByte(HANDLE).putByte(tag).putShort(writer.newMethod(item.g, item.h, item.i, tag == Opcodes.H_INVOKEINTERFACE));
                }
            }
        }

        final ClassReader reader = new ClassReader(bytes);
        reader.accept(writer, 0);
        return writer.toByteArray();
    }
}
