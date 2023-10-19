package com.zero.retrowrapper.injector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ListIterator;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import com.zero.retrowrapper.RetroTweaker;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.LogWrapper;

public final class DisplayTweakInjector implements IClassTransformer {
    /**
     *
     * THIS IS MODIFIED VERSION OF INDEVVANILLATWEAKINJECTOR
     *   ALL RIGHTS TO MOJANG
     *
     */

    /**
     * This is a patch to always use a 24 bit depth buffer when creating the display.
     * This fixes clouds looking strange on AMD video cards.
     * TODO @Nullable?
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
                final Collection<MethodInsnNode> foundDisplayCreateCalls = new ArrayList<MethodInsnNode>();
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

                        if ((opcode == Opcodes.INVOKESTATIC) && "org/lwjgl/opengl/Display".equals(methodOwner)) {
                            if ("()V".equals(methodDesc) && "create".equals(methodName)) {
                                foundDisplayCreateCalls.add(methodInsNode);
                                changed = true;
                            }

                            // Alpha 1.1.1 fix
                            if ("(FFF)V".equals(methodDesc) && "setDisplayConfiguration".equals(methodName)) {
                                final AbstractInsnNode prev1 = methodInsNode.getPrevious();

                                if (prev1.getOpcode() == Opcodes.FCONST_0) {
                                    final AbstractInsnNode prev2 = prev1.getPrevious();

                                    if (prev2.getOpcode() == Opcodes.FCONST_0) {
                                        final AbstractInsnNode prev3 = prev2.getPrevious();

                                        if (prev3.getOpcode() == Opcodes.FCONST_1) {
                                            LogWrapper.fine("Patching call to Display.setDisplayConfiguration(1.0F, 0.0F, 0.0F) at class " + name);
                                            methodNode.instructions.insertBefore(methodInsNode.getNext(), new InsnNode(Opcodes.NOP));
                                            methodNode.instructions.remove(prev3);
                                            methodNode.instructions.remove(prev2);
                                            methodNode.instructions.remove(prev1);
                                            methodNode.instructions.remove(methodInsNode);
                                            changed = true;

                                            if (!"a1.1.1".equals(RetroTweaker.profile)) {
                                                LogWrapper.warning("Applying Alpha 1.1.1 fix to class " + name + " when profile version isn't Alpha 1.1.1");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                for (final MethodInsnNode toPatch : foundDisplayCreateCalls) {
                    LogWrapper.fine("Patching call to Display.create() at class " + name);
                    // Replace calls to Display.create() with Display.create(new PixelFormat().withDepthBits(24))
                    final TypeInsnNode newPixelFormat = new TypeInsnNode(Opcodes.NEW, "org/lwjgl/opengl/PixelFormat");
                    final InsnNode dup = new InsnNode(Opcodes.DUP);
                    final MethodInsnNode initPixelFormat = new MethodInsnNode(Opcodes.INVOKESPECIAL, "org/lwjgl/opengl/PixelFormat", "<init>", "()V");
                    final IntInsnNode push24 = new IntInsnNode(Opcodes.BIPUSH, RetroTweaker.bitDepthFixBits);
                    final MethodInsnNode setDepthBitsTo24 = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "org/lwjgl/opengl/PixelFormat", "withDepthBits", "(I)Lorg/lwjgl/opengl/PixelFormat;");
                    final MethodInsnNode createDisplayWithPixelFormat = new MethodInsnNode(Opcodes.INVOKESTATIC, "org/lwjgl/opengl/Display", "create", "(Lorg/lwjgl/opengl/PixelFormat;)V");
                    methodNode.instructions.insertBefore(toPatch, newPixelFormat);
                    methodNode.instructions.insertBefore(toPatch, dup);
                    methodNode.instructions.insertBefore(toPatch, initPixelFormat);
                    methodNode.instructions.insertBefore(toPatch, push24);
                    methodNode.instructions.insertBefore(toPatch, setDepthBitsTo24);
                    methodNode.instructions.insertBefore(toPatch, createDisplayWithPixelFormat);
                    methodNode.instructions.remove(toPatch);
                }
            }

            if (!changed) {
                return bytesOld;
            }

            final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(writer);
            return writer.toByteArray();
        } catch (final Exception e) {
            LogWrapper.severe("Exception while transforming class " + name + ": " + ExceptionUtils.getStackTrace(e));
            return bytesOld;
        }
    }
}
