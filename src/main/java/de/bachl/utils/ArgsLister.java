/*
 * Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting.
 * All rights reserved.
 */

package de.bachl.utils;

import java.io.File;

public class ArgsLister {

    public void print() {
        File file = new File(System.getProperty("user.home") + "/.webdeploy/servers/");
        File[] files = file.listFiles();
        for (File f : files) {
            Log.info(f.getName());
        }

    }
}
