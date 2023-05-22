package com.zero.retrowrapper.injector;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ListIterator;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL12;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RetroTweakClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.zero.retrowrapper.RetroTweaker;
import com.zero.retrowrapper.util.JavaUtil;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.LogWrapper;

public final class M1ColorTweakInjector implements IClassTransformer {
    /**
     *
     * THIS IS MODIFIED VERSION OF INDEVVANILLATWEAKINJECTOR
     *   ALL RIGHTS TO MOJANG
     *
     */

    private static final String[] toPatch3 = {
        "glColor3b",
        "glColor3f",
        "glColor3ub"
    };

    private static final String[] toPatch3Double = {
        "glColor3d"
    };

    private static final String[] toPatch4 = {
        "glColor4b",
        "glColor4f",
        "glColor4ub",
        "glClearColor"
    };

    private static final String[] toPatch4Double = {
        "glColor4d"
    };

    public static boolean isMinecraftFullscreen = true;

    private static String reloadTexturesMethodName;
    private static String reloadTexturesClassName;

    private static Object reloadTexturesInstance;
    private static Method reloadTexturesMethod;

    /**
     * TODO WIP patches
     * TODO @Nullable?
     */
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
                final Collection<MethodInsnNode> foundSetFullscreenCalls = new ArrayList<MethodInsnNode>();
                final Collection<MethodInsnNode> foundGlTexImage2DLikeCalls = new ArrayList<MethodInsnNode>();
                final Collection<MethodInsnNode> foundFogFloatBufCalls = new ArrayList<MethodInsnNode>();
                final Collection<MethodInsnNode> foundSwap3Calls = new ArrayList<MethodInsnNode>();
                final Collection<MethodInsnNode> foundSwap3DoubleCalls = new ArrayList<MethodInsnNode>();
                final Collection<MethodInsnNode> foundSwap4Calls = new ArrayList<MethodInsnNode>();
                final Collection<MethodInsnNode> foundSwap4DoubleCalls = new ArrayList<MethodInsnNode>();
                @SuppressWarnings("unchecked")
                final ListIterator<AbstractInsnNode> iterator = methodNode.instructions.iterator();
                boolean hasHashes = false;
                boolean hasPercents = false;
                boolean hasBlur = false;
                boolean hasClamp = false;
                boolean callsImageIO = false;

