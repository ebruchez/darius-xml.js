/**
 * Copyright 2015 Orbeon, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.orbeon.darius.xml

import org.orbeon.darius.xml.api.API
import org.orbeon.darius.xml.demo.XMLEventCollector
import utest._

object ParseTest extends TestSuite {

  override def tests = Tests {
    test("SimpleParse") {
      val collector = new XMLEventCollector
      API.parseString("""<simple foo="bar" xml:x="y" baz:gaga="aaa" xmlns:baz="http://baz.com/"/>""", collector)
      println(collector.events map (_.toString) mkString "\n")
    }
  }
}
