/* Copyright (c) 2008 lib4j
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * You should have received a copy of The MIT License (MIT) along with this
 * program. If not, see <http://opensource.org/licenses/MIT/>.
 */

package org.libx4j.expect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.lib4j.exec.Processes;
import org.lib4j.expect.ProcessType;
import org.lib4j.expect.RuleType;
import org.lib4j.expect.Script;
import org.lib4j.io.input.NonBlockingInputStream;
import org.lib4j.io.scanner.InputStreamScanner;
import org.lib4j.io.scanner.ScannerHandler;
import org.lib4j.util.ELs;
import org.lib4j.util.HashTree;
import org.lib4j.xml.jaxb.JAXBUtil;

public final class Expect {
  public static void start(final InputStream in, final OutputStream out, final OutputStream err, final ExpectCallback callback, final URL scriptUrl) throws Exception {
    final Script script = JAXBUtil.parse(Script.class, scriptUrl);

    final ProcessType processType = script.getProcess();
    final String exec = processType.getExec().trim();
    final Map<String,String> variables = callback.process(exec);
    final Process process;
    final List<String> args = new ArrayList<String>();
    final StringTokenizer tokenizer = new StringTokenizer(dereference(exec, variables));
    while (tokenizer.hasMoreTokens())
      args.add(tokenizer.nextToken());

    final boolean sync = processType.getFork() != null && "sync".equals(processType.getFork());
    if (exec.startsWith("java")) {
      String className = null;
      final Map<String,String> props = new HashMap<String,String>();
      final List<String> javaArgs = new ArrayList<String>();
      for (String arg : args) {
        if (arg.startsWith("-D")) {
          arg = arg.substring(2);
          final String[] split = arg.split("=", 2);
          props.put(split[0], split[1]);
        }
        else if (arg.matches("([_a-zA-Z0-9]+\\.)+[_a-zA-Z0-9]+")) {
          if (className != null)
            throw new UnknownError("There is a problem with the regex used to determine the final class name. We have matched it twice!!");

          className = arg;
        }
        else if (!"java".equals(arg))
          javaArgs.add(arg);
      }

      if (sync)
        process = Processes.forkSync(in, out, err, false, props, Class.forName(className), javaArgs.toArray(new String[javaArgs.size()]));
      else
        process = Processes.forkAsync(in, out, err, false, props, Class.forName(className), javaArgs.toArray(new String[javaArgs.size()]));
    }
    else {
      if (sync)
        process = Processes.forkSync(in, out, err, false, new String[args.size()]);
      else
        process = Processes.forkAsync(in, out, err, false, args.toArray(new String[args.size()]));
    }

    // This is important: since we are not reading from STDERR, we must start a NonBlockingInputStream
    // on it such that its buffer doesn't fill. This is necessary because the STDERR of the sub-process
    // is teed into 2 input streams that both need to be read from: System.err, and process.getErrorStream()
    try (final NonBlockingInputStream stream = new NonBlockingInputStream(process.getErrorStream(), 1024)) {
      final InputStream stdout = process.getInputStream();

      HashTree.Node<ScannerHandler> firstTreeNode = null;
      final List<RuleType> rules = processType.getRule();
      final Map<String,ScannerHandler> scannerMap = new HashMap<String,ScannerHandler>();
      final Map<String,HashTree.Node<ScannerHandler>> treeNodeMap = new HashMap<String,HashTree.Node<ScannerHandler>>();
      for (final RuleType rule : rules) {
        final ScannerHandler scanner = new ScannerHandler(rule.getExpect()) {
          @Override
          public void match(final String match) throws IOException {
            try {
              String response = rule.getRespond();
              final Map<String,String> variables = callback.rule(rule.getId(), rule.getExpect(), response);
              response = dereference(response, variables);
              if (!response.endsWith("\n"))
                response += "\n";

              process.getOutputStream().write(response.getBytes());
              process.getOutputStream().flush();
            }
            catch (final ParseException e) {
              throw new IOException(e);
            }
          }
        };
        scannerMap.put(rule.getId(), scanner);

        final HashTree.Node<ScannerHandler> treeNode = new HashTree.Node<ScannerHandler>(scanner);
        treeNodeMap.put(rule.getId(), treeNode);
        if (firstTreeNode == null)
          firstTreeNode = treeNode;
      }

      final List<ProcessType.Tree.Node> nodes = processType.getTree().getNode();
      for (final ProcessType.Tree.Node node : nodes) {
        final HashTree.Node<ScannerHandler> treeNode = treeNodeMap.get(node.getRule());
        final List<Object> children = node.getChildren();
        if (children == null)
          continue;

        for (final Object childId : children)
          treeNode.addChild(treeNodeMap.get(childId));
      }

      final HashTree<ScannerHandler> tree = new HashTree<ScannerHandler>();
      tree.addChild(firstTreeNode);

      final InputStreamScanner scanner = new InputStreamScanner(stdout, tree);
      scanner.start();
    }
  }

  private static String dereference(final String string, final Map<String,String> variables) throws ParseException {
    return ELs.dereference(string, variables);
  }

  private Expect() {
  }
}