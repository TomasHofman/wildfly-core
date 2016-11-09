/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package org.jboss.as.cli.completion.address.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import org.jboss.aesh.console.AeshConsoleCallback;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.terminal.Key;
import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandCompleter;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.CommandRegistry;
import org.jboss.as.cli.completion.mock.MockCommandContext;
import org.jboss.as.cli.impl.CommandContextConfiguration;
import org.jboss.as.cli.impl.Console;
import org.junit.Test;

/**
 * @author Tomas Hofman (thofman@redhat.com)
 */
public class CompletionIntegrationTestCase {

    private Console console;
    private PipedOutputStream outputStream = new PipedOutputStream();
    private PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
    private ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private CommandContext commandContext;

    public CompletionIntegrationTestCase() throws IOException, CliInitializationException {
        CommandContextConfiguration configuration = new CommandContextConfiguration.Builder()
                .setInitConsole(true)
                .build();
//        commandContext = CommandContextFactory.getInstance().newCommandContext(configuration);
        commandContext = new MockCommandContext();
        CommandLineCompleter defaultCommandCompleter = commandContext.getDefaultCommandCompleter();

        Settings settings = new SettingsBuilder()
                .inputStream(pipedInputStream)
                .outputStream(new PrintStream(byteArrayOutputStream))
                .logging(true)
                .create();

        console = Console.Factory.getConsole(commandContext, settings);
        console.isCompletionEnabled();
        console.setCallback(new AeshConsoleCallback() {
            @Override
            public int execute(ConsoleOperation output) throws InterruptedException {
                return 0;
            }
        });
//        console.addCompleter(OperationRequestCompleter.INSTANCE);
        console.addCompleter(new CommandCompleter(new CommandRegistry()));
        console.start();
    }

    @Test
    public void test() throws IOException, InterruptedException, CommandLineException {
//        commandContext.handle("/subsystem=datasources/data-s");

        outputStream.write("/subs".getBytes());
        outputStream.write(Key.CTRL_I.getFirstValue());
        outputStream.flush();

        Thread.sleep(3000);

        System.out.println(byteArrayOutputStream.toString());
    }
}
