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
 * $Id: XmlFormatter.java 117433 2014-11-05 15:14:52Z sippel $
 *
 * Copyright (c) 2003 ABACUS Research AG, All Rights Reserved
 */
package ch.abacus.abaconnecttools;

import org.xml.sax.Attributes;

import java.util.ArrayList;

public class XmlFormatter extends SimpleXmlSaxParser {

    private boolean mStripWhitespaces = false;
    private String mLineFeed = "\n";
    private String mIndent = "  ";
    private String mLastStartElementName = "";
    private StringBuffer mFormattedXml = new StringBuffer();

    ArrayList<String> mElementNames = new ArrayList<String>();

    private boolean mRemoveNamespacePrefixes = true;  // Default removes the namespaces from the Xml element names

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

    public boolean isStripWhitespaces() {
        return mStripWhitespaces;
    }

    public void setStripWhitespaces(boolean stripWhitespaces) {
        mStripWhitespaces = stripWhitespaces;
    }

    public void setIndent(String singleIndent) {
        mIndent = singleIndent == null ? "" : singleIndent;
    }

    public void setLineFeed(String lineFeed) {
        mLineFeed = lineFeed == null ? "" : lineFeed;
    }

    @Override
    public void endElement(String name, String value) {
        boolean isLastCharLineFeed = false;
        mElementNames.remove(name);
        if ( name.equals(mLastStartElementName) ) {
            mLastStartElementName = "";
        } else {
            mFormattedXml.append(mLineFeed);
            int level = mElementNames.size();
            isLastCharLineFeed = level == 0;
            for ( int index = 0; index < level; index++ ) {
                mFormattedXml.append(mIndent);
            }
        }
        if ( value.contains("&") ) {
            mFormattedXml.append(value.replaceAll("&","&amp;"));
        } else {
            if ( ! mStripWhitespaces || (!isLastCharLineFeed && !mLineFeed.equals(value)) ) {
                mFormattedXml.append(value);
            }
        }
        mFormattedXml.append("</");
        if ( mRemoveNamespacePrefixes ) {
            mFormattedXml.append(removeNamespacePrefix(name));
        } else {
            mFormattedXml.append(name);
        }
        mFormattedXml.append(">");
//        mFormattedXml.append(mLineFeed);
    }

    @Override
    public void startElement(String elementName, Attributes atts) {
        if ( mStripWhitespaces ) this.elementValue.reset();
//        mFormattedXml.append(mLineFeed);
        int level = mElementNames.size();
        if ( level > 0 /* && !"".equals(mLastStartElementName)  */) {
            mFormattedXml.append(mLineFeed);
        }
        for ( int index = 0; index < level; index++ ) {
            mFormattedXml.append(mIndent);
        }
        mElementNames.add(elementName);
        mFormattedXml.append("<");
        if ( mRemoveNamespacePrefixes ) {
            mFormattedXml.append(removeNamespacePrefix(elementName));
        } else {
            mFormattedXml.append(elementName);
        }
        if ( atts != null ) {
            int attLength = atts.getLength();
            if ( attLength > 0 ) {
                for ( int index = 0; index < attLength; index++ ) {
                    mFormattedXml.append(" ");
                    mFormattedXml.append(atts.getLocalName(index));
                    mFormattedXml.append("=");
                    mFormattedXml.append("\"");
                    mFormattedXml.append(atts.getValue(index));
                    mFormattedXml.append("\"");
                }
            }
        }
        mFormattedXml.append(">");

        mLastStartElementName = elementName;
    }

    public String getFormattedXml() {
        return mFormattedXml.toString();
    }

    private String removeNamespacePrefix(String elementName) {
        if ( elementName == null ) return elementName;
        int posColon = elementName.indexOf(":");
        return (posColon >= 0 ? elementName.substring(posColon+1) : elementName);
    }
}
