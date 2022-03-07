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
public class Link {
    public int i, j;
    
    public Link(int i, int j){
        this.i = i;
        this.j = j;
    }
    
    public boolean equals(Object o){
        Link rhs = (Link)o;
        
        return rhs.i == i && rhs.j == j;
    }
    
    public int hashCode(){
        return i*1000+j;
    }
}
