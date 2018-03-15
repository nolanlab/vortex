package vortex.scripts;

import it.unimi.dsi.fastutil.Hash;
import org.apache.commons.csv.CSVFormat;

import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CalculateNicheOverlapForCodex {

    public static void main (String [] args) throws IOException{

        Iterable<CSVRecord> records = CSVFormat.EXCEL.withFirstRecordAsHeader().parse(new FileReader(new File("C:\\Users\\Nikolay Samusik\\Box Sync\\CODEX PAPER REVISION\\CELL resubmission\\Niche analysis\\Suppl.Table3.CODEX_paper_MRLdataset_neighborhood_graph.csv")));

        AtomicInteger ai = new AtomicInteger();
        AtomicInteger match = new AtomicInteger();

        HashMap<String,Integer > cellToNiche = getNicheMapping();

        records.forEach(s -> {

            String k1 = getCell1Key(s);
            String k2 = getCell2Key(s);
            if(k1 != null && k2 != null){
                ai.incrementAndGet();
                if(cellToNiche.get(k1)==cellToNiche.get(k2)){
                    match.incrementAndGet();
                }
            }
        });
        System.out.println("percent match:" + match.get()/(double)ai.get());
    }

    public static String getCell1Key(CSVRecord r){
        return  r.get("Cell1 Sample_Xtile_Ytile") + r.get("X1")+r.get("Y1");
    }

    public static String getCell2Key(CSVRecord r){
        return  r.get("Cell2 Sample_Xtile_Ytile") + r.get("X2")+r.get("Y2");
    }


    public static HashMap<String, Integer> getNicheMapping() throws  IOException{
        Iterable<CSVRecord> records = CSVFormat.EXCEL.withFirstRecordAsHeader().parse(new FileReader(new File("C:\\Users\\Nikolay Samusik\\Box Sync\\CODEX PAPER REVISION\\CELL resubmission\\Niche analysis\\Suppl.Table2.CODEX_paper_MRLdatasetexpression.csv")));

        AtomicInteger ai = new AtomicInteger();

        HashMap <String, Integer> cellToNiche = new HashMap<>();

        records.forEach(s -> {
            ai.incrementAndGet();
            if(!s.get("i-niche cluster ID").equals("NA")) {
                cellToNiche.put(getKey(s),
                        Integer.parseInt(s.get("i-niche cluster ID")));
            }
        });

        System.out.println("records:"+ai);
        System.out.println("map entries:"+cellToNiche.entrySet().size());
        return cellToNiche;
    }

    public static String getKey(CSVRecord r){
        return  r.get("sample_Xtile_Ytile") + r.get("X.X")+r.get("Y.Y");
    }

}
