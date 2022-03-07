/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vrp;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

/**
 *
 * @author michael
 */
public class Network {
    
    public final double epsilon = 0.001;
    
    
    private Node[] nodes;
    private int[][] cost;
    private int capacity;
    private IloCplex cplex;
    private int k;
    
    public Network(int n) throws IloException{
        // n is the number of nodes
        
        Random rand = new Random(0);
        
        nodes = new Node[n];
        
        nodes[0] = new Node(0, 0, 0);
        
        for(int i = 1; i < n; i++){
            nodes[i] = new Node(rand.nextInt(100), rand.nextInt(100), rand.nextInt(30));
        }
        
        capacity = rand.nextInt(300)+30;
        k = n/3;
        
        cplex = new IloCplex();
        
        cost = new int[n][n];
        
        for(int i = 0; i < n; i++){
            for(int j = 0; j < n; j++){
                if(i != j){
                    cost[i][j] = nodes[i].cost(nodes[j]);
                }
            }
        }
    }
    
    
    private Map<Link, IloRange> bounds;
    private PriorityQueue<BranchNode> branchnodes;
    private IloNumVar[][] x;
    private IloNumVar[] u;
    
    private double best_ub = Integer.MAX_VALUE;
    
    public void branchAndBound() throws IloException {
        x = new IloNumVar[nodes.length][nodes.length];
        
        for(int i = 0; i < nodes.length; i++){
            for(int j = 0; j < nodes.length; j++){
                if(i != j){
                    x[i][j] = cplex.numVar(0, 1);
                }
            }
        }
        
        // obj
        IloLinearNumExpr lhs = cplex.linearNumExpr();
        
        for(int i = 0; i < nodes.length; i++){
            for(int j = 0; j < nodes.length; j++){
                if(i != j){
                    lhs.addTerm(x[i][j], cost[i][j]);
                }
            }
        }
        
        cplex.addMinimize(lhs);
        
        
        for(int j = 0; j < nodes.length; j++){
            lhs = cplex.linearNumExpr();
            IloLinearNumExpr lhs2 = cplex.linearNumExpr();
            
            for(int i = 0; i < nodes.length; i++){
                if(i != j){
                    lhs.addTerm(x[i][j], 1);
                    lhs2.addTerm(x[j][i], 1);
                }
            }
            
            if(j == 0){
                cplex.addEq(lhs, k);
                cplex.addEq(lhs2, k);
            }
            else{
                cplex.addEq(lhs, 1);
                cplex.addEq(lhs2, 1);
            }
        }
        
        
        u = new IloNumVar[nodes.length];
        
        for(int i = 1; i < u.length; i++){
            u[i] = cplex.numVar(0, Integer.MAX_VALUE);
        }
        
        for(int i = 1; i < nodes.length; i++){
            cplex.addGe(u[i], nodes[i].demand);
            cplex.addLe(u[i], capacity);
            
            
            for(int j = 1; j < nodes.length; j++){
                if(i != j){
                    cplex.addLe(cplex.sum(cplex.sum(u[i], cplex.prod(-1, u[j])), cplex.prod(capacity, x[i][j])),
                        capacity - nodes[j].demand);
                }
            }
        }
        
        
        branchnodes = new PriorityQueue<BranchNode>();
        bounds = new HashMap<>();
        
        branchnodes.add(new BranchNode(0));
        
        BranchNode prev = null;
        
        while(!branchnodes.isEmpty()){
            BranchNode bnode = branchnodes.poll();
            
            if(bnode.lb > best_ub){
                break;
            }
            evaluate(prev, bnode);
            prev = bnode;
        }
    }
    
    
    
    public void evaluate(BranchNode prev, BranchNode child) throws IloException {
        
        Map<Link, IloRange> newBounds = new HashMap<>();
        
        // if prev is null then bounds is empty
        for(Link l : bounds.keySet()){
            
            if(child.containsImposed(l)){
                if(prev.containsImposed(l)){
                    newBounds.put(l, bounds.get(l));
                }
                else if(prev.containsExcluded(l)){
                    cplex.remove(bounds.get(l));
                    newBounds.put(l, cplex.addEq(x[l.i][l.j], 1));
                }
            }
            else if(child.containsExcluded(l)){
                if(prev.containsExcluded(l)){
                    newBounds.put(l, bounds.get(l));
                }
                else if(prev.containsImposed(l)){
                    cplex.remove(bounds.get(l));
                    newBounds.put(l, cplex.addEq(x[l.i][l.j], 0));
                }
            }
        }
        
        bounds = newBounds;
        
        for(Link l : child.imposed){
            if(!bounds.containsKey(l)){
                bounds.put(l, cplex.addEq(x[l.i][l.j], 1));
            }
        }
        
        for(Link l : child.excluded){
            if(!bounds.containsKey(l)){
                bounds.put(l, cplex.addEq(x[l.i][l.j], 0));
            }
        }
        
        cplex.solve();
        
        List<Link> nonInteger = new ArrayList<>();
        
        for(int i = 0; i < nodes.length; i++){
            for(int j = 0; j < nodes.length; j++){
                if(i != j){
                    double val = cplex.getValue(x[i][j]);
                    
                    if(Math.abs(val - 0) >= epsilon && Math.abs(val - 1) >= epsilon){
                        nonInteger.add(new Link(i, j));
                    }
                }
            }
        }
        
        double obj = cplex.getObjValue();
        
        
        
        if(nonInteger.size() > 0){
            // add branches
        }
        else{
            best_ub = Math.min(best_ub, obj);
        }
    }
    
    
    
