/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.scripts;

import java.sql.SQLException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import clustering.Cluster;
import clustering.ClusterSet;

import clustering.Datapoint;
import main.Dataset;
import clustering.Scorer;
import annotations.Annotation;
import util.IO;
import vortex.util.ConnectionManager;
import util.MatrixOp;
import util.logger;
import vortex.main.ClusterSetCache;

/**
 *
 * @author Nikolay
 */
public class MatchMycobacteriaToGWSByPhenoScore implements Script {

    @Override
    public Object runScript() throws SQLException {

        //Dataset hela = Dataset.getInstance("MSD endocytosis assay");

        Dataset oldhela = ConnectionManager.getStorageEngine().loadDataset("MSD Endocytosis Myc Hit Cluster Rescale (CS27078)");
        Dataset oldGWS = ConnectionManager.getStorageEngine().loadDataset("MSD Endocytosis Myc Hit Cluster Rescale (CS27078)");


        ClusterSet cs = ClusterSetCache.getInstance(oldhela, 27172);
        //ClusterSet cs2 = ClusterSet.getInstance(oldhela.toNDataset(), 18847);

        Dataset hela = oldhela; //Dataset.quantileNormalize(oldhela, oldGWS, false).toNDataset();

        //Cluster mycCluster = cs2.getClusters()[3];

        //HashMap<String, Double> hmPSS = new HashMap<String, Double>();

        //for (ClusterMember cm : mycCluster.getClusterMembers()) {
        //    hmPSS.put(cm.getProfileID().trim(),MatrixOp.lenght(cm.getDatapoint().getVector()));
        //}
        Dataset GWS = oldGWS;
        //cm.getMembership()

        // ClusterSet cs = ClusterSet.getInstance(4895L);

        final Cluster[] cl = cs.getClusters();

        for (int i = 0; i < 2; i++) {
            /*
             ProfileAverager pa = new ProfileAverager();
             for (ClusterMember cm : cl[i].getClusterMembers()) {
             double weight = (cm.getSimilarityToMode()>0.7)?1:0;
             //double weight = cm.getMembership();
             pa.addProfile(hela.getDPbyName(cm.getProfileID()).getVector(), weight);
             }
             logger.print(cl[i].toString());
            
             */
            double[] modeD = cl[i].getMode().getVector(); //cl[i].getMode().getVector().toArray();

            modeD = MatrixOp.toUnityLen(modeD);
            double[] mode = modeD;
            //mode = //hela.getDatapointByProfileID("HALOPERIDOL").getUnityLengthVector();//
            //logger.print(pa.getAverage());

            Scorer scorer = new Scorer(GWS);
            // ChiSquareScorer chi2 = new ChiSquareScorer(GWS);

            ArrayList<SimpleEntry<Datapoint, Double>> scores = new ArrayList<SimpleEntry<Datapoint, Double>>();

            // Cluster GWS_uptake_down = ClusterSet.getInstance(4043).getClusters()[3];
            Scorer chi = new Scorer(GWS);

            for (Datapoint d : GWS.getDatapoints()) {

                double[] proj = MatrixOp.copy(modeD);

                MatrixOp.mult(proj, -MatrixOp.mult(d.getVector(), modeD));

                double[] res = MatrixOp.sum(proj, d.getVector());

                //MatrixOp.mult(modeD, d.getUnityLengthVector().toArray())* MatrixOp.lenght( d.getVector().toArray());//
                if (MatrixOp.mult(modeD, d.getUnityLengthVector()) < 0.7) {
                    //logger.print("Skipped ", MatrixOp.mult(modeD, d.getUnityLengthVector().toArray()), MatrixOp.lenght( d.getVector().toArray()));
                    continue;
                }

                if (MatrixOp.lenght(res) > 150) {
                    continue;
                }

                double ps = scorer.getPSS(mode, d.getVector());
                if (Double.isNaN(ps)) {
                    ps = 1.1;
                }
                if (ps < 0.90) {
                    continue;
                }
                // logger.print("Included " + MatrixOp.mult(modeD, d.getUnityLengthVector().toArray()), MatrixOp.lenght(d.getVector().toArray()));
                /*
                 double score = scorer.getScore(mode, d.getVector());// MatrixOp.mult(modeD, d.getUnityLengthVector().toArray()); //
                 if(Double.isNaN(score)) score = 1.1;
                 //if(!GWS_uptake_down.containsProfileID(d.getProfileID())) continue;
                 */
                scores.add(new SimpleEntry<Datapoint, Double>(d, ps));
            }

            Collections.sort(scores, new Comparator<SimpleEntry<Datapoint, Double>>() {
                @Override
                public int compare(SimpleEntry<Datapoint, Double> o1, SimpleEntry<Datapoint, Double> o2) {
                    return (int) Math.signum(o2.getValue() - o1.getValue());
                }
            });

            int TOP_SIZE = scores.size();
            logger.print(TOP_SIZE);
            Datapoint[] sample = new Datapoint[TOP_SIZE];
            for (int j = 0; j < TOP_SIZE; j++) {
                sample[j] = scores.get(j).getKey();
                logger.print(sample[j].getName());
            }

            Annotation[] ann = GWS.getAnnotations();

            for (int j = 0; j < ann.length - 1; j++) {
                ann[0] = Annotation.merge(ann[0], ann[j], "tmp");
            }

            ArrayList<String> alAutoGeneIDS = IO.getListOfStringsFromStream(getClass().getClassLoader().getResourceAsStream("vortex/pipeline/AutophagyDBLists"));
            for (String string : alAutoGeneIDS) {
                String[] s2 = string.split("\t");
                Datapoint dpp = GWS.getDPbyName(s2[0]);
                if (dpp == null) {
                    continue;
                }
                if (chi.getChi2Prob(dpp.getVector()) > 0.0) {
                    //ann[0].addAnnotationPair(s2[0],"[AutoThierry]: " + s2[1]);
                }
            }


            ArrayList<String> alLipinskiGenes = IO.getListOfStringsFromStream(getClass().getClassLoader().getResourceAsStream("vortex/pipeline/AutophagyLipinski"));

            for (String string : alLipinskiGenes) {
                String[] s2 = string.split("\t");
                Datapoint dpp = GWS.getDPbyName(s2[0]);
                if (dpp == null) {
                    continue;
                }
                if (chi.getChi2Prob(dpp.getVector()) > 0.0) {
//                    ann[0].addAnnotationPair(s2[0], s2[1]);
//                    ann[0].addAnnotationPair(s2[0], "Lipinski screen hits");
                }
            }
            /*
             EnrichmentInfo[] info = Enrichment.computeEnrichment(util.getDatapointPIDs(GWS.getDatapoints()), util.getDatapointPIDs(sample), ann[0]);

             logger.print("******Uncorrected p-values*************************");
             for (EnrichmentInfo ei : info) {
             if (ei.count> 0) {
             logger.print(ei.term + ";" + ei.enrichment + ";" + ei.annotatedItems);
             }
             ei.enrichment = 0;
             }
             */
        }

        return null;
    }
}
