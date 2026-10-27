package org.orbeon.darius.xml

import org.orbeon.apache.xerces.api.API
import org.orbeon.apache.xerces.demo._
import org.orbeon.apache.xerces.impl.dtd.XMLDTDDescription
import org.orbeon.apache.xerces.util.{NamespaceSupport, SecurityManager, SymbolTable, XMLAttributesImpl, XMLGrammarPoolImpl}
import org.orbeon.apache.xerces.xni.{QName, XMLString}
import org.orbeon.apache.xerces.xni.grammars.{Grammar, XMLGrammarDescription}
import utest._

object TranslationFixesTest extends TestSuite {

  override def tests = Tests {

    test("Bug 1: Processing Instructions, Comments, and CDATA in scanData") {
      val collector = new XMLEventCollector
      val xml =
        """<root>
          |  <?target some processing instruction data?>
          |  <!-- test comment -->
          |  <![CDATA[some <cdata> content]]>
          |  <?another-pi?>
          |</root>""".stripMargin

      API.parseString(xml, collector)
      val events = collector.events
      val hasPI1 = events.exists {
        case PI("target", "some processing instruction data") => true
        case _ => false
      }
      val hasPI2 = events.exists {
        case PI("another-pi", "") => true
        case _ => false
      }
      val hasComment = events.exists {
        case Comment(text) if text.trim == "test comment" => true
        case _ => false
      }
      val hasCData = events.exists {
        case Characters(text) if text.contains("some <cdata> content") => true
        case _ => false
      }
      assert(hasPI1)
      assert(hasPI2)
      assert(hasComment)
      assert(hasCData)
    }

    test("Bug 2: SymbolTable collision chain traversal and containsSymbol") {
      val st = new SymbolTable(4) // small bucket count to force collisions
      val candidates = (1 to 50).map(i => s"symbol_$i")
      // Add all candidate symbols
      candidates.foreach(s => st.addSymbol(s))

      // Verify containsSymbol(String) for every symbol, including collided ones
      for (s <- candidates) {
        assert(st.containsSymbol(s))
      }

      // Verify containsSymbol(Array[Char], Int, Int) for every symbol
      for (s <- candidates) {
        val chars = s.toCharArray
        assert(st.containsSymbol(chars, 0, chars.length))
      }

      // Verify addSymbol(Array[Char], Int, Int) returns identical interned references without duplicating
      val fCountField = classOf[SymbolTable].getDeclaredField("fCount")
      fCountField.setAccessible(true)
      val initialCount = fCountField.getInt(st)

      for (s <- candidates) {
        val chars = s.toCharArray
        val sym = st.addSymbol(chars, 0, chars.length)
        assert(candidates.contains(sym))
      }
      assert(fCountField.getInt(st) == initialCount)
    }

    test("Bug 3: NamespaceSupport.getAllPrefixes with redeclared prefixes") {
      val ns = new NamespaceSupport()
      ns.reset()
      ns.pushContext()
      ns.declarePrefix("p1", "http://uri1")
      ns.declarePrefix("p2", "http://uri2")

      // Child context redeclaring p1 and adding p3
      ns.pushContext()
      ns.declarePrefix("p1", "http://uri1-modified")
      ns.declarePrefix("p3", "http://uri3")

      val allPrefixes = ns.getAllPrefixes.toSet
      assert(allPrefixes.contains("p1"))
      assert(allPrefixes.contains("p2"))
      assert(allPrefixes.contains("p3"))
    }

    test("Bug 4: XMLGrammarPoolImpl grammar caching and retrieval without NPE") {
      val pool = new XMLGrammarPoolImpl()
      val desc1 = new XMLDTDDescription("pub1", "lit1", "base1", "exp1", "root1") {
        override def getGrammarType: String = XMLGrammarDescription.XML_DTD
      }
      val desc2 = new XMLDTDDescription("pub2", "lit2", "base2", "exp2", "root2") {
        override def getGrammarType: String = XMLGrammarDescription.XML_DTD
      }
      val g1 = new Grammar { def getGrammarDescription: XMLGrammarDescription = desc1 }
      val g2 = new Grammar { def getGrammarDescription: XMLGrammarDescription = desc2 }

      pool.putGrammar(g1)
      pool.putGrammar(g2)

      assert(pool.containsGrammar(desc1))
      assert(pool.containsGrammar(desc2))
      assert(pool.getGrammar(desc1) eq g1)
      assert(pool.getGrammar(desc2) eq g2)

      val initialSet = pool.retrieveInitialGrammarSet(XMLGrammarDescription.XML_DTD)
      assert(initialSet.length == 2)
      assert(initialSet.contains(g1))
      assert(initialSet.contains(g2))

      val removed = pool.removeGrammar(desc1)
      assert(removed eq g1)
      assert(!pool.containsGrammar(desc1))
      assert(pool.containsGrammar(desc2))
    }

    test("Bug 5: XMLAttributesImpl.cleanTableView does not hang on overflow") {
      val attrs = new XMLAttributesImpl()
      // Use reflection or subclass method to trigger cleanTableView with simulated overflow
      val cleanMethod = classOf[XMLAttributesImpl].getDeclaredMethod("cleanTableView")
      cleanMethod.setAccessible(true)
      val largeCountField = classOf[XMLAttributesImpl].getDeclaredField("fLargeCount")
      largeCountField.setAccessible(true)

      // Set fLargeCount to Int.MaxValue so that next cleanTableView increments it to negative
      largeCountField.setInt(attrs, Int.MaxValue)
      cleanMethod.invoke(attrs) // Must terminate cleanly without infinite loop
      assert(largeCountField.getInt(attrs) == 1)
    }

    test("Bug 6: QName.equals symmetry") {
      val qNs = new QName("p", "loc", "p:loc", "http://example.com")
      val qNoNs = new QName(null, "loc", "p:loc", null)

      // Different namespaces must not be equal, regardless of order
      assert(!qNs.equals(qNoNs))
      assert(!qNoNs.equals(qNs))

      // Same namespace and localpart must be equal
      val qNs2 = new QName("p", "loc", "other:loc", "http://example.com")
      assert(qNs.equals(qNs2))
      assert(qNs2.equals(qNs))

      // No namespace matching rawname
      val qNoNs2 = new QName(null, "loc", "p:loc", null)
      assert(qNoNs.equals(qNoNs2))
      assert(qNoNs2.equals(qNoNs))
    }

    test("Bug 7: XMLString.equals correctness and performance") {
      val chars = "hello world".toCharArray
      val xmlStr = new XMLString(chars, 0, chars.length)

      assert(xmlStr.equals("hello world"))
      assert(!xmlStr.equals("hello there"))
      assert(xmlStr.equals(chars, 0, chars.length))
      assert(!xmlStr.equals(chars, 0, 5))
      assert(!xmlStr.equals("short"))
      assert(!xmlStr.equals(null: String))
      assert(!xmlStr.equals(null: Array[Char], 0, 0))
    }

    test("SecurityManager BeanProperties") {
      val sec = new SecurityManager()
      sec.setEntityExpansionLimit(50000)
      assert(sec.getEntityExpansionLimit() == 50000)
    }
  }
}
