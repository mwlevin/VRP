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
public class Node {
    public int demand;
    
    public int x, y;
    
    
    public Node(int x, int y, int demand){
        this.x = x;
        this.y = y;
        this.demand = demand;
    }
    
    public int cost(Node n2){
        return (int)Math.ceil( (x - n2.x) * (x - n2.x) + (y - n2.y) * (y - n2.y));
    }
}
