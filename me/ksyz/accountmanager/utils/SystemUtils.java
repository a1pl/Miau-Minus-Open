package me.ksyz.accountmanager.utils;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.net.URI;

public class SystemUtils {
    public static void openWebLink(URI url) {
        try {
            Class<?> desktop = Class.forName("java.awt.Desktop");
            Object object = desktop.getMethod("getDesktop").invoke(null);
            desktop.getMethod("browse", URI.class).invoke(object, url);
        } catch (Exception var3) {
        }
    }

    public static void setClipboard(String text) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        } catch (Exception var2) {
        }
    }
}
