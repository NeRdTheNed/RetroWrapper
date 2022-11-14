package com.zero.retrowrapper.injector;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.zero.retrowrapper.emulator.RetroEmulator;

import net.minecraft.launchwrapper.IClassTransformer;

public final class M1ColorTweakInjector implements IClassTransformer {
    /**
     *
     * THIS IS MODIFIED VERSION OF INDEVVANILLATWEAKINJECTOR
     *   ALL RIGHTS TO MOJANG
     *
     */

    String[] toPatch3 = {
        "glColor3b",
        "glColor3f",
        "glColor3ub"
    };

    String[] toPatch3Double = {
        "glColor3d"
    };

    String[] toPatch4 = {
        "glColor4b",
        "glColor4f",
        "glColor4ub",
        "glClearColor"
    };

    String[] toPatch4Double = {
        "glColor4d"
    };

    public static boolean isMinecraftFullscreen = true;

    /**
     * TODO WIP patches
     * TODO @Nullable?
     */
    @Override
    public byte[] transform(final String name, final String transformedName, final byte[] bytesOld) {
        try {
            if (bytesOld == null) {
                return null;
            }

            final ClassReader classReader = new ClassReader(bytesOld);
            final ClassNode classNode = new ClassNode();
            classReader.accept(classNode, ClassReader.EXPAND_FRAMES);

            for (final Object methodNodeO : classNode.methods) {
                final MethodNode methodNode = (MethodNode) methodNodeO;
                final List<MethodInsnNode> foundSetFullscreenCalls = new ArrayList<MethodInsnNode>();
                final List<MethodInsnNode> foundGetRGBCalls = new ArrayList<MethodInsnNode>();
                final List<MethodInsnNode> foundSwap3Calls = new ArrayList<MethodInsnNode>();
                final List<MethodInsnNode> foundSwap3DoubleCalls = new ArrayList<MethodInsnNode>();
                final List<MethodInsnNode> foundSwap4Calls = new ArrayList<MethodInsnNode>();
                final List<MethodInsnNode> foundSwap4DoubleCalls = new ArrayList<MethodInsnNode>();
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

                        if ((opcode == Opcodes.INVOKEVIRTUAL) && "java/awt/image/BufferedImage".equals(methodOwner) && "(IIII[III)[I".equals(methodDesc) && "getRGB".equals(methodName)) {
                            foundGetRGBCalls.add(methodInsNode);
                        }

                        if (opcode == Opcodes.INVOKESTATIC) {
                            if ("org/lwjgl/opengl/Display".equals(methodOwner) && "(Z)V".equals(methodDesc) && "setFullscreen".equals(methodName)) {
                                foundSetFullscreenCalls.add(methodInsNode);
                            }

                            if ("org/lwjgl/opengl/GL11".equals(methodOwner)) {
                                for (final String patch3 : toPatch3) {
                                    if (patch3.equals(methodName)) {
                                        foundSwap3Calls.add(methodInsNode);
                                        break;
                                    }
                                }

                                for (final String patch3Dub : toPatch3Double) {
                                    if (patch3Dub.equals(methodName)) {
                                        foundSwap3DoubleCalls.add(methodInsNode);
                                        break;
                                    }
                                }

                                for (final String patch4 : toPatch4) {
                                    if (patch4.equals(methodName)) {
                                        foundSwap4Calls.add(methodInsNode);
                                        break;
                                    }
                                }

                                for (final String patch4Dub : toPatch4Double) {
                                    if (patch4Dub.equals(methodName)) {
                                        foundSwap4DoubleCalls.add(methodInsNode);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                for (final MethodInsnNode toPatch : foundSetFullscreenCalls) {
                    System.out.println("Hooking fullscreen call at class " + name);
                    final MethodInsnNode methodInsNode = new MethodInsnNode(INVOKESTATIC, "com/zero/retrowrapper/injector/M1ColorTweakInjector", "setFullscreenWrapper", "(Z)V");
                    methodNode.instructions.insertBefore(toPatch, methodInsNode);
                    methodNode.instructions.remove(toPatch);
                }

                for (final MethodInsnNode toPatch : foundGetRGBCalls) {
                    System.out.println("Patching call to getRGB at class " + name);
                    final MethodInsnNode methodInsNode = new MethodInsnNode(INVOKESTATIC, "com/zero/retrowrapper/injector/M1ColorTweakInjector", "buffImageTweaker", "(Ljava/awt/image/BufferedImage;IIII[III)[I");
                    methodNode.instructions.insertBefore(toPatch, methodInsNode);
                    methodNode.instructions.remove(toPatch);
                }

                /*
                for (final MethodInsnNode toPatch : foundSwap3Calls) {
                    System.out.println("Patching call to " + toPatch.owner + "." + toPatch.name + toPatch.desc + " at class " + name);
                    // RGB to RBG: Swap last stack values.
                    // Non-double version, doubles require special handling.
                    final InsnNode swap = new InsnNode(Opcodes.SWAP);
                    methodNode.instructions.insertBefore(toPatch, swap);
                }
                */

                for (final MethodInsnNode toPatch : foundSwap3Calls) {
                    System.out.println("Patching call to " + toPatch.owner + "." + toPatch.name + toPatch.desc + " at class " + name);
                    // RGB to BGR
                    // Non-double version, doubles require special handling.
                    final LabelNode target = new LabelNode();
                    final FieldInsnNode getFullscreen = new FieldInsnNode(Opcodes.GETSTATIC, "com/zero/retrowrapper/injector/M1ColorTweakInjector", "isMinecraftFullscreen", "Z");
                    final JumpInsnNode skipIfFullscreen = new JumpInsnNode(Opcodes.IFNE, target);
                    // RGB
                    final InsnNode dup_x2 = new InsnNode(Opcodes.DUP_X2);
                    // BRGB
                    final InsnNode pop = new InsnNode(Opcodes.POP);
                    // BRG
                    final InsnNode swap = new InsnNode(Opcodes.SWAP);
                    // BGR
                    methodNode.instructions.insertBefore(toPatch, getFullscreen);
                    methodNode.instructions.insertBefore(toPatch, skipIfFullscreen);
                    methodNode.instructions.insertBefore(toPatch, dup_x2);
                    methodNode.instructions.insertBefore(toPatch, pop);
                    methodNode.instructions.insertBefore(toPatch, swap);
                    methodNode.instructions.insertBefore(toPatch, target);
                }

                /*
                for (final MethodInsnNode toPatch : foundSwap3DoubleCalls) {
                    System.out.println("Patching call to " + toPatch.owner + "." + toPatch.name + toPatch.desc + " at class " + name);
                    // RGB to RBG: Swap last stack values.
                    // Double version, doubles require special handling.
                    final InsnNode dup2_x2 = new InsnNode(Opcodes.DUP2_X2);
                    final InsnNode pop2 = new InsnNode(Opcodes.POP2);
                    methodNode.instructions.insertBefore(toPatch, dup2_x2);
                    methodNode.instructions.insertBefore(toPatch, pop2);
                }
                */

                for (final MethodInsnNode toPatch : foundSwap3DoubleCalls) {
                    System.out.println("Patching call to " + toPatch.owner + "." + toPatch.name + toPatch.desc + " at class " + name);
                    // RGB to BGR
                    // Double version, doubles require special handling.
                    final LabelNode target = new LabelNode();
                    final FieldInsnNode getFullscreen = new FieldInsnNode(Opcodes.GETSTATIC, "com/zero/retrowrapper/injector/M1ColorTweakInjector", "isMinecraftFullscreen", "Z");
                    final JumpInsnNode skipIfFullscreen = new JumpInsnNode(Opcodes.IFNE, target);
                    methodNode.maxLocals += 2;
                    final int index = methodNode.maxLocals;
                    // RGB
                    final InsnNode dup2_x2 = new InsnNode(Opcodes.DUP2_X2);
                    // RBGB
                    final InsnNode pop2 = new InsnNode(Opcodes.POP2);
                    // RBG
                    final VarInsnNode storeG = new VarInsnNode(Opcodes.DSTORE, index);
                    // RB
                    final InsnNode dup2_x2_2 = new InsnNode(Opcodes.DUP2_X2);
                    // BRB
                    final InsnNode pop2_2 = new InsnNode(Opcodes.POP2);
                    // BR
                    final VarInsnNode loadG = new VarInsnNode(Opcodes.DLOAD, index);
                    // BRG
                    final InsnNode dup2_x2_3 = new InsnNode(Opcodes.DUP2_X2);
                    // BGRG
                    final InsnNode pop2_3 = new InsnNode(Opcodes.POP2);
                    // BGR
                    methodNode.instructions.insertBefore(toPatch, getFullscreen);
                    methodNode.instructions.insertBefore(toPatch, skipIfFullscreen);
                    methodNode.instructions.insertBefore(toPatch, dup2_x2);
                    methodNode.instructions.insertBefore(toPatch, pop2);
                    methodNode.instructions.insertBefore(toPatch, storeG);
                    methodNode.instructions.insertBefore(toPatch, dup2_x2_2);
                    methodNode.instructions.insertBefore(toPatch, pop2_2);
                    methodNode.instructions.insertBefore(toPatch, loadG);
                    methodNode.instructions.insertBefore(toPatch, dup2_x2_3);
                    methodNode.instructions.insertBefore(toPatch, pop2_3);
                    methodNode.instructions.insertBefore(toPatch, target);
                }

                /*for (final MethodInsnNode toPatch : foundSwap4Calls) {
                    System.out.println("Patching call to " + toPatch.owner + "." + toPatch.name + toPatch.desc + " at class " + name);
                    // RGBA to RBGA: Swap stack variable order from (1, 2, 3, 4) to (1, 3, 2, 4).
                    // Non-double version, doubles require special handling.
                    // 1, 2, 3, 4
                    final InsnNode dup_x2 = new InsnNode(Opcodes.DUP_X2);
                    // 1, 4, 2, 3, 4
                    final InsnNode pop = new InsnNode(Opcodes.POP);
                    // 1, 4, 2, 3
                    final InsnNode swap = new InsnNode(Opcodes.SWAP);
                    // 1, 4, 3, 2
                    final InsnNode dup2_x1 = new InsnNode(Opcodes.DUP2_X1);
                    // 1, 3, 2, 4, 3, 2
                    final InsnNode pop2 = new InsnNode(Opcodes.POP2);
                    // 1, 3, 2, 4
                    methodNode.instructions.insertBefore(toPatch, dup_x2);
                    methodNode.instructions.insertBefore(toPatch, pop);
                    methodNode.instructions.insertBefore(toPatch, swap);
                    methodNode.instructions.insertBefore(toPatch, dup2_x1);
                    methodNode.instructions.insertBefore(toPatch, pop2);
                }*/

                for (final MethodInsnNode toPatch : foundSwap4Calls) {
                    System.out.println("Patching call to " + toPatch.owner + "." + toPatch.name + toPatch.desc + " at class " + name);
                    // RGBA to BRGA
                    // Non-double version, doubles require special handling.
                    final Type storeType = Type.getArgumentTypes(toPatch.desc)[0];
                    methodNode.maxLocals++;
                    final int index = methodNode.maxLocals;
                    final LabelNode target = new LabelNode();
                    final FieldInsnNode getFullscreen = new FieldInsnNode(Opcodes.GETSTATIC, "com/zero/retrowrapper/injector/M1ColorTweakInjector", "isMinecraftFullscreen", "Z");
                    final JumpInsnNode skipIfFullscreen = new JumpInsnNode(Opcodes.IFNE, target);
                    final VarInsnNode storeA = new VarInsnNode(storeType.getOpcode(Opcodes.ISTORE), index);
                    final InsnNode dup_x2 = new InsnNode(Opcodes.DUP_X2);
                    final InsnNode pop = new InsnNode(Opcodes.POP);
                    final InsnNode swap = new InsnNode(Opcodes.SWAP);
                    final VarInsnNode loadA = new VarInsnNode(storeType.getOpcode(Opcodes.ILOAD), index);
                    methodNode.instructions.insertBefore(toPatch, getFullscreen);
                    methodNode.instructions.insertBefore(toPatch, skipIfFullscreen);
                    methodNode.instructions.insertBefore(toPatch, storeA);
                    methodNode.instructions.insertBefore(toPatch, dup_x2);
                    methodNode.instructions.insertBefore(toPatch, pop);
                    methodNode.instructions.insertBefore(toPatch, swap);
                    methodNode.instructions.insertBefore(toPatch, loadA);
                    methodNode.instructions.insertBefore(toPatch, target);
                }

                for (final MethodInsnNode toPatch : foundSwap4DoubleCalls) {
                    System.out.println("Patching call to " + toPatch.owner + "." + toPatch.name + toPatch.desc + " at class " + name);
                    // RGBA to BGRA
                    // Never used anyways.
                    // TODO Test
                    // Double version, doubles require special handling.
                    methodNode.maxLocals += 2;
                    final int index = methodNode.maxLocals;
                    methodNode.maxLocals += 2;
                    final int index2 = methodNode.maxLocals;
                    final LabelNode target = new LabelNode();
                    final FieldInsnNode getFullscreen = new FieldInsnNode(Opcodes.GETSTATIC, "com/zero/retrowrapper/injector/M1ColorTweakInjector", "isMinecraftFullscreen", "Z");
                    final JumpInsnNode skipIfFullscreen = new JumpInsnNode(Opcodes.IFNE, target);
                    // RGBA
                    final VarInsnNode storeA = new VarInsnNode(Opcodes.DSTORE, index);
                    // RGB
                    final InsnNode dup2_x2 = new InsnNode(Opcodes.DUP2_X2);
                    // RBGB
                    final InsnNode pop2 = new InsnNode(Opcodes.POP2);
                    // RBG
                    final VarInsnNode storeG = new VarInsnNode(Opcodes.DSTORE, index2);
                    // RB
                    final InsnNode dup2_x2_2 = new InsnNode(Opcodes.DUP2_X2);
                    // BRB
                    final InsnNode pop2_2 = new InsnNode(Opcodes.POP2);
                    // BR
                    final VarInsnNode loadG = new VarInsnNode(Opcodes.DLOAD, index2);
                    // BRG
                    final InsnNode dup2_x2_3 = new InsnNode(Opcodes.DUP2_X2);
                    // BGRG
                    final InsnNode pop2_3 = new InsnNode(Opcodes.POP2);
                    // BGR
                    final VarInsnNode loadA = new VarInsnNode(Opcodes.DLOAD, index);
                    // BGRA
                    methodNode.instructions.insertBefore(toPatch, getFullscreen);
                    methodNode.instructions.insertBefore(toPatch, skipIfFullscreen);
                    methodNode.instructions.insertBefore(toPatch, storeA);
                    methodNode.instructions.insertBefore(toPatch, dup2_x2);
                    methodNode.instructions.insertBefore(toPatch, pop2);
                    methodNode.instructions.insertBefore(toPatch, storeG);
                    methodNode.instructions.insertBefore(toPatch, dup2_x2_2);
                    methodNode.instructions.insertBefore(toPatch, pop2_2);
                    methodNode.instructions.insertBefore(toPatch, loadG);
                    methodNode.instructions.insertBefore(toPatch, dup2_x2_3);
                    methodNode.instructions.insertBefore(toPatch, pop2_3);
                    methodNode.instructions.insertBefore(toPatch, loadA);
                    methodNode.instructions.insertBefore(toPatch, target);
                }
            }

            final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(writer);
            final byte[] bytes = writer.toByteArray();
            final File imageCache = new File(RetroEmulator.getInstance().getCacheDirectory(), name + ".class");

            try (FileOutputStream fos = new FileOutputStream(imageCache)) {
                fos.write(bytes);
            }

            return bytes;
        } catch (final Exception e) {
            System.out.println("Exception while transforming class " + name);
            e.printStackTrace();
            System.out.println(e);
            return bytesOld;
        }
    }

    public static void setFullscreenWrapper(boolean fullscreen) throws LWJGLException {
        Display.setFullscreen(fullscreen);
        isMinecraftFullscreen = fullscreen;
    }

    public static int[] buffImageTweaker(BufferedImage image, int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize) {
        final int[] adaptedFormat = image.getRGB(startX, startY, w, h, rgbArray, offset, scansize);

        if (!isMinecraftFullscreen) {
            for (int i = 0; i < adaptedFormat.length; i++) {
                final int color = adaptedFormat[i];
                adaptedFormat[i] =
                    (color & 0xFF00FF00)   |
                    ((color >> 16) & 0xFF) |
                    ((color & 0xFF) << 16) ;
            }
        }

        return adaptedFormat;
    }
}
