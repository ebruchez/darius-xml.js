package org.apache.xerces.xni.parser

import org.apache.xerces.xni.XMLDocumentHandler

/**
 * Defines a document filter that acts as both a receiver and an emitter
 * of document events.
 */
trait XMLDocumentFilter extends XMLDocumentHandler with XMLDocumentSource
