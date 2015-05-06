/*
 * XmlFormatter.java  
 *
 * Creator:
 * 20.01.11 12:01 Sippel
 *
 * Maintainer:
 * 20.01.11 12:01 Sippel
 *
 * Last Modification:
 * $Id: XmlFormatter.java 72525 2012-02-03 11:44:17Z sippel $
 *
 * Copyright (c) 2003 ABACUS Research AG, All Rights Reserved
 */
package ch.abacus.abaconnecttools;

import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.HashMap;

public class XmlSoapAcReformatter extends SimpleXmlSaxParser {

    private String mLineFeed = "\n";
    private String mIndent = "  ";
    private String mLastStartElementName = "";
    private StringBuffer mFormattedXml = new StringBuffer();

    ArrayList<String> mElementNames = new ArrayList<String>();

    private boolean mRemoveNamespacePrefixes = true;  // Default removes the namespaces from the Xml element names

    // Specific flags for AbaConnect Data Handling
    private String mAbaConnectImportMode = "";
    private boolean mInSaveInsertUpdateRequest = false;
    private boolean mInSaveInsertUpdateDateElement = false;
    private boolean mInExtendedFieldElement = false;
    private String mExtendedFieldAttributeName = "";
    private boolean mConvertExtendedFieldsToXmlFormat = false;

    private HashMap<String, String> mExtendedFieldsInfo = new HashMap<String, String>();

    @Override
    public void startDocument() {
        super.startDocument();
        mFormattedXml.setLength(0);
    }

    public boolean isRemoveNamespacePrefixes() {
        return mRemoveNamespacePrefixes;
    }

    public void setRemoveNamespacePrefixes(boolean removeNamespacePrefixes) {
        mRemoveNamespacePrefixes = removeNamespacePrefixes;
    }

    public boolean isConvertExtendedFieldsToXmlFormat() {
        return mConvertExtendedFieldsToXmlFormat;
    }

    public void setConvertExtendedFieldsToXmlFormat(boolean convertExtendedFieldsToXmlFormat) {
        mConvertExtendedFieldsToXmlFormat = convertExtendedFieldsToXmlFormat;
    }

    public void setIndent(String singleIndent) {
        mIndent = singleIndent == null ? "" : singleIndent;
    }

    public void setLineFeed(String lineFeed) {
        mLineFeed = lineFeed == null ? "" : lineFeed;
    }

    public String getIndent() {
        return mIndent;
    }

    @Override
    public void endElement(String name, String value) {
        StringBuilder sbFormattedXml = new StringBuilder();

        mElementNames.remove(name);
        if ( name.equals(mLastStartElementName) ) {
            mLastStartElementName = "";
            sbFormattedXml.append(">");
        } else {
            sbFormattedXml.append(mLineFeed);
            int level = mElementNames.size();
            for ( int index = 0; index < level; index++ ) {
                sbFormattedXml.append(mIndent);
            }
        }
        if ( value.contains("&") ) {
            sbFormattedXml.append(value.replaceAll("&","&amp;"));
        } else {
            sbFormattedXml.append(value);
        }
        sbFormattedXml.append("</");
        if ( mRemoveNamespacePrefixes ) {
            sbFormattedXml.append(removeNamespacePrefix(name));
        } else {
            sbFormattedXml.append(name);
        }
        sbFormattedXml.append(">");
//        sbFormattedXml.append(mLineFeed);

        if ( mInExtendedFieldElement ) {
            if ( !"".equals(mExtendedFieldAttributeName) ) {
                mFormattedXml.append(mLineFeed);
                int level = mElementNames.size();
                for ( int index = 0; index < (level-1); index++ ) {
                    mFormattedXml.append(mIndent);
                }
                mFormattedXml.append("<");
                mFormattedXml.append(mExtendedFieldAttributeName);
                mFormattedXml.append(">");
                if ( value.contains("&") ) {
                    mFormattedXml.append(value.replaceAll("&","&amp;"));
                } else {
                    mFormattedXml.append(value);
                }
                mFormattedXml.append("</");
                mFormattedXml.append(mExtendedFieldAttributeName);
                mFormattedXml.append(">");
                mExtendedFieldAttributeName = "";
            }
        } else {
            mFormattedXml.append(sbFormattedXml.toString());
        }

        if ( mInSaveInsertUpdateDateElement && isExtendedFieldsElementName(name) ) {
            mInExtendedFieldElement = false;
        }
        if ( mInSaveInsertUpdateRequest ) {
            if ( isDataElementName(name) ) {
                mInSaveInsertUpdateDateElement = false;
            }
        }
        if ( isAbaConnectImportModeActive() ) {
            if (isElementNameSaveInsertUpdateRequest(name) ) {
                mInSaveInsertUpdateRequest = false;
            }
        }
    }

