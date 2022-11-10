package com.zero.retrowrapper.injector;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import java.awt.Canvas;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import net.minecraft.launchwrapper.IClassTransformer;

public final class MouseTweakInjector implements IClassTransformer {
    /**
     *
     * THIS IS MODIFIED VERSION OF INDEVVANILLATWEAKINJECTOR
     *   ALL RIGHTS TO MOJANG
     *
     */

    private static final BufferedImage hiddenCursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    private static final java.awt.Cursor hiddenCursor = Toolkit.getDefaultToolkit().createCustomCursor(hiddenCursorImg, new Point(0, 0), "Hidden cursor");
    private static final java.awt.Cursor normalCursor = java.awt.Cursor.getDefaultCursor();

    // TODO @Nullable?
    @Override
    public byte[] transform(final String name, final String transformedName, final byte[] bytesOld) {
        try {
            if (bytesOld == null) {
                return null;
            }

            final ClassReader classReader = new ClassReader(bytesOld);
            final ClassNode classNode = new ClassNode();
            classReader.accept(classNode, ClassReader.EXPAND_FRAMES);

            // Patch calls to setNativeCursor when running on MacOS. This prevents some versions of Minecraft from crashing,
            // because setNativeCursor throws an IllegalStateException, and Minecraft doesn't handle it.
            // Also patch the entire input system so we can call setGrabbed without issues :/
            if (SystemUtils.IS_OS_MAC) {
                for (final Object methodNodeO : classNode.methods) {
                    final MethodNode methodNode = (MethodNode) methodNodeO;
                    final List<MethodInsnNode> foundNativeCursorMethodCalls = new ArrayList<MethodInsnNode>();
                    final List<MethodInsnNode> foundMouseDXYMethodCalls = new ArrayList<MethodInsnNode>();
                    final List<MethodInsnNode> foundMouseInfoMethodCalls = new ArrayList<MethodInsnNode>();
                    @SuppressWarnings("unchecked")
                    final ListIterator<AbstractInsnNode> iterator = methodNode.instructions.iterator();

                    while (iterator.hasNext()) {
                        final AbstractInsnNode instruction = iterator.next();
                        final int opcode = instruction.getOpcode();

                        if ((opcode <= Opcodes.INVOKEINTERFACE) && (opcode >= Opcodes.INVOKEVIRTUAL)) {
                            final MethodInsnNode methodInsNode = (MethodInsnNode) instruction;
                            final String methodOwner = methodInsNode.owner;
                            final String methodName = methodInsNode.name;
                            final String methodDesc = methodInsNode.desc;

                            if (opcode == Opcodes.INVOKESTATIC) {
                                if ("org/lwjgl/input/Mouse".equals(methodOwner) && "(Lorg/lwjgl/input/Cursor;)Lorg/lwjgl/input/Cursor;".equals(methodDesc) && "setNativeCursor".equals(methodName)) {
                                    foundNativeCursorMethodCalls.add(methodInsNode);
                                }

                                if (System.getProperties().getProperty("retrowrapper.enableExperimentalPatches") != null) {
                                    if ("org/lwjgl/input/Mouse".equals(methodOwner) && "()I".equals(methodDesc) && ("getDX".equals(methodName) || "getDY".equals(methodName))) {
                                        foundMouseDXYMethodCalls.add(methodInsNode);
                                    }
                                }
                            }

                            if (System.getProperties().getProperty("retrowrapper.enableExperimentalPatches") != null) {
                                if ((opcode == Opcodes.INVOKEVIRTUAL) && "java/awt/PointerInfo".equals(methodOwner) && "()Ljava/awt/Point;".equals(methodDesc) && "getLocation".equals(methodName)) {
                                    foundMouseInfoMethodCalls.add(methodInsNode);
                                }
                            }
                        }
                    }

                    for (final MethodInsnNode toPatch : foundMouseInfoMethodCalls) {
                        final AbstractInsnNode prev = toPatch.getPrevious();

                        if (prev.getOpcode() == Opcodes.INVOKESTATIC) {
                            final MethodInsnNode prevCall = (MethodInsnNode) prev;

                            // Patch the call to MouseInfo.getPointerInfo().getLocation().
                            if ("java/awt/MouseInfo".equals(prevCall.owner) && "()Ljava/awt/PointerInfo;".equals(prevCall.desc) && "getPointerInfo".equals(prevCall.name)) {
                                System.out.println("Patching call to getLocation at class " + name);
                                final MethodInsnNode methodInsNode = new MethodInsnNode(INVOKESTATIC, "com/zero/retrowrapper/injector/MouseTweakInjector", "getLocationPatch", "()Ljava/awt/Point;");
                                methodNode.instructions.insertBefore(toPatch.getNext(), methodInsNode);
                                methodNode.instructions.remove(prev);
                                methodNode.instructions.remove(toPatch);
                            } else {
                                System.out.println("Warning: Something went wrong when trying to patch " + toPatch.name + " at class " + name);
                            }
                        } else {
                            System.out.println("Warning: Something went wrong when trying to patch " + toPatch.name + " at class " + name);
                        }
                    }

                    for (final MethodInsnNode toPatch : foundMouseDXYMethodCalls) {
                        final AbstractInsnNode next = toPatch.getNext();

                        // Patch calls to Mouse.getDX or Mouse.getDY that are discarded.
                        if (next.getOpcode() == Opcodes.POP) {
                            System.out.println("Patching call to " + toPatch.name + " at class " + name);
                            methodNode.instructions.remove(next);
                            methodNode.instructions.remove(toPatch);
                        } else {
                            //System.out.println("Warning: Return value of call to " + toPatch.name + " at class " + name + " was actually used, this should never happen!");
                        }
                    }

                    for (final MethodInsnNode toPatch : foundNativeCursorMethodCalls) {
                        System.out.println("Patching call to setNativeCursor at class " + name);
                        // Check if the method deliberately loaded null. This implies the cursor should be shown.
                        final int shouldHide;

                        if (toPatch.getPrevious().getOpcode() == Opcodes.ACONST_NULL) {
                            shouldHide = 0;
                        } else {
                            shouldHide = 1;
                        }

                        final LdcInsnNode loadShouldHide = new LdcInsnNode(Integer.valueOf(shouldHide));
                        final MethodInsnNode methodInsNode = new MethodInsnNode(INVOKESTATIC, "com/zero/retrowrapper/injector/MouseTweakInjector", "setNativeCursorPatch", "(Lorg/lwjgl/input/Cursor;Z)Lorg/lwjgl/input/Cursor;");
                        methodNode.instructions.insertBefore(toPatch, loadShouldHide);
                        methodNode.instructions.insertBefore(toPatch, methodInsNode);
                        methodNode.instructions.remove(toPatch);
                    }
                }
            }

            final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(writer);
            return writer.toByteArray();
        } catch (final Exception e) {
            System.out.println("Exception while transforming class " + name);
            e.printStackTrace();
            System.out.println(e);
            return bytesOld;
        }
    }

