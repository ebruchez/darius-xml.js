/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.orbeon.darius.impl

object Constants {

  val NS_XMLSCHEMA = "http://www.w3.org/2001/XMLSchema".intern()

  val NS_DTD = "http://www.w3.org/TR/REC-xml".intern()

  /**
   SAX feature prefix ("http://xml.org/sax/features/").
   */
  val SAX_FEATURE_PREFIX = "http://xml.org/sax/features/"

  /**
   Namespaces feature ("namespaces").
   */
  val NAMESPACES_FEATURE = "namespaces"

  /**
   Namespace prefixes feature ("namespace-prefixes").
   */
  val NAMESPACE_PREFIXES_FEATURE = "namespace-prefixes"

  /**
   String interning feature ("string-interning").
   */
  val STRING_INTERNING_FEATURE = "string-interning"

  /**
   Validation feature ("validation").
   */
  val VALIDATION_FEATURE = "validation"

  /**
   External general entities feature ("external-general-entities ").
   */
  val EXTERNAL_GENERAL_ENTITIES_FEATURE = "external-general-entities"

  /**
   External parameter entities feature ("external-parameter-entities ").
   */
  val EXTERNAL_PARAMETER_ENTITIES_FEATURE = "external-parameter-entities"

  /**
   Lexical handler parameter entities feature ("lexical-handler/parameter-entities").
   */
  val LEXICAL_HANDLER_PARAMETER_ENTITIES_FEATURE = "lexical-handler/parameter-entities"

  /**
   Is standalone feature ("is-standalone").
   */
  val IS_STANDALONE_FEATURE = "is-standalone"

  /**
   Resolve DTD URIs feature ("resolve-dtd-uris").
   */
  val RESOLVE_DTD_URIS_FEATURE = "resolve-dtd-uris"

  /**
   Use Attributes2 feature ("use-attributes2").
   */
  val USE_ATTRIBUTES2_FEATURE = "use-attributes2"

  /**
   Use Locator2 feature ("use-locator2").
   */
  val USE_LOCATOR2_FEATURE = "use-locator2"

  /**
   Use EntityResolver2 feature ("use-entity-resolver2").
   */
  val USE_ENTITY_RESOLVER2_FEATURE = "use-entity-resolver2"

  /**
   Unicode normalization checking feature ("unicode-normalization-checking").
   */
  val UNICODE_NORMALIZATION_CHECKING_FEATURE = "unicode-normalization-checking"

  /**
   xmlns URIs feature ("xmlns-uris").
   */
  val XMLNS_URIS_FEATURE = "xmlns-uris"

  /**
   XML 1.1 feature ("xml-1.1").
   */
  val XML_11_FEATURE = "xml-1.1"

  /**
   Allow unparsed entity and notation declaration events to be sent after the end DTD event ("allow-dtd-events-after-endDTD")
   */
  val ALLOW_DTD_EVENTS_AFTER_ENDDTD_FEATURE = "allow-dtd-events-after-endDTD"

  /**
   SAX property prefix ("http://xml.org/sax/properties/").
   */
  val SAX_PROPERTY_PREFIX = "http://xml.org/sax/properties/"

  /**
   Declaration handler property ("declaration-handler").
   */
  val DECLARATION_HANDLER_PROPERTY = "declaration-handler"

  /**
   Lexical handler property ("lexical-handler").
   */
  val LEXICAL_HANDLER_PROPERTY = "lexical-handler"

  /**
   DOM node property ("dom-node").
   */
  val DOM_NODE_PROPERTY = "dom-node"

  /**
   XML string property ("xml-string").
   */
  val XML_STRING_PROPERTY = "xml-string"

  /**
   Document XML version property ("document-xml-version").
   */
  val DOCUMENT_XML_VERSION_PROPERTY = "document-xml-version"

  /**
   JAXP property prefix ("http://java.sun.com/xml/jaxp/properties/").
   */
  val JAXP_PROPERTY_PREFIX = "http://java.sun.com/xml/jaxp/properties/"

  /**
   JAXP schemaSource property: when used internally may include DTD sources (DOM)
   */
  val SCHEMA_SOURCE = "schemaSource"

  /**
   JAXP schemaSource language: when used internally may include DTD namespace (DOM)
   */
  val SCHEMA_LANGUAGE = "schemaLanguage"

