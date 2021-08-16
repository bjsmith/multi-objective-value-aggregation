package tools.valuefunction;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import tools.valuefunction.interfaces.ActionSelector;
import tools.valuefunction.interfaces.LookupTable;

public class WSLookupTable extends LookupTable implements ActionSelector {
    
    double[] weights = null;
    
    public WSLookupTable( int numberOfObjectives, int numberOfActions, int numberOfStates, int initValue,double[] weights ) {
        super(numberOfObjectives, numberOfActions, numberOfStates, initValue);
        this.weights = weights;        
    }

    @Override
    public int chooseGreedyAction(int state) {
        ArrayList<Integer> bestActions = new ArrayList<>();        
        double bestValue = 0.0;
        
        for (int i = 0; i < numberOfObjectives; i++) {
            bestValue += weights[i] * valueFunction.get(i) [0][state];
        }
        bestActions.add(0);

        for (int a = 1; a < numberOfActions; a++) {
            
            double scalarValue = 0.0;

            for (int i = 0; i < numberOfObjectives; i++) {
                scalarValue += weights[i] * valueFunction.get(i)[a][state];
            }
            
            if ( scalarValue >= bestValue ) {
                if ( scalarValue > bestValue ) {
                    bestActions.clear();
                    bestActions.add(a);
                    bestValue = scalarValue;
                } else {
                    bestActions.add(a);
                }
            }
            
        }

        if (bestActions.size() > 1) {
            return bestActions.get(r.nextInt(bestActions.size()));
        } else {
            return bestActions.get(0);
        }
        
    }
    
    // returns true if action is amongst the greedy actions for the specified
    // state, otherwise false
    public boolean isGreedy(int state, int action)
    {       
        // calculate value of the chosen action
    	double actionValue = 0.0;
        
        for (int i = 0; i < numberOfObjectives; i++) {
            actionValue += weights[i] * valueFunction.get(i) [action][state];
        }
        // check if any other actions are valued more highly - can
        // return false immediately if they are
        for (int a = 0; a < numberOfActions; a++) 
        {
            if (a!=action)
            {
            	double scalarValue = 0.0;
	            for (int i = 0; i < numberOfObjectives; i++) {
	                scalarValue += weights[i] * valueFunction.get(i)[a][state];
	            }
	            
	            if ( scalarValue > actionValue ) {
	            	return false;
	            }
            }
            
        }
        // if we get here then no better options were found so can return true
        return true;   	
    }
    
    // implements conventional single-objective softmax,using the weighted sum of the objectives
    private int softmax(double temperature, int state)
    {
    	int best = chooseGreedyAction(state);
    	double scalarisedValue[] = new double[numberOfActions];
        for (int a = 0; a < numberOfActions; a++) 
        {
            scalarisedValue[a] = 0.0;
            for (int i = 0; i < numberOfObjectives; i++) {
            	scalarisedValue[a] += weights[i] * valueFunction.get(i)[a][state];
            }
        }
    	return Softmax.getAction(scalarisedValue,temperature,best);
    }
    
    // For this type of agent, the mo-softmax operations are just mapped onto
    // single-objective softmax using the weighted scalarisation
    protected int softmaxTournament(double temperature, int state)
    {
    	int best = chooseGreedyAction(state);
    	double scalarisedValue[] = new double[numberOfActions];
        for (int a = 0; a < numberOfActions; a++) 
        {
            scalarisedValue[a] = 0.0;
            for (int i = 0; i < numberOfObjectives; i++) {
            	scalarisedValue[a] += weights[i] * valueFunction.get(i)[a][state];
            }
        }
    	return Softmax.getTournamentAction(scalarisedValue,temperature,best);
    }
    
    protected int softmaxAdditiveEpsilon(double temperature, int state)
    {
    	return softmax(temperature, state);
    }
    
    public double[] getWeights() {
        return weights;
    }

    public void setWeights(double[] weights) {
        this.weights = weights;
    }
    
     public void saveValueFunction(String theFileName) {
    	System.out.println(theFileName);
        for (int s = 0; s < numberOfStates; s++) {
            for (int a = 0; a < numberOfActions; a++) {
            	System.out.print("State "+s+"\tAction "+a+"\t");         	
            	for (int i = 0; i < numberOfObjectives; i++) {
                    System.out.print(valueFunction.get(i)[a][s] +"\t");
                }
            	System.out.println();
            }                
        }  	
        try {
            DataOutputStream DO = new DataOutputStream(new FileOutputStream(new File(theFileName)));
            for (int i = 0; i < numberOfObjectives; i++) {
                for (int a = 0; a < numberOfActions; a++) {
                    for (int s = 0; s < numberOfStates; s++) {
                        DO.writeDouble( valueFunction.get(i)[a][s] );
                    }
                }                
            }
            DO.close();
        } catch (FileNotFoundException ex) {
            System.err.println("Problem saving value function to file: " + theFileName + " :: " + ex);
        } catch (IOException ex) {
            System.err.println("Problem writing value function to file:: " + ex);
        }
    }
    
    public void loadValueFunction(String theFileName) {
        try {
            DataInputStream DI = new DataInputStream(new FileInputStream(new File(theFileName)));
            for (int i = 0; i < numberOfObjectives; i++) {
                for (int a = 0; a < numberOfActions; a++) {
                    for (int s = 0; s < numberOfStates; s++) {
                        valueFunction.get(i)[a][s] = DI.readDouble();
                    }
                }                
            }
            DI.close();
        } catch (FileNotFoundException ex) {
            System.err.println("Problem loading value function from file: " + theFileName + " :: " + ex);
        } catch (IOException ex) {
            System.err.println("Problem reading value function from file:: " + ex);
        }
    }
   
}