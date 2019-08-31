/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.scripts;

import util.logger;

/**
 *
 * @author Nikolay
 */
public class Pipelines {

    public static void runAll() {
        try {
         // new GatedFilesToAnnotationTable().runScript();
            // new Derivatives_MahalonobisClassification_ALL05v2().runScript();
            //new ComputeFMeasures().runScript();
            //new AutomaticXshiftClustering().runScript();
            //(new CreateAnnotationByMahalonobisClassification_ALL05()).runScript();
            //(new CountEventsInGate()).runScript();
            //(new ImportGatedFiles()).runScript();
            //(new FindDensityConnectedPathPipeline()).runScript();
            /*
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
             *//*
             NDataset ds = (NDataset)(new GenerateSynthDataset()).runScript();
             NNNKernel nnn = new NNNKernel(new EuclideanDistance());
            
           
             double [][] vec= ds.getVectors();
             for (NDatapoint dp : ds.getDatapoints()) {
             StringBuilder s = new StringBuilder("");
             for (int i = 50; i <= 50; i+=5) {
             nnn.setBandwidth(i);
             //s.append(", ");
             s.append(-nnn.getDensity(dp.getVector(), vec).doubleValue());
             s.append(", ");
             s.append(Math.log(dp.getSideVector()[0]));
             }
               
             logger.print(s);
             }
             //         (new MatchMycobacteriaToGWSByPSS()).runPipeline();*/
        } catch (Exception e) {
            logger.showException(e);
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        runAll();
    }
}
