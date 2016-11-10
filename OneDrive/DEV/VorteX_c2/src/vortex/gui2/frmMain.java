package vortex.gui2;

/*
 * frmMain.java
 *
 * Created on February 4, 2008, 12:48 PM
 */
import clustering.ClusterMember;
import clustering.Dataset;
import clustering.Mode;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import javax.swing.*;
import vortex.scripts.Pipelines;
import vortex.util.Config;
import vortex.util.ConnectionManager;
import util.logger;

/**
 * @author Nikolay
 */
public class frmMain extends javax.swing.JFrame {

    private static frmMain Instance = null;
    private static final long serialVersionUID = 1L;
    DatasetBrowser dsb;
    ClusteringResultList cssb;
    ClusterSetBrowser csb;
    ClusterBrowserPane cbp;

    public void setActionInProgress(String actionName) {
        if (actionName == null) {
            mainProgressBar.setVisible(false);
            mainProgressBar.setString("");
            mainProgressBar.setIndeterminate(false);
        } else {
            mainProgressBar.setVisible(true);
            mainProgressBar.setString(actionName);
            mainProgressBar.setIndeterminate(true);
        }
        repaint();

    }


    public static frmMain getInstance() {
        return Instance;
    }
    private static HashMap<Dataset, frmProfilePlot> hmProfilePlots = new HashMap<>();

    public static frmProfilePlot getProfilePlot(Dataset nd) {
        if (hmProfilePlots.get(nd) == null) {
            frmProfilePlot frm = new frmProfilePlot(nd);
            int w = (int) (Toolkit.getDefaultToolkit().getScreenSize().width * 0.9);
            int h = (int) (Toolkit.getDefaultToolkit().getScreenSize().height * 0.9);
            int x = (int) (Toolkit.getDefaultToolkit().getScreenSize().width / 2 - w / 2);
            int y = (int) (Toolkit.getDefaultToolkit().getScreenSize().height / 2 - h / 2);
            frm.setBounds(x, y, w, h);
            frm.setTitle("Profile plot for Dataset" + nd.getName());
            hmProfilePlots.put(nd, frm);
        }
        return hmProfilePlots.get(nd);
    }

    class LoggerStream extends FilterOutputStream {

        BufferedWriter br;
        public LoggerStream(OutputStream aStream, PrintStream sysErr) {
            super(aStream);
            try {
                br = new BufferedWriter(new FileWriter("vortex.log"));
            } catch (Exception e) {
                ByteArrayOutputStream bs = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(bs);
                e.printStackTrace(ps);
                String s = bs.toString();
                String[] s2 = s.split("\n");
                String s3 = "";
                for (int i = 0; i < s2.length; i++) {
                    s3 += s2[i] + "\n";
                }
                s3 += "...";
                System.out.print(s3);
            }
        }

        @Override
        public void write(byte b[]) throws IOException {
            String aString = new String(b);
            br.write(aString);
            br.flush();
            System.out.print(aString);
        }

        @Override
        public void write(byte b[], int off, int len) throws IOException {
            String aString = new String(b, off, len);
            br.write(aString);
            br.flush();
            System.out.print(aString);
        }
    }

    /**
     * Creates new form frmMain
     */
    @SuppressWarnings("deprecation")
    public frmMain() {

        PrintStream ls = new PrintStream(new LoggerStream(new ByteArrayOutputStream(), System.out));

        System.setErr(ls);

        //System.setOut(ls);
        if (Config.isDevelopmentMode()) {
            logger.setOutputMode(logger.OUTPUT_MODE_CONSOLE);
        } else {
            logger.setOutputMode(logger.OUTPUT_MODE_GUI);
        }

        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
            UIManager.getLookAndFeelDefaults().put("ScrollBar.minimumThumbSize", new Dimension(30, 30));
            //UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            logger.showException(e);
        }

        do {
            try {
                if (Config.getDefaultDatabaseHost() == null) {
                    ConnectionManager.showDlgSelectDatabaseHost();
                }
                if (Config.getDefaultDatabaseHost() == null) {
                    System.exit(0);
                }
                ConnectionManager.setDatabaseHost(Config.getDefaultDatabaseHost());
            } catch (Exception e) {
                logger.showException(e);
                ConnectionManager.showDlgSelectDatabaseHost();
            }
        } while (ConnectionManager.getDatabaseHost() == null);

        this.setExtendedState(MAXIMIZED_BOTH);

        initComponents();
        
        Mode.setUseMedian(false);
        
        try {
            Image img = new javax.swing.ImageIcon(getClass().getResource("/vortex/resources/vortex.png")).getImage();
            this.setIconImage(img);
        } catch (Exception e) {
            logger.showException(e);
        }

        dsb = new DatasetBrowser();
        cssb = new ClusteringResultList();
        csb = new ClusterSetBrowser();
        cbp = new ClusterBrowserPane();
        csb.setClusterBrowserPane(cbp);

