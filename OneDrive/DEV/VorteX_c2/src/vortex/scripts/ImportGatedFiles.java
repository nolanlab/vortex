/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.scripts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.SQLException;
import org.cytobank.fcs_files.events.FcsFile;
import clustering.Dataset;
import vortex.util.Config;
import vortex.util.ConnectionManager;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class ImportGatedFiles implements Script {

    @Override
    public Object runScript() throws Exception {
        //(new FindDensityConnectedPathPipeline()).runPipeline();
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

        BufferedWriter out = new BufferedWriter(new FileWriter("C:\\Users\\Nikolay\\Local Working Folder\\Hip Surgery CyTOF\\Sept 2013_BL_1Mio_ann.txt"));

        File f = new File("C:\\Users\\Nikolay\\Local Working Folder\\Hip Surgery CyTOF\\Sept 2013 Experiment\\");
        Dataset ds = ConnectionManager.getStorageEngine().loadDataset("26patients BL 1Mio");


        //    HashMap<String, String> hmDpID = new HashMap<>();
        /*
         for (NDatapoint d : ds.getDatapoints()) {
         String[] s = d.getName().split(" ")[0].split("_");
         String sample_id = s[s.length - 2] + "_" + s[s.length - 1];
               
         String time = d.getName().split(" ")[2];
         hmDpID.put(sampleID + " " + time, d.getName());
         }*/

        int fileCNT = 0;

        for (File f2 : f.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return (name.endsWith(".fcs")&&name.contains("_BL_"));
            }
        })) {
            FcsFile fcs = new FcsFile(f2);


            for (int i = 0; i < fcs.getNumChannels(); i++) {
                if (fcs.getChannelName(i).equalsIgnoreCase("time")) {
                    
                    String formatString = "%07d";
                    int evtCnt = 0;
                    for (int k = 0; k < fcs.getEventCount(); k++) {
                        
                        String name = f2.getName().substring(0, f2.getName().indexOf(".fcs"));
                        
                        String popID = "";
                        
                        if(name.startsWith("run1")||name.startsWith("run2")||name.startsWith("run7")){
                            name =  name.substring(0, 27)+"CD45+66-";
                            name += name.substring(19,26);
                            popID = f2.getName().substring(27, f2.getName().length()-11);
                           // logger.print(name);
                        }else{
                             name =  name.substring(0, 34)+"CD45+66-";
                             try{
                                 popID = f2.getName().substring(34, f2.getName().length()-4);
                             }catch(StringIndexOutOfBoundsException e){
                                 logger.print(e);
                                 logger.print(f2.getName());
                             }
                        }
                        
                       
                        String evtID = name + " Event " + String.format(formatString, i);
                       //"run6_cct_normalized_140_BL_BL_140_CD45+66- Time 1822037"
                        //evtID = "Time " + String.format(formatString, (int) events[i][k]);
                        //evtID  =.getName();
                        //logger.print(evtID);
                        //logger.print(f2.getName());
                        

                        if (ds.getDPbyName(evtID) != null) {
                            evtCnt++;
                            out.write(evtID + "\t" + popID);
                            out.newLine();
                        }
                    }
                    logger.print("File#" + fileCNT++ + ", " + f2.getName() + " selected " + evtCnt + "events");
                    break;
                }
            }
        }
        out.flush();
        out.close();
        return null;
    }
}
