/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import sandbox.dataIO.DatasetStub;
import util.IO;
import util.logger;

/**
 *
 * @author 10286566
 */
public class DatasetStubTxtFJ extends DatasetStub{
    
    

            ArrayList<String> stringList;
            final String del = "[\t,]+";
            //String [] firstStringDelimited;
            String[] headStringDelimited;
            ArrayList<String> skippedRows = new ArrayList<>();
            
            File f;
            
            public DatasetStubTxtFJ(File f){
                this.f = f;
            }
            

            @Override
            public String getFileName() {
                return (f.getName().endsWith(".csv")||f.getName().endsWith(".txt"))?f.getName().substring(0, f.getName().length() - 4):f.getName();
            }

            @Override
            public String[] getShortColumnNames() {
                if (headStringDelimited == null) {
                    try {
                        BufferedReader br = new BufferedReader(new FileReader(f));
                        headStringDelimited = br.readLine().split(del);
                    } catch (IOException e) {
                        logger.showException(e);
                        return null;
                    }
                }
                String[] names = Arrays.copyOfRange(headStringDelimited, 1, headStringDelimited.length);
                for (int i = 0; i < names.length; i++) {
                    names[i] = names[i].replaceAll("\"", "");
                    names[i] = (names[i].matches(".+:+.+")? names[i].split(":+")[0]:names[i]).trim();
                }
                return names;
            }

            @Override
            public void close() throws IOException {
            }

            @Override
            public String[] getSkippedRows() {
                return skippedRows.toArray(new String[skippedRows.size()]);
            }

            @Override
            public String[] getLongColumnNames() {
                if (headStringDelimited == null) {
                    try {
                        BufferedReader br = new BufferedReader(new FileReader(f));
                        headStringDelimited = br.readLine().split(del);
                    } catch (IOException e) {
                        logger.showException(e);
                        return null;
                    }
                }
                String[] names = Arrays.copyOfRange(headStringDelimited, 1, headStringDelimited.length);

                for (int i = 0; i < names.length; i++) {
                    names[i] = names[i].replaceAll("\"", "");
                    names[i] = (names[i].matches(".+:+.+") ? names[i].split(":+")[1] : "").trim();
                }
                return names;
            }

            @Override
            public String getRowName(int i) {
                try {
                    if (stringList == null) {
                        stringList = IO.getListOfStringsFromStream(new FileInputStream(f));
                    }
                    try {
                        return stringList.get(i + 1).split("[\t,]")[0];
                    } catch (StringIndexOutOfBoundsException e) {
                        return null;
                    }
                } catch (IOException e) {
                    logger.showException(e);
                    return null;
                }
            }

            @Override
            public double[] getRow(int i) {
                try {
                    if (stringList == null) {
                        stringList = IO.getListOfStringsFromStream(new FileInputStream(f));
                    }
                    String[] s = stringList.get(i + 1).split(del);
                    if (s.length != getLongColumnNames().length + 1) {
                        skippedRows.add(i + ", reason: invalid number of columns (" + stringList.get(i) + " instead of " + stringList.get(i));
                        return null;
                    }
                    double[] vec = new double[s.length - 1];
                    for (int j = 0; j < vec.length; j++) {
                        try {
                            vec[j] = Double.parseDouble(s[j + 1].replace('"', ' ').trim());
                        } catch (NumberFormatException e) {
                            skippedRows.add(i + ", reason: invalid numerical format (" + s[j] + ")");
                            return null;
                        }

                    }
                    return vec;
                } catch (IOException e) {
                    logger.showException(e);
                    return null;
                }
            }

            @Override
            public long getRowCount() {
                try {
                    if (stringList == null) {
                        stringList = IO.getListOfStringsFromStream(new FileInputStream(f));
                    }
                } catch (IOException e) {
                    logger.showException(e);
                    return 0;
                }
                return stringList.size() - 1;
            }
        
}
