/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.scripts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.Arrays;
import sandbox.dataIO.DatasetStub;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class GatedFilesToAnnotationTable implements Script {

    /*File ungatedDIR = new File("C:\\Users\\Nikolay\\Local Working Folder\\Hip Surgery CyTOF\\Sep 2013CD45+CD66-\\BL only\\");
    File gatedDIR = new File("C:\\Users\\Nikolay\\Local Working Folder\\Hip Surgery CyTOF\\Sept 2013 Experiment gated\\");
    File out = new File("C:\\Users\\Nikolay\\Local Working Folder\\Hip Surgery CyTOF\\Complete annotation.txt\\");*/
    File ungatedDIR = new File("C:\\Users\\Nikolay\\Local Working Folder\\Matt Spitzer\\Panorama BM\\Non-neutrophils");
    File gatedDIR = new File("C:\\Users\\Nikolay\\Downloads\\Panorama_revGating_Zina_comped_fcs_files");
    File out = new File("C:\\Users\\Nikolay\\Local Working Folder\\Matt Spitzer\\Panorama BM\\revised gating3_Zina.txt");

    private long getHash(double[]evtList){
        long hash = (long) (evtList[0]);
              hash*=10000L;
               hash += (long) (((evtList[9]*5000))%5000);
               hash += (long) (((evtList[11]*5000))%5000);
               return hash;
    }
    
    @Override
    public Object runScript() throws Exception {
        File[] ungated = ungatedDIR.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".fcs");
            }
        });

        BufferedWriter bw = new BufferedWriter(new FileWriter(out));
        
        for (final File file : ungated) {
            File[] gated = gatedDIR.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".fcs") && name.startsWith(file.getName().substring(0, 21));
                }
            });
            
            DatasetStub fcsUngated = DatasetStub.createFromFCS(file);
            
            logger.print(fcsUngated.getShortColumnNames()[0],fcsUngated.getShortColumnNames()[9]+fcsUngated.getShortColumnNames()[11]);
            long[] hash =new long[(int)fcsUngated.getRowCount()];
            for (int i = 0; i < hash.length; i++) {
              hash[i] = getHash(fcsUngated.getRow(i));
            }
            
            for (File gf : gated) {
                String name = gf.getName();
                String annotationTerm = name.substring("BM2_cct_normalized_01_".length(), name.length());
                
                if(annotationTerm!=null){
                    DatasetStub fcsGated = DatasetStub.createFromFCS(gf);
               
                    for (int i = 0; i < fcsGated.getRowCount(); i++) {
                        long gatedHash = getHash(fcsGated.getRow(i));
                        boolean found = false;
                        j:for (int j = 0; j < hash.length; j++) {
                            if(hash[j]==gatedHash){
                                bw.write(fcsUngated.getFileName()+ " Event "+String.format("%07d", j)+"\t"+annotationTerm+"\n");
                                break j;
                            }
                        }
                    }
                }
            }
        }

        bw.flush();
        bw.close();
        return null;
    }
    
    public static void main(String[] args) throws Exception{
        (new GatedFilesToAnnotationTable()).runScript();
    }
}
