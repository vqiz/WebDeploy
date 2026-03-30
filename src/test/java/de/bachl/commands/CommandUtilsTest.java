/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.commands;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommandUtilsTest {

    @Mock
    Session session;

    @Mock
    ChannelExec channel;

    @BeforeEach
    void setUpMocks() throws Exception {
        when(session.openChannel("exec")).thenReturn(channel);
    }

    private void configureChannel(String output, int exitCode) throws Exception {
        InputStream is = new ByteArrayInputStream(output.getBytes());
        when(channel.getInputStream()).thenReturn(is);
        // isClosed: first call returns false (so we enter the loop once), then true
        when(channel.isClosed()).thenReturn(false, true);
        when(channel.getExitStatus()).thenReturn(exitCode);
    }

    // -----------------------------------------------------------------------
    // sendCommand
    // -----------------------------------------------------------------------

    @Test
    void sendCommand_withStreamOutputTrue_doesNotThrowOnSuccess() throws Exception {
        configureChannel("some output", 0);
        assertDoesNotThrow(() -> CommandUtils.sendCommand("echo hello", session, true));
        verify(channel).connect();
        verify(channel).disconnect();
    }

    @Test
    void sendCommand_withStreamOutputFalse_doesNotThrowOnSuccessExitCode() throws Exception {
        configureChannel("", 0);
        assertDoesNotThrow(() -> CommandUtils.sendCommand("ls", session, false));
    }

    @Test
    void sendCommand_withStreamOutputFalse_throwsOnNonZeroExitCode() throws Exception {
        configureChannel("", 1);
        Exception ex = assertThrows(Exception.class,
                () -> CommandUtils.sendCommand("bad-cmd", session, false));
        assertTrue(ex.getMessage().contains("Exit code"), "Exception message should mention exit code");
    }

    @Test
    void sendCommand_withStreamOutputTrue_doesNotThrowEvenOnNonZeroExit() throws Exception {
        // When streamOutput=true the non-zero exit is silently ignored
        configureChannel("output", 127);
        assertDoesNotThrow(() -> CommandUtils.sendCommand("bad-cmd", session, true));
    }

    @Test
    void sendCommand_setsCommandOnChannel() throws Exception {
        configureChannel("", 0);
        CommandUtils.sendCommand("df -h", session, false);
        verify(channel).setCommand("df -h");
    }

    // -----------------------------------------------------------------------
    // runCommandWithOutput
    // -----------------------------------------------------------------------

    @Test
    void runCommandWithOutput_returnsOutputString() throws Exception {
        configureChannel("hello from server", 0);
        String result = CommandUtils.runCommandWithOutput("echo hello", session);
        assertEquals("hello from server", result);
    }

    @Test
    void runCommandWithOutput_returnsEmptyStringForNoOutput() throws Exception {
        configureChannel("", 0);
        String result = CommandUtils.runCommandWithOutput("true", session);
        assertEquals("", result);
    }

    @Test
    void runCommandWithOutput_trimsTrailingWhitespace() throws Exception {
        configureChannel("  trimmed  \n", 0);
        String result = CommandUtils.runCommandWithOutput("cmd", session);
        assertEquals("trimmed", result);
    }

    @Test
    void runCommandWithOutput_connectsAndDisconnectsChannel() throws Exception {
        configureChannel("out", 0);
        CommandUtils.runCommandWithOutput("pwd", session);
        verify(channel).connect();
        verify(channel).disconnect();
    }
}