  /**
   Comments feature ("include-comments").
   */
  val INCLUDE_COMMENTS_FEATURE = "include-comments"

  /**
   Create cdata nodes feature ("create-cdata-nodes").
   */
  val CREATE_CDATA_NODES_FEATURE = "create-cdata-nodes"

  /**
   Feature id: load as infoset.
   */
  val LOAD_AS_INFOSET = "load-as-infoset"

  val DOM_CANONICAL_FORM = "canonical-form"

  val DOM_CDATA_SECTIONS = "cdata-sections"

  val DOM_COMMENTS = "comments"

  val DOM_CHARSET_OVERRIDES_XML_ENCODING = "charset-overrides-xml-encoding"

  val DOM_DATATYPE_NORMALIZATION = "datatype-normalization"

  val DOM_ENTITIES = "entities"

  val DOM_INFOSET = "infoset"

  val DOM_NAMESPACES = "namespaces"

  val DOM_NAMESPACE_DECLARATIONS = "namespace-declarations"

  val DOM_SUPPORTED_MEDIATYPES_ONLY = "supported-media-types-only"

  val DOM_VALIDATE_IF_SCHEMA = "validate-if-schema"

  val DOM_VALIDATE = "validate"

  val DOM_ELEMENT_CONTENT_WHITESPACE = "element-content-whitespace"

  val DOM_DISCARD_DEFAULT_CONTENT = "discard-default-content"

  val DOM_NORMALIZE_CHARACTERS = "normalize-characters"

  val DOM_CHECK_CHAR_NORMALIZATION = "check-character-normalization"

  val DOM_WELLFORMED = "well-formed"

  val DOM_SPLIT_CDATA = "split-cdata-sections"

  val DOM_FORMAT_PRETTY_PRINT = "format-pretty-print"

  val DOM_XMLDECL = "xml-declaration"

  val DOM_UNKNOWNCHARS = "unknown-characters"

  val DOM_CERTIFIED = "certified"

  val DOM_DISALLOW_DOCTYPE = "disallow-doctype"

  val DOM_IGNORE_UNKNOWN_CHARACTER_DENORMALIZATIONS = "ignore-unknown-character-denormalizations"

  val DOM_RESOURCE_RESOLVER = "resource-resolver"

  val DOM_ERROR_HANDLER = "error-handler"

  val DOM_SCHEMA_TYPE = "schema-type"

  val DOM_SCHEMA_LOCATION = "schema-location"

  val DOM_PSVI = "psvi"

  /**
   Xerces features prefix ("http://apache.org/xml/features/").
   */
  val XERCES_FEATURE_PREFIX = "http://apache.org/xml/features/"

  /**
   Schema validation feature ("validation/schema").
   */
  val SCHEMA_VALIDATION_FEATURE = "validation/schema"

  /**
   Expose schema normalized values
   */
  val SCHEMA_NORMALIZED_VALUE = "validation/schema/normalized-value"

  /**
   Send schema default value via characters()
   */
  val SCHEMA_ELEMENT_DEFAULT = "validation/schema/element-default"

  /**
   Schema full constraint checking ("validation/schema-full-checking").
   */
  val SCHEMA_FULL_CHECKING = "validation/schema-full-checking"

  /**
   Augment Post-Schema-Validation-Infoset
   */
  val SCHEMA_AUGMENT_PSVI = "validation/schema/augment-psvi"

  /**
   Dynamic validation feature ("validation/dynamic").
   */
  val DYNAMIC_VALIDATION_FEATURE = "validation/dynamic"

  /**
   Warn on duplicate attribute declaration feature ("validation/warn-on-duplicate-attdef").
   */
  val WARN_ON_DUPLICATE_ATTDEF_FEATURE = "validation/warn-on-duplicate-attdef"

  /**
   Warn on undeclared element feature ("validation/warn-on-undeclared-elemdef").
   */
  val WARN_ON_UNDECLARED_ELEMDEF_FEATURE = "validation/warn-on-undeclared-elemdef"

  /**
   Warn on duplicate entity declaration feature ("warn-on-duplicate-entitydef").
   */
  val WARN_ON_DUPLICATE_ENTITYDEF_FEATURE = "warn-on-duplicate-entitydef"

