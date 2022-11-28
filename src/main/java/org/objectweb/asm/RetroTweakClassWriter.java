package org.objectweb.asm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import com.zero.retrowrapper.emulator.registry.EmulatorRegistry;
import com.zero.retrowrapper.injector.RetroTweakInjectorTarget;

import net.minecraft.launchwrapper.LogWrapper;

public final class RetroTweakClassWriter extends ClassWriter {
    public static String mobClass;

    private static final String foundUrlTextString = "Found URL!: ";
    private static final String replacedWithTextString = "Replaced with: ";
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
    private static final Pattern minecraftNetPattern = Pattern.compile("www.minecraft.net", Pattern.LITERAL);
    private static final Pattern comPattern = Pattern.compile(".com");
    private static final Pattern netPattern = Pattern.compile(".net");
    private final String className;

    public RetroTweakClassWriter(int a, String className) {
        super(a);
        this.className = className;
    }

    public byte[] toByteArray() {
        final ClassWriter writer = new ClassWriter(0);
        final byte[] bytes = super.toByteArray();
        final Collection<Item> items = new ArrayList<Item>();

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
                final String constant = item.g;

                if ((constant.contains("random.splash") || constant.contains("char.png")) && (mobClass == null)) {
                    mobClass = className;
                }

                final String transformed;

                if ("minecraft.net".equals(constant)) {
                    LogWrapper.info(foundUrlTextString + constant);
                    transformed = "127.0.0.1";
                    LogWrapper.info(replacedWithTextString + transformed);
                } else if (constant.contains("joinserver.jsp") || constant.contains("checkserver.jsp")) {
                    LogWrapper.info(foundUrlTextString + constant);
                    transformed = minecraftNetPattern.matcher(constant).replaceAll("session.minecraft.net");
                    LogWrapper.info(replacedWithTextString + transformed);
                } else if ((RetroTweakInjectorTarget.serverIP != null) && "79.136.77.240".equals(constant)) {
                    // 79.136.77.240 is a hardcoded URL for early multiplayer tests.
                    // TODO Allow patching the hardcoded port (5565)
                    LogWrapper.info(foundUrlTextString + constant);
                    transformed = RetroTweakInjectorTarget.serverIP;
                    LogWrapper.info(replacedWithTextString + transformed);
                } else {
                    final boolean isNet = constant.contains(".net");
                    final boolean isCom = constant.contains(".com");

                    if (isNet || isCom) {
                        LogWrapper.info(foundUrlTextString + constant);

                        if (constant.contains("minecraft.net") || (EmulatorRegistry.getHandlerByUrl(constant) != null)) {
                            final String prepend = isCom ?
                                                   constant.contains("https://") || constant.contains("http://") ? "http://" : "" :
                                                   (constant.contains("https://") ? "https://" : "") + (constant.contains("http://") ? "http://" : "");
                            String postpend = isCom ?
                                              constant.replace(comPattern.split(constant)[0] + ".com", "") :
                                              constant.replace(netPattern.split(constant)[0] + ".net", "");

                            if (constant.contains("login.minecraft.net") && (EmulatorRegistry.getHandlerByUrl(constant) == null)) {
                                postpend += "/login/session.jsp";
                            }

                            transformed = prepend + "127.0.0.1:" + RetroTweakInjectorTarget.localServerPort + postpend;
                            LogWrapper.info(replacedWithTextString + transformed);
                        } else {
                            transformed = constant;
                            LogWrapper.warning("No handler for " + transformed + ", did not replace.");
                        }
                    } else {
                        transformed = constant;
                    }
                }

                writer.d.putByte(UTF8).putUTF8(transformed);
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
                writer.d.putByte(INDY).putShort((int) item.d).putShort(writer.newNameType(item.g, item.h));
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
