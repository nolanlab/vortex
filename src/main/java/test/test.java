package test;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Nikolay Samusik
 */
public class test {

    public static void main(String[] args) throws Exception {
        test t = new test();
        t.runTest();
    }

    public test() {

       
    }
    
    private void runTest() throws Exception {
       testReadingResource( "/HSQLDB_init_script.sql");
       
       testReadingResource( "/HSQLDB_init_script2.sql");
        
        
    }
    
    private void testReadingResource(String inp)throws Exception {
        System.out.println("***testing reading resource: " + inp);
        InputStream is2 = getClass().getResourceAsStream(inp);
        
        BufferedReader br = new BufferedReader(new InputStreamReader(is2));
        String s = null;
        while((s=br.readLine())!=null){
            System.out.println(s);
        }
        return;
    }

}
