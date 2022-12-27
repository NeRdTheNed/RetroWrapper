package com.zero.retrowrapper.hack;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.concurrent.Callable;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.objectweb.asm.RetroTweakClassWriter;

import com.zero.retrowrapper.injector.RetroTweakInjectorTarget;
import com.zero.retrowrapper.util.JavaUtil;
import com.zero.retrowrapper.util.SwingUtil;

import net.minecraft.launchwrapper.LogWrapper;

public final class HackRunnable implements Runnable {
    static final NumberFormat outputNumberFormat = NumberFormat.getNumberInstance();

    static {
        outputNumberFormat.setMaximumFractionDigits(2);
    }

    // TODO Refactor
    JButton button;

    JTextField xf;
    JTextField yf;
    JTextField zf;

    JLabel xl;
    JLabel yl;
    JLabel zl;

    JButton xrel;
    JButton yrel;
    JButton zrel;

    public void run() {
        try {
            SwingUtilities.invokeAndWait(new SetupSwingRunnable());
        } catch (final Exception e) {
            // TODO Better error handling
            LogWrapper.warning("Something went wrong with starting the hack thread: " + ExceptionUtils.getStackTrace(e));
        }

        try {
            final RetroPlayer player = constructPlayer(this, button);
            xrel.addActionListener(new SetNumListener(xf, new GetXCallable(player)));
            yrel.addActionListener(new SetNumListener(yf, new GetYCallable(player)));
            zrel.addActionListener(new SetNumListener(zf, new GetZCallable(player)));
            button.addActionListener(new TeleportActionListener(player, xf, yf, zf));

            while (true) {
                player.tick();
                Thread.sleep(100L);
            }
        } catch (final Exception e) {
            LogWrapper.warning("Something went wrong with the hack thread: " + ExceptionUtils.getStackTrace(e));
        }
    }

    private static RetroPlayer constructPlayer(HackRunnable self, JButton button) throws Exception {
        RetroTweakInjectorTarget.minecraftField.setAccessible(true);
        final Object minecraft = RetroTweakInjectorTarget.minecraftField.get(RetroTweakInjectorTarget.applet);
        final Class<?> mcClass = JavaUtil.getMostSuper(minecraft.getClass());
        LogWrapper.fine("Minecraft class: " + mcClass.getName());
        button.setText("Finding mob class...");

        // TODO Is this safe?
        while (RetroTweakClassWriter.mobClass == null) {
            Thread.sleep(1000L);
        }

        LogWrapper.fine("Mob class: " + RetroTweakClassWriter.mobClass);
        Object playerObj = null;
        final Class<?> mobClass = RetroTweakInjectorTarget.getaClass(RetroTweakClassWriter.mobClass);
        button.setText("Finding player...");
        Field playerField = null;

        while (playerObj == null) {
            for (final Field f : mcClass.getDeclaredFields()) {
                if (mobClass.isAssignableFrom(f.getType()) || f.getType().equals(mobClass)) {
                    playerField = f;
                    playerObj = f.get(minecraft);
                    break;
                }
            }

            Thread.sleep(1000L);
        }

        LogWrapper.fine("Player class: " + playerObj.getClass().getName());
        final Class<?> entityClass = JavaUtil.getMostSuper(mobClass);
        LogWrapper.fine("Entity class: " + entityClass.getName());
        boolean foundFloat = false;
        Field aabbField = null;

        for (final Field f : entityClass.getDeclaredFields()) {
            if (!foundFloat) {
                if (f.getType().equals(Float.TYPE)) {
                    foundFloat = true;
                }
            } else if (!f.getType().isPrimitive()) {
                aabbField = f;
                break;
            }
        }

        return new RetroPlayer(self, minecraft, playerField, aabbField);
    }

    void setLabelText(final double x, final double y, final double z) {
        setLabelText(outputNumberFormat.format(x), outputNumberFormat.format(y), outputNumberFormat.format(z));
    }

    void setLabelText(final String all) {
        setLabelText(all, all, all);
    }

    void setLabelText(final String x, final String y, final String z) {
        try {
            SwingUtilities.invokeLater(new LabelRunnable(xl, yl, zl, x, y, z));
        } catch (final Exception e) {
            // TODO Better error handling
            LogWrapper.warning("Something went wrong with setting the label text in the hack thread: " + ExceptionUtils.getStackTrace(e));
        }
    }

    void setTeleportActive(boolean active) {
        button.setText(active ? "Teleport" : "No player found");
        button.setEnabled(active);
        xrel.setEnabled(active);
        yrel.setEnabled(active);
        zrel.setEnabled(active);
    }

