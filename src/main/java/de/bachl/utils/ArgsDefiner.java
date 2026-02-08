/*
 * Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting.
 * All rights reserved.
 */

package de.bachl.utils;

import java.io.File;
import java.util.HashMap;

import de.bachl.setup.DeployService;
import de.bachl.setup.ProjectProvider;
import de.bachl.setup.SetupProvider;

public class ArgsDefiner {

    private final HashMap<String, String> args;

    public ArgsDefiner(HashMap<String, String> args) {
        this.args = args;
    }

    public void define() {
        if (args.containsKey("--setup")) {
            new SetupProvider(args).setup();
            System.exit(0);
        }
        if (args.containsKey("--setupproject")) {
            new ProjectProvider(args).setup();
            System.exit(0);
        }
        if (args.containsKey("--editproject")) {
            new de.bachl.commands.handlers.EditProjectHandler().handle(args);
            System.exit(0);
        }
        if (args.containsKey("--deploy")) {
            new DeployService().deploy();
            System.exit(0);
        }
        if (args.containsKey("--listservers")) {
            new ArgsLister().print();
            System.exit(0);
        }

        new de.bachl.services.CommandService(args).handle();
    }
}
