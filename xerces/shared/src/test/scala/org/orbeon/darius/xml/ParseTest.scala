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

    test("Large number of namespaces") {

      val collector = new AllCollector

      API.parseString(
        """
          |<resources xmlns:sql="http://orbeon.org/oxf/xml/sql" xmlns:fr="http://orbeon.org/oxf/xml/form-runner" xmlns:ev="http://www.w3.org/2001/xml-events" xmlns:xxf="http://orbeon.org/oxf/xml/xforms" xmlns:map="http://www.w3.org/2005/xpath-functions/map" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xbl="http://www.w3.org/ns/xbl" xmlns:xxbl="http://orbeon.org/oxf/xml/xbl" xmlns:xh="http://www.w3.org/1999/xhtml" xmlns:frf="java:org.orbeon.oxf.fr.FormRunner" xmlns:array="http://www.w3.org/2005/xpath-functions/array" xmlns:math="http://www.w3.org/2005/xpath-functions/math" xmlns:exf="http://www.exforms.org/exf/1-0" xmlns:saxon="http://saxon.sf.net/" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:p="http://www.orbeon.com/oxf/pipeline" xmlns:fbf="java:org.orbeon.oxf.fb.FormBuilderXPathApi" xmlns:fb="http://orbeon.org/oxf/xml/form-builder" xmlns:xxi="http://orbeon.org/oxf/xml/xinclude" xmlns:xi="http://www.w3.org/2001/XInclude" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:component="http://orbeon.org/oxf/xml/form-builder/component/orbeon/library" xmlns:xf="http://www.w3.org/2002/xforms">
          |  <resource xml:lang="en"/>
          |</resources>
          |""".stripMargin, collector)
      println(collector.events map (_.toString) mkString "\n")
    }
  }
}
