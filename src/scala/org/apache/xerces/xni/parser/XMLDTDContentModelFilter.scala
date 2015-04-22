package org.apache.xerces.xni.parser

import org.apache.xerces.xni.XMLDTDContentModelHandler

/**
 * Defines a DTD content model filter that acts as both a receiver and
 * an emitter of DTD content model events.
 */
trait XMLDTDContentModelFilter extends XMLDTDContentModelHandler with XMLDTDContentModelSource
