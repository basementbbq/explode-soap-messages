/*
 * FileNameTransferHandler.java  
 *
 * Creator:
 * 4/06/14 11:57 AM Sippel
 *
 * Maintainer:
 * 4/06/14 11:57 AM Sippel
 *
 * Last Modification:
 * $Id: $
 *
 * Copyright (c) 2003 ABACUS Research AG, All Rights Reserved
 */
package ch.abacus.abaconnecttools;

import javax.swing.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class handles the drog and drop functionality for a filename into a JTextField UI component.
 * <p/>
 * It can be used with the JTextField and setTransferHandler() method as follows :
 * txf_FileNameField.setTransferHandler(new FileNameTransferHandler(txf_FileNameField));
 */
public class FileNameTransferHandler extends TransferHandler {

    private ActionListener mEditFieldChangedListener = null;
    private JTextField m_txfFileNameEditField = null;

    private ArrayList<String> mAllowedFileExtensionList = null;
    private int mMatchingExtensionCount = -1;
    private long mLastCanImportCheck = 0;
    private long mLastCanImportCheckInterval = 1200;  // 1.2 seconds

    public FileNameTransferHandler(JTextField txfFilenameEditField) {
        this(txfFilenameEditField, null, null);
    }

    public FileNameTransferHandler(JTextField txfFilenameEditField, ActionListener editFieldChangedListener) {
        this(txfFilenameEditField, editFieldChangedListener, null);
    }

    public FileNameTransferHandler(JTextField txfFilenameEditField, String[] allowedFileExtension) {
        this(txfFilenameEditField, null, allowedFileExtension);
    }

    public FileNameTransferHandler(JTextField txfFilenameEditField, ActionListener editFieldChangedListener, String[] allowedFileExtension) {
        m_txfFileNameEditField = txfFilenameEditField;
        mEditFieldChangedListener = editFieldChangedListener;
        mAllowedFileExtensionList = null;
        if ( allowedFileExtension != null ) {
            if ( allowedFileExtension.length > 0 ) {
                mAllowedFileExtensionList = new ArrayList<String>();
                mAllowedFileExtensionList.addAll(Arrays.asList(allowedFileExtension));
            }
        }
    }

    @Override
    public boolean canImport(TransferSupport support) {
//        System.out.println("   canImport  support ");
        boolean isImportOk = super.canImport(support);
        if ( (System.currentTimeMillis() - mLastCanImportCheck) > mLastCanImportCheckInterval ) {
            mMatchingExtensionCount = -1;
        }
        mLastCanImportCheck = System.currentTimeMillis();
        if ( !support.isDrop() ) return isImportOk;
        if( !isImportOk ) return false;
        if ( mMatchingExtensionCount < 0 && mAllowedFileExtensionList != null ) {
//            mTransferringFileExtensionList = new ArrayList<String>();
            // Fill the transferring file extension list from the actual files being dragged
            Transferable trans = support.getTransferable();
            if ( trans.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ) {

                int matchingExtensionCount = 0;
                try {
                    Object transferData = trans.getTransferData(DataFlavor.javaFileListFlavor);
                    if ( transferData instanceof List) {
                        List<File> fl = (List<File>) trans.getTransferData(DataFlavor.javaFileListFlavor);
                        for (File ff : fl) {
//                            System.out.println("  Importing filename : " + ff.getName() + "   from : " + ff.getAbsolutePath());
                            String shortFilename = ff.getName();
                            int ipos = shortFilename.lastIndexOf(".");
                            if ( ipos > 0 ) {
                                String extension = shortFilename.substring(ipos+1);
                                if ( mAllowedFileExtensionList.contains(extension) ) {
                                    matchingExtensionCount++;
                                }
                            }
                        }
//                        System.out.println("  SET mMatchingExtensionCount : " + mMatchingExtensionCount );
                        mMatchingExtensionCount = matchingExtensionCount;
                    }
                } catch (UnsupportedFlavorException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (java.awt.dnd.InvalidDnDOperationException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if ( mAllowedFileExtensionList != null && mMatchingExtensionCount >= 0) {
            // If import is ok then there must be a least one file matching the Allowed extension list
            isImportOk = (mMatchingExtensionCount > 0);
//            System.out.println("  Returning  mMatchingExtensionCount : " + mMatchingExtensionCount + "   canImport[" + isImportOk + "]");
        }
        return isImportOk;
    }


    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
//        System.out.println("   canImport  component,  flavours");
        if (transferFlavors == null) return false;
        if (transferFlavors.length != 1) return false;
        boolean importOk = (transferFlavors[0].isFlavorJavaFileListType() || transferFlavors[0].isFlavorTextType());
        return importOk;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        if (c instanceof JTextField) {
            JTextField source = (JTextField) c;
            String data = source.getSelectedText();
            return new StringSelection(data);
        }
        return super.createTransferable(c);
    }

    @Override
    public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
        super.exportToClipboard(comp, clip, action);
        if (action == MOVE && comp instanceof JTextField) {
            ((JTextField) comp).replaceSelection("");
        }
    }

    @Override
    public int getSourceActions(JComponent c) {
        return ((c instanceof JTextField) ? COPY_OR_MOVE : super.getSourceActions(c));
    }

    public boolean importData(JComponent comp, Transferable transferable) {
//        System.out.println("  ***************** importData  ");
        String droppedFileName = "";
        Object data;
        mMatchingExtensionCount = -1;  // Reset matching extension count when data is imported
        DataFlavor[] transferFlavors = transferable.getTransferDataFlavors();
        if (transferFlavors == null) return false;
        if (transferFlavors.length > 1) {
            // Handle cut and pastes via the clipboard
            for (DataFlavor transferFlavor : transferFlavors) {
                try {
                    // Look for String objects to copy
                    if (transferFlavor.isFlavorTextType() && transferable.isDataFlavorSupported(transferFlavor)) {
                        data = transferable.getTransferData(transferFlavor);
                        if (data instanceof String) {
                            droppedFileName = data.toString();
//                                System.out.println(" ** Plain Text : [" + droppedFileName + "]  Flavor[" + transferFlavor.getHumanPresentableName() + "]  Class[" + data.getClass().getName() + "]");
                            if (m_txfFileNameEditField != null) {
                                m_txfFileNameEditField.replaceSelection(droppedFileName);
                                updatedFileNameEditField();
                                return true;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        if (transferFlavors.length != 1) return false;
        // Handle drag and drops via mouse from other programs
        try {
            data = transferable.getTransferData(transferFlavors[0]);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        if (data == null) return false;
        if (transferFlavors[0].isFlavorJavaFileListType() && data instanceof java.util.List) {
            java.util.List arrData = (java.util.List) data;
            if (arrData.size() == 1) {
                droppedFileName = arrData.get(0).toString();
            }
        } else if (transferFlavors[0].isFlavorTextType()) {
            droppedFileName = data.toString();
        }
        if (droppedFileName == null || "".equals(droppedFileName)) return false;
        if (m_txfFileNameEditField != null) {
            m_txfFileNameEditField.setText(droppedFileName);
            updatedFileNameEditField();
        } else {
            System.out.println("Incompatible filename : " + droppedFileName);
        }
        return true;
    }

    private void updatedFileNameEditField() {
        if (mEditFieldChangedListener != null) {
            mEditFieldChangedListener.actionPerformed(new ActionEvent(m_txfFileNameEditField, 0, "CHANGED"));
        }
    }
}
