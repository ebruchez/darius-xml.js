package org.orbeon.darius.xni.parser

import org.orbeon.darius.xni.XMLDocumentHandler

/**
 * Defines a document filter that acts as both a receiver and an emitter
 * of document events.
 */
trait XMLDocumentFilter extends XMLDocumentHandler with XMLDocumentSource
