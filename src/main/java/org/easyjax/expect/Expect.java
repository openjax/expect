/* Copyright (c) 2008 EasyJAX
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

package org.easyjax.expect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.easyjax.expect_0_2_12.ProcessType;
import org.easyjax.expect_0_2_12.RuleType;
import org.easyjax.expect_0_2_12.Script;
import org.fastjax.exec.Processes;
import org.fastjax.util.ClassLoaders;
import org.fastjax.util.Strings;
import org.fastjax.jaxb.JaxbUtil;

public class Expect {
  public static void start(final InputStream in, final OutputStream out, final OutputStream err, final ExpectCallback callback, final URL scriptUrl) throws Exception {
    final Script script = JaxbUtil.parse(Script.class, scriptUrl);

    final ProcessType processType = script.getProcess();
    final String exec = processType.getExec().trim();
    final Map<String,String> variables = callback.process(exec);
    final List<String> args = new ArrayList<>();
    final StringTokenizer tokenizer = new StringTokenizer(dereference(exec, variables));
    while (tokenizer.hasMoreTokens())
      args.add(tokenizer.nextToken());

    final Process process;
    final boolean sync = processType.getFork() != null && "sync".equals(processType.getFork());
    if (exec.startsWith("java")) {
      String className = null;
      final Map<String,String> props = new HashMap<>();
      final List<String> javaArgs = new ArrayList<>();
      for (final String arg : args) {
        if (arg.startsWith("-D")) {
          final String[] parts = arg.substring(2).split("=", 2);
          props.put(parts[0], parts[1]);
        }
        else if (arg.matches("([_a-zA-Z0-9]+\\.)+[_a-zA-Z0-9]+")) {
          if (className != null)
            throw new IllegalArgumentException("There is a problem with the regex used to determine the final class name. We have matched it twice!!");

          className = arg;
        }
        else if (!"java".equals(arg)) {
          javaArgs.add(arg);
        }
      }

      process = Processes.forkAsync(in, out, err, false, null, null, ClassLoaders.getClassPath(), null, props, Class.forName(className), javaArgs.toArray(new String[javaArgs.size()]));
      if (sync)
        process.waitFor();
    }
    else {
      process = Processes.forkAsync(in, out, err, false, null, null, args.toArray(new String[args.size()]));
      if (sync)
        process.waitFor();
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
      for (final RuleType rule : rules) {
        final ScannerHandler scanner = new ScannerHandler(rule.getExpect()) {
          @Override
          public void match(final String pattern) throws IOException {
            String response = rule.getRespond();
            final Map<String,String> variables = callback.rule(rule.getId(), rule.getExpect(), response);
            response = dereference(response, variables);
            if (!response.endsWith("\n"))
              response += "\n";

            process.getOutputStream().write(response.getBytes());
            process.getOutputStream().flush();
          }
        };
        scannerMap.put(rule.getId(), scanner);

        final ListTree.Node<ScannerHandler> treeNode = new ListTree.Node<>(scanner);
        treeNodeMap.put(rule.getId(), treeNode);
        if (firstTreeNode == null)
          firstTreeNode = treeNode;
      }

      final List<ProcessType.Tree.Node> nodes = processType.getTree().getNode();
      for (final ProcessType.Tree.Node node : nodes) {
        final ListTree.Node<ScannerHandler> treeNode = treeNodeMap.get((String)node.getRule());
        final List<Object> children = node.getChildren();
        if (children == null)
          continue;

        for (final Object childId : children)
          treeNode.addChild(treeNodeMap.get((String)childId));
      }

      final ListTree<ScannerHandler> tree = new ListTree<>();
      tree.addChild(firstTreeNode);

      final InputStreamScanner scanner = new InputStreamScanner(stdout, tree);
      scanner.start();
    }
  }

  private static String dereference(final String string, final Map<String,String> variables) {
    return Strings.derefEL(string, variables);
  }

  private Expect() {
  }
}