package com.zero.retrowrapper.hack;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.apache.commons.lang3.exception.ExceptionUtils;

import net.minecraft.launchwrapper.LogWrapper;

public final class RetroPlayer {
    private Field x, y, z, x2, y2, z2;
    private double ax, ay, az;
    private Object aabb;
    public Class<?> entityClass;
    public Object playerObj;

    private final HackThread thread;
    public Field playerField;
    public Object minecraft;
    private boolean modeFloat;

    public RetroPlayer(HackThread thread) {
        this.thread = thread;
    }

    public void tick() throws IllegalArgumentException, IllegalAccessException, InterruptedException {
        try {
            playerObj = playerField.get(minecraft);

            if (playerObj != null) {
                setAABB();
                ax = getVariable(x2, aabb) - getX();
                ay = getVariable(y2, aabb) - getY();
                az = getVariable(z2, aabb) - getZ();
                thread.setLabelText(getLabelText());
            }
        } catch (final Exception e) {
            LogWrapper.warning("Something went wrong with RetroPlayer on tick: " + ExceptionUtils.getStackTrace(e));
            Thread.sleep(1000);
        }
    }

    private String getLabelText() {
        Double tempX = null, tempY = null, tempZ = null;

        try {
            tempX = Math.floor(getX() * 10) / 10;
            tempY = Math.floor(getY() * 10) / 10;
            tempZ = Math.floor(getZ() * 10) / 10;
        } catch (final Exception e) {
            LogWrapper.warning("Something went wrong with getting the label text from RetroPlayer: " + ExceptionUtils.getStackTrace(e));
        }

        return "<html>Position:<br>&nbsp&nbsp&nbsp;x: " + tempX + "<br>&nbsp&nbsp&nbsp;y: " + tempY + "<br>&nbsp&nbsp&nbsp;z: " + tempZ + "</html>";
    }

    public void setAABB() throws IllegalArgumentException, IllegalAccessException {
        boolean flag2 = false;

        for (final Field f : entityClass.getDeclaredFields()) {
            if (!flag2) {
                if (f.getType().equals(Float.TYPE)) {
                    flag2 = true;
                }
            } else if (!f.getType().isPrimitive()) {
                aabb = f.get(playerObj);
                break;
            }
        }

        if (aabb != null) {
            int doubleCount = 0;

            for (final Field f : aabb.getClass().getDeclaredFields()) {
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

                    case 5:
                        z2 = f;
                        break;

                    default:
                        return;
                    }

                    doubleCount++;
                }
            }
        }
    }

    public boolean isAABBNonNull() {
        return aabb != null;
    }

    private double getX() throws IllegalArgumentException, IllegalAccessException {
        return getVariable(x, aabb);
    }

    private double getY() throws IllegalArgumentException, IllegalAccessException {
        return getVariable(y, aabb);
    }

    private double getZ() throws IllegalArgumentException, IllegalAccessException {
        return getVariable(z, aabb);
    }

    private double getVariable(Field f, Object o) throws IllegalArgumentException, IllegalAccessException {
        if (modeFloat) {
            return ((Float)f.getFloat(o)).doubleValue();
        }

        return f.getDouble(o);
    }

    public void teleport(double dx, double dy, double dz) throws IllegalArgumentException, IllegalAccessException {
        if (modeFloat) {
            x.set(aabb, (float)dx);
            y.set(aabb, (float)dy);
            z.set(aabb, (float)dz);
            x2.set(aabb, (float)(dx + ax));
            y2.set(aabb, (float)(dy + ay));
            z2.set(aabb, (float)(dz + az));
        } else {
            x.set(aabb, dx);
            y.set(aabb, dy);
            z.set(aabb, dz);
            x2.set(aabb, dx + ax);
            y2.set(aabb, dy + ay);
            z2.set(aabb, dz + az);
        }
    }
}
