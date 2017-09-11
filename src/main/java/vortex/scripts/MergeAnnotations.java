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

import fmeasure.IO;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.HashMap;

/**
 *
 * @author Nikolay
 */
public class MergeAnnotations {

    public static void main(String[] args) throws Exception {
        File[] gatedFiles = new File[]{
            new File("C:\\Users\\Nikolay\\Local Working Folder\\Matt Spitzer\\Panorama BM\\revised gating3.txt"),
            new File("C:\\Users\\Nikolay\\Local Working Folder\\Matt Spitzer\\Panorama BM\\revised gating3_Kara.txt"),
            new File("C:\\Users\\Nikolay\\Local Working Folder\\Matt Spitzer\\Panorama BM\\revised gating3_Zina.txt")
        };

        File out = new File("C:\\Users\\Nikolay\\Local Working Folder\\Matt Spitzer\\Panorama BM\\gating_revized_NKZ.txt");

        BufferedWriter bw = new BufferedWriter(new FileWriter(out));

        HashMap<String, Boolean> allCells = new HashMap<>();
        HashMap<String, String>[] hmGates = new HashMap[gatedFiles.length];

        for (int i = 0; i < hmGates.length; i++) {
            hmGates[i] = new HashMap<>();
            for (String s : IO.getListOfStringsFromStream(new FileInputStream(gatedFiles[i]))) {
                String[] s2 = s.split("\t");
                allCells.put(s2[0], Boolean.TRUE);
                hmGates[i].put(s2[0], s2[1].trim().replaceAll("calls", "cells").replaceAll("T-cells", "T cells"));
            }
        }

        for (String cellID : allCells.keySet()) {
            String[] ann = new String[hmGates.length];
            for (int i = 0; i < hmGates.length; i++) {
                ann[i] = hmGates[i].get(cellID);
                if (ann[i] == null) {
                    ann[i] = "";
                }
            }
            String mostFreqTerm = "";
            int HighestFreq = 0;
            for (String a : ann) {
                int freq = 0;
                for (String a2 : ann) {
                    if (a2.equals(a)) {
                        freq++;
                    }
                }
                if (freq > HighestFreq) {
                    HighestFreq = freq;
                    mostFreqTerm = a;
                }
            }
            if (mostFreqTerm.length() > 0) {
                bw.write(cellID + "\t" + mostFreqTerm + "\n");
            }
        }

    }
}