  /**
   Allow Java encoding names feature ("allow-java-encodings").
   */
  val ALLOW_JAVA_ENCODINGS_FEATURE = "allow-java-encodings"

  /**
   Disallow DOCTYPE declaration feature ("disallow-doctype-decl").
   */
  val DISALLOW_DOCTYPE_DECL_FEATURE = "disallow-doctype-decl"

  /**
   Continue after fatal error feature ("continue-after-fatal-error").
   */
  val CONTINUE_AFTER_FATAL_ERROR_FEATURE = "continue-after-fatal-error"

  /**
   Load dtd grammar when nonvalidating feature ("nonvalidating/load-dtd-grammar").
   */
  val LOAD_DTD_GRAMMAR_FEATURE = "nonvalidating/load-dtd-grammar"

  /**
   Load external dtd when nonvalidating feature ("nonvalidating/load-external-dtd").
   */
  val LOAD_EXTERNAL_DTD_FEATURE = "nonvalidating/load-external-dtd"

  /**
   Defer node expansion feature ("dom/defer-node-expansion").
   */
  val DEFER_NODE_EXPANSION_FEATURE = "dom/defer-node-expansion"

  /**
   Create entity reference nodes feature ("dom/create-entity-ref-nodes").
   */
  val CREATE_ENTITY_REF_NODES_FEATURE = "dom/create-entity-ref-nodes"

  /**
   Include ignorable whitespace feature ("dom/include-ignorable-whitespace").
   */
  val INCLUDE_IGNORABLE_WHITESPACE = "dom/include-ignorable-whitespace"

  /**
   Default attribute values feature ("validation/default-attribute-values").
   */
  val DEFAULT_ATTRIBUTE_VALUES_FEATURE = "validation/default-attribute-values"

  /**
   Validate content models feature ("validation/validate-content-models").
   */
  val VALIDATE_CONTENT_MODELS_FEATURE = "validation/validate-content-models"

  /**
   Validate datatypes feature ("validation/validate-datatypes").
   */
  val VALIDATE_DATATYPES_FEATURE = "validation/validate-datatypes"

  /**
   Balance syntax trees feature ("validation/balance-syntax-trees").
   */
  val BALANCE_SYNTAX_TREES = "validation/balance-syntax-trees"

  /**
   Notify character references feature (scanner/notify-char-refs").
   */
  val NOTIFY_CHAR_REFS_FEATURE = "scanner/notify-char-refs"

  /**
   Notify built-in (&amp;, etc.) references feature (scanner/notify-builtin-refs").
   */
  val NOTIFY_BUILTIN_REFS_FEATURE = "scanner/notify-builtin-refs"

  /**
   Standard URI conformant feature ("standard-uri-conformant").
   */
  val STANDARD_URI_CONFORMANT_FEATURE = "standard-uri-conformant"

  /**
   Generate synthetic annotations feature ("generate-synthetic-annotations").
   */
  val GENERATE_SYNTHETIC_ANNOTATIONS_FEATURE = "generate-synthetic-annotations"

  /**
   Validate annotations feature ("validate-annotations").
   */
  val VALIDATE_ANNOTATIONS_FEATURE = "validate-annotations"

  /**
   Honour all schemaLocations feature ("honour-all-schemaLocations").
   */
  val HONOUR_ALL_SCHEMALOCATIONS_FEATURE = "honour-all-schemaLocations"

  /**
   Namespace growth feature ("namespace-growth").
   */
  val NAMESPACE_GROWTH_FEATURE = "namespace-growth"

  /**
   Tolerate duplicates feature ("internal/tolerate-duplicates").
   */
  val TOLERATE_DUPLICATES_FEATURE = "internal/tolerate-duplicates"

  /**
   String interned feature ("internal/strings-interned").
   */
  val STRINGS_INTERNED_FEATURE = "internal/strings-interned"

  /**
   XInclude processing feature ("xinclude").
   */
  val XINCLUDE_FEATURE = "xinclude"

  /**
   XInclude fixup base URIs feature ("xinclude/fixup-base-uris").
   */
  val XINCLUDE_FIXUP_BASE_URIS_FEATURE = "xinclude/fixup-base-uris"

  /**
   XInclude fixup language feature ("xinclude/fixup-language").
   */
  val XINCLUDE_FIXUP_LANGUAGE_FEATURE = "xinclude/fixup-language"

