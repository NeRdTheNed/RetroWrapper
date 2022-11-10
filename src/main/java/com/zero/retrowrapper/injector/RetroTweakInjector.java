package com.zero.retrowrapper.injector;

import static org.objectweb.asm.Opcodes.ASM4;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.TABLESWITCH;

import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RetroTweakClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.zero.retrowrapper.util.SwingUtil;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

public final class RetroTweakInjector implements IClassTransformer {
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

            final ClassReader cr = new ClassReader(bytesOld);
            final ClassNode classNodeOld = new ClassNode();
            cr.accept(classNodeOld, ClassReader.EXPAND_FRAMES);
            final RetroTweakClassWriter cw = new RetroTweakClassWriter(0, classNodeOld.name.replace('/', '.'));
            // TODO The linter doesn't like this for some reason
            final ClassVisitor s = new ClassVisitor(ASM4, cw) {};
            cr.accept(s, 0);
            byte[] bytes = cw.toByteArray();
            final ClassReader classReader = new ClassReader(bytes);
            final ClassNode classNode = new ClassNode();
            classReader.accept(classNode, ClassReader.EXPAND_FRAMES);

            // Patch calls to setNativeCursor when running on MacOS. This prevents some versions of Minecraft from crashing,
            // because setNativeCursor throws an IllegalStateException, and Minecraft doesn't handle it.
            // TODO Hide cursor when it should be hidden
            if (SystemUtils.IS_OS_MAC) {
                for (final Object methodNodeO : classNode.methods) {
                    final MethodNode methodNode = (MethodNode) methodNodeO;
                    final List<MethodInsnNode> foundMethodCalls = new ArrayList<MethodInsnNode>();
                    @SuppressWarnings("unchecked")
                    final ListIterator<AbstractInsnNode> iterator = methodNode.instructions.iterator();

                    while (iterator.hasNext()) {
                        final AbstractInsnNode instruction = iterator.next();

                        if (instruction.getOpcode() == Opcodes.INVOKESTATIC) {
                            final MethodInsnNode methodInsNode = (MethodInsnNode) instruction;

                            if ("org/lwjgl/input/Mouse".equals(methodInsNode.owner) && "setNativeCursor".equals(methodInsNode.name) && "(Lorg/lwjgl/input/Cursor;)Lorg/lwjgl/input/Cursor;".equals(methodInsNode.desc)) {
                                foundMethodCalls.add(methodInsNode);
                            }
                        }
                    }

                    for (final MethodInsnNode toPatch : foundMethodCalls) {
                        System.out.println("Patching call to setNativeCursor at class " + name);
                        // Check if the method deliberately loaded null. This implies the cursor should be shown.
                        final int shouldHide;

                        if (toPatch.getPrevious().getOpcode() == Opcodes.ACONST_NULL) {
                            shouldHide = 0;
                        } else {
                            shouldHide = 1;
                        }

                        final LdcInsnNode loadShouldHide = new LdcInsnNode(Integer.valueOf(shouldHide));
                        final MethodInsnNode methodInsNode = new MethodInsnNode(INVOKESTATIC, "com/zero/retrowrapper/injector/RetroTweakInjector", "setNativeCursorPatch", "(Lorg/lwjgl/input/Cursor;Z)Lorg/lwjgl/input/Cursor;");
                        methodNode.instructions.insertBefore(toPatch, loadShouldHide);
                        methodNode.instructions.insertBefore(toPatch, methodInsNode);
                        methodNode.instructions.remove(toPatch);
                    }
                }
            }

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(writer);
            bytes = writer.toByteArray();

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

            System.out.println("Probably the Minecraft class (it has run && is applet!): " + name);
            @SuppressWarnings("unchecked")
            final ListIterator<AbstractInsnNode> iterator = runMethod.instructions.iterator();
            int firstSwitchJump = -1;

            while (iterator.hasNext()) {
                AbstractInsnNode instruction = iterator.next();

                if (instruction.getOpcode() == TABLESWITCH) {
                    final TableSwitchInsnNode tableSwitchInsnNode = (TableSwitchInsnNode) instruction;
                    firstSwitchJump = runMethod.instructions.indexOf((AbstractInsnNode) tableSwitchInsnNode.labels.get(0));
                } else if ((firstSwitchJump >= 0) && (runMethod.instructions.indexOf(instruction) == firstSwitchJump)) {
                    int endOfSwitch = -1;

                    while (iterator.hasNext()) {
                        instruction = iterator.next();

                        if (instruction.getOpcode() == GOTO) {
                            endOfSwitch = runMethod.instructions.indexOf(((JumpInsnNode) instruction).label);
                            break;
                        }
                    }

                    if (endOfSwitch >= 0) {
                        while ((runMethod.instructions.indexOf(instruction) != endOfSwitch) && iterator.hasNext()) {
                            instruction = iterator.next();
                        }

                        instruction = iterator.next();
                        // TODO Figure out which version of ASM RetroWrapper should target
                        @SuppressWarnings("deprecation")
                        final MethodInsnNode methodInsNode = new MethodInsnNode(INVOKESTATIC, "com/zero/retrowrapper/injector/RetroTweakInjector", "inject", "()Ljava/io/File;");
                        runMethod.instructions.insertBefore(instruction, methodInsNode);
                        runMethod.instructions.insertBefore(instruction, new VarInsnNode(ASTORE, 2));
                    }
                }
            }

            writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(writer);
            return writer.toByteArray();
        } catch (final Exception e) {
            System.out.println("Exception while transforming class " + name);
            e.printStackTrace();
            System.out.println(e);
            return bytesOld;
        }
    }

    public static File inject() throws IllegalArgumentException, IllegalAccessException, ClassNotFoundException {
        System.out.println("Turning off ImageIO disk-caching");
        ImageIO.setUseCache(false);
        SwingUtil.loadIconsOnFrames();
        System.out.println("Setting gameDir to: " + Launch.minecraftHome);
        return Launch.minecraftHome;
    }

    public static Cursor setNativeCursorPatch(Cursor cursor, boolean shouldHide) throws LWJGLException {
        try {
            final java.awt.Cursor useCursor = shouldHide ? hiddenCursor : normalCursor;
            Display.getParent().setCursor(useCursor);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        try {
            return Mouse.setNativeCursor(cursor);
        } catch (final IllegalStateException e) {
            //throw new LWJGLException(e);
            return cursor;
        }
    }
}
