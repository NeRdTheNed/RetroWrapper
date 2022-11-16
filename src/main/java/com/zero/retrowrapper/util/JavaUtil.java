package com.zero.retrowrapper.util;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

public final class JavaUtil {

    public static Class<?> getMostSuper(Class<?> toGet) {
        while (true) {
            if (toGet.getSuperclass().equals(Object.class)) {
                break;
            }

            toGet = toGet.getSuperclass();
        }

        return toGet;
    }

    public static AbstractInsnNode cloneLoadInsOrNull(AbstractInsnNode ins) {
        final int opcode = ins.getOpcode();

        if ((opcode >= Opcodes.ACONST_NULL) && (opcode <= Opcodes.SALOAD)) {
            return ins.clone(null);
        }

        return null;
    }

    private JavaUtil() {
        // As this is a helper class, there should be no reason to instantiate an instance of it.
    }
}
