/*
 * ExplodeTcpMonMessages.java
 *
 * Creator:
 * 21.12.11 08:26 Sippel
 *
 * Maintainer:
 * 21.12.11 08:26 Sippel
 *
 * Last Modification:
 * $Id: ExplodeTcpMonMessages.java 21267 2011-08-19 07:20:28Z sippel $
 *
 * Copyright (c) 2003 ABACUS Research AG, All Rights Reserved
 */
package ch.abacus.abaconnecttools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.URL;
import java.util.*;

public class ExplodeTcpMonMessages  extends JFrame {
    private String m_StartPath = null;

    private static String OPTION_TEXT_REFORMAT_SOAP_MESSAGES = "Reformat XML SOAP Messages";
    private static String OPTION_TEXT_REMOVE_NAMESPACES = "Remove Namespaces from Messages";
    private static String OPTION_TEXT_OUTPUT_SOAP_HEADERS = "Output SOAP Headers";
    private static String OPTION_TEXT_CONVERT_EXTENDED_FIELDS = "Convert Extended Fields to XML Format";
    private static String BUTTON_TEXT_DELETE_EXISTING_XML_FILES = "Delete existing XML Files";
    private static String BUTTON_TEXT_EXPLODE_MESSAGES = "Explode Messages";

    private String m_StartXmlFileName = null;
    private JTextField m_txfTcpMonFileName = null;
    private JTextField m_txfFilenamePrefix = null;
    private JCheckBox m_chkReFormatSoapMessages = null;
    private JCheckBox m_chkRemoveNamespaces = null;
    private JCheckBox m_chkConvertExtendedFieldsToXmlFormat = null;
    private JCheckBox m_chkOutputSoapHeaders = null;

    private JTextPane m_txpInfoBox = null;
    private boolean mIsInfoBoxHtmlFormat = true;

    private String m_FilenamePrefix = "SM_";
    private String m_LineFeed = "\r\n";

    public StringBuilder m_sbMessages = new StringBuilder();
    public ArrayList<SoapEnvelopeInfo> m_SoapEnvelopeList = new ArrayList<SoapEnvelopeInfo>();

    ArrayList<String> mXmlProblemFileNameMessages = new ArrayList<String>();

    class SoapEnvelopeInfo implements Comparable {
        final String SOAP_RESPONSE_MESSAGE = "RESPONSE";
        final String SOAP_REQUEST_MESSAGE = "REQUEST";

        String mSoapEnvelopeXml = "";
        String mSoapHeader = "";
        String mSoapBodyName = "";
        String mMessageType = "";   // Either be REQUEST or RESPONSE
        int mOrderIndex = -1;

        SoapEnvelopeInfo(String soapEnvelopeXml) {
            mSoapEnvelopeXml = (soapEnvelopeXml == null ? "" : soapEnvelopeXml);
        }

        @Override
        public String toString() {
            return getSoapEnvelopeXml();
        }

        public String getSoapEnvelopeXml() {
            return (mSoapEnvelopeXml == null ? "" : mSoapEnvelopeXml);
        }

        public void setSoapEnvelopeXml(String soapEnvelopeXml) {
            mSoapEnvelopeXml = (soapEnvelopeXml == null ? "" : soapEnvelopeXml);
        }

        public String getSoapHeader() {
            return mSoapHeader;
        }

        public void setSoapHeader(String soapHeader) {
            mSoapHeader = soapHeader;
        }

        public String getSoapBodyName() {
            if ( mSoapBodyName == null || "".equals(mSoapBodyName) ) {
                mSoapBodyName = extractActionNameFromBodyName(getSoapEnvelopeXml());
                if ( mSoapBodyName.endsWith("/") ) {
                    mSoapBodyName = mSoapBodyName.substring(0,mSoapBodyName.length()-1);
                }
            }
            return mSoapBodyName;
        }

        public String getMessageType() {
            if ( mMessageType == null || "".equals(mMessageType) ) {
                if ( mSoapHeader.startsWith("HTTP/1.") ) {
                    mMessageType = SOAP_RESPONSE_MESSAGE;
                } else if ( mSoapHeader.startsWith("POST ") ) {
                    mMessageType = SOAP_REQUEST_MESSAGE;
                }
            }
            return mMessageType;
        }

        public boolean isRequestMessage() {
            return SOAP_REQUEST_MESSAGE.equals(getMessageType());
        }

        public boolean isResponseMessage() {
            return SOAP_RESPONSE_MESSAGE.equals(getMessageType());
        }

        public int getOrderIndex() {
            return mOrderIndex;
        }

        public void setOrderIndex(int orderIndex) {
            mOrderIndex = orderIndex;
        }

        public String extractActionNameFromBodyName(String soapEnvelope) {
            String bodyName = "";
            int bodyPos = soapEnvelope.indexOf("Body");
            while ( bodyPos > 0 ) {
                if ( soapEnvelope.charAt(bodyPos-1) == '<' || soapEnvelope.charAt(bodyPos-1) == ':' ) {
                    int startTagPos = soapEnvelope.indexOf("<",bodyPos);
                    if ( startTagPos > bodyPos ) {
                        int endTagPos = soapEnvelope.indexOf(">",startTagPos);
                        if ( endTagPos > startTagPos ) {
                            int spacePos = soapEnvelope.indexOf(" ",startTagPos);
                            if ( spacePos >= 0 && spacePos < endTagPos ) {
                                endTagPos = spacePos;
                            }
                            int namespaceSeparatorPos = soapEnvelope.indexOf(":",startTagPos);
                            if ( namespaceSeparatorPos < endTagPos && namespaceSeparatorPos > startTagPos) {
                                startTagPos = namespaceSeparatorPos;
                            }
                            if ( endTagPos > startTagPos ) {
                                bodyName = soapEnvelope.substring(startTagPos+1,endTagPos);
                                return bodyName;
                            }
                        }
                    }
                }
                bodyPos = soapEnvelope.indexOf("Body",bodyPos+1);
            }
            return bodyName;
        }