    public static Point getLocationPatch() {
        final Canvas canvas = Display.getParent();
        final Point location = canvas.getLocationOnScreen();
        return new Point(Mouse.getDX() + location.x + (canvas.getWidth() / 2), -Mouse.getDY() + location.y + (canvas.getHeight() / 2));
    }

    public static Cursor setNativeCursorPatch(Cursor cursor, boolean shouldHide) throws LWJGLException {
        try {
            final java.awt.Cursor useCursor = shouldHide ? hiddenCursor : normalCursor;
            Display.getParent().setCursor(useCursor);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        if (System.getProperties().getProperty("retrowrapper.enableExperimentalPatches") != null) {
            final Canvas canvas = Display.getParent();

            if (!shouldHide) {
                Mouse.setCursorPosition(canvas.getWidth() / 2, canvas.getHeight() / 2);
            }

            Mouse.setGrabbed(shouldHide);

            if (shouldHide) {
                Mouse.getDX();
                Mouse.getDY();
            }
        }

        try {
            return Mouse.setNativeCursor(cursor);
        } catch (final IllegalStateException e) {
            // Although this is pretty close to the expected behavior...
            //throw new LWJGLException(e);
            // ...it's super annoying for logging, because Minecraft prints a stacktrace every time this is thrown.
            // I've opted to just return null.
            // As the value isn't used and the cursor passed will always be null, it doesn't matter what's returned here.
            return null;
        }
    }
}