        splitLeft.setTopComponent(dsb);
        splitLeft.setBottomComponent(cssb);
        splitRight.setTopComponent(csb);
        splitRight.setBottomComponent(cbp);

        dsb.addDatasetSelectionListener(cssb);
        cssb.addClusterSetSelectionListener(csb);
        csb.addClusterSelectionListener(cbp);

        String[] dsIDs = Config.getDatasetIDsForLoading();
        dsb.loadDatasets(dsIDs);
        /*
         for (URL urls : ClasspathHelper.forPackage("vortex", this.getClass().getClassLoader())) {
         logger.print("URLs for vortex package");
         logger.print(urls);
         }*/

        this.setExtendedState(JFrame.MAXIMIZED_BOTH);

        this.setVisible(true);
        this.repaint();
        //splash.setVisible(false);
        //logger.print(this.getMaximumSize());

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel2 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        splitMain = new javax.swing.JSplitPane();
        splitLeft = new javax.swing.JSplitPane();
        splitRight = new javax.swing.JSplitPane();
        mainProgressBar = new javax.swing.JProgressBar();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("VorteX clustering environment");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setMinimumSize(new java.awt.Dimension(500, 339));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(jPanel2, gridBagConstraints);

        jPanel4.setBackground(new java.awt.Color(255, 255, 255));
        jPanel4.setLayout(new java.awt.GridBagLayout());

        splitMain.setDividerLocation(400);

        splitLeft.setDividerLocation(400);
        splitLeft.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        splitLeft.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                splitLeftPropertyChange(evt);
            }
        });
        splitMain.setLeftComponent(splitLeft);

        splitRight.setDividerLocation(400);
        splitRight.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        splitRight.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                splitRightPropertyChange(evt);
            }
        });
        splitMain.setRightComponent(splitRight);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel4.add(splitMain, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(jPanel4, gridBagConstraints);

        mainProgressBar.setMinimumSize(new java.awt.Dimension(200, 18));
        mainProgressBar.setPreferredSize(new java.awt.Dimension(200, 18));
        mainProgressBar.setStringPainted(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 3);
        getContentPane().add(mainProgressBar, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents
    HashMap<ClusterMember, Double> hmPSS = new HashMap<ClusterMember, Double>();
    HashMap<ClusterMember, Double> hmMaxChi2 = new HashMap<ClusterMember, Double>();
    HashMap<ClusterMember, Integer> hmNumOligos = new HashMap<ClusterMember, Integer>();
    HashMap<ClusterMember, Object[]> hmRows = new HashMap<ClusterMember, Object[]>();
    File selectedFile = null;
    double psThreshold = 0;
    double chi2Threshold = 0;

    private void splitRightPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_splitRightPropertyChange
        if (evt.getPropertyName().equals("dividerLocation")) {
            splitLeft.setDividerLocation(splitRight.getDividerLocation());
        }
    }//GEN-LAST:event_splitRightPropertyChange

    private void splitLeftPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_splitLeftPropertyChange
        if (evt.getPropertyName().equals("dividerLocation")) {
            splitRight.setDividerLocation(splitLeft.getDividerLocation());
        }
    }//GEN-LAST:event_splitLeftPropertyChange

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized

        splitLeft.setDividerLocation(0.33);
        splitRight.setDividerLocation(0.33);
    }//GEN-LAST:event_formComponentResized

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        ConnectionManager.getStorageEngine().shutdown();
    }//GEN-LAST:event_formWindowClosing

    public ClusteringResultList getClusterSetsBrowser() {
        return cssb;
    }

    public DatasetBrowser getDatasetBrowser() {
        return dsb;
    }

    public ClusterSetBrowser getClusterSetBrowser() {
        return csb;
    }

    public ClusterBrowserPane getClusterBrowserPane() {
        return cbp;
    }

    public static void main(String args[]) {
        for (String string : args) {
            if (string.equals("-dev")) {
                Config.setDevelopmentMode(true);
            }
            if (string.equals("-pipeline")) {
                Pipelines.runAll();
                System.exit(0);
            }

            /*
             double [][] mtx = new double [3][];
             mtx[0] =new double[]{1, 0.5, 0.15};
             mtx[1] =new double[]{0.5, 1, 0.3};
             mtx[2] =new double[]  {0.15, 0.3, 1};
             logger.print(Algebra.DEFAULT.inverse(new DenseDoubleMatrix2D(mtx)).toString());
                
             */
        }

        // splash = new frmSplash();
        // splash.setVisible(true);
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                //vortex.scripts.NewGroovyScript.main(new String[]{"abc","efd"});
                try {
                    Instance = new frmMain();
                } catch (Exception e) {
                    logger.showException(e);
                }
                //new frmPartialCorrelations(Dataset.getInstance("All_GeneProfiles_Aug")).setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JProgressBar mainProgressBar;
    private javax.swing.JSplitPane splitLeft;
    private javax.swing.JSplitPane splitMain;
    private javax.swing.JSplitPane splitRight;
    // End of variables declaration//GEN-END:variables
}