        @Override
        public int compareTo(Object o) {
            if ( o instanceof SoapEnvelopeInfo ) {
                return new Integer(getOrderIndex()).compareTo(((SoapEnvelopeInfo)o).getOrderIndex());
            }
            return 0;
        }
    }

    public static void main(String[] args) {
        String startXmlFilename = "";
        if ( args != null  &&  args.length > 0 ) {
            for (String arg : args) {
                if (new File(arg).exists()) {
                    startXmlFilename = arg;
                }
            }
        }
        final ExplodeTcpMonMessages frm = new ExplodeTcpMonMessages();

        frm.setStartXmlFilename(startXmlFilename);

        frm.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                frm.dispose();
            }
        });
        int xc = 100;
        int yc = 100;
        int width = 800;
        int height = 500;

        frm.initUI();
        frm.setLocation(xc, yc);
        frm.setSize(width, height);
        frm.setVisible(true);


//        ExplodeTcpMonMessages tcpmonTest = new ExplodeTcpMonMessages();
//
//        String tcpMonLogFilename = "d:\\data\\abaconnect\\customers\\soreco\\2011_12_16\\Walo_Insert_komplett.tcptxt";
//        String outputDirectory = "";
//        int lastSlashPos = tcpMonLogFilename.lastIndexOf(File.separator);
//        if ( lastSlashPos > 0 ) {
//            outputDirectory = tcpMonLogFilename.substring(0,lastSlashPos);
//        }
//
//
//        tcpmonTest.explodeTcpMonFile(tcpMonLogFilename);
//        tcpmonTest.outputSoapMessages(outputDirectory);
    }

    public void setStartXmlFilename( String startXmlFilename) {
        m_StartXmlFileName = (startXmlFilename == null ? "" : startXmlFilename);
    }

    public void explodeTcpMonFile(String tcpMonLogFilename) {
        m_SoapEnvelopeList.clear();
        if ( ! new File(tcpMonLogFilename).exists() ) {
            addMessage("Specified filename [" + tcpMonLogFilename + "] cannot be found.");
            return;
        }
        StringBuilder sbSoapEnvelope = new StringBuilder();
        StringBuilder sbSoapHeader = new StringBuilder();
        try {
            BufferedReader bufIn = new BufferedReader(new InputStreamReader(new FileInputStream(tcpMonLogFilename)));
            String inputLine;
            String envelopeNameSpace;
            String envelopeEndTag = "";
            boolean inSoapEnvelope = false;
            boolean inSoapHeader = false;
            inputLine = bufIn.readLine();
            // Read from the file
            while ( inputLine != null) {
//                System.out.println(inputLine);

                if ( inputLine.contains("POST ") || inputLine.contains("HTTP/1.")) {
                    inSoapHeader = true;
                }

    			int iEnvelopeStart = inputLine.indexOf("Envelop");
				if ( iEnvelopeStart > 0 ) {
                    if ( inputLine.charAt(iEnvelopeStart-1) == '<' || inputLine.charAt(iEnvelopeStart-1) == ':' ) {
                        int startEnvelopeTag = inputLine.lastIndexOf("<", iEnvelopeStart);
                        if ( startEnvelopeTag >= 0 && iEnvelopeStart > startEnvelopeTag ) {
                            if ( inputLine.charAt(startEnvelopeTag+1) != '/' ) {
    //                            int endEnvelopeTag = inputLine.indexOf(">", iEnvelopeStart);
                                envelopeNameSpace = inputLine.substring(startEnvelopeTag+1,iEnvelopeStart);
                                inputLine = inputLine.substring(startEnvelopeTag);
                                envelopeEndTag = "</" + envelopeNameSpace + "Envelop";
                                inSoapEnvelope = true;
                                inSoapHeader = false; // The SOAP Header must have finished
                                if ( sbSoapEnvelope.length() > 0 ) {
                                    sbSoapEnvelope.setLength(0);
                                }
                            }
                        }
                    }
                }

                if ( inSoapHeader ) {
                    if ( sbSoapHeader.length() == 0 ) {
                        // Trim off preceding text when POST or HTTP is not at the start of line
                        int ipos = inputLine.indexOf("POST ");
                        if ( ipos > 0 ) {
                            inputLine = inputLine.substring(ipos);
                        } else if ( ipos < 0 ) {
                            ipos = inputLine.indexOf("HTTP/1.");
                            if ( ipos > 0 ) {
                                inputLine = inputLine.substring(ipos);
                            }
                        }
                    }
                    sbSoapHeader.append(inputLine.trim());
                    sbSoapHeader.append(m_LineFeed);
                }

                if ( inSoapEnvelope ) {
                    int endSoapTagPos = inputLine.indexOf(envelopeEndTag);
                    if ( endSoapTagPos >= 0 ) {
                        String textAfterEnvelope = "";
                        int endTagPos = inputLine.indexOf(">",endSoapTagPos);
                        if ( endTagPos > endSoapTagPos ) {
                            textAfterEnvelope = inputLine.substring(endTagPos+1);
                            inputLine = inputLine.substring(0,endTagPos+1);
                        }
                        sbSoapEnvelope.append(inputLine.trim());
                        inSoapEnvelope = false;
                        SoapEnvelopeInfo soapEnvelopeInfo = new SoapEnvelopeInfo(sbSoapEnvelope.toString());
                        soapEnvelopeInfo.setSoapHeader(sbSoapHeader.toString());
                        m_SoapEnvelopeList.add(soapEnvelopeInfo);
                        sbSoapEnvelope.setLength(0);
                        sbSoapHeader.setLength(0);
                        if ( textAfterEnvelope.contains(envelopeEndTag) ) {
                            String soapMessages[] = extractXmlElementValues("Envelope", textAfterEnvelope, true);
                            for ( String soapMsg : soapMessages ) {
                                m_SoapEnvelopeList.add(new SoapEnvelopeInfo(soapMsg));
                            }
                        }
                        if ( textAfterEnvelope.contains("POST ") || textAfterEnvelope.contains("HTTP/1.")) {
                            // Trim off preceding text when POST or HTTP is not at the start of line
                            int ipos = textAfterEnvelope.indexOf("POST ");
                            if ( ipos > 0 ) {
                                textAfterEnvelope = textAfterEnvelope.substring(ipos);
                            } else if ( ipos < 0 ) {
                                ipos = textAfterEnvelope.indexOf("HTTP/1.");
                                if ( ipos > 0 ) {
                                    textAfterEnvelope = textAfterEnvelope.substring(ipos);
                                }
                            }
                            sbSoapHeader.append(textAfterEnvelope);
                        }
                    } else {
                        if ( inputLine.trim().length() > 4 ) {
                            sbSoapEnvelope.append(inputLine.trim());
                        } else if ( inputLine.trim().length() > 0) {
                            System.out.println("Suspect Line (probably chunked breaks) : " + inputLine);
                        }
                    }


                }

                inputLine = bufIn.readLine();
            }
            bufIn.close();
        } catch (IOException e) {
            addMessage("Input-Output Error for filename [" + tcpMonLogFilename + "]. [" + e.getMessage() + "]" );
            e.printStackTrace();
        }

     }

    public void outputSoapMessages(String outputDirectory) {
        if ( m_SoapEnvelopeList.size() < 1 ) {
            // Nothing to output
            return;
        }
        StringBuilder sbAbaConnectImportFile = new StringBuilder();

        boolean outputFiles = (outputDirectory != null && !"".equals(outputDirectory) );
        XmlSoapAcReformatter xmlFormatter = new XmlSoapAcReformatter();
        boolean createAbaConnectImportFile = false;
        if ( m_chkRemoveNamespaces.isSelected() ) {
            xmlFormatter.setAbaConnectImportMode("SAVE");
            createAbaConnectImportFile = true;
        }
        xmlFormatter.setConvertExtendedFieldsToXmlFormat(m_chkConvertExtendedFieldsToXmlFormat.isSelected());

        String singleIndent = xmlFormatter.getIndent();

        xmlFormatter.setRemoveNamespacePrefixes(m_chkRemoveNamespaces.isSelected());
        boolean reformatXml = m_chkReFormatSoapMessages.isSelected();
        boolean outputSoapHeaders = m_chkOutputSoapHeaders.isSelected();

        mXmlProblemFileNameMessages.clear();

        int requestCounter = 0;
        int responseCounter = 0;

        int additiveConstant = 0;
        int factorConstant = 10;
        // Resort the messages for Request / Response Order
        for ( SoapEnvelopeInfo soapEnvelopeInfo : m_SoapEnvelopeList ) {
            // Set the Request Order in the order of appearance
            if ( soapEnvelopeInfo.isRequestMessage() ) {
                requestCounter++;
                int orderIndex = ((requestCounter + additiveConstant) * factorConstant) + 1;
                soapEnvelopeInfo.setOrderIndex(orderIndex);
            } else if ( soapEnvelopeInfo.isResponseMessage() ) {
                responseCounter++;
                int orderIndex = ((responseCounter + additiveConstant) * factorConstant) + 2;
                soapEnvelopeInfo.setOrderIndex(orderIndex);
            } else {
                System.out.println("NO Message Type for " + soapEnvelopeInfo.getSoapBodyName() + " possible.");
            }
        }

        if ( requestCounter != responseCounter ) {
            addMessage("WARNING : Sorting of Request and Response Messages cannot be performed with uneven request and response counts !");
        }
        if ( requestCounter > 0 && responseCounter > 0 ) {
            Collections.sort(m_SoapEnvelopeList);
        }
        int orderIndex = 0;
        String outputFilename;
        for ( SoapEnvelopeInfo soapEnvelopeInfo : m_SoapEnvelopeList ) {
            orderIndex++;

            String soapEnvelope = soapEnvelopeInfo.getSoapEnvelopeXml();
            String soapHeader = soapEnvelopeInfo.getSoapHeader();

            String bodyName = soapEnvelopeInfo.getSoapBodyName();
//            System.out.println("==============================");
//            System.out.println("BodyName : [" + bodyName + "]");
//            System.out.println(soapEnvelope);
//            System.out.println("------------------------------");

            if ( outputFiles ) {
                outputFilename = outputDirectory + File.separator + m_FilenamePrefix + String.format("%05d",orderIndex) + "_" + bodyName + ".xml";
//                System.out.println("  ORDER [" + String.format("%8d",soapEnvelopeInfo.getOrderIndex()) + "]   Filename [" +  outputFilename + "]------------------------------");
                try {
                    BufferedOutputStream outputFileStream = new BufferedOutputStream(new FileOutputStream(outputFilename));

                    if ( reformatXml ) {
                        try {
                            xmlFormatter.saxParse(new ByteArrayInputStream(soapEnvelope.getBytes()));
                            soapEnvelope = xmlFormatter.getFormattedXml();
                        } catch (Exception e1) {
                            try {
                                xmlFormatter.saxParse(new ByteArrayInputStream(soapEnvelope.getBytes("UTF-8")));
                                soapEnvelope = xmlFormatter.getFormattedXml();
                                mXmlProblemFileNameMessages.add(outputFilename);
                                mXmlProblemFileNameMessages.add("   File could be formatted using UTF-8 encoding");
                            } catch (Exception e2) {
                                System.out.println("Error reformatting the SOAP XML !!");
                                mXmlProblemFileNameMessages.add(outputFilename);
                                mXmlProblemFileNameMessages.add("   " + e1.getMessage());
                                mXmlProblemFileNameMessages.add("   " + e2.getMessage());
                                if ( e1.getCause() != null ) {
                                    mXmlProblemFileNameMessages.add("   " + e1.getCause().toString());
                                    mXmlProblemFileNameMessages.add("   " + e1.getCause().getMessage());
                                }
                                if ( e2.getCause() != null ) {
                                    mXmlProblemFileNameMessages.add("   " + e2.getCause().toString());
                                    mXmlProblemFileNameMessages.add("   " + e2.getCause().getMessage());
                                }
                                //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                            }
                        }
                    }

                    outputFileStream.write(soapEnvelope.getBytes("UTF-8"));
                    if ( createAbaConnectImportFile && (soapEnvelope.contains("SaveRequest>") || soapEnvelope.contains("InsertRequest>") || soapEnvelope.contains("UpdateRequest>")) ) {
                        writeToAbaConnectImportFile(singleIndent, sbAbaConnectImportFile, soapEnvelope);
                    }

                    if ( outputSoapHeaders && soapHeader != null && !"".equals(soapHeader) ) {
                        outputFileStream.write(m_LineFeed.getBytes("UTF-8"));
                        outputFileStream.write("<!--  ".getBytes("UTF-8"));
//                        outputFileStream.write(" <![CDATA[ ".getBytes("UTF-8"));
                        outputFileStream.write(m_LineFeed.getBytes("UTF-8"));
                        outputFileStream.write(getValueAsXmlCompatible(soapHeader).getBytes("UTF-8"));
                        outputFileStream.write(m_LineFeed.getBytes("UTF-8"));
//                        outputFileStream.write(" ]]> ".getBytes("UTF-8"));
                        outputFileStream.write("  -->".getBytes("UTF-8"));
                        outputFileStream.write(m_LineFeed.getBytes("UTF-8"));
                    }

                    outputFileStream.close();

                } catch (FileNotFoundException e) {
                    addMessage(e.getMessage());
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    addMessage(e.getMessage());
                    e.printStackTrace();
                } catch (IOException e) {
                    addMessage(e.getMessage());
                    e.printStackTrace();
                }

            }
        }

        if ( sbAbaConnectImportFile.length() > 0 ) {
            if ( outputFiles ) {
                outputFilename = outputDirectory + File.separator + "ac_import_data.xml";
                try {
                    BufferedOutputStream outputFileStream = new BufferedOutputStream(new FileOutputStream(outputFilename));

                    String application = "UKNOWN";
                    String acStartHeader = "<?xml version='1.0' encoding='UTF-8'?>" + m_LineFeed +
                            "<AbaConnectContainer>" + m_LineFeed +
                            "  <TaskCount>1</TaskCount>" + m_LineFeed +
                            "  <Task>" + m_LineFeed +
                            "    <Parameter>" + m_LineFeed +
                            "      <Application>" + application + "</Application>" + m_LineFeed +
                            "      <Id></Id>" + m_LineFeed +
                            "      <MapId>AbaDefault</MapId>" + m_LineFeed +
                            "      <Version>2010.00</Version>" + m_LineFeed +
                            "    </Parameter>" + m_LineFeed;
                    outputFileStream.write(acStartHeader.getBytes("UTF-8"));

                    outputFileStream.write(sbAbaConnectImportFile.toString().getBytes("UTF-8"));

                    String acClosingFooter =  "  </Task>" + m_LineFeed +
                            "</AbaConnectContainer>"  + m_LineFeed;
                    outputFileStream.write(acClosingFooter.getBytes("UTF-8"));

                    outputFileStream.close();

                } catch (FileNotFoundException e) {
                    addMessage(e.getMessage());
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    addMessage(e.getMessage());
                    e.printStackTrace();
                } catch (IOException e) {
                    addMessage(e.getMessage());
                    e.printStackTrace();
                }
            }

        }
    }

    private boolean writeToAbaConnectImportFile(String singleIndent, StringBuilder sbAbaConnectImportFile, String soapEnvelope) {
        if ( sbAbaConnectImportFile == null || soapEnvelope == null || "".equals(soapEnvelope) ) return false;
        boolean addedToFileOutput = false;
        String ac_mode = "";
        if ( soapEnvelope.contains("SaveRequest>") ) {
            ac_mode = "SAVE";
        } else if (soapEnvelope.contains("InsertRequest>") ) {
            ac_mode = "INSERT";
        } else if (soapEnvelope.contains("UpdateRequest>") ) {
            ac_mode = "UPDATE";
        }
        if ( "".equals(ac_mode ) ) return false;
        String data[] = extractXmlElementValues("Data", soapEnvelope, false);
        for ( String saveData : data) {
            sbAbaConnectImportFile.append(singleIndent);
            sbAbaConnectImportFile.append(singleIndent);
            sbAbaConnectImportFile.append("<Transaction>");
            sbAbaConnectImportFile.append(m_LineFeed);
            sbAbaConnectImportFile.append(singleIndent);
            sbAbaConnectImportFile.append(singleIndent);
            sbAbaConnectImportFile.append(singleIndent);
            sbAbaConnectImportFile.append(saveData.trim());
            sbAbaConnectImportFile.append(m_LineFeed);
            sbAbaConnectImportFile.append(singleIndent);
            sbAbaConnectImportFile.append(singleIndent);
            sbAbaConnectImportFile.append("</Transaction>");
            sbAbaConnectImportFile.append(m_LineFeed);
            addedToFileOutput = true;
        }
        return addedToFileOutput;
    }

    protected String getValueAsXmlCompatible(String value)
    {
        String xmlValue = value;

//        xmlValue = xmlValue.replace("&", "&amp;");

//        xmlValue = xmlValue.replace("'", "&apos;");
//        xmlValue = xmlValue.replace("\"", "&quot;");
//        xmlValue = xmlValue.replace(">", "&gt;");
//        xmlValue = xmlValue.replace("<", "&lt;");
//        xmlValue = xmlValue.replace("-", "&ndash;");
        xmlValue = xmlValue.replace("--", "&#8211;&#8211;");


        return xmlValue;
    }

    public void addMessage(String message) {
        m_sbMessages.append(message);
    }



    private void initUI() {
        Container rootPane = this.getRootPane();
        if ( rootPane == null ) return;
        rootPane.setLayout(new BorderLayout(5,5));

        JPanel pnlMain = new JPanel();
        pnlMain.setLayout(new BoxLayout(pnlMain,BoxLayout.PAGE_AXIS));
        pnlMain.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        pnlMain.setMinimumSize(new Dimension(10,10));
        pnlMain.setMaximumSize(new Dimension(9999,9999));
        pnlMain.setPreferredSize(new Dimension(600,500));

        m_txfTcpMonFileName = getTextField();
        m_txfFilenamePrefix = getTextField();

        int chkbxWidth = 380;  // Should be about half the size of the frame window defined with setSize(..) in the main method.
        int chkbxHeight = 20;
        m_chkReFormatSoapMessages = new JCheckBox();
        m_chkReFormatSoapMessages.setMinimumSize(new Dimension(chkbxWidth,chkbxHeight));
        m_chkReFormatSoapMessages.setMaximumSize(new Dimension(chkbxWidth,chkbxHeight));
        m_chkReFormatSoapMessages.setPreferredSize(new Dimension(chkbxWidth,chkbxHeight));

        m_chkReFormatSoapMessages.setText(OPTION_TEXT_REFORMAT_SOAP_MESSAGES);
        m_chkReFormatSoapMessages.setSelected(true);  // Default is selected so that soap messages are reformatted for easy reading.
        
        m_chkRemoveNamespaces = new JCheckBox();
        m_chkRemoveNamespaces.setMinimumSize(new Dimension(chkbxWidth,chkbxHeight));
        m_chkRemoveNamespaces.setMaximumSize(new Dimension(chkbxWidth,chkbxHeight));
        m_chkRemoveNamespaces.setPreferredSize(new Dimension(chkbxWidth,chkbxHeight));
        m_chkRemoveNamespaces.setText(OPTION_TEXT_REMOVE_NAMESPACES);

        m_chkConvertExtendedFieldsToXmlFormat = new JCheckBox();
        m_chkConvertExtendedFieldsToXmlFormat.setMinimumSize(new Dimension(chkbxWidth,chkbxHeight));
        m_chkConvertExtendedFieldsToXmlFormat.setMaximumSize(new Dimension(chkbxWidth,chkbxHeight));
        m_chkConvertExtendedFieldsToXmlFormat.setPreferredSize(new Dimension(chkbxWidth,chkbxHeight));
        m_chkConvertExtendedFieldsToXmlFormat.setText(OPTION_TEXT_CONVERT_EXTENDED_FIELDS);

        m_chkOutputSoapHeaders = new JCheckBox();
        m_chkOutputSoapHeaders.setMinimumSize(new Dimension(chkbxWidth,chkbxHeight));
        m_chkOutputSoapHeaders.setMaximumSize(new Dimension(chkbxWidth,chkbxHeight));
        m_chkOutputSoapHeaders.setPreferredSize(new Dimension(chkbxWidth,chkbxHeight));
        m_chkOutputSoapHeaders.setText(OPTION_TEXT_OUTPUT_SOAP_HEADERS);

        m_txfTcpMonFileName.setTransferHandler(new FileNameTransferHandler(m_txfTcpMonFileName));

        JButton btnXmlFileSelect = getButton();

        if ( m_StartXmlFileName != null ) {
            if ( new File(m_StartXmlFileName).exists() ) {
                m_txfTcpMonFileName.setText(m_StartXmlFileName);
            }
            int iPos = m_StartXmlFileName.lastIndexOf("\\");
            if ( iPos > 0 ) {
                m_StartPath = m_StartXmlFileName.substring(0,iPos);
            }
        }
        btnXmlFileSelect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                action_ChooseFile("xml", "XML Files (*.xml)");
            }
        });

        JButton blankButton = getButton();
        blankButton.setEnabled(false);
        blankButton.setVisible(false);
        int textFieldHeight = m_txfFilenamePrefix.getHeight();
        if ( textFieldHeight < 20 ) {
            textFieldHeight = 20;
        }
        m_txfFilenamePrefix.setMaximumSize(new Dimension(100,textFieldHeight));
        m_txfFilenamePrefix.setMinimumSize(new Dimension(100,textFieldHeight));
        m_txfFilenamePrefix.setPreferredSize(new Dimension(100,textFieldHeight));

        JButton btnDeleteExistingXmlFiles = new JButton();
        btnDeleteExistingXmlFiles.setText(BUTTON_TEXT_DELETE_EXISTING_XML_FILES);
        btnDeleteExistingXmlFiles.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action_DeleteXmlFiles();
            }
        });

        pnlMain.add(getFileInputLine(getLabel("TcpMon Filename:"), m_txfTcpMonFileName,btnXmlFileSelect));
        pnlMain.add(Box.createVerticalStrut(3));

        JPanel pnlOptionsLine1 = new JPanel();
        pnlOptionsLine1.setLayout(new BoxLayout(pnlOptionsLine1,BoxLayout.LINE_AXIS));
        pnlOptionsLine1.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        pnlOptionsLine1.setMinimumSize(new Dimension(10, 20));
        pnlOptionsLine1.setMaximumSize(new Dimension(9999, 20));
        pnlOptionsLine1.setPreferredSize(new Dimension(600, 20));
        pnlOptionsLine1.add(m_chkReFormatSoapMessages);
        pnlOptionsLine1.add(Box.createHorizontalGlue());
        pnlOptionsLine1.add(m_chkOutputSoapHeaders);
        pnlOptionsLine1.add(Box.createHorizontalGlue());

        JPanel pnlOptionsLine2 = new JPanel();
        pnlOptionsLine2.setLayout(new BoxLayout(pnlOptionsLine2,BoxLayout.LINE_AXIS));
        pnlOptionsLine2.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        pnlOptionsLine2.setMinimumSize(new Dimension(10, 20));
        pnlOptionsLine2.setMaximumSize(new Dimension(9999, 20));
        pnlOptionsLine2.setPreferredSize(new Dimension(600, 20));
        pnlOptionsLine2.add(m_chkRemoveNamespaces);
        pnlOptionsLine2.add(Box.createHorizontalGlue());
        pnlOptionsLine2.add(m_chkConvertExtendedFieldsToXmlFormat);
        pnlOptionsLine2.add(Box.createHorizontalGlue());

        JPanel pnlLine = new JPanel();
        pnlLine.setLayout(new BoxLayout(pnlLine,BoxLayout.LINE_AXIS));
        pnlLine.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        pnlLine.setMinimumSize(new Dimension(10,20));
        pnlLine.setMaximumSize(new Dimension(9999,20));
        pnlLine.setPreferredSize(new Dimension(600,20));

        JButton btnExplodeMessages = new JButton();
        btnExplodeMessages.setText(BUTTON_TEXT_EXPLODE_MESSAGES);
        btnExplodeMessages.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                action_ExplodeMessages();
            }
        });

        pnlLine.add(getLabel("Filename Prefix:"));
        pnlLine.add(m_txfFilenamePrefix);
        pnlLine.add(Box.createHorizontalStrut(5));
        pnlLine.add(btnDeleteExistingXmlFiles);
        pnlLine.add(Box.createHorizontalGlue());
        pnlLine.add(btnExplodeMessages);
        pnlMain.add(pnlLine);

        pnlMain.add(Box.createVerticalStrut(3));

        pnlMain.add(pnlOptionsLine1);
        pnlMain.add(Box.createVerticalStrut(3));
        pnlMain.add(pnlOptionsLine2);
        pnlMain.add(Box.createVerticalStrut(3));

        m_txpInfoBox = new JTextPane();
        JScrollPane scrPane = new JScrollPane();
        scrPane.setMaximumSize(new Dimension(9999,9999));
        scrPane.setMinimumSize(new Dimension(20,20));
        scrPane.setPreferredSize(new Dimension(700,300));
        scrPane.setViewportView(m_txpInfoBox);

        m_txpInfoBox.setEditable(false);
        m_txpInfoBox.setBackground(new Color(250,250,250));
        m_txpInfoBox.setContentType(isInfoBoxHtmlFormat() ? "text/html" : "text/plain");

        m_txfFilenamePrefix.setText(m_FilenamePrefix);

        String helpMessage = "Select the SOAP Message/TCPMon file with the File Chooser or drag and drop the file to the edit field.\n\nThe separate" +
                " SOAP files will be extracted to individual XML files for each request and response. The output files will be written to the same directory " +
                "as the input TCPMon input file.\n\nThe output file names will use the Prefix to name the output files.";

        setInfoText(getHelpText());

        pnlMain.add(scrPane);

        rootPane.add(pnlMain);

        setTitle("Explode TCPMon SOAP Messages - " + getCurrentProgramVersionInfo());
    }

    private String getHelpText() {
        StringBuilder sbText = new StringBuilder();
        String linefeed = m_LineFeed;

        sbText.append("Select the SOAP Message/TCPMon file with the File Chooser or drag and drop the file to the edit field.");
        sbText.append(linefeed);
        sbText.append(linefeed);
        sbText.append("The separate SOAP files will be extracted to individual XML files for each request and response. ");
        sbText.append("The output files will be written to the same directory as the input TCPMon input file.");
        sbText.append(linefeed);
        sbText.append(linefeed);
        sbText.append("The output file names will use the Prefix to name the output files.");
        sbText.append(linefeed);
        sbText.append(linefeed);
        sbText.append("Options :");
        sbText.append(linefeed);
        if ( isInfoBoxHtmlFormat() ) sbText.append("<b>");
        sbText.append(OPTION_TEXT_REFORMAT_SOAP_MESSAGES);
        if ( isInfoBoxHtmlFormat() ) sbText.append("</b>");
        sbText.append(" : Reformats the extracted XML files to a more readable format.");
        sbText.append(linefeed);
        sbText.append(linefeed);
        if ( isInfoBoxHtmlFormat() ) sbText.append("<b>");
        sbText.append(OPTION_TEXT_REMOVE_NAMESPACES);
        if ( isInfoBoxHtmlFormat() ) sbText.append("</b>");
        sbText.append(" : Removes the SOAP namespaces from the XML output (useful when converting SOAP messages to AbaConnect XML Import files)");
        sbText.append(linefeed);
        sbText.append(linefeed);
        if ( isInfoBoxHtmlFormat() ) sbText.append("<b>");
        sbText.append(OPTION_TEXT_OUTPUT_SOAP_HEADERS);
        if ( isInfoBoxHtmlFormat() ) sbText.append("</b>");
        sbText.append(" : Includes the SOAP Headers in the extracted output XML files (rarely required)");
        sbText.append(linefeed);
        sbText.append(linefeed);
        if ( isInfoBoxHtmlFormat() ) sbText.append("<b>");
        sbText.append(OPTION_TEXT_CONVERT_EXTENDED_FIELDS);
        if ( isInfoBoxHtmlFormat() ) sbText.append("</b>");
        sbText.append(" : Converts SOAP Extended Fields stucture blocks to plain AbaConnect XML Import file format.");
        sbText.append(linefeed);
        sbText.append(linefeed);

        sbText.append("Buttons :");
        sbText.append(linefeed);
        if ( isInfoBoxHtmlFormat() ) sbText.append("<b>");
        sbText.append(BUTTON_TEXT_DELETE_EXISTING_XML_FILES);
        if ( isInfoBoxHtmlFormat() ) sbText.append("</b>");
        sbText.append(" : Deletes all XML files in directory matching the specified prefix.");
        sbText.append(linefeed);
        sbText.append(linefeed);
        if ( isInfoBoxHtmlFormat() ) sbText.append("<b>");
        sbText.append(BUTTON_TEXT_EXPLODE_MESSAGES);
        if ( isInfoBoxHtmlFormat() ) sbText.append("</b>");
        sbText.append(" : Starts the extraction of SOAP Messages to individual SOAP XML files");
        sbText.append(linefeed);

//        private static String OPTION_TEXT_REFORMAT_SOAP_MESSAGES = "Reformat XML SOAP Messages";
//        private static String OPTION_TEXT_REMOVE_NAMESPACES = "Remove Namespaces from Messages";
//        private static String OPTION_TEXT_OUTPUT_SOAP_HEADERS = "Output SOAP Headers";
//        private static String OPTION_TEXT_CONVERT_EXTENDED_FIELDS = "Convert Extended Fields to XML Format";
//        private static String BUTTON_TEXT_DELETE_EXISTING_XML_FILES = "Delete existing XML Files";
//        private static String BUTTON_TEXT_EXPLODE_MESSAGES = "Explode Messages";

        return sbText.toString();
    }

    private void action_DeleteXmlFiles() {
        String tcpMonLogFileDirectory = m_txfTcpMonFileName.getText();
        if ( m_FilenamePrefix == null || "".equals(m_FilenamePrefix) ) {
            m_FilenamePrefix = "SM_";
            m_txfFilenamePrefix.setText(m_FilenamePrefix);
        }
        int ipos = tcpMonLogFileDirectory.lastIndexOf(File.separator);
        if ( ipos > 0 ) {
            tcpMonLogFileDirectory = tcpMonLogFileDirectory.substring(0,ipos);
        }
        int deletedFileCount = 0;
        String messageText = "";
        int retVal = JOptionPane.showConfirmDialog(this,"Do you want to delete all existing XML files in : \n   " + tcpMonLogFileDirectory + "\nwith the prefix \"" + m_FilenamePrefix + "\" ?\n\nFiles matching pattern :  " + m_FilenamePrefix + "*.xml", "Delete Files", JOptionPane.OK_CANCEL_OPTION );
        if ( retVal ==  JOptionPane.OK_OPTION ) {

            String lowerCasePrefix = m_FilenamePrefix.toLowerCase();
            File[] fileList =  new File(tcpMonLogFileDirectory).listFiles();
            for ( File ff : fileList ) {
                String filename = ff.getName().toLowerCase();
                System.out.println("File  : " + ff.getName() );
                if ( ff.exists() && filename.startsWith(lowerCasePrefix) && filename.endsWith(".xml") ) {
                    if ( ff.delete() ) {
                        deletedFileCount++;
//                        System.out.println("Deleted File  : " + ff.getName() );
                    } else {
                        System.out.println("File could not be delete  : " + ff.getName() );
                        messageText += ("\nFile " + ff.getName() + "could not be delete !" );
                    }
                }
            }
        }
        if ( deletedFileCount > 0 ) {
            messageText += ("\n\nInformation : A total of [" + deletedFileCount + "] were deleted." );
        } else{
            messageText += ("\n\nInformation : No files were deleted." );
        }
        setInfoText(messageText);
    }

    private void action_ExplodeMessages() {
        String tcpMonLogFilename = m_txfTcpMonFileName.getText();
        m_FilenamePrefix = m_txfFilenamePrefix.getText();
        if ( m_FilenamePrefix == null || "".equals(m_FilenamePrefix) ) {
            m_FilenamePrefix = "SM_";
            m_txfFilenamePrefix.setText(m_FilenamePrefix);
        }

        String outputDirectory = "";
        int lastSlashPos = tcpMonLogFilename.lastIndexOf(File.separator);
        if ( lastSlashPos > 0 ) {
            outputDirectory = tcpMonLogFilename.substring(0,lastSlashPos);
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        explodeTcpMonFile(tcpMonLogFilename);
        outputSoapMessages(outputDirectory);

        String helpMessage = "\nA total of " + m_SoapEnvelopeList.size() + " output files were written to the output directory.\n\nOutput Directory :\n   " + outputDirectory;

        if ( mXmlProblemFileNameMessages.size() > 0 ) {
            helpMessage += "\n\n";
            helpMessage += "The following XML Files may have format syntax problems :";
            for( String problemFiles : mXmlProblemFileNameMessages ) {
                helpMessage += "\n";
                helpMessage += problemFiles;
            }
            helpMessage += "\n\n";
        }
        setCursor(Cursor.getDefaultCursor());

        setInfoText(helpMessage);
    }

    public boolean isInfoBoxHtmlFormat() {
        return mIsInfoBoxHtmlFormat;
    }

    private void setInfoText(String text) {
        if (m_txpInfoBox != null) {
            if ( text != null && isInfoBoxHtmlFormat() ) {
                String fontSize = "12";
                String htmlStyle = " style=\"font: " + fontSize + " arial,sans-serif;\"";
                String preStartTag = "<pre" + htmlStyle + ">";
                String preEndTag = "</pre>";
                String corrText = text.replaceAll("\n","<br>");
                if ( !corrText.contains("\n") && (corrText.contains("<br>") || corrText.contains("<br/>"))  ) {
                    preStartTag = "";
                    preEndTag = "";
                }
                m_txpInfoBox.setText(text == null ? "<html><body></body></html>" : ("<html><body" + htmlStyle + ">" + preStartTag + corrText + preEndTag + "</body></html>") );
//                m_txpInfoBox.setText(text == null ? "<html><body></body></html>" : ("<html><body" + htmlStyle + "><pre>" + text.replaceAll("\n","<br/>") + "</pre></body></html>") );
            } else {
                m_txpInfoBox.setText(text == null ? "" : text );
            }
        }
    }

    private JPanel getFileInputLine(JLabel lbl, JTextField txf, JButton btn) {
        JPanel pnlLine = new JPanel();
        pnlLine.setLayout(new BoxLayout(pnlLine,BoxLayout.LINE_AXIS));
        pnlLine.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        pnlLine.setMinimumSize(new Dimension(10,20));
        pnlLine.setMaximumSize(new Dimension(9999,20));
        pnlLine.setPreferredSize(new Dimension(600,20));

        pnlLine.add(lbl);
        pnlLine.add(txf);
        if ( btn != null ) {
            pnlLine.add(Box.createHorizontalStrut(3));
            pnlLine.add(btn);
        }
        return pnlLine;
    }

    private JLabel getLabel(String caption) {
        int lblWidth = 140;
        JLabel lbl = new JLabel();
        lbl.setMinimumSize(new Dimension(lblWidth,20));
        lbl.setMaximumSize(new Dimension(lblWidth,20));
        lbl.setPreferredSize(new Dimension(lblWidth,20));
        lbl.setText(caption);
        return lbl;
    }

    private JTextField getTextField() {
        JTextField txf = new JTextField();
        txf.setMinimumSize(new Dimension(10,20));
        txf.setMaximumSize(new Dimension(9999,20));
        txf.setPreferredSize(new Dimension(600,20));
        return txf;
    }

    private JButton getButton() {
        JButton btn = new JButton();
        btn.setMinimumSize(new Dimension(25,20));
        btn.setMaximumSize(new Dimension(25,20));
        btn.setPreferredSize(new Dimension(25,20));
        btn.setText("...");
        return btn;
    }

    private void action_ChooseFile(String fileExtension, String fileDescription) {
//        if ( ! "xsl".equals(fileExtension) && ! "xml".equals(fileExtension) ) {
//            return;
//        }
        if ( m_StartPath == null || "".equals(m_StartPath) ) m_StartPath = System.getProperty("user.dir","");
        JFileChooser fc = new JFileChooser(m_StartPath);
//        String[] projectExtnList = {fileExtension};
//        fc.addChoosableFileFilter(new LocalOpenFileFilter(projectExtnList,fileDescription));
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int result = fc.showOpenDialog(this);
        if ( result == JFileChooser.APPROVE_OPTION ) {
            String selectedFileName = fc.getSelectedFile().getPath();
            m_txfTcpMonFileName.setText(selectedFileName);
            int iPos = selectedFileName.lastIndexOf("\\");
            if ( iPos > 0 ) {
                m_StartPath = selectedFileName.substring(0,iPos);
            }
        }
    }

    /**
     * Extracts an element value of a specified xmlElementName tag from an XML formatted text.
     *
     * @param xmlTagName the xmlElement Name tag
     * @param text  the XML Formatted text
     * @return the value of the specified xml element tag or blank if not found
     */
    private String[] extractXmlElementValues(String xmlTagName, String text, boolean includeStartEndTags) {
        ArrayList<String> foundElements = new ArrayList<String>();
        int currentPos = 0;

        while ( currentPos >= 0 ) {
            String startTag = "<" + xmlTagName + ">";
            String endTag = "</" + xmlTagName;

            String rootStartTag = "";
            String rootEndTag = "";

            String resultText = "";
            int startValuePos = text.indexOf(startTag,currentPos);
            if ( startValuePos < 0 ) {
                startTag = "<" + xmlTagName + " ";
                startValuePos = text.indexOf(startTag,currentPos);
            }
            if ( startValuePos >= 0 ) {
                int rootStartTagPos = startValuePos;
                currentPos = -1;
                //iPos += 3;
                startValuePos = text.indexOf(">", startValuePos);
                if ( startValuePos >= 0 ) {
                    startValuePos = startValuePos + 1;
                    int endValuePos = text.indexOf(endTag, startValuePos);
                    if ( endValuePos >= 0 && endValuePos > startValuePos ) {
                        resultText = text.substring(startValuePos, endValuePos);
                        if ( includeStartEndTags ) {
                            rootStartTag = text.substring(rootStartTagPos, startValuePos);
                            rootEndTag = text.substring(endValuePos, endValuePos+endTag.length()+1);
//                            System.out.println("");
//                            System.out.println("START TAG : ]" + rootStartTag + "[");
//                            System.out.println("END TAG   : ]" + rootEndTag + "[");
                            foundElements.add(rootStartTag + resultText + rootEndTag);
                        } else {
                            foundElements.add(resultText);
                        }
                        currentPos = endValuePos;
                    }
                }
            } else {
                // Procedure for namespace defined tags
                startTag = ":" + xmlTagName;
                startValuePos = text.indexOf(startTag,currentPos);
                if ( startValuePos < 0 ) {
                    startTag = ":" + xmlTagName + " ";
                    startValuePos = text.indexOf(startTag,currentPos);
                }
                currentPos = -1;
                if ( startValuePos >= 0 ) {
                    int rootStartTagPos = startValuePos;
                    int startBracket = text.lastIndexOf("<", startValuePos );
                    if ( startBracket >= 0 && startBracket < startValuePos ) {
                        String nsName = text.substring(startBracket+1,startValuePos);
                        endTag = "</" + nsName + ":" + xmlTagName;
                        startValuePos = text.indexOf(">", startValuePos);
                        if ( startValuePos >= 0 ) {
                            startValuePos = startValuePos + 1;
                            int endValuePos = text.indexOf(endTag, startValuePos);
                            if ( endValuePos >= 0 && endValuePos > startValuePos ) {
                                resultText = text.substring(startValuePos, endValuePos);
                                if ( includeStartEndTags ) {
                                    rootStartTag = "<" + nsName + text.substring(rootStartTagPos, startValuePos);
                                    rootEndTag = text.substring(endValuePos, endValuePos+endTag.length()+1);
//                                    System.out.println("");
//                                    System.out.println("NAMESPACE START TAG : ]" + rootStartTag + "[");
//                                    System.out.println("NAMESPACE END TAG   : ]" + rootEndTag + "[");
                                    foundElements.add(rootStartTag + resultText + rootEndTag);
                                } else {
                                    foundElements.add(resultText);
                                }
                                currentPos = endValuePos + endTag.length();
                            }
                        }
                    }
                }
            }
        }
        return foundElements.toArray(new String[foundElements.size()]);
    }

    /**
     * Read a version string from a resource Text file.  Used for display in the Title
     * bar of some applications.
     *
     * If resource file is not found then just returns the hardcoded version string.
     *
     * @return returns a version string e.g. "14.05"
     */
    public static String getCurrentProgramVersionInfo() {
        String versionText = "V14.06";
        String versionFilename = "version.txt";
        URL versionUrl = ExplodeTcpMonMessages.class.getResource(versionFilename);
        InputStream versionInputStream = null;
        if ( versionUrl != null ) {
            try {
                versionInputStream = versionUrl.openStream();
                int count;
                int bufferSize = 256;
                byte data[] = new byte[bufferSize];
                while ((count = versionInputStream.read(data, 0, bufferSize)) != -1) {
                    String value = new String(data,0,count);
                    if ( value == null || "".equals(value) ) {
                        int iPos = value.indexOf("\n");
                        if ( iPos > 0 ) {
                            value = value.substring(0, iPos).trim();
                        }
                        iPos = value.indexOf("\r");
                        if ( iPos > 0 ) {
                            value = value.substring(0, iPos).trim();
                        }
                        iPos = value.indexOf(".");
                        if ( iPos == 4 ) {
                            versionText = "V" + value.substring(2).trim();
                        } else if ( iPos > 0 ) {
                            versionText = "V" + value.trim();
                        }
                    }
                }
                versionInputStream.close();
            } catch ( Exception ce ) {
                ce.printStackTrace();
                return versionText; // Version cannot be loaded
            }
        }
        return versionText;
    }

}
