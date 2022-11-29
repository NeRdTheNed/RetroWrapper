package com.zero.retrowrapper.injector;

import java.io.File;
import java.util.ListIterator;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RetroTweakClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.zero.retrowrapper.util.SwingUtil;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LogWrapper;

public final class RetroTweakInjector implements IClassTransformer {
    /**
     *
     * THIS IS MODIFIED VERSION OF INDEVVANILLATWEAKINJECTOR
     *   ALL RIGHTS TO MOJANG
     *
     */

    // TODO @Nullable?
    public byte[] transform(final String name, final String transformedName, final byte[] bytesOld) {
        try {
            if (bytesOld == null) {
                return null;
            }

            final ClassReader cr = new ClassReader(bytesOld);
            final ClassNode classNodeOld = new ClassNode();
            cr.accept(classNodeOld, ClassReader.EXPAND_FRAMES);
            final RetroTweakClassWriter cw = new RetroTweakClassWriter(0, classNodeOld.name.replace('/', '.'));
            // TODO The linter doesn't like this for some reason
            final ClassVisitor s = new NoOpClassVisitor(cw);
            cr.accept(s, 0);
            final byte[] bytes = cw.toByteArray();
            final ClassReader classReader = new ClassReader(bytes);
            final ClassNode classNode = new ClassNode();
            classReader.accept(classNode, ClassReader.EXPAND_FRAMES);

            if (!classNode.interfaces.contains("java/lang/Runnable")) {
                return bytes;
            }

            MethodNode runMethod = null;

            for (final Object methodNode : classNode.methods) {
                final MethodNode m = (MethodNode) methodNode;

                if ("run".equals(m.name)) {
                    runMethod = m;
                    break;
                }
            }

            if (runMethod == null) {
                return bytes;
            }

            LogWrapper.fine("Probably the Minecraft class (it has run && is applet!): " + name);
            @SuppressWarnings("unchecked")
            final ListIterator<AbstractInsnNode> iterator = runMethod.instructions.iterator();
            int firstSwitchJump = -1;

            while (iterator.hasNext()) {
                AbstractInsnNode instruction = iterator.next();

                if (instruction.getOpcode() == Opcodes.TABLESWITCH) {
                    final TableSwitchInsnNode tableSwitchInsnNode = (TableSwitchInsnNode) instruction;
                    firstSwitchJump = runMethod.instructions.indexOf((AbstractInsnNode) tableSwitchInsnNode.labels.get(0));
                } else if ((firstSwitchJump >= 0) && (runMethod.instructions.indexOf(instruction) == firstSwitchJump)) {
                    int endOfSwitch = -1;

                    while (iterator.hasNext()) {
                        instruction = iterator.next();

                        if (instruction.getOpcode() == Opcodes.GOTO) {
                            endOfSwitch = runMethod.instructions.indexOf(((JumpInsnNode) instruction).label);
                            break;
                        }
                    }

                    if (endOfSwitch >= 0) {
                        while ((runMethod.instructions.indexOf(instruction) != endOfSwitch) && iterator.hasNext()) {
                            instruction = iterator.next();
                        }

                        instruction = iterator.next();
                        final MethodInsnNode methodInsNode = new MethodInsnNode(Opcodes.INVOKESTATIC, "com/zero/retrowrapper/injector/RetroTweakInjector", "inject", "()Ljava/io/File;");
                        runMethod.instructions.insertBefore(instruction, methodInsNode);
                        runMethod.instructions.insertBefore(instruction, new VarInsnNode(Opcodes.ASTORE, 2));
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

    public static File inject() throws IllegalArgumentException, IllegalAccessException, ClassNotFoundException {
        LogWrapper.fine("Turning off ImageIO disk-caching");
        ImageIO.setUseCache(false);
        SwingUtil.loadIconsOnFrames();
        LogWrapper.fine("Setting gameDir to: " + Launch.minecraftHome);
        return Launch.minecraftHome;
    }

    private static final class NoOpClassVisitor extends ClassVisitor {
        public NoOpClassVisitor(RetroTweakClassWriter cw) {
            super(Opcodes.ASM4, cw);
        }
    }
}
