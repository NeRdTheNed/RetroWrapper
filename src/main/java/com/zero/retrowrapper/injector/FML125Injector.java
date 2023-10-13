package com.zero.retrowrapper.injector;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ListIterator;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LogWrapper;

/** Based on the patch used by the Fabric loader */
public final class FML125Injector implements IClassTransformer {
    public static final String FML_PATCH = "FML patch: ";

    public static File getMinecraftJar() {
        File file = null;

        for (final URL url : Launch.classLoader.getSources()) {
            try {
                file = new File(url.toURI());

                if ("minecraft.jar".equals(file.getName())) {
                    return file;
                }
            } catch (final URISyntaxException e) {
                LogWrapper.warning(FML_PATCH + "Issue when searching for Minecraft file: " + ExceptionUtils.getStackTrace(e));
            }
        }

        LogWrapper.warning(FML_PATCH + "Could not find minecraft.jar in injector, returning " + file + " instead");
        return file;
    }

    // Fixes methods which look for modloader's location through a specific method call
    // TODO very bad code
    private static byte[] fixModLoader(byte[] bytesOld, String name) {
        if (bytesOld == null) {
            return null;
        }

        try {
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

                    if (opcode == Opcodes.INVOKESPECIAL) {
                        final MethodInsnNode methodInsNode = (MethodInsnNode) instruction;
                        final String methodOwner = methodInsNode.owner;
                        final String methodName = methodInsNode.name;
                        final String methodDesc = methodInsNode.desc;

                        if ("java/io/File".equals(methodOwner) && "<init>".equals(methodName) && "(Ljava/net/URI;)V".equals(methodDesc)) {
                            final AbstractInsnNode prev1 = instruction.getPrevious();
                            final int opcodePrev1 = prev1.getOpcode();

                            if (opcodePrev1 == Opcodes.INVOKEVIRTUAL) {
                                final MethodInsnNode prevMethod1 = (MethodInsnNode) prev1;
                                final String prevOwner1 = prevMethod1.owner;
                                final String prevName1 = prevMethod1.name;
                                final String prevDesc1 = prevMethod1.desc;

                                if ("java/net/URL".equals(prevOwner1) && "toURI".equals(prevName1) && "()Ljava/net/URI;".equals(prevDesc1)) {
                                    final AbstractInsnNode prev2 = prev1.getPrevious();
                                    final int opcodePrev2 = prev2.getOpcode();

                                    if (opcodePrev2 == Opcodes.INVOKEVIRTUAL) {
                                        final MethodInsnNode prevMethod2 = (MethodInsnNode) prev2;
                                        final String prevOwner2 = prevMethod2.owner;
                                        final String prevName2 = prevMethod2.name;
                                        final String prevDesc2 = prevMethod2.desc;

                                        if ("java/security/CodeSource".equals(prevOwner2) && "getLocation".equals(prevName2) && "()Ljava/net/URL;".equals(prevDesc2)) {
                                            final AbstractInsnNode prev3 = prev2.getPrevious();
                                            final int opcodePrev3 = prev3.getOpcode();

                                            if (opcodePrev3 == Opcodes.INVOKEVIRTUAL) {
                                                final MethodInsnNode prevMethod3 = (MethodInsnNode) prev3;
                                                final String prevOwner3 = prevMethod3.owner;
                                                final String prevName3 = prevMethod3.name;
                                                final String prevDesc3 = prevMethod3.desc;

                                                if ("java/security/ProtectionDomain".equals(prevOwner3) && "getCodeSource".equals(prevName3) && "()Ljava/security/CodeSource;".equals(prevDesc3)) {
                                                    final AbstractInsnNode prev4 = prev3.getPrevious();
                                                    final int opcodePrev4 = prev4.getOpcode();

                                                    if (opcodePrev4 == Opcodes.INVOKEVIRTUAL) {
                                                        final MethodInsnNode prevMethod4 = (MethodInsnNode) prev4;
                                                        final String prevOwner4 = prevMethod4.owner;
                                                        final String prevName4 = prevMethod4.name;
                                                        final String prevDesc4 = prevMethod4.desc;

                                                        if ("java/lang/Class".equals(prevOwner4) && "getProtectionDomain".equals(prevName4) && "()Ljava/security/ProtectionDomain;".equals(prevDesc4)) {
                                                            final AbstractInsnNode prev5 = prev4.getPrevious();
                                                            final int opcodePrev5 = prev5.getOpcode();

                                                            if (opcodePrev5 == Opcodes.LDC) {
                                                                final LdcInsnNode prev5LDC = (LdcInsnNode) prev5;

                                                                if (prev5LDC.cst instanceof Type) {
                                                                    final Type type = (Type) prev5LDC.cst;
                                                                    final int sort = type.getSort();

                                                                    if (sort == Type.OBJECT) {
                                                                        final String prev5Type = type.getClassName();

                                                                        if ("ModLoader".equals(prev5Type)) {
                                                                            final AbstractInsnNode prev6 = prev5.getPrevious();
                                                                            final int opcodePrev6 = prev6.getOpcode();

                                                                            if (opcodePrev6 == Opcodes.DUP) {
                                                                                final AbstractInsnNode prev7 = prev6.getPrevious();
                                                                                final int opcodePrev7 = prev7.getOpcode();

                                                                                if (opcodePrev7 == Opcodes.NEW) {
                                                                                    final TypeInsnNode prev7type = (TypeInsnNode) prev7;

                                                                                    if ("java/io/File".equals(prev7type.desc)) {
                                                                                        LogWrapper.fine(FML_PATCH + "Found modloader location patch point");
                                                                                        final MethodInsnNode findMinecraftJar = new MethodInsnNode(Opcodes.INVOKESTATIC, "com/zero/retrowrapper/injector/FML125Injector", "getMinecraftJar", "()Ljava/io/File;");
                                                                                        methodNode.instructions.insertBefore(methodInsNode, findMinecraftJar);
                                                                                        methodNode.instructions.remove(methodInsNode);
                                                                                        methodNode.instructions.remove(prev1);
                                                                                        methodNode.instructions.remove(prev2);
                                                                                        methodNode.instructions.remove(prev3);
                                                                                        methodNode.instructions.remove(prev4);
                                                                                        methodNode.instructions.remove(prev5);
                                                                                        methodNode.instructions.remove(prev6);
                                                                                        methodNode.instructions.remove(prev7);
                                                                                        changed = true;
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!changed) {
                return bytesOld;
            }

            final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(writer);
            return writer.toByteArray();
        } catch (final Exception e) {
            LogWrapper.severe(FML_PATCH + "Exception while transforming class " + name + ": " + ExceptionUtils.getStackTrace(e));
            return bytesOld;
        }
    }


    public byte[] transform(final String name, final String transformedName, final byte[] bytesOld) {
        if (bytesOld == null) {
            return null;
        }

        byte[] transformed = bytesOld;

        if ("cpw.mods.fml.common.ModClassLoader".equals(name)) {
            try {
                final String classLoaderName = FML125ClassLoader.class.getName();
                final byte[] loader = Launch.classLoader.getClassBytes(classLoaderName);

                if (loader == null) {
                    LogWrapper.severe(FML_PATCH + "Replacement class loader bytes was null, unable to patch");
                } else {
                    final String classLoaderInternalName = classLoaderName.replace('.', '/');
                    final ClassReader classReader = new ClassReader(loader);
                    final ClassNode patchedClassLoader = new ClassNode();
                    classReader.accept(patchedClassLoader, ClassReader.EXPAND_FRAMES);
                    final ClassNode remappedClassLoader = new ClassNode();
                    patchedClassLoader.accept(new RemappingClassAdapter(remappedClassLoader, new Remapper() {
                        @Override
                        public String map(String internalName) {
                            return classLoaderInternalName.equals(internalName) ? "cpw/mods/fml/common/ModClassLoader" : internalName;
                        }
                    }));
                    final ClassWriter writer = new ClassWriter(0);
                    remappedClassLoader.accept(writer);
                    transformed = writer.toByteArray();
                }
            } catch (final IOException e) {
                LogWrapper.severe(FML_PATCH + "Issue when getting class bytes for patched class loader: " + ExceptionUtils.getStackTrace(e));
            }
        }

        return fixModLoader(transformed, name);
    }

}
