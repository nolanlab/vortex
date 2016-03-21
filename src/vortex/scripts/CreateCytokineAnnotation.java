/*
 * Copyright (C) 2016 Nikolay
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

import annotations.Annotation;
import clustering.Datapoint;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map.Entry;
import main.Dataset;
import util.DefaultEntry;
import util.logger;
import vortex.util.Config;
import vortex.util.ConnectionManager;

/**
 *
 * @author Nikolay
 */
public class CreateCytokineAnnotation {

    private static final String dsName = "Blood_CNS_Cytokine_April";

    private static final Entry<String, Double>[] rules = new Entry[]{
        new DefaultEntry<>("IL-10", 0.619589584),
        new DefaultEntry<>("TMF-a", 1.360112562),
        new DefaultEntry<>("GM-CSF", 0.732668256),
        new DefaultEntry<>("IFN-G", 0.568824899),
        new DefaultEntry<>("IL-6", 0.909376928),
        new DefaultEntry<>("IL-17A",0.668974227),
        new DefaultEntry<>("TGF-B", 0.763599222),
        new DefaultEntry<>("IFN-A", .621258035)
    };

    public static void main(String[] args) throws Exception {
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

        Dataset ds = ConnectionManager.getStorageEngine().loadDataset(dsName);

        Annotation ann = new Annotation(ds, "Cytokine thresholds_ByCompartment_Corr2");
        String[] parN = Arrays.copyOf(ds.getSideVarNames(), ds.getSideVarNames().length);

        for (int i = 0; i < parN.length; i++) {
            parN[i] = parN[i].replaceAll("\\(.*", "");
        }

        for (Entry<String, Double> rule : rules) {
            String cytN = rule.getKey();
            double cytThs = rule.getValue();
            int idx = -1;
            for (int i = 0; i < ds.getSideVarNames().length; i++) {
                if (cytN.equalsIgnoreCase(parN[i])) {
                    if (idx < 0) {
                        idx = i;
                        break;
                    } else {
                        logger.print("Duplicate match! " + cytN + "\n" + Arrays.toString(parN));
                    }
                }
            }
            if (idx < 0) {
                logger.print("No match here! " + cytN + "\n" + Arrays.toString(parN));
                return;
            }
            LinkedList<String> pidsBlood = new LinkedList<>();
            LinkedList<String> pidsCNS = new LinkedList<>();
            for (Datapoint d : ds.getDatapoints()) {
                if (d.getSideVector()[idx] > cytThs) {
                    if (d.getName().toLowerCase().contains("blood")) {
                        pidsBlood.add(d.getName());
                    } else {
                        pidsCNS.add(d.getName());
                    }
                }
            }

            ann.addTerm(pidsBlood, "Blood " + cytN);
            ann.addTerm(pidsCNS, "CNS " + cytN);
        }

        LinkedList<String> pidsBlood = new LinkedList<>();
        LinkedList<String> pidsCNS = new LinkedList<>();
        for (Datapoint d : ds.getDatapoints()) {
            if (d.getName().toLowerCase().contains("blood")) {
                pidsBlood.add(d.getName());
            } else {
                pidsCNS.add(d.getName());
            }
        }
        ann.addTerm(pidsBlood, "Blood_ALL");
        ann.addTerm(pidsCNS, "CNS_ALL");
        
        logger.print("saving ann");
        ds.addAnnotation(ann);
    }
}
