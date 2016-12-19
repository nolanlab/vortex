/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ClusterBrowserPane.java
 *
 * Created on 17-Sep-2010, 18:22:43
 */
package vortex.gui2;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.sql.SQLException;
import javax.swing.JPopupMenu;
import clustering.Cluster;
import clustering.ClusterMember;
import clustering.Dataset;

/**
 *
 * @author Nikolay
 */
public class ClusterBrowserPane extends javax.swing.JPanel implements ClusterSetBrowser.ClusterSelectionListener, ClipboardOwner {

    private static final long serialVersionUID = 1L;

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }

    /**
     * Creates new form ClusterBrowserPane
     */
    public ClusterBrowserPane() {
        initComponents();
        // tabPane.removeAll();
    }

    
    @Override
    public void clusterSelected(ClusterSetBrowser.ClusterSelectionEvent evt) throws SQLException {
    }

    public void selectClusterMember(ClusterMember cm) throws SQLException {
        displayCluster(cm.getCluster());
        if (tabPane.getSelectedComponent() instanceof ClusterBrowser) {
            ClusterBrowser cb = ((ClusterBrowser) tabPane.getSelectedComponent());
            cb.selectClusterMember(cm);
        }
    }

    public static JPopupMenu getProfilePopup() {
        return pmProfiles;
    }

    public void displayCluster(Cluster cl) throws SQLException {
        boolean found = false;
        for (int i = 0; i < tabPane.getTabCount(); i++) {
            if (tabPane.getComponentAt(i) instanceof ClusterBrowser) {
                if (((ClusterBrowser) tabPane.getComponentAt(i)).getCluster().equals(cl)) {
                    tabPane.setSelectedIndex(i);
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            ClusterBrowser cb = new ClusterBrowser();
            cb.setCluster(cl);
            tabPane.addTab(cl.toString(), cb);
            ClusterTabComponent ctc = new ClusterTabComponent(cl.toString());
            ctc.addCloseActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    tabPane.remove(tabPane.getSelectedIndex());
                }
            });
            tabPane.setTabComponentAt(tabPane.getTabCount() - 1, ctc);
            tabPane.setSelectedIndex(tabPane.getTabCount() - 1);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pmProfiles = new javax.swing.JPopupMenu();
        pmiPlotProfile = new javax.swing.JMenuItem();
        pmiCopyPIDs = new javax.swing.JMenuItem();
        tabPane = new javax.swing.JTabbedPane();

        pmiPlotProfile.setText("Plot profile(s)");
        pmiPlotProfile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiPlotProfileActionPerformed(evt);
            }
        });
        pmProfiles.add(pmiPlotProfile);

        pmiCopyPIDs.setText("Copy ProfileIDs");
        pmiCopyPIDs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiCopyPIDsActionPerformed(evt);
            }
        });
        pmProfiles.add(pmiCopyPIDs);

        setLayout(new java.awt.BorderLayout());
        add(tabPane, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void pmiPlotProfileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiPlotProfileActionPerformed
        if (tabPane.getSelectedIndex() >= 0) {
            ClusterBrowser cb = ((ClusterBrowser) tabPane.getComponentAt(tabPane.getSelectedIndex()));
            ClusterMember[] cm = cb.getSelectedProfiles();
            if (cm != null) {
                if (cm.length > 0) {
                    Dataset nd = cb.getCluster().getClusterSet().getDataset();
                    frmProfilePlot frm = frmMain.getProfilePlot(nd);
                    frm.setVisible(true);

                    frm.addClusterMembers(cm);
                }
            }
        }
    }//GEN-LAST:event_pmiPlotProfileActionPerformed

    private void pmiCopyPIDsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiCopyPIDsActionPerformed
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        if (tabPane.getSelectedIndex() >= 0) {
            ClusterBrowser cb = ((ClusterBrowser) tabPane.getComponentAt(tabPane.getSelectedIndex()));
            ClusterMember[] cm = cb.getSelectedProfiles();
            if (cm != null) {
                if (cm.length > 0) {
                    final StringBuilder sb = new StringBuilder();
                    for (ClusterMember cmm : cm) {
                        sb.append(cmm.getDatapointName());
                        sb.append("\n");
                    }
                    clipboard.setContents(new Transferable() {
                        @Override
                        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                            return sb.toString();
                        }

                        @Override
                        public DataFlavor[] getTransferDataFlavors() {
                            return new DataFlavor[]{DataFlavor.stringFlavor};
                        }

                        @Override
                        public boolean isDataFlavorSupported(DataFlavor flavor) {
                            return flavor.equals(DataFlavor.stringFlavor);
                        }
                    }, this);
                }
            }
        }

    }//GEN-LAST:event_pmiCopyPIDsActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private static javax.swing.JPopupMenu pmProfiles;
    private javax.swing.JMenuItem pmiCopyPIDs;
    private javax.swing.JMenuItem pmiPlotProfile;
    private javax.swing.JTabbedPane tabPane;
    // End of variables declaration//GEN-END:variables
}