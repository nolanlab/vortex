/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.util;

import java.sql.SQLException;
import javax.swing.table.DefaultTableModel;
import annotations.Annotation;
import clustering.Cluster;
import java.util.Arrays;
import util.MatrixOp;

/**
 *
 * @author Nikolay
 */
public class GroupStatisticsInClusters {

    /**
     * @param cs - Cluster set to analyze
     * @param relative - if true, group contributions are expressed as
     * percentages of cluster size, if false - as absolute numbers
     * @return DefaultTableModel object, where each NDatapoint corresponds to
     * one group
     *
     */
    public static DefaultTableModel getGroupStatistics(Cluster[] cl, Annotation ann, boolean relative) throws SQLException {
        String[] colNames = new String[cl[0].getClusterSet().getDataset().getFeatureNamesCombined().length + 3];
        System.arraycopy(cl[0].getClusterSet().getDataset().getFeatureNamesCombined(), 0, colNames, 3, cl[0].getClusterSet().getDataset().getFeatureNamesCombined().length);
        colNames[0] = "Cluster";
        colNames[1] = "Term";
        colNames[2] = "Count";
        DefaultTableModel model = new DefaultTableModel(colNames, 0);
        String[] s = ann.getTerms();
        for (Cluster c : cl) {

            double[][] vec = c.getMedianVecByAnnotation(ann);
            for (int i = 0; i < vec.length; i++) {
                Object[] row = new Object[colNames.length];
                row[0] = c.getID();
                row[1] = s[i];
                for (int j = 0; j < vec[i].length; j++) {
                    row[j + 2] = vec[i][j];
                }
                model.addRow(row);
            }
        }
        return model;
    }
    
    public static double[][] getGroupStatisticsArrayLists(Cluster[] cl, Annotation ann, boolean relative) throws SQLException {
        double[][] res = new double [cl.length][ann.getTerms().length];
        
        for (int i = 0; i < cl.length; i++) {
            double[][] vec = cl[i].getMedianVecByAnnotation(ann);
            for (int j = 0; j < ann.getTerms().length; j++) {
                res[i][j] = vec[j][0];
            }
        }
        return res;
    }
    
    public static double[][] getGroupStatisticsFunctionalArrayLists(Cluster[] cl, Annotation ann, boolean relative) throws SQLException {
        double[][] res = new double [cl.length][0];
        
        for (int i = 0; i < cl.length; i++) {
            double[][] vec = cl[i].getMedianVecByAnnotation(ann);
            for (int j = 0; j < ann.getTerms().length; j++) {
                res[i] = MatrixOp.concat(res[i], Arrays.copyOfRange(vec[j],1,vec[j].length));
            }
        }
        return res;
    }

}
