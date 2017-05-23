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

package org.safris.commons.expect;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public final class ExpectSimulator {
  private static List<Prompt> prompts = new ArrayList<Prompt>();

  private static final class Prompt {
    private List<String> answers = new ArrayList<String>();
    private final String prompt;
    private final String matchLeft;
    private Prompt left;
    private final String matchRight;
    private Prompt right;

    public Prompt(final String prompt, final String matchLeft, Prompt left, final String matchRight, final Prompt right) {
      this.prompt = prompt;
      this.matchLeft = matchLeft;
      this.left = left;
      this.matchRight = matchRight;
      this.right = right;
      prompts.add(this);
    }

    public String getPrompt() {
      return prompt;
    }

    public void setLeft(final Prompt left) {
      this.left = left;
    }

    public void setRight(final Prompt right) {
      this.right = right;
    }

    public Prompt getNext(final String line) {
      if (line.matches(matchLeft)) {
        answers.add(line);
        return left;
      }

      if (line.matches(matchRight)) {
        answers.add(line);
        return right;
      }

      return this;
    }

    public void printAnswers() {
      for (final String answer : answers)
        System.out.println(answer);
    }
  }

  private static final Prompt r2 = new Prompt("What is the name of the person?", ".*", null, ".*", null);
  private static final Prompt r14 = new Prompt("Quit?", "[yY]", null, "[nN]", null);
  private static final Prompt r13 = new Prompt("Print the roster?", "[yY]", r14, "[nN]", r2);
  private static final Prompt r12 = new Prompt("Will ${person} need a ride?", "[yY]", r13, "[nN]", r13);
  private static final Prompt r11 = new Prompt("Will ${person} neet to be driving?", "[yY]", r12, "[nN]", r13);
  private static final Prompt r10 = new Prompt("Will ${person} be drinking?", "[yY]", r11, "[nN]", r13);
  private static final Prompt r9 = new Prompt("Will ${person} bring stuff to the bbq?", "[yY]", r10, "[nN]", r10);
  private static final Prompt r8 = new Prompt("On what date will you see ${person}?", ".*", r9, ".*", r9);
  private static final Prompt r5 = new Prompt("Will the ${person} be attending the bbq?", "[yY]", r8, "[nN]", r2);
  private static final Prompt r7 = new Prompt("What is ${person}'s age?", ".*", r5, ".*", r5);
  private static final Prompt r6 = new Prompt("Is this a boy or a girl?", "[bB]", r7, "[gG]", r7);
  private static final Prompt r4 = new Prompt("Is the person a male or female?", "[mM]", r5, "[fF]", r5);
  private static final Prompt r3 = new Prompt("Is ${person} an adult or a child?", "[aA]", r4, "[cC]", r6);
  private static final Prompt r1 = new Prompt("Would you like to create a roster?", "[yY]", r2, "[nN]", r14);

  static {
    r2.setLeft(r3);
    r2.setRight(r3);
    r14.setRight(r1);
  }

  public static void main(final String[] args) {
    if (args.length != 1)
      System.exit(1);

    System.out.println("Running for date: " + args[0]);
    Prompt prompt = r1;
    while (prompt != null) {
      System.out.print(prompt.getPrompt() + " ");
      try (final Scanner input = new Scanner(System.in)) {
        final String line = input.nextLine().trim();
        prompt = prompt.getNext(line);
      }
    }

    for (final Prompt p : prompts)
      p.printAnswers();

    System.out.println("Thanks for playing!");
  }
}