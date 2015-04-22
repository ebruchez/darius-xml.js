package org.orbeon.darius.xni.parser

import org.orbeon.darius.xni.XMLDTDContentModelHandler

/**
 * Defines a DTD content model filter that acts as both a receiver and
 * an emitter of DTD content model events.
 */
trait XMLDTDContentModelFilter extends XMLDTDContentModelHandler with XMLDTDContentModelSource