    @Override
    public void startElement(String elementName, Attributes atts) {
        StringBuilder sbFormattedXml = new StringBuilder();

        if ( mInSaveInsertUpdateDateElement && isExtendedFieldsElementName(elementName) ) {
            // Only convert Extended Field Format if mConvertExtendedFieldsToXmlFormat option is active
            mInExtendedFieldElement = mConvertExtendedFieldsToXmlFormat;
            mExtendedFieldsInfo.clear();
        }
//        sbFormattedXml.append(mLineFeed);
        int level = mElementNames.size();
        if ( level > 0 /* && !"".equals(mLastStartElementName)  */) {
            if ( ! "".equals(mLastStartElementName) ) {
                // This must be a parent element with child elements
                if ( mInSaveInsertUpdateDateElement && isAbaConnectImportModeActive() ) {
                    if ( !isDataElementName(mLastStartElementName) ) {
                        // Do not output mode attribute for the Data element
                        sbFormattedXml.append(" mode=\"");
                        sbFormattedXml.append(mAbaConnectImportMode);
                        sbFormattedXml.append("\"");
                    }
                }
                sbFormattedXml.append(">");
            }
            sbFormattedXml.append(mLineFeed);
        }
        for ( int index = 0; index < level; index++ ) {
            sbFormattedXml.append(mIndent);
        }
        mElementNames.add(elementName);
        sbFormattedXml.append("<");
        if ( mRemoveNamespacePrefixes ) {
            sbFormattedXml.append(removeNamespacePrefix(elementName));
        } else {
            sbFormattedXml.append(elementName);
        }
        mExtendedFieldAttributeName = "";
        if ( atts != null ) {
            int attLength = atts.getLength();
            if ( attLength > 0 ) {
                for ( int index = 0; index < attLength; index++ ) {
                    boolean writeAttribute = true;
                    String attribVal = atts.getValue(index);
                    String attribName = atts.getLocalName(index);
                    if ( isAbaConnectImportModeActive() && attribVal.contains("http://www.abacus.ch/abaconnect") ) {
                        writeAttribute = false;
                    }
                    if ( writeAttribute ) {
                        sbFormattedXml.append(" ");
                        sbFormattedXml.append(attribName);
                        sbFormattedXml.append("=");
                        sbFormattedXml.append("\"");
                        sbFormattedXml.append(attribVal);
                        sbFormattedXml.append("\"");
                    }
                    if ( mInExtendedFieldElement && "Name".equals(attribName) ) {
                        mExtendedFieldAttributeName = attribVal;
                    }
                }
            }
        }
//        sbFormattedXml.append(">");
        if ( ! mInExtendedFieldElement ) {
            mFormattedXml.append(sbFormattedXml.toString());
        }

        mLastStartElementName = elementName;

        if ( mInSaveInsertUpdateRequest ) {
            if ( isDataElementName(elementName) ) {
                mInSaveInsertUpdateDateElement = true;
            }
        }
        if ( isAbaConnectImportModeActive() ) {
            if ( isElementNameSaveInsertUpdateRequest(elementName) ) {
                mInSaveInsertUpdateRequest = true;
            }
        }
    }

    private boolean isDataElementName( String elementName) {
        if ( elementName == null ) return false;
        if ( !elementName.endsWith("Data") ) return false;
        String elementTagName = elementName;
        int colonPos = elementTagName.indexOf(":");
        if ( colonPos >= 0 ) {
            // Cut off namespace
            elementTagName = elementTagName.substring(colonPos+1);
        }
        if ( "Data".equals(elementTagName) ) {
            return true;
        }
        return false;
    }

    private boolean isExtendedFieldsElementName( String elementName) {
        if ( elementName == null ) return false;
        if ( !elementName.endsWith("ExtendedFields") ) return false;
        String elementTagName = elementName;
        int colonPos = elementTagName.indexOf(":");
        if ( colonPos >= 0 ) {
            // Cut off namespace
            elementTagName = elementTagName.substring(colonPos+1);
        }
        if ( "ExtendedFields".equals(elementTagName) ) {
            return true;
        }
        return false;
    }

    private boolean isElementNameSaveInsertUpdateRequest( String elementName) {
        if ( elementName == null ) return false;
        if ( !elementName.endsWith("SaveRequest") && !elementName.endsWith("InsertRequest") && !elementName.endsWith("InsertRequest") ) return false;
        String elementTagName = elementName;
        int colonPos = elementTagName.indexOf(":");
        if ( colonPos >= 0 ) {
            // Cut off namespace
            elementTagName = elementTagName.substring(colonPos+1);
        }
        if ( "SaveRequest".equals(elementTagName) || "InsertRequest".equals(elementTagName) || "UpdateRequest".equals(elementTagName) ) {
            return true;
        }

        return false;
    }

    public String getFormattedXml() {
        return mFormattedXml.toString();
    }

    private String removeNamespacePrefix(String elementName) {
        if ( elementName == null ) return elementName;
        int posColon = elementName.indexOf(":");
        return (posColon >= 0 ? elementName.substring(posColon+1) : elementName);
    }

    private boolean isAbaConnectImportModeActive() {
        return (mAbaConnectImportMode != null && !"".equals(mAbaConnectImportMode));
    }

    public void setAbaConnectImportMode(String mode) {
        if ( mode == null ) {
            mAbaConnectImportMode = "";
        } else if ( "SAVE".equalsIgnoreCase(mode) || "INSERT".equalsIgnoreCase(mode) || "UPDATE".equalsIgnoreCase(mode) ) {
            mAbaConnectImportMode = mode.toUpperCase();
        } else {
            mAbaConnectImportMode = "";
        }
    }

    public String getAbaConnectImportMode() {
        return mAbaConnectImportMode;
    }

}
