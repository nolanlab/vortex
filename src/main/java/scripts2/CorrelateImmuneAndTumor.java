package scripts2;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import clustering.AngularDistance;
import clustering.Datapoint;
import clustering.Dataset;
import util.Correlation;
import util.IO;
import util.logger;
import clustering.DistanceMatrix;

/*
 * Copyright (C) 2015 Nikolay
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
/**
 *
 * @author Nikolay
 */
public class CorrelateImmuneAndTumor {

    public static void main(String[] args) throws Exception{
        
        
        Dataset tumor = getDatasetFromFlatFile("C:\\Users\\Nikolay\\Local Working Folder\\Ovarian Cancer\\OCFinal\\Tumor MergeClus X35\\Cluster freq per sample.txt");
        Dataset immune = getDatasetFromFlatFile("C:\\Users\\Nikolay\\Local Working Folder\\Ovarian Cancer\\OCFinal\\Immune1\\Cluster freq per sample.txt");        
        
        DistanceMatrix dmImmune = new DistanceMatrix(immune, new AngularDistance());
        DistanceMatrix dmTumor = new DistanceMatrix(tumor, new AngularDistance());
        int dim = immune.getDatapoints().length;

        for (int i = 0; i < dim; i++) {
            for (int j = i + 1; j < dim; j++) {
                
                
                
                logger.print(immune.getDatapoints()[i].getFullName() + "\t"+ immune.getDatapoints()[j].getFullName() + "\t" + tumor.getDatapoints()[i].getFullName() + "\t"+ tumor.getDatapoints()[j].getFullName() + "\t"+ Correlation.getUncenteredCorrelation(immune.getDatapoints()[i].getVector(), immune.getDatapoints()[j].getVector()) + "\t" + Correlation.getUncenteredCorrelation(tumor.getDatapoints()[i].getVector(), tumor.getDatapoints()[j].getVector()) );
            }
        }
    }

    private static Dataset getDatasetFromFlatFile(String file) throws IOException {
        
        ArrayList<String> s = IO.getListOfStringsFromStream(new FileInputStream(new File(file)));

        String[] header = s.get(0).split("\t");
        header = Arrays.copyOfRange(header, 1, header.length);

        Datapoint[] dp = new Datapoint[s.size() - 1];
        for (int i = 1; i < s.size(); i++) {
            String[] arr = s.get(i).split("\t");
            String pid = arr[0];
            arr = Arrays.copyOfRange(arr, 1, arr.length);

            double[] vec = new double[arr.length];
            for (int j = 0; j < vec.length; j++) {
                vec[j] = Double.parseDouble(arr[j]);
            }
            dp[i - 1] = new Datapoint(pid, vec, i - 1);
        }

        return new Dataset(file, dp, header);

    }
}
