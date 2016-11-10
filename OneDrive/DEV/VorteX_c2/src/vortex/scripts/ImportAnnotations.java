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
package vortex.scripts;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import util.IO;
import annotations.Annotation;
import clustering.Datapoint;
import clustering.Dataset;
import java.util.ArrayList;
import vortex.util.Config;
import vortex.util.ConnectionManager;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class ImportAnnotations {

    static String[] dsNames = new String[]{
        "BM2_cct_normalized_01_non-Neutrophils",
        "BM2_cct_normalized_02_non-Neutrophils",
        "BM2_cct_normalized_03_non-Neutrophils",
        "BM2_cct_normalized_04_non-Neutrophils",
        "BM2_cct_normalized_05_non-Neutrophils",
        "BM2_cct_normalized_06_non-Neutrophils",
        "BM2_cct_normalized_07_non-Neutrophils",
        "BM2_cct_normalized_08_non-Neutrophils",
        "BM2_cct_normalized_09_non-Neutrophils",
        "BM2_cct_normalized_10_non-Neutrophils"/*
        "BM2_cct_normalized_11_non-Neutrophils",
        "BM2_cct_normalized_12_non-Neutrophils",
       /* "BM2_cct_normalized_13_non-Neutrophils",
        "BM2_cct_normalized_14_non-Neutrophils"*/
    };
    static String in = "C:\\Users\\Nikolay\\Local Working Folder\\Matt Spitzer\\Panorama BM\\gating_revised_NKZ.txt";

    public static void main(String[] args) throws Exception {

        List<String> annStrings = IO.getListOfStringsFromStream(new FileInputStream(new File(in)));

        HashMap<String, String> hmAnn = new HashMap<String, String>();

        for (String a : annStrings) {
            String[] s2 = a.split("\t");
            try {
                hmAnn.put(s2[0], s2[1]);
            } catch (ArrayIndexOutOfBoundsException e) {
                logger.print("out of bounds: " + a);
            }
        }

        do {

            if (Config.getDefaultDatabaseHost() == null) {
                ConnectionManager.showDlgSelectDatabaseHost();
            }
            if (Config.getDefaultDatabaseHost() == null) {
                System.exit(0);
            }
            ConnectionManager.setDatabaseHost(Config.getDefaultDatabaseHost());

        } while (ConnectionManager.getDatabaseHost() == null);

        for (String dsName : dsNames) {
            Dataset ds = ConnectionManager.getStorageEngine().loadDataset(dsName);
            if (ds == null) {
                continue;
            }
            logger.print(Arrays.toString(ds.getAnnotations()));//

            for (Annotation a : ds.getAnnotations()) {
                if (!a.getAnnotationName().toLowerCase().startsWith("auto")) {
                    ConnectionManager.getStorageEngine().deleteAnnotation(a);
                }
            }

            Annotation ann = new Annotation(ds, dsName + "_gating_NKZ");
            HashMap<String, List<String>> hmProfileIDsForTerm = new HashMap<>();
            for (Datapoint d : ds.getDatapoints()) {
                if (hmAnn.get(d.getFullName()) == null) {
                    continue;
                }
                String[] terms = hmAnn.get(d.getFullName()).split(";");
                String pid = d.getFullName();
                if (terms != null) {
                    for (String t : terms) {
                        if (hmProfileIDsForTerm.get(t) == null) {
                            hmProfileIDsForTerm.put(t, new ArrayList<String>());
                        }
                        hmProfileIDsForTerm.get(t).add(pid);
                    }
                }
            }
            for (String t : hmProfileIDsForTerm.keySet()) {
                List<String> terms = hmProfileIDsForTerm.get(t);
                int[] ids = new int[terms.size()];
                for (int i = 0; i < ids.length; i++) {
                    ids[i] = ds.getDPbyName(terms.get(i)).getID();
                }
                ann.addTerm(ids, t);
            }
            ds.addAnnotation(ann);
        }
    }
}
