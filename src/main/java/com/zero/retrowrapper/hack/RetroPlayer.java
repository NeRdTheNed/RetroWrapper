package com.zero.retrowrapper.hack;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.apache.commons.lang3.exception.ExceptionUtils;

import net.minecraft.launchwrapper.LogWrapper;

final class RetroPlayer {
    private Field x, y, z, x2, y2, z2;
    private Object aabb;
    private boolean modeFloat;

    private final HackRunnable thread;
    private final Object minecraft;
    private final Field playerField;
    private final Field aabbField;

    RetroPlayer(HackRunnable thread, Object minecraft, Field playerField, Field aabbField) {
        this.thread = thread;
        this.minecraft = minecraft;
        this.playerField = playerField;
        this.aabbField = aabbField;
    }

    void tick() throws InterruptedException {
        try {
            final Object playerObj = playerField.get(minecraft);
            final Object tempAabb = playerObj != null ? aabbField.get(playerObj) : null;
            final boolean changed = setAABB(tempAabb);

            if (isAABBNonNull()) {
                thread.setLabelText(getX(), getY(), getZ());
            } else if (changed) {
                thread.setLabelText("null");
            }
        } catch (final Exception e) {
            LogWrapper.warning("Something went wrong with RetroPlayer on tick: " + ExceptionUtils.getStackTrace(e));
            Thread.sleep(1000L);
        }
    }

    private boolean setAABB(Object tempAABB) {
        if (tempAABB != aabb) {
            aabb = tempAABB;
            final boolean aabbNotNull = aabb != null;

            if (aabbNotNull) {
                int doubleCount = 0;
                fieldLoop: for (final Field f : aabb.getClass().getDeclaredFields()) {
                    if (Modifier.isPublic(f.getModifiers()) && (f.getType().equals(Double.TYPE) || f.getType().equals(Float.TYPE))) {
                        if (f.getType().equals(Float.TYPE)) {
                            modeFloat = true;
                        }

                        switch (doubleCount) {
                        case 0:
                            x = f;
                            break;

                        case 1:
                            y = f;
                            break;

                        case 2:
                            z = f;
                            break;

                        case 3:
                            x2 = f;
                            break;

                        case 4:
                            y2 = f;
                            break;

                        default:
                            z2 = f;
                            break fieldLoop;
                        }

                        doubleCount++;
                    }
                }
            } else {
                x = y = z = x2 = y2 = z2 = null;
            }

            thread.setTeleportActive(aabbNotNull);
            return true;
        }

        return false;
    }

    boolean isAABBNonNull() {
        return aabb != null;
    }

    double getX() throws IllegalArgumentException, IllegalAccessException {
        return getVariable(x, aabb);
    }

    double getY() throws IllegalArgumentException, IllegalAccessException {
        return getVariable(y, aabb);
    }

    double getZ() throws IllegalArgumentException, IllegalAccessException {
        return getVariable(z, aabb);
    }

    private double getVariable(Field f, Object o) throws IllegalArgumentException, IllegalAccessException {
        return o == null ? 0.0 : modeFloat ? f.getFloat(o) : f.getDouble(o);
    }

    void teleport(double dx, double dy, double dz) throws IllegalArgumentException, IllegalAccessException {
        final double ax = getVariable(x2, aabb) - getX();
        final double ay = getVariable(y2, aabb) - getY();
        final double az = getVariable(z2, aabb) - getZ();
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
