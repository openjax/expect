/* Copyright (c) 2006 OpenJAX
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
import java.util.List;

public class InputStreamScanner extends Thread {
  private final InputStream in;
  private List<ListTree.Node<ScannerHandler>> currentNodes;

  public InputStreamScanner(final InputStream in, final ListTree<ScannerHandler> handlers) {
    super(InputStreamScanner.class.getSimpleName());
    this.in = in;
    this.currentNodes = handlers == null ? null : handlers.getChildren();
  }

  private boolean onMatch(final String line, final List<ListTree.Node<ScannerHandler>> nodes) throws IOException {
    boolean match = false;
    for (final ListTree.Node<ScannerHandler> node : nodes) {
      if (node.getValue() == null) {
        for (final ListTree.Node<ScannerHandler> child : node.getChildren())
          onMatch(line, child.getChildren());
      }
      else if (line.matches(node.getValue().getPattern())) {
        match = true;
        node.getValue().match(line);
        currentNodes = node.getChildren();
      }
    }

    return match;
  }

  @Override
  public void run() {
    final StringBuilder builder = new StringBuilder();
    try {
      char ch;
      while ((ch = (char)in.read()) != -1) {
        if (ch == '\n')
          builder.setLength(0);
        else if (ch != ' ' || builder.length() != 0)
          builder.append(ch);

        if (currentNodes == null)
          continue;

        if (onMatch(builder.toString(), currentNodes)) {
          if (currentNodes == null)
            return;

          builder.setLength(0);
        }
      }
    }
    catch (final Exception e) {
      if ("Pipe broken".equals(e.getMessage()))
        return;

      throw new IllegalStateException(e);
    }
    finally {
      synchronized (this) {
        try {
          notifyAll();
        }
        catch (final IllegalMonitorStateException e) {
        }
      }
    }
  }
}