  /**
   * Feature to ignore xsi:type attributes on elements during validation,
   * until a global element declaration is found. ("validation/schema/ignore-xsi-type-until-elemdecl")
   * If this feature is on when validating a document, then beginning at the validation root
   * element, xsi:type attributes are ignored until a global element declaration is
   * found for an element.  Once a global element declaration has been found, xsi:type
   * attributes will start being processed for the sub-tree beginning at the element for
   * which the declaration was found.
   *
   * Suppose an element A has two element children, B and C.
   *
   * If a global element declaration is found for A, xsi:type attributes on A, B and C,
   * and all of B and C's descendents, will be processed.
   *
   * If no global element declaration is found for A or B, but one is found for C,
   * then xsi:type attributes will be ignored on A and B (and any descendents of B,
   * until a global element declaration is found), but xsi:type attributes will be
   * processed for C and all of C's descendents.
   *
   * Once xsi:type attributes stop being ignored for a subtree, they do not start
   * being ignored again, even if more elements are encountered for which no global
   * element declaration can be found.
   */
  val IGNORE_XSI_TYPE_FEATURE = "validation/schema/ignore-xsi-type-until-elemdecl"

  /**
   Perform checking of ID/IDREFs ("validation/id-idref-checking")
   */
  val ID_IDREF_CHECKING_FEATURE = "validation/id-idref-checking"

  /**
   Feature to ignore errors caused by identity constraints ("validation/identity-constraint-checking")
   */
  val IDC_CHECKING_FEATURE = "validation/identity-constraint-checking"

  /**
   Feature to ignore errors caused by unparsed entities ("validation/unparsed-entity-checking")
   */
  val UNPARSED_ENTITY_CHECKING_FEATURE = "validation/unparsed-entity-checking"

  /**
   * Internal feature. When set to true the schema validator will only use
   * schema components from the grammar pool provided.
   */
  val USE_GRAMMAR_POOL_ONLY_FEATURE = "internal/validation/schema/use-grammar-pool-only"

  /**
   Internal performance related feature:
   * false - the parser settings (features/properties) have not changed between 2 parses
   * true - the parser settings have changed between 2 parses
   * NOTE: this feature should only be set by the parser configuration.
   */
  val PARSER_SETTINGS = "internal/parser-settings"

  /**
   Xerces properties prefix ("http://apache.org/xml/properties/").
   */
  val XERCES_PROPERTY_PREFIX = "http://apache.org/xml/properties/"

  /**
   Current element node property ("dom/current-element-node").
   */
  val CURRENT_ELEMENT_NODE_PROPERTY = "dom/current-element-node"

  /**
   Document class name property ("dom/document-class-name").
   */
  val DOCUMENT_CLASS_NAME_PROPERTY = "dom/document-class-name"

  /**
   Symbol table property ("internal/symbol-table").
   */
  val SYMBOL_TABLE_PROPERTY = "internal/symbol-table"

  /**
   Error reporter property ("internal/error-reporter").
   */
  val ERROR_REPORTER_PROPERTY = "internal/error-reporter"

  /**
   Error handler property ("internal/error-handler").
   */
  val ERROR_HANDLER_PROPERTY = "internal/error-handler"

  /**
   XInclude handler property ("internal/xinclude-handler").
   */
  val XINCLUDE_HANDLER_PROPERTY = "internal/xinclude-handler"

  /**
   XPointer handler property ("internal/xpointer-handler").
   */
  val XPOINTER_HANDLER_PROPERTY = "internal/xpointer-handler"

  /**
   Entity manager property ("internal/entity-manager").
   */
  val ENTITY_MANAGER_PROPERTY = "internal/entity-manager"

  /**
   Input buffer size property ("input-buffer-size").
   */
  val BUFFER_SIZE_PROPERTY = "input-buffer-size"

  /**
   Security manager property ("security-manager").
   */
  val SECURITY_MANAGER_PROPERTY = "security-manager"

  /**
   Entity resolver property ("internal/entity-resolver").
   */
  val ENTITY_RESOLVER_PROPERTY = "internal/entity-resolver"

  /**
   Grammar pool property ("internal/grammar-pool").
   */
  val XMLGRAMMAR_POOL_PROPERTY = "internal/grammar-pool"

