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

package org.openjax.support.expect;

import java.util.ArrayList;
import java.util.List;

public class ListTree<T> {
  private List<Node<T>> children;

  public List<Node<T>> getChildren() {
    return children;
  }

  public boolean hasChildren() {
    return children != null;
  }

  public void addChild(final Node<T> node) {
    if (children == null)
      children = new ArrayList<>();

    children.add(node);
  }

  public Node<T> getChild(final int index) {
    return children.get(index);
  }

  public static class Node<T> {
    private final T value;
    private List<Node<T>> children;

    public Node(final T value) {
      this.value = value;
    }

    public boolean hasChildren() {
      return children != null;
    }

    public List<Node<T>> getChildren() {
      return children;
    }

    public T getValue() {
      return value;
    }

    public void addChild(final Node<T> node) {
      if (children == null)
        children = new ArrayList<>();

      children.add(node);
    }

    public Node<T> getChild(final int index) {
      return children.get(index);
    }
  }
}