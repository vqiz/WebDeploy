/*
 * Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting.
 * All rights reserved.
 */

package de.bachl;

import java.util.HashMap;

import de.bachl.utils.ArgsConstructor;
import de.bachl.utils.ArgsDefiner;

public class WebDeploy {

    public static void main(String[] argmain) {
      HashMap<String, String> args = new ArgsConstructor(argmain).parse();
      new ArgsDefiner(args).define();
    }

}
