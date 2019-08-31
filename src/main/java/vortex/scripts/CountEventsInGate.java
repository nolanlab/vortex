/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.scripts;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.sql.SQLException;
import sandbox.dataIO.DatasetStub;
import vortex.util.Config;
import vortex.util.ConnectionManager;
import util.MatrixOp;
import util.Optimization;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class CountEventsInGate implements Script {

    @Override
    public Object runScript() throws Exception {
        do {
            try {
                if (Config.getDefaultDatabaseHost() == null) {
                    ConnectionManager.showDlgSelectDatabaseHost();
                }
                if (Config.getDefaultDatabaseHost() == null) {
                    System.exit(0);
                }
                ConnectionManager.setDatabaseHost(Config.getDefaultDatabaseHost());
            } catch (SQLException | IOException e) {
                logger.showException(e);
                ConnectionManager.showDlgSelectDatabaseHost();
            }
        } while (ConnectionManager.getDatabaseHost() == null);

        File f = new File("C:\\Users\\Nikolay\\Local Working Folder\\Hip Surgery CyTOF\\Sep 2013CD45+CD66-\\");


        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".fcs");// && pathname.getName().contains("CD45+CD66-");
            }
        });


        double[][][] stubVecLists = new double[files.length][][];

        for (int i = 0; i < files.length; i++) {
            logger.print("reading: " + files[i].getName());
            DatasetStub stub = DatasetStub.createFromFCS(files[i]);

            int[] featureColIdx = new int[5];
            featureColIdx[0] = Optimization.indexOf(stub.getShortColumnNames(), "CD7");
            featureColIdx[1] = Optimization.indexOf(stub.getShortColumnNames(), "CD3");
            featureColIdx[2] = Optimization.indexOf(stub.getShortColumnNames(), "CD123");
             featureColIdx[3] = Optimization.indexOf(stub.getShortColumnNames(), "CD11c");
              featureColIdx[4] = Optimization.indexOf(stub.getShortColumnNames(), "HLADR");

            int cntGate = 0;
            int cntAll = 0;
            double[][] vec = new double[(int)stub.getRowCount()][];
            for (int j = 0; j < stub.getRowCount(); j++) {
                vec[j] = MatrixOp.subset(stub.getRow(j), featureColIdx);
                if (vec[j][0] < 10 && vec[j][1] < 10 && vec[j][2] >100 &&  vec[j][3] <10 &&  vec[j][4] >10) {
                    cntGate++;
                }
                cntAll++;
            }
            logger.print(files[i].getName() + " cntGate: " + cntGate + ", total: " + cntAll);

        }

        return null;
    }
}
