package com.zero.retrowrapper.hack;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.regex.Pattern;

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

import net.minecraft.launchwrapper.LogWrapper;

public final class HackThread extends Thread {
    // TODO Refactor
    JLabel label;
    private JButton button;
    RetroPlayer player;

    void setupSwingGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (final Exception e) {
            LogWrapper.warning("Could not set look and feel: " + ExceptionUtils.getStackTrace(e));
        }

        final JFrame frame = new JFrame("RetroWrapper");
        final Dimension dim = new Dimension(654, 310);
        frame.setPreferredSize(dim);
        frame.setMinimumSize(dim);
        frame.setLayout(null);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        label = new JLabel("<html>Position:<br>&nbsp&nbsp&nbsp;x: null<br>&nbsp&nbsp&nbsp;y: null<br>&nbsp&nbsp&nbsp;z: null</html>");
        label.setBounds(30, 10, 500, 80);
        frame.add(label);
        final JLabel xl = new JLabel("x:");
        xl.setBounds(30, 103, 50, 20);
        frame.add(xl);
        final JLabel yl = new JLabel("y:");
        yl.setBounds(30, 135, 50, 20);
        frame.add(yl);
        final JLabel zl = new JLabel("z:");
        zl.setBounds(30, 167, 50, 20);
        frame.add(zl);
        final JTextField x = new JTextField();
        x.setBounds(50, 100, 200, 30);
        frame.add(x);
        final JTextField y = new JTextField();
        y.setBounds(50, 132, 200, 30);
        frame.add(y);
        final JTextField z = new JTextField();
        z.setBounds(50, 164, 200, 30);
        frame.add(z);
        button = new JButton("Setting up hacks...");
        button.setBounds(50, 202, 200, 40);
        button.addActionListener(new TeleportActionListener(x, y, z));
        button.setEnabled(false);
        frame.add(button);
        frame.setVisible(true);
    }

    public void run() {
        player = new RetroPlayer(this);

        try {
            SwingUtilities.invokeAndWait(new SetupSwingRunnable());
        } catch (final Exception e) {
            // TODO Better error handling
            LogWrapper.warning("Something went wrong with starting the hack thread: " + ExceptionUtils.getStackTrace(e));
        }

        try {
            RetroTweakInjectorTarget.minecraftField.setAccessible(true);
            player.minecraft = RetroTweakInjectorTarget.minecraftField.get(RetroTweakInjectorTarget.applet);
            final Class<?> mcClass = JavaUtil.getMostSuper(player.minecraft.getClass());
            LogWrapper.fine("Minecraft class: " + mcClass.getName());
            button.setText("Finding mob class...");

            // TODO Is this safe?
            while (RetroTweakClassWriter.mobClass == null) {
                Thread.sleep(1000);
            }

            LogWrapper.fine("Mob class: " + RetroTweakClassWriter.mobClass);
            player.playerObj = null;
            final Class<?> mobClass = RetroTweakInjectorTarget.getaClass(RetroTweakClassWriter.mobClass);
            button.setText("Finding player...");

            while (player.playerObj == null) {
                for (final Field f : mcClass.getDeclaredFields()) {
                    if (mobClass.isAssignableFrom(f.getType()) || f.getType().equals(mobClass)) {
                        player.playerField = f;
                        player.playerObj = f.get(player.minecraft);
                        break;
                    }
                }

                Thread.sleep(1000);
            }

            button.setText("Teleport");
            LogWrapper.fine("Player class: " + player.playerObj.getClass().getName());
            player.entityClass = JavaUtil.getMostSuper(mobClass);
            LogWrapper.fine("Entity class: " + player.entityClass.getName());
            player.setAABB();

            if (player.isAABBNonNull()) {
                button.setEnabled(true);

                while (true) {
                    player.tick();
                    Thread.sleep(100);
                }
            }
        } catch (final Exception e) {
            LogWrapper.warning("Something went wrong with the hack thread: " + ExceptionUtils.getStackTrace(e));
        }
    }

    void setLabelText(final String text) {
        try {
            SwingUtilities.invokeLater(new LabelRunnable(text));
        } catch (final Exception e) {
            // TODO Better error handling
            LogWrapper.warning("Something went wrong with setting the label text in the hack thread: " + ExceptionUtils.getStackTrace(e));
        }
    }

    private final class TeleportActionListener implements ActionListener {
        private final Pattern commaPattern = Pattern.compile(",", Pattern.LITERAL);
        private final Pattern spacePattern = Pattern.compile(" ", Pattern.LITERAL);

        private final JTextField x;
        private final JTextField y;
        private final JTextField z;

        public TeleportActionListener(JTextField x, JTextField y, JTextField z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public void actionPerformed(ActionEvent e) {
            try {
                final float dx = Float.parseFloat(spacePattern.matcher(commaPattern.matcher(x.getText()).replaceAll("")).replaceAll(""));
                final float dy = Float.parseFloat(spacePattern.matcher(commaPattern.matcher(y.getText()).replaceAll("")).replaceAll(""));
                final float dz = Float.parseFloat(spacePattern.matcher(commaPattern.matcher(z.getText()).replaceAll("")).replaceAll(""));
                player.teleport(dx, dy, dz);
            } catch (final Exception ee) {
                JOptionPane.showMessageDialog(null, "Exception occurred!\n" + ee.getClass().getName() + "\n" + ee.getMessage());
            }
        }
    }

    private final class SetupSwingRunnable implements Runnable {
        SetupSwingRunnable() {
        }

        public void run() {
            setupSwingGUI();
        }
    }

    private final class LabelRunnable implements Runnable {
        private final String text;

        public LabelRunnable(String text) {
            this.text = text;
        }

        public void run() {
            label.setText(text);
        }
    }
}