  /**
   Datatype validator factory ("internal/datatype-validator-factory").
   */
  val DATATYPE_VALIDATOR_FACTORY_PROPERTY = "internal/datatype-validator-factory"

  /**
   Document scanner property ("internal/document-scanner").
   */
  val DOCUMENT_SCANNER_PROPERTY = "internal/document-scanner"

  /**
   DTD scanner property ("internal/dtd-scanner").
   */
  val DTD_SCANNER_PROPERTY = "internal/dtd-scanner"

  /**
   DTD processor property ("internal/dtd-processor").
   */
  val DTD_PROCESSOR_PROPERTY = "internal/dtd-processor"

  /**
   Validator property ("internal/validator").
   */
  val VALIDATOR_PROPERTY = "internal/validator"

  /**
   Validator property ("internal/validator/dtd").
   */
  val DTD_VALIDATOR_PROPERTY = "internal/validator/dtd"

  /**
   Validator property ("internal/validator/schema").
   */
  val SCHEMA_VALIDATOR_PROPERTY = "internal/validator/schema"

  /**
   No namespace schema location property ("schema/external-schemaLocation").
   */
  val SCHEMA_LOCATION = "schema/external-schemaLocation"

  /**
   Schema location property ("schema/external-noNamespaceSchemaLocation").
   */
  val SCHEMA_NONS_LOCATION = "schema/external-noNamespaceSchemaLocation"

  /**
   Namespace binder property ("internal/namespace-binder").
   */
  val NAMESPACE_BINDER_PROPERTY = "internal/namespace-binder"

  /**
   Namespace context property ("internal/namespace-context").
   */
  val NAMESPACE_CONTEXT_PROPERTY = "internal/namespace-context"

  /**
   Validation manager property ("internal/validation-manager").
   */
  val VALIDATION_MANAGER_PROPERTY = "internal/validation-manager"

  /**
   Schema type for the root element in a document ("validation/schema/root-type-definition").
   */
  val ROOT_TYPE_DEFINITION_PROPERTY = "validation/schema/root-type-definition"

  /**
   Schema element declaration for the root element in a document ("validation/schema/root-element-declaration").
   */
  val ROOT_ELEMENT_DECLARATION_PROPERTY = "validation/schema/root-element-declaration"

  /**
   Schema element declaration for the root element in a document ("internal/validation/schema/dv-factory").
   */
  val SCHEMA_DV_FACTORY_PROPERTY = "internal/validation/schema/dv-factory"

  /**
   Element PSVI is stored in augmentations using string "ELEMENT_PSVI"
   */
  val ELEMENT_PSVI = "ELEMENT_PSVI"

  /**
   Attribute PSVI is stored in augmentations using string "ATTRIBUTE_PSVI"
   */
  val ATTRIBUTE_PSVI = "ATTRIBUTE_PSVI"

  /**
   * Boolean indicating whether an attribute is declared in the DTD is stored
   * in augmentations using the string "ATTRIBUTE_DECLARED". The absence of this
   * augmentation indicates that the attribute was not declared in the DTD.
   */
  val ATTRIBUTE_DECLARED = "ATTRIBUTE_DECLARED"

  /**
   * Boolean indicating whether an entity referenced in the document has
   * not been read is stored in augmentations using the string "ENTITY_SKIPPED".
   * The absence of this augmentation indicates that the entity had a
   * declaration and was expanded.
   */
  val ENTITY_SKIPPED = "ENTITY_SKIPPED"

  /**
   * Boolean indicating whether a character is a probable white space
   * character (ch <= 0x20) that was the replacement text of a character
   * reference is stored in augmentations using the string "CHAR_REF_PROBABLE_WS".
   * The absence of this augmentation indicates that the character is not
   * probable white space and/or was not included from a character reference.
   */
  val CHAR_REF_PROBABLE_WS = "CHAR_REF_PROBABLE_WS"

  val XML_VERSION_ERROR = -1

  val XML_VERSION_1_0: Short = 1
  val XML_VERSION_1_1: Short = 2

  val SCHEMA_1_1_SUPPORT = false
  val SCHEMA_VERSION_1_0: Short  = 1
  val SCHEMA_VERSION_1_0_EXTENDED: Short  = 2
}
