package com.zero.retrowrapper.injector;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

final class WindowClosingAdapter extends WindowAdapter {
    @Override
    public void windowClosing(WindowEvent e) {
        System.exit(0);
    }
}
