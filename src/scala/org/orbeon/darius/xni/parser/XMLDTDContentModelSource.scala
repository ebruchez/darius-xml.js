package org.orbeon.darius.xni.parser

import org.orbeon.darius.xni.XMLDTDContentModelHandler

/**
 * Defines a DTD content model source. In other words, any object that
 * implements this interface is able to emit DTD content model "events"
 * to the registered DTD content model handler. These events could be
 * produced by parsing an XML document's internal or external subset,
 * could be generated from some other source, or could be created
 * programmatically. This interface does not say *how* the events
 * are created, only that the implementor is able to emit them.
 */
trait XMLDTDContentModelSource {

  /**
   Sets the DTD content model handler.
   */
  def setDTDContentModelHandler(handler: XMLDTDContentModelHandler): Unit

  /**
   Returns the DTD content model handler.
   */
  def getDTDContentModelHandler: XMLDTDContentModelHandler
}
