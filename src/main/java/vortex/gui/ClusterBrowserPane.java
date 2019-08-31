/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

 /*
 * ClusterBrowserPane.java
 *
 * Created on 17-Sep-2010, 18:22:43
 */
package vortex.gui;

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
import sandbox.clustering.Cluster;
import sandbox.clustering.ClusterMember;
import sandbox.clustering.ClusterSet;
import java.util.HashMap;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicInternalFrameUI;

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
        tabPane.setVisible(false);
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

    Thread clt = null;
    public void setClusterPlot(ClusterSet c) {



        PanProfilePlot pp = new PanProfilePlot(c, false);

        if(clt!=null){
            clt.interrupt();
        }

        clt = new Thread(
            new Runnable(){
                @Override
                public void run() {
                    pp.addClusters(c.getClusters());
                }
            }
            );
        clt.start();

        int idx = -1;
        for (int i = 0; i < tabPane.getTabCount(); i++) {
            if (tabPane.getComponentAt(i) instanceof PanProfilePlot) {

                idx = i;
            }
        }

        ClusterTabComponent ctc = new ClusterTabComponent("Profile Plot CS" + c.getID());
        ctc.addCloseActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pp.clear();
                if (tabPane.getComponentCount() == 0) {
                    tabPane.setVisible(false);
                }
            }
        });

        if (idx >= 0) {
            tabPane.add(pp, idx);
            tabPane.setSelectedIndex(idx);
            tabPane.setTitleAt(idx, "Profile Plot CS" + c.getID());
            tabPane.remove(idx + 1);
        } else {
            tabPane.add(pp);
            tabPane.setSelectedIndex(tabPane.getComponentCount() - 1);
            tabPane.setTitleAt(tabPane.getComponentCount() - 1, "Profile Plot CS" + c.getID());
        }

        for (Cluster cl : c.getClusters()) {
            cl.addPropertyChangeListener(pp);
        }

        tabPane.setVisible(true);

    }
    /*
    public void addClusterPlot(Cluster c, Annotation ann){
        if(hmProfilePlots ==null) hmProfilePlots= new HashMap<>();
        if (hmProfilePlots.get(c.getClusterSet()) == null) {
            panProfilePlot pan = new panProfilePlot(c.getClusterSet());
            pan.addCluster(c,ann);
            addPanel(pan);
            hmProfilePlots.put(c.getClusterSet(), pan);
        }else{
            hmProfilePlots.get(c.getClusterSet()).addCluster(c,ann);
        }
        
        tabPane.setVisible(true);
        panProfilePlot pp = hmProfilePlots.get(c.getClusterSet());
        for (int i = 0; i < tabPane.getTabCount(); i++) {
            if (tabPane.getComponentAt(i) instanceof ClusterBrowser) {
                if (((ClusterBrowser) tabPane.getComponentAt(i)).equals(pp)) {
                    tabPane.setSelectedIndex(i);
                    break;
                }
            }
        }      
    }*/

    private HashMap<ClusterSet, PanProfilePlot> hmProfilePlots = new HashMap<>();

    public void displayCluster(Cluster cl) throws SQLException {
        tabPane.setVisible(true);
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

            tabPane.setTabComponentAt(tabPane.getTabCount() - 1, ctc);
            tabPane.setSelectedIndex(tabPane.getTabCount() - 1);
        }
    }

    public void addPanel(final JPanel p) {
        tabPane.setVisible(true);

        ClusterTabComponent ctc = new ClusterTabComponent(p.getName());
        ctc.addCloseActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                p.setEnabled(false);
                p.setVisible(false);
                if (tabPane.getComponentCount() == 0) {
                    tabPane.setVisible(false);
                }
            }
        });
        JInternalFrame jif = new JInternalFrame(p.getName());
        BasicInternalFrameUI bi = (BasicInternalFrameUI) jif.getUI();
        bi.setNorthPane(null);
        jif.add(p);
        jif.setBorder(new EmptyBorder(0, 0, 0, 0));
        tabPane.addTab(p.getName(), jif);
        tabPane.setTabComponentAt(tabPane.getTabCount() - 1, ctc);
        tabPane.setSelectedIndex(tabPane.getTabCount() - 1);

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
        pmiCopyPIDs = new javax.swing.JMenuItem();
        tabPane = new javax.swing.JTabbedPane();

        pmiCopyPIDs.setText("Copy ProfileIDs");
        pmiCopyPIDs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiCopyPIDsActionPerformed(evt);
            }
        });
        pmProfiles.add(pmiCopyPIDs);

        setLayout(new java.awt.BorderLayout());

        tabPane.setOpaque(true);
        add(tabPane, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

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
    private javax.swing.JTabbedPane tabPane;
    // End of variables declaration//GEN-END:variables
}