    public Solution solveCplex() throws IloException{
        IloIntVar[][] x = new IloIntVar[nodes.length][nodes.length];
        
        for(int i = 0; i < nodes.length; i++){
            for(int j = 0; j < nodes.length; j++){
                if(i != j){
                    x[i][j] = cplex.intVar(0, 1);
                }
            }
        }
        
        // obj
        IloLinearNumExpr lhs = cplex.linearNumExpr();
        
        for(int i = 0; i < nodes.length; i++){
            for(int j = 0; j < nodes.length; j++){
                if(i != j){
                    lhs.addTerm(x[i][j], cost[i][j]);
                }
            }
        }
        
        cplex.addMinimize(lhs);
        
        
        for(int j = 0; j < nodes.length; j++){
            lhs = cplex.linearNumExpr();
            IloLinearNumExpr lhs2 = cplex.linearNumExpr();
            
            for(int i = 0; i < nodes.length; i++){
                if(i != j){
                    lhs.addTerm(x[i][j], 1);
                    lhs2.addTerm(x[j][i], 1);
                }
            }
            
            if(j == 0){
                cplex.addEq(lhs, k);
                cplex.addEq(lhs2, k);
            }
            else{
                cplex.addEq(lhs, 1);
                cplex.addEq(lhs2, 1);
            }
        }
        
        
        IloNumVar[] u = new IloNumVar[nodes.length];
        
        for(int i = 1; i < u.length; i++){
            u[i] = cplex.numVar(0, Integer.MAX_VALUE);
        }
        
        for(int i = 1; i < nodes.length; i++){
            cplex.addGe(u[i], nodes[i].demand);
            cplex.addLe(u[i], capacity);
            
            
            for(int j = 1; j < nodes.length; j++){
                if(i != j){
                    cplex.addLe(cplex.sum(cplex.sum(u[i], cplex.prod(-1, u[j])), cplex.prod(capacity, x[i][j])),
                        capacity - nodes[j].demand);
                }
            }
        }
        
        
        cplex.solve();
        
        
        double[][] output = new double[nodes.length][nodes.length];
        
        for(int i = 0; i < nodes.length; i++){
            for(int j = 0; j < nodes.length; j++){
                if(i != j){
                    try{
                        output[i][j] = cplex.getValue(x[i][j]);
                    }
                    catch(IloException ex){

                    }
                }
            }
        }
        
        double obj = cplex.getObjValue();
        
        
        cplex.clearModel();

        
        return new Solution(output, obj);
    }
    
    
    
    public boolean validate(double[][] x){
        // everything is 0 or 1
        
        
        
        for(int i = 0; i < x.length; i++){
            for(int j = 0; j < x.length; j++){
                if(i == j && x[i][j] != 0){
                    System.out.println("fail1");
                    return false;
                }
                else if(!(Math.abs(x[i][j] - 1) > epsilon || Math.abs(x[i][j] - 0) > epsilon)){
                    System.out.println("fail2");
                    return false;
                }
            }
        }
        
        
        // all customers are visited exactly once
        for(int i = 1; i < nodes.length; i++){
            double sum_inc = 0;
            double sum_out = 0;
            
            for(int j = 0; j < nodes.length; j++){
                sum_inc += x[j][i];
                sum_out += x[i][j];
            }
            
            if(sum_inc != 1 || sum_out != 1){
                System.out.println("fail3");
                return false;
            }
        }
        
        // origin has k outgoing and k incoming visits
        int sum_inc = 0;
        int sum_out = 0;
        
        for(int j = 0; j < nodes.length; j++){
            sum_inc += x[j][0];
            sum_out += x[0][j];
        }
        
        if(sum_inc != k || sum_out != k){
            System.out.println("fail4");
            return false;
        }
        
        
        
        // validate capacity constraints
        for(int i = 1; i < nodes.length; i++){
            if(x[0][i] == 1){
                int curr = i;
                int rem_cap = capacity;
                
                outer:while(curr != 0){
                    
                    
                    rem_cap -= nodes[curr].demand;
                    
                    for(int j = 0; j < nodes.length; j++){
                        if(Math.abs(x[curr][j]-1) <= epsilon){
                            curr = j;
                            continue outer;
                        }
                    }
                    
                    System.out.println("cycle");
                    return false;
                }
                
                if(rem_cap < 0){
                    System.out.println("fail5");
                    return false;
                }
            }
        }
        
        return true;
    }
    
    
}
