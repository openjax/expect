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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

public class ExpectTest {
  @Test
  // FIXME
  @Ignore("FIXME")
  public void testStart() throws Exception {
    final ExpectCallback callback = new ExpectCallback() {
      private final Map<String,String> variables = new HashMap<String,String>();
      private int index = -1;

      @Override
      public Map<String,String> process(final String exec) {
        variables.put("date", "080630");
        return variables;
      }

      @Override
      public Map<String,String> rule(final String ruleId, final String prompt, final String response) {
        if (!"r2".equals(ruleId))
          return variables;

        switch (++index) {
          case 0:
            variables.put("name", "Lisa\n");
            variables.put("adultOrChild", "C\n");
            variables.put("boyOrGirl", "G\n");
            variables.put("age", "14\n");
            variables.put("attending", "N\n");
            break;
          case 1:
            variables.put("name", "David\n");
            variables.put("adultOrChild", "C\n");
            variables.put("boyOrGirl", "B\n");
            variables.put("age", "10\n");
            variables.put("attending", "Y\n");
            variables.put("date", "080630\n");
            variables.put("bringingStuff", "N\n");
            variables.put("drinking", "N\n");
            variables.put("driving", "N\n");
            variables.put("printRoster", "N\n");
            break;
          case 2:
            variables.put("name", "Natalie\n");
            variables.put("adultOrChild", "C\n");
            variables.put("boyOrGirl", "G\n");
            variables.put("age", "19\n");
            variables.put("attending", "Y\n");
            variables.put("date", "080630\n");
            variables.put("bringingStuff", "N\n");
            variables.put("drinking", "Y\n");
            variables.put("driving", "N\n");
            variables.put("printRoster", "N\n");
            break;
          case 3:
            variables.put("name", "Sylvie\n");
            variables.put("adultOrChild", "C\n");
            variables.put("boyOrGirl", "G\n");
            variables.put("age", "19\n");
            variables.put("attending", "Y\n");
            variables.put("date", "080630\n");
            variables.put("bringingStuff", "N\n");
            variables.put("drinking", "Y\n");
            variables.put("driving", "Y\n");
            variables.put("needRide", "Y\n");
            variables.put("printRoster", "N\n");
            break;
          case 4:
            variables.put("name", "Jon\n");
            variables.put("adultOrChild", "A\n");
            variables.put("maleOrFemale", "M\n");
            variables.put("attending", "N\n");
            break;
          case 5:
            variables.put("name", "Jake\n");
            variables.put("adultOrChild", "A\n");
            variables.put("maleOrFemale", "M\n");
            variables.put("attending", "Y\n");
            variables.put("date", "080630\n");
            variables.put("bringingStuff", "N\n");
            variables.put("drinking", "N\n");
            variables.put("printRoster", "N\n");
            break;
          case 6:
            variables.put("name", "Shoshana\n");
            variables.put("adultOrChild", "A\n");
            variables.put("maleOrFemale", "F\n");
            variables.put("attending", "Y\n");
            variables.put("date", "080630\n");
            variables.put("bringingStuff", "N\n");
            variables.put("drinking", "Y\n");
            variables.put("driving", "N\n");
            variables.put("printRoster", "N\n");
            break;
          case 7:
            variables.put("name", "Katie\n");
            variables.put("adultOrChild", "A\n");
            variables.put("maleOrFemale", "F\n");
            variables.put("attending", "Y\n");
            variables.put("date", "080630\n");
            variables.put("bringingStuff", "N\n");
            variables.put("drinking", "Y\n");
            variables.put("driving", "Y\n");
            variables.put("needRide", "Y\n");
            variables.put("printRoster", "Y\n");
            break;
        }

        return variables;
      }
    };

    Expect.start(System.in, System.out, System.err, callback, new File("src/test/resources/xml/expect.xml"));
  }
}