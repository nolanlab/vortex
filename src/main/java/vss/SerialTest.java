/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vss;

import clustering.Datapoint;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Modifier;

/**
 *
 * @author Nikolay Samusik
 */
public class SerialTest implements Serializable {
    
    int i = 100;
    double d = Math.PI;
    String st = "abc";
    
    int[] arr = new int[]{1,23,4225,142534};
    
    transient Datapoint dp = new Datapoint("dp", new double[]{0,0,0}, 0);
    
    public static void main(String[] args) {
        
    }
    
    
    public String toJSON() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT) // include static
                .create();
        String js = gson.toJson(this).replaceAll(",", ",\n");
        return js;
    }

    public static SerialTest loadFromJSON(File f) throws FileNotFoundException {
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(f));
        SerialTest exp = gson.fromJson(reader, SerialTest.class);
        return exp;
    }
    
    
     public void saveToFile(File f) throws IOException {
        String js = this.toJSON();
        BufferedWriter bw = new BufferedWriter(new FileWriter(f));
        bw.write(js);
        bw.flush();
        bw.close();
    }
    
}
