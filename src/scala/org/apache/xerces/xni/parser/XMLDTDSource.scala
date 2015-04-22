package org.apache.xerces.xni.parser

import org.apache.xerces.xni.XMLDTDHandler

/**
 * Defines a DTD source. In other words, any object that implements
 * this interface is able to emit DTD "events" to the registered
 * DTD handler. These events could be produced by parsing an XML
 * document's internal or external subset, could be generated from
 * some other source, or could be created programmatically. This
 * interface does not say *how* the events are created, only
 * that the implementor is able to emit them.
 */
trait XMLDTDSource {

  /**
   Sets the DTD handler.
   */
  def setDTDHandler(handler: XMLDTDHandler): Unit

  /**
   Returns the DTD handler.
   */
  def getDTDHandler: XMLDTDHandler
}