    private static final class TeleportActionListener implements ActionListener {
        private static final NumberFormat inputNumberFormat = NumberFormat.getNumberInstance();

        private final RetroPlayer player;

        private final JTextField x;
        private final JTextField y;
        private final JTextField z;

        TeleportActionListener(RetroPlayer player, JTextField x, JTextField y, JTextField z) {
            this.player = player;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public void actionPerformed(ActionEvent e) {
            try {
                final double dx = inputNumberFormat.parse(x.getText()).doubleValue();
                final double dy = inputNumberFormat.parse(y.getText()).doubleValue();
                final double dz = inputNumberFormat.parse(z.getText()).doubleValue();
                player.teleport(dx, dy, dz);
            } catch (final ParseException pe) {
                JOptionPane.showMessageDialog(null, "Could not parse number from input!\n" + pe.getClass().getName() + "\n" + pe.getMessage());
            } catch (final Exception ee) {
                JOptionPane.showMessageDialog(null, "Exception occurred!\n" + ee.getClass().getName() + "\n" + ee.getMessage());
            }
        }
    }

    private final class SetupSwingRunnable implements Runnable {
        SetupSwingRunnable() {
        }

        public void run() {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (final Exception e) {
                LogWrapper.warning("Could not set look and feel: " + ExceptionUtils.getStackTrace(e));
            }

            final JFrame frame = new JFrame("RetroWrapper");
            final Dimension dim = new Dimension(654, 310);
            frame.setPreferredSize(dim);
            frame.setMinimumSize(dim);
            frame.setLayout(new GridLayout(0, 3));
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setResizable(true);
            frame.setLocationRelativeTo(null);
            xl = new JLabel("x: null");
            SwingUtil.addJLabelCentered(frame, xl);
            xf = new JTextField();
            SwingUtil.addJTextFieldCentered(frame, xf);
            xrel = new JButton("Copy x coordinate");
            xrel.setEnabled(false);
            SwingUtil.addJButtonCentered(frame, xrel);
            yl = new JLabel("y: null");
            SwingUtil.addJLabelCentered(frame, yl);
            yf = new JTextField();
            SwingUtil.addJTextFieldCentered(frame, yf);
            yrel = new JButton("Copy y coordinate");
            yrel.setEnabled(false);
            SwingUtil.addJButtonCentered(frame, yrel);
            zl = new JLabel("z: null");
            SwingUtil.addJLabelCentered(frame, zl);
            zf = new JTextField();
            SwingUtil.addJTextFieldCentered(frame, zf);
            zrel = new JButton("Copy z coordinate");
            zrel.setEnabled(false);
            SwingUtil.addJButtonCentered(frame, zrel);
            frame.add(new JLabel(""));
            button = new JButton("Setting up hacks...");
            button.setEnabled(false);
            frame.add(button);
            frame.add(new JLabel(""));
            frame.setVisible(true);
        }
    }

    private static final class LabelRunnable implements Runnable {
        private final JLabel xl;
        private final JLabel yl;
        private final JLabel zl;

        private final String x;
        private final String y;
        private final String z;

        LabelRunnable(JLabel xl, JLabel yl, JLabel zl, final String x, final String y, final String z) {
            this.xl = xl;
            this.yl = yl;
            this.zl = zl;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public void run() {
            xl.setText("x: " + x);
            yl.setText("y: " + y);
            zl.setText("z: " + z);
        }
    }

    private static final class SetNumListener implements ActionListener {
        private final JTextField f;
        private final Callable<Double> d;

        SetNumListener(JTextField f, Callable<Double> d) {
            this.f = f;
            this.d = d;
        }

        public void actionPerformed(ActionEvent e) {
            try {
                f.setText(outputNumberFormat.format(d.call()));
            } catch (final Exception e1) {
                LogWrapper.warning("???: " + ExceptionUtils.getStackTrace(e1));
            }
        }
    }

    private static final class GetXCallable implements Callable<Double> {
        private final RetroPlayer player;

        GetXCallable(RetroPlayer player) {
            this.player = player;
        }

        public Double call() throws Exception {
            return player.getX();
        }
    }

    private static final class GetYCallable implements Callable<Double> {
        private final RetroPlayer player;

        GetYCallable(RetroPlayer player) {
            this.player = player;
        }

        public Double call() throws Exception {
            return player.getY();
        }
    }

    private static final class GetZCallable implements Callable<Double> {
        private final RetroPlayer player;

        GetZCallable(RetroPlayer player) {
            this.player = player;
        }

        public Double call() throws Exception {
            return player.getZ();
        }
    }
}
