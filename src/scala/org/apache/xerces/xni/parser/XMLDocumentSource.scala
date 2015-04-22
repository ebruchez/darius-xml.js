package org.apache.xerces.xni.parser

import org.apache.xerces.xni.XMLDocumentHandler

/**
 * Defines a document source. In other words, any object that implements
 * this interface is able to emit document "events" to the registered
 * document handler. These events could be produced by parsing an XML
 * document, could be generated from some other source, or could be
 * created programmatically. This interface does not say *how*
 * the events are created, only that the implementor is able to emit
 * them.
 */
trait XMLDocumentSource {

  /**
   Sets the document handler.
   */
  def setDocumentHandler(handler: XMLDocumentHandler): Unit

  /**
   Returns the document handler
   */
  def getDocumentHandler: XMLDocumentHandler
}
