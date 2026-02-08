/*
 * Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting.
 * All rights reserved.
 */

package de.bachl.commands;

import java.util.HashMap;
import com.jcraft.jsch.Session;
import de.bachl.Config.ProjectConfig;

public interface Command {

    /**
     * Executes the command.
     * 
     * @param session       Active SSH session
     * @param args          Command line arguments
     * @param projectConfig Project configuration (may be null)
     * @throws Exception if execution fails
     */
    void execute(Session session, HashMap<String, String> args, ProjectConfig projectConfig) throws Exception;

    /**
     * @return The primary trigger for this command (e.g., "--status")
     */
    String getCommand();

    /**
     * @return A short description for the help menu
     */
    String getDescription();
}
