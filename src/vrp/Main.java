/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vrp;

/**
 *
 * @author michael
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception{
        Network test = new Network(15);
        
        Solution sol = test.solveCplex();
        System.out.println(sol.obj);
        System.out.println(test.validate(sol.x));
    }
    
}
