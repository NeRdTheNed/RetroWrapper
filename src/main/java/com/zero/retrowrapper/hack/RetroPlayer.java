package com.zero.retrowrapper.hack;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.apache.commons.lang3.exception.ExceptionUtils;

import net.minecraft.launchwrapper.LogWrapper;

final class RetroPlayer {
    private Object aabb;

    private final Field x, y, z, x2, y2, z2;
    private final boolean modeFloat;
    private final HackRunnable thread;
    private final Object minecraft;
    private final Field playerField;
    private final Field aabbField;

    RetroPlayer(HackRunnable thread, Object minecraft, Field playerField, Field aabbField) {
        this.thread = thread;
        this.minecraft = minecraft;
        this.playerField = playerField;
        this.aabbField = aabbField;
        Field xTemp = null;
        Field yTemp = null;
        Field zTemp = null;
        Field x2Temp = null;
        Field y2Temp = null;
        Field z2Temp = null;
        boolean tempMode = false;
        int doubleCount = 0;
        fieldLoop: for (final Field f : aabbField.getType().getDeclaredFields()) {
            if (Modifier.isPublic(f.getModifiers()) && (f.getType().equals(Double.TYPE) || f.getType().equals(Float.TYPE))) {
                if (f.getType().equals(Float.TYPE)) {
                    tempMode = true;
                }

                switch (doubleCount) {
                case 0:
                    xTemp = f;
                    break;

                case 1:
                    yTemp = f;
                    break;

                case 2:
                    zTemp = f;
                    break;

                case 3:
                    x2Temp = f;
                    break;

                case 4:
                    y2Temp = f;
                    break;

                default:
                    z2Temp = f;
                    break fieldLoop;
                }

                doubleCount++;
            }
        }

        x = xTemp;
        y = yTemp;
        z = zTemp;
        x2 = x2Temp;
        y2 = y2Temp;
        z2 = z2Temp;
        modeFloat = tempMode;
    }

    void tick() throws InterruptedException {
        try {
            final Object tempAABB = getAABBFromPlayerOrNull();
            final boolean isTempNotNull = tempAABB != null;

            if (tempAABB != aabb) {
                aabb = tempAABB;
                thread.setTeleportActive(isTempNotNull);

                if (isTempNotNull) {
                    thread.setLabelText(getX(), getY(), getZ());
                } else {
                    thread.setLabelText("null");
                }
            } else if (isTempNotNull) {
                thread.setLabelText(getX(), getY(), getZ());
            }
        } catch (final Exception e) {
            LogWrapper.warning("Something went wrong with RetroPlayer on tick: " + ExceptionUtils.getStackTrace(e));
            Thread.sleep(1000L);
        }
    }

    private Object getAABBFromPlayerOrNull() throws IllegalAccessException {
        final Object playerObj = playerField.get(minecraft);
        return playerObj != null ? aabbField.get(playerObj) : null;
    }

    double getX() throws IllegalAccessException {
        return getVariable(x);
    }

    double getY() throws IllegalAccessException {
        return getVariable(y);
    }

    double getZ() throws IllegalAccessException {
        return getVariable(z);
    }

    private double getX2() throws IllegalAccessException {
        return getVariable(x2);
    }

    private double getY2() throws IllegalAccessException {
        return getVariable(y2);
    }

    private double getZ2() throws IllegalAccessException {
        return getVariable(z2);
    }

    private double getVariable(Field f) throws IllegalAccessException {
        return modeFloat ? f.getFloat(aabb) : f.getDouble(aabb);
    }

    void teleport(double dx, double dy, double dz) throws IllegalAccessException {
        final double ax = getX2() - getX();
        final double ay = getY2() - getY();
        final double az = getZ2() - getZ();
        final double dax = dx + ax;
        final double day = dy + ay;
        final double daz = dz + az;

        if (modeFloat) {
            x.setFloat(aabb, (float) dx);
            y.setFloat(aabb, (float) dy);
            z.setFloat(aabb, (float) dz);
            x2.setFloat(aabb, (float) dax);
            y2.setFloat(aabb, (float) day);
            z2.setFloat(aabb, (float) daz);
        } else {
            x.setDouble(aabb, dx);
            y.setDouble(aabb, dy);
            z.setDouble(aabb, dz);
            x2.setDouble(aabb, dax);
            y2.setDouble(aabb, day);
            z2.setDouble(aabb, daz);
        }
    }
}
