package com.zero.retrowrapper.util;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class JavaUtil {

    // TODO proper generic usage
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Object> Class<? super Object> getMostSuper(final Class<T> toGet) {
        Class toReturn = toGet;

        if (toReturn != null) {
            for (Class superClass = toReturn; !Object.class.equals(superClass); superClass = superClass.getSuperclass()) {
                toReturn = superClass;
            }
        }

        return toReturn;
    }

    public static boolean areAllOpcodesLoadIns(AbstractInsnNode... ins) {
        for (final AbstractInsnNode in : ins) {
            if (!isOpcodeLoadIns(in)) {
                return false;
            }
        }

        return true;
    }

    public static boolean areAllOpcodesLoadIns(int... ins) {
        for (final int in : ins) {
            if (!isOpcodeLoadIns(in)) {
                return false;
            }
        }

        return true;
    }

    public static boolean isOpcodeLoadIns(AbstractInsnNode ins) {
        return isOpcodeLoadIns(ins.getOpcode());
    }

    public static boolean isOpcodeLoadIns(int opcode) {
        return (opcode >= Opcodes.ACONST_NULL) && (opcode <= Opcodes.ALOAD);
    }

    public static AbstractInsnNode cloneLoadInsOrNull(AbstractInsnNode ins) {
        return isOpcodeLoadIns(ins) ? ins.clone(null) : null;
    }

    public static boolean doLoadInsMatch(AbstractInsnNode load1, AbstractInsnNode load2) {
        final int opcode = load1.getOpcode();

        if (opcode != load2.getOpcode()) {
            return false;
        }

        switch (opcode) {
        case Opcodes.ACONST_NULL:
        case Opcodes.ICONST_M1:
        case Opcodes.ICONST_0:
        case Opcodes.ICONST_1:
        case Opcodes.ICONST_2:
        case Opcodes.ICONST_3:
        case Opcodes.ICONST_4:
        case Opcodes.ICONST_5:
        case Opcodes.LCONST_0:
        case Opcodes.LCONST_1:
        case Opcodes.FCONST_0:
        case Opcodes.FCONST_1:
        case Opcodes.FCONST_2:
        case Opcodes.DCONST_0:
        case Opcodes.DCONST_1:
            return true;

        case Opcodes.BIPUSH:
        case Opcodes.SIPUSH:
            final IntInsnNode ldi1 = (IntInsnNode) load1;
            final IntInsnNode ldi2 = (IntInsnNode) load2;
            return ldi1.operand == ldi2.operand;

        case Opcodes.LDC:
            final LdcInsnNode ldc1 = (LdcInsnNode) load1;
            final LdcInsnNode ldc2 = (LdcInsnNode) load2;
            return ldc1.cst.equals(ldc2.cst);

        case Opcodes.ILOAD:
        case Opcodes.LLOAD:
        case Opcodes.FLOAD:
        case Opcodes.DLOAD:
        case Opcodes.ALOAD:
            final VarInsnNode ldv1 = (VarInsnNode) load1;
            final VarInsnNode ldv2 = (VarInsnNode) load2;
            return ldv1.var == ldv2.var;

        default:
            return false;
        }
    }

    private JavaUtil() {
        // As this is a helper class, there should be no reason to instantiate an instance of it.
    }
}