                while (iterator.hasNext()) {
                    final AbstractInsnNode instruction = iterator.next();
                    final int opcode = instruction.getOpcode();

                    if ("()V".equals(methodNode.desc) && ((methodNode.access & Opcodes.ACC_PUBLIC) != 0)) {
                        if (opcode == Opcodes.LDC) {
                            final LdcInsnNode ldc = (LdcInsnNode) instruction;

                            if (ldc.cst instanceof String) {
                                final String string = (String) ldc.cst;

                                if ("##".equals(string)) {
                                    hasHashes = true;
                                } else if ("%%".equals(string)) {
                                    hasPercents = true;
                                } else if ("%clamp%".equals(string)) {
                                    hasClamp = true;
                                } else if ("%blur%".equals(string)) {
                                    hasBlur = true;
                                }
                            }
                        } else if (opcode == Opcodes.INVOKESTATIC) {
                            final MethodInsnNode methodInsNode = (MethodInsnNode) instruction;

                            if ("javax/imageio/ImageIO".equals(methodInsNode.owner) && "read".equals(methodInsNode.name)) {
                                callsImageIO = true;
                            }
                        }
                    }

                    if ((opcode <= Opcodes.INVOKEINTERFACE) && (opcode >= Opcodes.INVOKEVIRTUAL)) {
                        final MethodInsnNode methodInsNode = (MethodInsnNode) instruction;
                        final String methodOwner = methodInsNode.owner;
                        final String methodName = methodInsNode.name;
                        final String methodDesc = methodInsNode.desc;

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

                                if ("glFog".equals(methodName) && "(ILjava/nio/FloatBuffer;)V".equals(methodDesc)) {
                                    foundFogFloatBufCalls.add(methodInsNode);
                                }

                                if ("glTexImage2D".equals(methodName) && "(IIIIIIIILjava/nio/ByteBuffer;)V".equals(methodDesc)) {
                                    foundGlTexImage2DLikeCalls.add(methodInsNode);
                                }

                                if ("glTexSubImage2D".equals(methodName) && "(IIIIIIIILjava/nio/ByteBuffer;)V".equals(methodDesc)) {
                                    foundGlTexImage2DLikeCalls.add(methodInsNode);
                                }
                            }
                        } else if ((RetroTweakClassWriter.tesClass != null) && ((opcode == Opcodes.INVOKEVIRTUAL) || (opcode == Opcodes.INVOKESPECIAL))) {
                            if (RetroTweakClassWriter.tesClass.equals(methodOwner) && "(IIII)V".equals(methodDesc)) {
                                foundSwap4Calls.add(methodInsNode);
                            }
                        }
                    }
                }

                if (hasHashes && (hasPercents || (hasClamp && hasBlur) || callsImageIO)) {
                    LogWrapper.fine("Found texture reload method at class " + name);
                    reloadTexturesClassName = name;
                    reloadTexturesMethodName = methodNode.name;
                }

                for (final MethodInsnNode toPatch : foundSetFullscreenCalls) {
                    LogWrapper.fine("Hooking fullscreen call at class " + name);
                    final MethodInsnNode methodInsNode = new MethodInsnNode(Opcodes.INVOKESTATIC, "com/zero/retrowrapper/injector/M1ColorTweakInjector", "setFullscreenWrapper", "(Z)V");
                    methodNode.instructions.insertBefore(toPatch, methodInsNode);
                    methodNode.instructions.remove(toPatch);
                }

                for (final MethodInsnNode toPatch : foundGlTexImage2DLikeCalls) {
                    LogWrapper.fine("Patching call to glTexImage2D / glTexSubImage2D at class " + name);
                    final LabelNode target = new LabelNode();
                    final LabelNode noOpenGL12 = new LabelNode();
                    final FieldInsnNode getFullscreen = new FieldInsnNode(Opcodes.GETSTATIC, "com/zero/retrowrapper/injector/M1ColorTweakInjector", "isMinecraftFullscreen", "Z");
                    final JumpInsnNode skipIfFullscreen = new JumpInsnNode(Opcodes.IFNE, target);
                    // Check if OpenGL 1.2 is supported
                    final MethodInsnNode getCapabilities = new MethodInsnNode(Opcodes.INVOKESTATIC, "org/lwjgl/opengl/GLContext", "getCapabilities", "()Lorg/lwjgl/opengl/ContextCapabilities;");
                    final FieldInsnNode getIsOpenGL12 = new FieldInsnNode(Opcodes.GETFIELD, "org/lwjgl/opengl/ContextCapabilities", "OpenGL12", "Z");
                    final JumpInsnNode skipIfNoOpenGL12 = new JumpInsnNode(Opcodes.IFEQ, noOpenGL12);
                    // Change texture type from RGBA to BGRA
                    // Move top two stack values out of the way
                    final InsnNode dup2_x1 = new InsnNode(Opcodes.DUP2_X1);
                    final InsnNode pop2 = new InsnNode(Opcodes.POP2);
                    // Pop the old texture type TODO Validate this is GL11.GL_RGBA
                    final InsnNode popOld = new InsnNode(Opcodes.POP);
                    // Replace with GL12.GL_BGRA
                    final LdcInsnNode loadNew = new LdcInsnNode(GL12.GL_BGRA);
                    // Shuffle value back into position
                    final InsnNode dup_x2 = new InsnNode(Opcodes.DUP_X2);
                    final InsnNode pop = new InsnNode(Opcodes.POP);
                    // Jump to end
                    final JumpInsnNode gotoEnd = new JumpInsnNode(Opcodes.GOTO, target);
                    // Manually change from RGBA to BGRA
                    final MethodInsnNode bindImageTweaker = new MethodInsnNode(Opcodes.INVOKESTATIC, "com/zero/retrowrapper/injector/M1ColorTweakInjector", "bindImageTweaker", "(Ljava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;");

                    if (RetroTweaker.m1PatchMode != RetroTweaker.M1PatchMode.ForceEnable) {
                        methodNode.instructions.insertBefore(toPatch, getFullscreen);
                        methodNode.instructions.insertBefore(toPatch, skipIfFullscreen);
                    }

                    // Check if OpenGL 1.2 is supported
                    methodNode.instructions.insertBefore(toPatch, getCapabilities);
                    methodNode.instructions.insertBefore(toPatch, getIsOpenGL12);
                    methodNode.instructions.insertBefore(toPatch, skipIfNoOpenGL12);
                    // Change texture type from RGBA to BGRA
                    // Move top two stack values out of the way
                    methodNode.instructions.insertBefore(toPatch, dup2_x1);
                    methodNode.instructions.insertBefore(toPatch, pop2);
                    // Pop the old texture type TODO Validate this is GL11.GL_RGBA
                    methodNode.instructions.insertBefore(toPatch, popOld);
                    // Replace with GL12.GL_BGRA
                    methodNode.instructions.insertBefore(toPatch, loadNew);
                    // Shuffle value back into position
                    methodNode.instructions.insertBefore(toPatch, dup_x2);
                    methodNode.instructions.insertBefore(toPatch, pop);
                    // Jump to end
                    methodNode.instructions.insertBefore(toPatch, gotoEnd);
                    // Manually change from RGBA to BGRA
                    methodNode.instructions.insertBefore(toPatch, noOpenGL12);
                    methodNode.instructions.insertBefore(toPatch, bindImageTweaker);
                    methodNode.instructions.insertBefore(toPatch, target);
                }

                // New variable indices
                // TODO refactor
                int newVarIndex1 = -1;
                int newVarIndex2 = -1;
                int newVarIndex3 = -1;
                int newVarIndex4 = -1;

                for (final MethodInsnNode toPatch : foundFogFloatBufCalls) {
                    LogWrapper.fine("Patching call to " + toPatch.owner + "." + toPatch.name + toPatch.desc + " at class " + name);
                    // RGBA to BRGA
                    final LabelNode target = new LabelNode();
                    final FieldInsnNode getFullscreen = new FieldInsnNode(Opcodes.GETSTATIC, "com/zero/retrowrapper/injector/M1ColorTweakInjector", "isMinecraftFullscreen", "Z");
                    final JumpInsnNode skipIfFullscreen = new JumpInsnNode(Opcodes.IFNE, target);
                    // Load float values from buffer
                    final InsnNode dupBuffer1 = new InsnNode(Opcodes.DUP);
                    final MethodInsnNode getValue1 = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/nio/FloatBuffer", "get", "()F");
                    final InsnNode swapValue1 = new InsnNode(Opcodes.SWAP);
                    final InsnNode dupBuffer2 = new InsnNode(Opcodes.DUP);
                    final MethodInsnNode getValue2 = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/nio/FloatBuffer", "get", "()F");
                    final InsnNode swapValue2 = new InsnNode(Opcodes.SWAP);
                    final InsnNode dupBuffer3 = new InsnNode(Opcodes.DUP);
                    final MethodInsnNode getValue3 = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/nio/FloatBuffer", "get", "()F");
                    final InsnNode swapValue3 = new InsnNode(Opcodes.SWAP);
                    final InsnNode dupBuffer4 = new InsnNode(Opcodes.DUP);
                    final MethodInsnNode getValue4 = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/nio/FloatBuffer", "get", "()F");
                    final InsnNode swapValue4 = new InsnNode(Opcodes.SWAP);

                    if (newVarIndex1 == -1) {
                        newVarIndex1 = methodNode.maxLocals;
                        methodNode.maxLocals++;
                    }

                    // Store reference to buffer
                    final VarInsnNode storeBuffer = new VarInsnNode(Opcodes.ASTORE, newVarIndex1);
                    // RGBA -> ARGB because values get consumed in reverse order when putting float values in buffer
                    final InsnNode swapBA = new InsnNode(Opcodes.SWAP);

                    if (newVarIndex2 == -1) {
                        newVarIndex2 = methodNode.maxLocals;
                        methodNode.maxLocals++;
                    }

                    final VarInsnNode storeB = new VarInsnNode(Opcodes.FSTORE, newVarIndex2);
                    final InsnNode dupX2A = new InsnNode(Opcodes.DUP_X2);
                    final InsnNode popA = new InsnNode(Opcodes.POP);
                    final VarInsnNode loadB = new VarInsnNode(Opcodes.FLOAD, newVarIndex2);
                    // Load reference to buffer
                    final VarInsnNode loadBuffer1 = new VarInsnNode(Opcodes.ALOAD, newVarIndex1);
                    final InsnNode dupBuffer5 = new InsnNode(Opcodes.DUP);
                    // Clear buffer
                    final MethodInsnNode clearBuffer = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/nio/FloatBuffer", "clear", "()Ljava/nio/Buffer;");
                    final InsnNode popReturn1 = new InsnNode(Opcodes.POP);
                    // Put float values back in buffer
                    final InsnNode swapBuffer1 = new InsnNode(Opcodes.SWAP);
                    final MethodInsnNode putValue1 = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/nio/FloatBuffer", "put", "(F)Ljava/nio/FloatBuffer;");
                    final InsnNode swapBuffer2 = new InsnNode(Opcodes.SWAP);
                    final MethodInsnNode putValue2 = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/nio/FloatBuffer", "put", "(F)Ljava/nio/FloatBuffer;");
                    final InsnNode swapBuffer3 = new InsnNode(Opcodes.SWAP);
                    final MethodInsnNode putValue3 = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/nio/FloatBuffer", "put", "(F)Ljava/nio/FloatBuffer;");
                    final InsnNode swapBuffer4 = new InsnNode(Opcodes.SWAP);
                    final MethodInsnNode putValue4 = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/nio/FloatBuffer", "put", "(F)Ljava/nio/FloatBuffer;");
                    // Flip buffer
                    final MethodInsnNode flipBuffer = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/nio/FloatBuffer", "flip", "()Ljava/nio/Buffer;");
                    final InsnNode popReturn2 = new InsnNode(Opcodes.POP);
                    final VarInsnNode loadBuffer2 = new VarInsnNode(Opcodes.ALOAD, newVarIndex1);

                    if (RetroTweaker.m1PatchMode != RetroTweaker.M1PatchMode.ForceEnable) {
                        methodNode.instructions.insertBefore(toPatch, getFullscreen);
                        methodNode.instructions.insertBefore(toPatch, skipIfFullscreen);
                    }

                    methodNode.instructions.insertBefore(toPatch, dupBuffer1);
                    methodNode.instructions.insertBefore(toPatch, getValue1);
                    methodNode.instructions.insertBefore(toPatch, swapValue1);
                    methodNode.instructions.insertBefore(toPatch, dupBuffer2);
                    methodNode.instructions.insertBefore(toPatch, getValue2);
                    methodNode.instructions.insertBefore(toPatch, swapValue2);
                    methodNode.instructions.insertBefore(toPatch, dupBuffer3);
                    methodNode.instructions.insertBefore(toPatch, getValue3);
                    methodNode.instructions.insertBefore(toPatch, swapValue3);
                    methodNode.instructions.insertBefore(toPatch, dupBuffer4);
                    methodNode.instructions.insertBefore(toPatch, getValue4);
                    methodNode.instructions.insertBefore(toPatch, swapValue4);
                    methodNode.instructions.insertBefore(toPatch, storeBuffer);
                    methodNode.instructions.insertBefore(toPatch, swapBA);
                    methodNode.instructions.insertBefore(toPatch, storeB);
                    methodNode.instructions.insertBefore(toPatch, dupX2A);
                    methodNode.instructions.insertBefore(toPatch, popA);
                    methodNode.instructions.insertBefore(toPatch, loadB);
                    methodNode.instructions.insertBefore(toPatch, loadBuffer1);
                    methodNode.instructions.insertBefore(toPatch, dupBuffer5);
                    methodNode.instructions.insertBefore(toPatch, clearBuffer);
                    methodNode.instructions.insertBefore(toPatch, popReturn1);
                    methodNode.instructions.insertBefore(toPatch, swapBuffer1);
                    methodNode.instructions.insertBefore(toPatch, putValue1);
                    methodNode.instructions.insertBefore(toPatch, swapBuffer2);
                    methodNode.instructions.insertBefore(toPatch, putValue2);
                    methodNode.instructions.insertBefore(toPatch, swapBuffer3);
                    methodNode.instructions.insertBefore(toPatch, putValue3);
                    methodNode.instructions.insertBefore(toPatch, swapBuffer4);
                    methodNode.instructions.insertBefore(toPatch, putValue4);
                    methodNode.instructions.insertBefore(toPatch, flipBuffer);
                    methodNode.instructions.insertBefore(toPatch, popReturn2);
                    methodNode.instructions.insertBefore(toPatch, loadBuffer2);

                    if (RetroTweaker.m1PatchMode != RetroTweaker.M1PatchMode.ForceEnable) {
                        methodNode.instructions.insertBefore(toPatch, target);
                    }
                }

                for (final MethodInsnNode toPatch : foundSwap3Calls) {
                    // RGB to BGR
                    // Non-double version, doubles require special handling.
                    final AbstractInsnNode p1 = toPatch.getPrevious();
                    final AbstractInsnNode p2 = p1.getPrevious();
                    final AbstractInsnNode p3 = p2.getPrevious();

                    if (!JavaUtil.isOpcodeLoadIns(p2) || !JavaUtil.doLoadInsMatch(p1, p3)) {
                        LogWrapper.fine("Patching call to " + toPatch.owner + "." + toPatch.name + toPatch.desc + " at class " + name);
                        final AbstractInsnNode[] reorderLoadIns = convLoadInsOrNull(new AbstractInsnNode[] {p3, p2, p1});

                        if (reorderLoadIns != null) {
                            reorder3LoadIns(methodNode, reorderLoadIns, toPatch, p1, p2, p3);
                        } else {
                            swap3Type1(methodNode, toPatch);
                        }
                    }
                }

                for (final MethodInsnNode toPatch : foundSwap3DoubleCalls) {
                    // RGB to BGR
                    // Double version, doubles require special handling.
                    final AbstractInsnNode p1 = toPatch.getPrevious();
                    final AbstractInsnNode p2 = p1.getPrevious();
                    final AbstractInsnNode p3 = p2.getPrevious();

                    if (!JavaUtil.isOpcodeLoadIns(p2) || !JavaUtil.doLoadInsMatch(p1, p3)) {
                        LogWrapper.fine("Patching call to " + toPatch.owner + "." + toPatch.name + toPatch.desc + " at class " + name);
                        final AbstractInsnNode[] reorderLoadIns = convLoadInsOrNull(new AbstractInsnNode[] {p3, p2, p1});

                        if (reorderLoadIns != null) {
                            reorder3LoadIns(methodNode, reorderLoadIns, toPatch, p1, p2, p3);
                        } else {
                            if (newVarIndex1 == -1) {
                                newVarIndex1 = methodNode.maxLocals;
                                methodNode.maxLocals++;
                            }

                            if (newVarIndex2 == -1) {
                                newVarIndex2 = methodNode.maxLocals;
                                methodNode.maxLocals++;
                            }

                            swap3Type2(methodNode, toPatch, newVarIndex1);
                        }
                    }
                }

                for (final MethodInsnNode toPatch : foundSwap4Calls) {
                    // RGBA to BRGA
                    // Non-double version, doubles require special handling.
                    final AbstractInsnNode p1 = toPatch.getPrevious();
                    final AbstractInsnNode p2 = p1.getPrevious();
                    final AbstractInsnNode p3 = p2.getPrevious();
                    final AbstractInsnNode p4 = p3.getPrevious();

                    if (!JavaUtil.areAllOpcodesLoadIns(p1, p3) || !JavaUtil.doLoadInsMatch(p2, p4)) {
                        LogWrapper.fine("Patching call to " + toPatch.owner + "." + toPatch.name + toPatch.desc + " at class " + name);
                        final AbstractInsnNode[] reorderLoadIns = convLoadInsOrNull(new AbstractInsnNode[] {p4, p3, p2});
                        final boolean isP1Load = JavaUtil.isOpcodeLoadIns(p1);

                        if (isP1Load && (reorderLoadIns != null)) {
                            reorder3LoadIns(methodNode, reorderLoadIns, p1, p2, p3, p4);
                        } else if (isP1Load) {
                            swap3Type1(methodNode, p1);
                        } else {
                            if (newVarIndex1 == -1) {
                                newVarIndex1 = methodNode.maxLocals;
                                methodNode.maxLocals++;
                            }

                            final Type storeType = Type.getArgumentTypes(toPatch.desc)[0];
                            swap4Type1(methodNode, toPatch, storeType, newVarIndex1);
                        }
                    }
                }

                for (final MethodInsnNode toPatch : foundSwap4DoubleCalls) {
                    // RGBA to BGRA
                    // Never used anyways.
                    // TODO Test
                    // Double version, doubles require special handling.
                    final AbstractInsnNode p1 = toPatch.getPrevious();
                    final AbstractInsnNode p2 = p1.getPrevious();
                    final AbstractInsnNode p3 = p2.getPrevious();
                    final AbstractInsnNode p4 = p3.getPrevious();

                    if (!JavaUtil.areAllOpcodesLoadIns(p1, p3) || !JavaUtil.doLoadInsMatch(p2, p4)) {
                        LogWrapper.fine("Patching call to " + toPatch.owner + "." + toPatch.name + toPatch.desc + " at class " + name);
                        final AbstractInsnNode[] reorderLoadIns = convLoadInsOrNull(new AbstractInsnNode[] {p4, p3, p2});
                        final boolean isP1Load = JavaUtil.isOpcodeLoadIns(p1);

                        if (isP1Load && (reorderLoadIns != null)) {
                            reorder3LoadIns(methodNode, reorderLoadIns, p1, p2, p3, p4);
                        } else if (isP1Load) {
                            if (newVarIndex1 == -1) {
                                newVarIndex1 = methodNode.maxLocals;
                                methodNode.maxLocals++;
                            }

                            if (newVarIndex2 == -1) {
                                newVarIndex2 = methodNode.maxLocals;
                                methodNode.maxLocals++;
                            }

                            swap3Type2(methodNode, p1, newVarIndex1);
                        } else {
                            if (newVarIndex1 == -1) {
                                newVarIndex1 = methodNode.maxLocals;
                                methodNode.maxLocals++;
                            }

                            if (newVarIndex2 == -1) {
                                newVarIndex2 = methodNode.maxLocals;
                                methodNode.maxLocals++;
                            }

                            if (newVarIndex3 == -1) {
                                newVarIndex3 = methodNode.maxLocals;
                                methodNode.maxLocals++;
                            }

                            if (newVarIndex4 == -1) {
                                newVarIndex4 = methodNode.maxLocals;
                                methodNode.maxLocals++;
                            }

                            swap4Type2(methodNode, toPatch, newVarIndex1, newVarIndex3);
                        }
                    }
                }
            }

            final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(writer);
            return writer.toByteArray();
        } catch (final Exception e) {
            LogWrapper.severe("Exception while transforming class " + name + ": " + ExceptionUtils.getStackTrace(e));
            return bytesOld;
        }
    }

    public static void setFullscreenWrapper(boolean fullscreen) throws LWJGLException {
        Display.setFullscreen(fullscreen);
        isMinecraftFullscreen = (RetroTweaker.m1PatchMode == RetroTweaker.M1PatchMode.EnableWindowedInverted) ^ fullscreen;

        if (((reloadTexturesInstance == null) || (reloadTexturesMethod == null)) && (reloadTexturesMethodName != null) && (reloadTexturesClassName != null)) {
            try {
                final Class<?> classWithReloadTextureMethod = RetroTweakInjectorTarget.getaClass(reloadTexturesClassName);
                reloadTexturesMethod = classWithReloadTextureMethod.getMethod(reloadTexturesMethodName);
                RetroTweakInjectorTarget.minecraftField.setAccessible(true);
                final Object minecraft = RetroTweakInjectorTarget.minecraftField.get(RetroTweakInjectorTarget.applet);
                final Class<?> mcClass = JavaUtil.getMostSuper(minecraft.getClass());

                for (final Field field : mcClass.getDeclaredFields()) {
                    if (classWithReloadTextureMethod.isAssignableFrom(field.getType()) || field.getType().equals(classWithReloadTextureMethod)) {
                        reloadTexturesInstance = field.get(minecraft);
                        break;
                    }
                }
            } catch (final Exception e) {
                LogWrapper.warning("Exception while trying to get reload textures method: " + ExceptionUtils.getStackTrace(e));
            }
        }

        if ((reloadTexturesInstance != null) && (reloadTexturesMethod != null)) {
            try {
                reloadTexturesMethod.invoke(reloadTexturesInstance);
            } catch (final Exception e) {
                LogWrapper.warning("Exception while trying to invoke reload textures method: " + ExceptionUtils.getStackTrace(e));
            }
        } else {
            LogWrapper.warning("Could not find reload textures method");
        }
    }

    private static void swap3Type1(MethodNode methodNode, AbstractInsnNode toPatch) {
        final LabelNode target = new LabelNode();
        final FieldInsnNode getFullscreen = new FieldInsnNode(Opcodes.GETSTATIC, "com/zero/retrowrapper/injector/M1ColorTweakInjector", "isMinecraftFullscreen", "Z");
        final JumpInsnNode skipIfFullscreen = new JumpInsnNode(Opcodes.IFNE, target);
        // RGB
        final InsnNode dupX2B = new InsnNode(Opcodes.DUP_X2);
        // BRGB
        final InsnNode popB = new InsnNode(Opcodes.POP);
        // BRG
        final InsnNode swapRG = new InsnNode(Opcodes.SWAP);

        // BGR
        if (RetroTweaker.m1PatchMode != RetroTweaker.M1PatchMode.ForceEnable) {
            methodNode.instructions.insertBefore(toPatch, getFullscreen);
            methodNode.instructions.insertBefore(toPatch, skipIfFullscreen);
        }

        methodNode.instructions.insertBefore(toPatch, dupX2B);
        methodNode.instructions.insertBefore(toPatch, popB);
        methodNode.instructions.insertBefore(toPatch, swapRG);

        if (RetroTweaker.m1PatchMode != RetroTweaker.M1PatchMode.ForceEnable) {
            methodNode.instructions.insertBefore(toPatch, target);
        }
    }

    private static void swap3Type2(MethodNode methodNode, AbstractInsnNode toPatch, int index) {
        final LabelNode target = new LabelNode();
        final FieldInsnNode getFullscreen = new FieldInsnNode(Opcodes.GETSTATIC, "com/zero/retrowrapper/injector/M1ColorTweakInjector", "isMinecraftFullscreen", "Z");
        final JumpInsnNode skipIfFullscreen = new JumpInsnNode(Opcodes.IFNE, target);
        // RGB
        final InsnNode dup2X2B1 = new InsnNode(Opcodes.DUP2_X2);
        // RBGB
        final InsnNode pop2B1 = new InsnNode(Opcodes.POP2);
        // RBG
        final VarInsnNode storeG = new VarInsnNode(Opcodes.DSTORE, index);
        // RB
        final InsnNode dup2X2B2 = new InsnNode(Opcodes.DUP2_X2);
        // BRB
        final InsnNode pop2B2 = new InsnNode(Opcodes.POP2);
        // BR
        final VarInsnNode loadG = new VarInsnNode(Opcodes.DLOAD, index);
        // BRG
        final InsnNode dup2X2G = new InsnNode(Opcodes.DUP2_X2);
        // BGRG
        final InsnNode pop2G = new InsnNode(Opcodes.POP2);

        // BGR
        if (RetroTweaker.m1PatchMode != RetroTweaker.M1PatchMode.ForceEnable) {
            methodNode.instructions.insertBefore(toPatch, getFullscreen);
            methodNode.instructions.insertBefore(toPatch, skipIfFullscreen);
        }

        methodNode.instructions.insertBefore(toPatch, dup2X2B1);
        methodNode.instructions.insertBefore(toPatch, pop2B1);
        methodNode.instructions.insertBefore(toPatch, storeG);
        methodNode.instructions.insertBefore(toPatch, dup2X2B2);
        methodNode.instructions.insertBefore(toPatch, pop2B2);
        methodNode.instructions.insertBefore(toPatch, loadG);
        methodNode.instructions.insertBefore(toPatch, dup2X2G);
        methodNode.instructions.insertBefore(toPatch, pop2G);

        if (RetroTweaker.m1PatchMode != RetroTweaker.M1PatchMode.ForceEnable) {
            methodNode.instructions.insertBefore(toPatch, target);
        }
    }

    private static void swap4Type1(MethodNode methodNode, AbstractInsnNode toPatch, Type storeType, int index) {
        final LabelNode target = new LabelNode();
        final FieldInsnNode getFullscreen = new FieldInsnNode(Opcodes.GETSTATIC, "com/zero/retrowrapper/injector/M1ColorTweakInjector", "isMinecraftFullscreen", "Z");
        final JumpInsnNode skipIfFullscreen = new JumpInsnNode(Opcodes.IFNE, target);
        // RGBA
        final VarInsnNode storeA = new VarInsnNode(storeType.getOpcode(Opcodes.ISTORE), index);
        // RGB
        final InsnNode dupX2B = new InsnNode(Opcodes.DUP_X2);
        // BRGB
        final InsnNode popB = new InsnNode(Opcodes.POP);
        // BRG
        final InsnNode swapRG = new InsnNode(Opcodes.SWAP);
        // BGR
        final VarInsnNode loadA = new VarInsnNode(storeType.getOpcode(Opcodes.ILOAD), index);
        // BGRA

        if (RetroTweaker.m1PatchMode != RetroTweaker.M1PatchMode.ForceEnable) {
            methodNode.instructions.insertBefore(toPatch, getFullscreen);
            methodNode.instructions.insertBefore(toPatch, skipIfFullscreen);
        }

        methodNode.instructions.insertBefore(toPatch, storeA);
        methodNode.instructions.insertBefore(toPatch, dupX2B);
        methodNode.instructions.insertBefore(toPatch, popB);
        methodNode.instructions.insertBefore(toPatch, swapRG);
        methodNode.instructions.insertBefore(toPatch, loadA);

        if (RetroTweaker.m1PatchMode != RetroTweaker.M1PatchMode.ForceEnable) {
            methodNode.instructions.insertBefore(toPatch, target);
        }
    }

    private static void swap4Type2(MethodNode methodNode, AbstractInsnNode toPatch, int index, int index2) {
        final LabelNode target = new LabelNode();
        final FieldInsnNode getFullscreen = new FieldInsnNode(Opcodes.GETSTATIC, "com/zero/retrowrapper/injector/M1ColorTweakInjector", "isMinecraftFullscreen", "Z");
        final JumpInsnNode skipIfFullscreen = new JumpInsnNode(Opcodes.IFNE, target);
        // RGBA
        final VarInsnNode storeA = new VarInsnNode(Opcodes.DSTORE, index);
        // RGB
        final InsnNode dup2X2B1 = new InsnNode(Opcodes.DUP2_X2);
        // RBGB
        final InsnNode pop2B1 = new InsnNode(Opcodes.POP2);
        // RBG
        final VarInsnNode storeG = new VarInsnNode(Opcodes.DSTORE, index2);
        // RB
        final InsnNode dup2X2B2 = new InsnNode(Opcodes.DUP2_X2);
        // BRB
        final InsnNode pop2B2 = new InsnNode(Opcodes.POP2);
        // BR
        final VarInsnNode loadG = new VarInsnNode(Opcodes.DLOAD, index2);
        // BRG
        final InsnNode dup2X2G = new InsnNode(Opcodes.DUP2_X2);
        // BGRG
        final InsnNode pop2G = new InsnNode(Opcodes.POP2);
        // BGR
        final VarInsnNode loadA = new VarInsnNode(Opcodes.DLOAD, index);

        // BGRA
        if (RetroTweaker.m1PatchMode != RetroTweaker.M1PatchMode.ForceEnable) {
            methodNode.instructions.insertBefore(toPatch, getFullscreen);
            methodNode.instructions.insertBefore(toPatch, skipIfFullscreen);
        }

        methodNode.instructions.insertBefore(toPatch, storeA);
        methodNode.instructions.insertBefore(toPatch, dup2X2B1);
        methodNode.instructions.insertBefore(toPatch, pop2B1);
        methodNode.instructions.insertBefore(toPatch, storeG);
        methodNode.instructions.insertBefore(toPatch, dup2X2B2);
        methodNode.instructions.insertBefore(toPatch, pop2B2);
        methodNode.instructions.insertBefore(toPatch, loadG);
        methodNode.instructions.insertBefore(toPatch, dup2X2G);
        methodNode.instructions.insertBefore(toPatch, pop2G);
        methodNode.instructions.insertBefore(toPatch, loadA);

        if (RetroTweaker.m1PatchMode != RetroTweaker.M1PatchMode.ForceEnable) {
            methodNode.instructions.insertBefore(toPatch, target);
        }
    }

    private static void reorder3LoadIns(MethodNode methodNode, AbstractInsnNode[] reorderLoadIns, AbstractInsnNode toPatch, AbstractInsnNode p1, AbstractInsnNode p2, AbstractInsnNode p3) {
        final LabelNode normalLoad = new LabelNode();
        final LabelNode callMethod = new LabelNode();
        final FieldInsnNode getFullscreen = new FieldInsnNode(Opcodes.GETSTATIC, "com/zero/retrowrapper/injector/M1ColorTweakInjector", "isMinecraftFullscreen", "Z");
        final JumpInsnNode skipIfFullscreen = new JumpInsnNode(Opcodes.IFNE, normalLoad);
        final JumpInsnNode jumpToCallMethod = new JumpInsnNode(Opcodes.GOTO, callMethod);

        if (RetroTweaker.m1PatchMode != RetroTweaker.M1PatchMode.ForceEnable) {
            methodNode.instructions.insertBefore(p3, getFullscreen);
            methodNode.instructions.insertBefore(p3, skipIfFullscreen);
        }

        methodNode.instructions.insertBefore(p3, reorderLoadIns[0]);
        methodNode.instructions.insertBefore(p3, reorderLoadIns[1]);
        methodNode.instructions.insertBefore(p3, reorderLoadIns[2]);

        if (RetroTweaker.m1PatchMode != RetroTweaker.M1PatchMode.ForceEnable) {
            methodNode.instructions.insertBefore(p3, jumpToCallMethod);
            methodNode.instructions.insertBefore(p3, normalLoad);
            methodNode.instructions.insertBefore(toPatch, callMethod);
        } else {
            methodNode.instructions.remove(p1);
            methodNode.instructions.remove(p2);
            methodNode.instructions.remove(p3);
        }
    }

    private static AbstractInsnNode[] convLoadInsOrNull(AbstractInsnNode[] from) {
        if (from.length != 3) {
            LogWrapper.severe("from.length was not 3!");
            return null;
        }

        final AbstractInsnNode l1 = JavaUtil.cloneLoadInsOrNull(from[0]);

        if (l1 != null) {
            final AbstractInsnNode l2 = JavaUtil.cloneLoadInsOrNull(from[1]);

            if (l2 != null) {
                final AbstractInsnNode l3 = JavaUtil.cloneLoadInsOrNull(from[2]);

                if (l3 != null) {
                    return new AbstractInsnNode[] {l3, l2, l1};
                }
            }
        }

        return null;
    }

    public static ByteBuffer bindImageTweaker(ByteBuffer in) {
        if (!isMinecraftFullscreen || (RetroTweaker.m1PatchMode == RetroTweaker.M1PatchMode.ForceEnable)) {
            for (int i = 0; i < in.limit(); i += 4) {
                final byte B = in.get(i);
                in.put(i, in.get(i + 2)).put(i + 2, B);
            }
        }

        return in;
    }
}
