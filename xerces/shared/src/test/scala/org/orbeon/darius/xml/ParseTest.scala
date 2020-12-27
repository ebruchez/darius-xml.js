package org.orbeon.darius.xml

import org.orbeon.apache.xerces.api.API
import org.orbeon.apache.xerces.demo.XMLEventCollector
import utest._


object ParseTest extends TestSuite {

  override def tests = Tests {

    test("SimpleParse with XMLDocumentHandler") {
      val collector = new XMLEventCollector
      API.parseString("""<simple foo="bar" xml:x="y" baz:gaga="aaa" xmlns:baz="http://baz.com/"/>""", collector)
      println(collector.events map (_.toString) mkString "\n")
    }

    test("SimpleParse with ContentHandler") {

      val collector = new AllCollector

      API.parseString("""<simple foo="bar" xml:x="y" baz:gaga="aaa" xmlns:baz="http://baz.com/"/>""", collector)
      println(collector.events map (_.toString) mkString "\n")
    }
  }
}
