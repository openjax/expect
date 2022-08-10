/* Copyright (c) 2008 OpenJAX
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

package org.openjax.expect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.bind.UnmarshalException;

import org.libj.exec.Processes;
import org.libj.lang.Strings;
import org.libj.util.ClassLoaders;
import org.openjax.expect_0_2.ProcessType;
import org.openjax.expect_0_2.RuleType;
import org.openjax.expect_0_2.Script;
import org.openjax.jaxb.xjc.JaxbUtil;

public final class Expect {
  private static final Pattern classNamePattern = Pattern.compile("([_a-zA-Z0-9]+\\.)+[_a-zA-Z0-9]+");

  public static void start(final InputStream in, final OutputStream out, final OutputStream err, final ExpectCallback callback, final URL scriptUrl) throws IOException, UnmarshalException, IllegalArgumentException, ClassNotFoundException, InterruptedException {
    final Script script = JaxbUtil.parse(Script.class, scriptUrl);

    final ProcessType processType = script.getProcess();
    final String exec = processType.getExec().trim();
    final Map<String,String> variables = callback.process(exec);
    final ArrayList<String> args = new ArrayList<>();

    final String command = dereference(exec, variables);
    int end = -1;

    while (end < command.length()) {
      int start = command.indexOf(' ', end + 1);
      if (start == -1)
        start = command.length();

      if (end + 1 == start) {
        end = start;
        continue;
      }

      String arg = command.substring(end + 1, start);
      int i = 0;
      for (; i < arg.length(); ++i) // [N]
        if (!Character.isWhitespace(arg.charAt(i)))
          break;

      final char ch0 = arg.charAt(0);
      if (ch0 == '"') {
        start = Strings.indexOfUnQuoted(command, '"', start + 2 + i);
        arg = command.substring(end + 2, start);
      }
      else if (ch0 == '\'') {
        start = Strings.indexOfUnQuoted(command, '\'', start + 2 + i);
        arg = command.substring(end + 2, start);
      }

      args.add(arg.trim());
      end = start;
    }

    final Process process;
    final boolean sync = processType.getFork() != null && "sync".equals(processType.getFork());
    if (exec.startsWith("java")) {
      String className = null;
      final Map<String,String> props = new HashMap<>();
      final ArrayList<String> javaArgs = new ArrayList<>();
      for (int i = 0, i$ = args.size(); i < i$; ++i) { // [RA]
        final String arg = args.get(i);
        if (arg.startsWith("-D")) {
          final String[] parts = arg.substring(2).split("=", 2);
          props.put(parts[0], parts[1]);
        }
        else if (classNamePattern.matcher(arg).matches()) {
          if (className != null)
            throw new IllegalArgumentException("There is a problem with the regex used to determine the final class name. We have matched it twice!!");

          className = arg;
        }
        else if (!"java".equals(arg)) {
          javaArgs.add(arg);
        }
      }

      process = Processes.forkAsync(in, out, err, false, null, null, ClassLoaders.getClassPath(), null, props, Class.forName(className), javaArgs.toArray(new String[javaArgs.size()]));
    }
    else {
      process = Processes.forkAsync(in, out, err, false, null, null, args.toArray(new String[args.size()]));
    }

    // This is important: since we are not reading from STDERR, we must start a NonBlockingInputStream
    // on it such that its buffer doesn't fill. This is necessary because the STDERR of the sub-process
    // is teed into 2 input streams that both need to be read from: System.err, and process.getErrorStream()
    try (final NonBlockingInputStream stream = new NonBlockingInputStream(process.getErrorStream(), 1024)) {
      final InputStream stdout = process.getInputStream();

      ListTree.Node<ScannerHandler> firstTreeNode = null;
      final List<RuleType> rules = processType.getRule();
      final Map<String,ScannerHandler> scannerMap = new HashMap<>();
      final Map<String,ListTree.Node<ScannerHandler>> treeNodeMap = new HashMap<>();
      for (final RuleType rule : rules) { // [?]
        final ScannerHandler scanner = new ScannerHandler(rule.getExpect()) {
          @Override
          public void match(final String line) throws IOException {
            try {
              String response = rule.getRespond();
              final Map<String,String> variables = callback.rule(rule.getId(), rule.getExpect(), response, line);
              response = dereference(response, variables);

              final OutputStream out = process.getOutputStream();
              out.write(response.getBytes());
              out.flush();
            }
            catch (final InterruptedException e) {
              process.destroy();
            }
          }
        };
        scannerMap.put(rule.getId(), scanner);

        final ListTree.Node<ScannerHandler> treeNode = new ListTree.Node<>(scanner);
        treeNodeMap.put(rule.getId(), treeNode);
        if (firstTreeNode == null)
          firstTreeNode = treeNode;
      }

      final List<ProcessType.Tree.Node> nodes = processType.getTree().getNode();
      for (final ProcessType.Tree.Node node : nodes) { // [?]
        final ListTree.Node<ScannerHandler> treeNode = treeNodeMap.get(((RuleType)node.getRule()).getId());
        final List<Object> children = node.getChildren();
        if (children != null)
          for (int j = 0, j$ = children.size(); j < j$; ++j) // [RA]
            treeNode.addChild(treeNodeMap.get(((RuleType)children.get(j)).getId()));
      }

      final ListTree<ScannerHandler> tree = new ListTree<>();
      tree.addChild(firstTreeNode);

      final InputStreamScanner scanner = new InputStreamScanner(stdout, tree);
      scanner.start();
    }

    if (sync)
      process.waitFor();
  }

  private static String dereference(final String string, final Map<String,String> variables) {
    return Strings.derefEL(string, variables);
  }

  private Expect() {
  }
}