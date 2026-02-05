package de.bachl.utils;

import java.io.File;

public class ArgsLister {

    public void print() {
        File file = new File("servers/");
        File[] files = file.listFiles();
        for (File f : files) {
            Log.info(f.getName());
        }

    }
}
