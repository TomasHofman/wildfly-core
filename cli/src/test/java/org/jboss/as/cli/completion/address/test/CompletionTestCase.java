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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.completion.mock.MockNode;
import org.junit.Assert;
import org.junit.Test;


/**
 * If candidates contain leading quote, offset should point at the quote character in the buffer,
 * if candidates don't contain leading quite, offset should point at character after the quote.
 *
 * Example 1:
 *
 * buffer: /type="type
 * offset:        ^
 * candidates: typeOne, typeTwo
 *
 * Example 2:
 *
 * buffer: /type="typeT
 * offset:       ^
 * candidates: "typeTwo"
 *
 * @author Tomas Hofman (thofman@redhat.com)
 */
@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
public class CompletionTestCase extends AbstractAddressCompleterTest {

    public CompletionTestCase() throws IOException, CliInitializationException {
        MockNode root = addRoot("type");
        addRoot("test");

        MockNode typeOne = root.addChild("typeOne");
        root.addChild("typeTwo");
        root.addChild("type\"Three");

        MockNode subtype = typeOne.addChild("subtype");
        subtype.addChild("subtypeOne");
    }

    @Test
    public void testNodeNameMultipleCandidates() throws CommandLineException {
        String cmd = "/type=";
        checkCompletion(cmd, cmd.length(), Arrays.asList("type\"Three", "typeOne", "typeTwo"));

        cmd = "/type=type";
        checkCompletion(cmd, cmd.indexOf('=') + 1, Arrays.asList("type\"Three", "typeOne", "typeTwo"));

        cmd = "/type=\"";
        checkCompletion(cmd, cmd.length(), Arrays.asList("type\"Three", "typeOne", "typeTwo"));

        cmd = "/type=\"type";
        checkCompletion(cmd, cmd.indexOf('"') + 1, Arrays.asList("type\"Three", "typeOne", "typeTwo"));

        cmd = "/type=\"typeT";
        checkCompletion(cmd, cmd.indexOf('"'), Arrays.asList("\"typeTwo\""));

        cmd = "/type=\"type\\\"";
        checkCompletion(cmd, cmd.indexOf('"'), Arrays.asList("\"type\\\"Three\""));

        cmd = "/type=\"typeTwo";
        checkCompletion(cmd, cmd.length(), Arrays.asList("\""));

        cmd = "/type=\"typeTwo\"";
        checkCompletion(cmd, cmd.length(), Arrays.asList(":", "/"));
    }

    @Test
    public void testNodeNameSingleCandidate() throws CommandFormatException {
        String cmd = "/type=typeOne/subtype=";
        checkCompletion(cmd, cmd.lastIndexOf('=') + 1, Arrays.asList("subtypeOne"));

        cmd = "/type=typeOne/subtype=subtype";
        checkCompletion(cmd, cmd.lastIndexOf('=') + 1, Arrays.asList("subtypeOne"));

        cmd = "/type=typeOne/subtype=\"";
        checkCompletion(cmd, cmd.lastIndexOf('"') + 1, Arrays.asList("subtypeOne"));

        cmd = "/type=typeOne/subtype=\"sub";
        checkCompletion(cmd, cmd.lastIndexOf('"'), Arrays.asList("\"subtypeOne\""));

        cmd = "/type=typeOne/subtype=\"subtypeOne";
        checkCompletion(cmd, cmd.length(), Arrays.asList("\""));

        cmd = "/type=typeOne/subtype=\"subtypeOne\"";
        checkCompletion(cmd, cmd.length(), Arrays.asList(":", "/"));
    }

    @Test
    public void testNodeTypeMultipleCandidates() throws CommandFormatException {
        String cmd = "/";
        checkCompletion(cmd, 1, Arrays.asList("test", "type"));

        cmd = "/t";
        checkCompletion(cmd, 1, Arrays.asList("test", "type"));

        cmd = "/te";
        checkCompletion(cmd, 1, Arrays.asList("test="));

        cmd = "/\"";
        checkCompletion(cmd, 2, Arrays.asList("test", "type"));

        cmd = "/\"t";
        checkCompletion(cmd, 2, Arrays.asList("test", "type"));

        cmd = "/\"te";
        checkCompletion(cmd, 1, Arrays.asList("\"test\"="));

        cmd = "/\"test";
        checkCompletion(cmd, cmd.length(), Arrays.asList("\""));

        cmd = "/\"test\"";
        checkCompletion(cmd, cmd.length(), Arrays.asList("="));
    }

    @Test
    public void testNodeTypeSingleCandidate() throws CommandFormatException {
        String cmd = "/type=typeOne/";
        checkCompletion(cmd, cmd.lastIndexOf('/') + 1, Arrays.asList("subtype="));

        cmd = "/type=typeOne/sub";
        checkCompletion(cmd, cmd.lastIndexOf('/') + 1, Arrays.asList("subtype="));

        cmd = "/type=typeOne/\""; // FAILS
        checkCompletion(cmd, cmd.lastIndexOf('"'), Arrays.asList("\"subtype\"="));

        cmd = "/type=typeOne/\"sub";
        checkCompletion(cmd, cmd.lastIndexOf('"'), Arrays.asList("\"subtype\"="));

        cmd = "/type=typeOne/\"subtype";
        checkCompletion(cmd, cmd.length(), Arrays.asList("\""));

        cmd = "/type=typeOne/\"subtype\"";
        checkCompletion(cmd, cmd.length(), Arrays.asList("="));
    }

    private void checkCompletion(String cmd, int expectedOffset, List<String> expectedCandidates) throws CommandFormatException {
        ArrayList<String> candidates = new ArrayList<>();
        int offset = complete(cmd, candidates);
        Assert.assertEquals("Wrong offset", expectedOffset, offset);
        Assert.assertEquals("Expected different candidates", expectedCandidates, candidates);
    }

    private int complete(String buffer, List<String> candidates) throws CommandFormatException {
        ctx.parseCommandLine(buffer, false);
        return completer.complete(ctx, buffer, 0, candidates);
    }
}
