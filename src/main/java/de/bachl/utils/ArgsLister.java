/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.utils;

import java.io.File;

public class ArgsLister {

    public void print() {
        File file = new File(System.getProperty("user.home") + "/.webdeploy/servers/");
        if (!file.exists() || !file.isDirectory()) {
            Log.warn("No servers directory found. Run --setup to add a server.");
            return;
        }
        File[] files = file.listFiles();
        if (files == null || files.length == 0) {
            Log.warn("No servers configured yet. Run --setup to add a server.");
            return;
        }
        for (File f : files) {
            if (!f.getName().startsWith(".")) {
                Log.info(f.getName());
            }
        }
    }
}
