package com.zero.retrowrapper.hack;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import com.zero.retrowrapper.emulator.EmulatorConfig;
import com.zero.retrowrapper.injector.RetroTweakInjectorTarget;
import com.zero.retrowrapper.util.JavaUtil;

import net.minecraft.launchwrapper.LogWrapper;

public final class HackThread extends Thread {
    // TODO Refactor
    private JLabel label;
    private RetroPlayer player;

    private void setupSwingGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (final Exception e) {
            LogWrapper.warning("Could not set look and feel", e);
        }

        final JFrame frame = new JFrame("Retrowrapper");
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
        final JButton b = new JButton("Teleport");
        b.setBounds(50, 202, 200, 40);
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    final float dx = Float.parseFloat(x.getText().replace(",", "").replace(" ", ""));
                    final float dy = Float.parseFloat(y.getText().replace(",", "").replace(" ", ""));
                    final float dz = Float.parseFloat(z.getText().replace(",", "").replace(" ", ""));
                    player.teleport(dx, dy, dz);
                } catch (final Exception ee) {
                    JOptionPane.showMessageDialog(null, "Exception occurred!\n" + ee.getClass().getName() + "\n" + ee.getMessage());
                }
            }
        });
        frame.add(b);
        frame.setVisible(true);
    }

    public void run() {
        player = new RetroPlayer(this);

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    setupSwingGUI();
                }
            });
        } catch (final Exception e) {
            // TODO Better error handling
            LogWrapper.warning("Something went wrong with starting the hack thread", e);
        }

        try {
            final EmulatorConfig config = EmulatorConfig.getInstance();
            config.minecraftField.setAccessible(true);
            player.minecraft = config.minecraftField.get(config.applet);
            final Class<?> mcClass = JavaUtil.getMostSuper(player.minecraft.getClass());
            LogWrapper.fine("Minecraft class: " + mcClass.getName());
            LogWrapper.fine("Mob class: " + config.mobClass);
            player.playerObj = null;
            final Class<?> mobClass = RetroTweakInjectorTarget.getaClass(config.mobClass);

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

            LogWrapper.fine("Player class: " + player.playerObj.getClass().getName());
            player.entityClass = JavaUtil.getMostSuper(mobClass);
            LogWrapper.fine("Entity class: " + player.entityClass.getName());
            player.setAABB();

            if (player.isAABBNonNull()) {
                while (true) {
                    player.tick();
                    Thread.sleep(100);
                }
            }
        } catch (final Exception e) {
            LogWrapper.warning("Something went wrong with the hack thread", e);
        }
    }

    void setLabelText(final String text) {
        try {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    label.setText(text);
                }
            });
        } catch (final Exception e) {
            // TODO Better error handling
            LogWrapper.warning("Something went wrong with setting the label text in the hack thread", e);
        }
    }
}
