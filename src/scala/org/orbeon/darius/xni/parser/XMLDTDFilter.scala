package org.orbeon.darius.xni.parser

import org.orbeon.darius.xni.XMLDTDHandler

/**
 * Defines a DTD filter that acts as both a receiver and an emitter
 * of DTD events.
 */
trait XMLDTDFilter extends XMLDTDHandler with XMLDTDSource
