package org.apache.xerces.xni.parser

import org.apache.xerces.xni.XMLDTDHandler

/**
 * Defines a DTD filter that acts as both a receiver and an emitter
 * of DTD events.
 */
trait XMLDTDFilter extends XMLDTDHandler with XMLDTDSource
