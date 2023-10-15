package com.zero.retrowrapper.injector;

import java.util.HashSet;
import java.util.ListIterator;
import java.util.regex.Pattern;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.zero.retrowrapper.emulator.registry.EmulatorRegistry;
import com.zero.retrowrapper.emulator.registry.IHandler;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.LogWrapper;

public final class URLTweakInjector implements IClassTransformer {
    public static String mobClass;
    public static String tesClass;

    private static final String foundUrlTextString = "Found URL!: ";
    private static final String replacedWithTextString = "Replaced with: ";
    private static final Pattern minecraftNetPattern = Pattern.compile("www.minecraft.net", Pattern.LITERAL);
    private static final Pattern comPattern = Pattern.compile(".com");
    private static final Pattern netPattern = Pattern.compile(".net");

    private static final HashSet<String> IGNORED_STRINGS;

    static {
        IGNORED_STRINGS = new HashSet<String>();
        IGNORED_STRINGS.add("http://snoop.minecraft.net/");
        IGNORED_STRINGS.add("Failed to authenticate: Can't connect to minecraft.net");
        IGNORED_STRINGS.add("http://assets.minecraft.net/1_6_has_been_released.flag");
        IGNORED_STRINGS.add("https://mcoapi.minecraft.net/");
        IGNORED_STRINGS.add("http://www.minecraft.net/prepurchase.jsp?source=pcgamerdemo");
        IGNORED_STRINGS.add("http://www.minecraft.net/store?source=demo");
    }

    private static boolean isStringIgnored(String str) {
        return IGNORED_STRINGS.contains(str);
    }

    /**
     * Finds and replaces URL constants.
     */
    public byte[] transform(final String name, final String transformedName, final byte[] bytesOld) {
        try {
            if (bytesOld == null) {
                return null;
            }

            boolean changed = false;
            final ClassReader classReader = new ClassReader(bytesOld);
            final ClassNode classNode = new ClassNode();
            classReader.accept(classNode, ClassReader.EXPAND_FRAMES);

            for (final Object methodNodeO : classNode.methods) {
                final MethodNode methodNode = (MethodNode) methodNodeO;
                @SuppressWarnings("unchecked")
                final ListIterator<AbstractInsnNode> iterator = methodNode.instructions.iterator();

                while (iterator.hasNext()) {
                    final AbstractInsnNode instruction = iterator.next();
                    final int opcode = instruction.getOpcode();

                    if (opcode == Opcodes.LDC) {
                        final LdcInsnNode ldc = (LdcInsnNode) instruction;

                        if (ldc.cst instanceof String) {
                            final String constant = (String) ldc.cst;

                            if ((constant.contains("random.splash") || constant.contains("char.png")) && (mobClass == null)) {
                                mobClass = name;
                                LogWrapper.fine("Probably the mob class: " + mobClass);
                            }

                            if (constant.contains("Not tesselating!") || constant.contains("Already tesselating!")) {
                                tesClass = name;
                                LogWrapper.fine("Probably the tessellator class: " + tesClass);
                            }

                            final String transformed;

                            if ("minecraft.net".equals(constant)) {
                                LogWrapper.info(foundUrlTextString + constant);
                                transformed = "127.0.0.1";
                                LogWrapper.info(replacedWithTextString + transformed);
                            } else if ((RetroTweakInjectorTarget.serverIP != null) && "79.136.77.240".equals(constant)) {
                                // 79.136.77.240 is a hardcoded URL for early multiplayer tests.
                                // TODO Allow patching the hardcoded port (5565)
                                LogWrapper.info(foundUrlTextString + constant);
                                transformed = RetroTweakInjectorTarget.serverIP;
                                LogWrapper.info(replacedWithTextString + transformed);
                            } else if ("http://www.minecraft.net/store/loot.jsp".equals(constant)) {
                                LogWrapper.info(foundUrlTextString + constant);
                                transformed = "https://web.archive.org/web/20110401175108/http://www.minecraft.net/store/loot.jsp";
                                LogWrapper.info(replacedWithTextString + transformed);
                            } else if (isStringIgnored(constant)) {
                                LogWrapper.info(foundUrlTextString + constant);
                                transformed = constant;
                                LogWrapper.warning("No handler for " + transformed + ", did not replace.");
                            } else {
                                final boolean isNet = constant.contains(".net");
                                final boolean isCom = constant.contains(".com");

                                if (isNet || isCom) {
                                    LogWrapper.info(foundUrlTextString + constant);
                                    final IHandler handler = EmulatorRegistry.getHandlerByUrl(constant);

                                    if ((handler == null) && (constant.contains("joinserver.jsp") || constant.contains("checkserver.jsp"))) {
                                        LogWrapper.info(foundUrlTextString + constant);
                                        transformed = minecraftNetPattern.matcher(constant).replaceAll("session.minecraft.net");
                                        LogWrapper.info(replacedWithTextString + transformed);
                                    } else if (constant.contains("minecraft.net") || (handler != null)) {
                                        final StringBuilder newUrl = new StringBuilder();
                                        newUrl.append(isCom ?
                                                      constant.contains("https://") || constant.contains("http://") ? "http://" : "" :
                                                      (constant.contains("https://") ? "https://" : "") + (constant.contains("http://") ? "http://" : ""))
                                        .append("127.0.0.1:")
                                        .append(RetroTweakInjectorTarget.localServerPort)
                                        .append(isCom ?
                                                constant.replace(comPattern.split(constant)[0] + ".com", "") :
                                                constant.replace(netPattern.split(constant)[0] + ".net", ""));

                                        if (constant.contains("login.minecraft.net") && (EmulatorRegistry.getHandlerByUrl(constant) == null)) {
                                            newUrl.append("/login/session.jsp");
                                        }

                                        transformed = newUrl.toString();
                                        LogWrapper.info(replacedWithTextString + transformed);
                                    } else {
                                        transformed = constant;
                                        LogWrapper.warning("No handler for " + transformed + ", did not replace.");
                                    }
                                } else {
                                    transformed = constant;
                                }
                            }

                            if (transformed != constant) {
                                final LdcInsnNode loadNew = new LdcInsnNode(transformed);
                                methodNode.instructions.insertBefore(ldc, loadNew);
                                methodNode.instructions.remove(ldc);
                                changed = true;
                            }
                        }
                    }
                }
            }

            if (!changed) {
                return bytesOld;
            }

            final ClassWriter writer = new ClassWriter(0);
            classNode.accept(writer);
            return writer.toByteArray();
        } catch (final Exception e) {
            LogWrapper.severe("Exception while transforming class " + name + ": " + ExceptionUtils.getStackTrace(e));
            return bytesOld;
        }
    }
}
