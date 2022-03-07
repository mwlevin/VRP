/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vrp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author michael
 */
public class BranchNode implements Comparable<BranchNode> {
    public Set<Link> imposed, excluded;
    
    public double lb;
    
    public BranchNode(double lb){
        imposed = new HashSet<>();
        excluded = new HashSet<>();
        this.lb = lb;
    }
    
    public BranchNode(double lb, BranchNode parent){
        this(lb);
        
        for(Link l : parent.imposed){
            imposed.add(l);
        }
        for(Link l : parent.excluded){
            excluded.add(l);
        }
    }
    
    
    public boolean containsBound(Link l){
        return imposed.contains(l) || excluded.contains(l);
    }
    
    public boolean containsImposed(Link l){
        return imposed.contains(l);
    }
    
    public boolean containsExcluded(Link l){
        return excluded.contains(l);
    }
    
    public double getLB(){
        return lb;
    }
    
    public void addImposed(Link l){
        imposed.add(l);
    }
    
    public void addExcluded(Link l){
        excluded.add(l);
    }
    
    public int compareTo(BranchNode rhs){
        if(lb < rhs.lb){
            return -1;
        }
        else if(lb > rhs.lb){
            return 1;
        }
        else if((rhs.imposed.size() + rhs.excluded.size()) != (imposed.size() + excluded.size())){
            return (rhs.imposed.size() + rhs.excluded.size()) - (imposed.size() + excluded.size());
        }
        else{
            return rhs.imposed.size() - imposed.size();
        }
    }
}
