// A modified version of the TLO_LookupTable created to support the satisficing variant of our potential-based
// side-effect impact minimisation agent
// Designed specifically for the side-effects problems, so it maximises the value of the second objective (alignment)
// subject to meeting the threshold value for the (first objective + accumulated
// reward for the second objective) - i.e. minimises impact subject to achieving a satisfactory level of performance on the primary
// objective

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

public class SatisficingLookupTable extends LookupTable implements ActionSelector 
{
    double thisStateValues[][];
    double summedPrimaryReward;
    double summedImpact;
    double thresholds[];

    public SatisficingLookupTable( int numberOfObjectives, int numberOfActions, int numberOfStates, int initValue, 
    								double rewardThreshold, double safetyThreshold) 
    {
        super(numberOfObjectives, numberOfActions, numberOfStates, initValue);
        if (numberOfObjectives!=3)
        	System.out.println("ERROR!!! Don't use SatisficingLookupTable for problems other than side-effects ones."); 
        thresholds = new double[2];
        thresholds[0] = rewardThreshold;
        thresholds[1] = safetyThreshold;
        thisStateValues = new double[numberOfActions][2]; // leave out the performance objective to avoid any risk of accidentally using it in action selection
        summedPrimaryReward = 0;
    }
    
       
    // for debugging purposes - print out Q- values for all actions for the current state
    public void printCurrentStateValues(int state)
    {
    	getActionValues(state); // copy the action values into the 2D array thisStateValues
    	System.out.print("State\t" + state + "\t" + summedPrimaryReward+"\t"+summedImpact+"\t"+"thP\t" +thresholds[0]+"\tthA\t" +thresholds[1] + "\tActions\t");
		for (int a=0; a<numberOfActions; a++)
		{
			System.out.print(a+"\t");
			for (int obj=0; obj<2; obj++)
			{
				System.out.print(thisStateValues[a][obj]+"\t");
			}
		}  
    }
    
    // This is a bit of a hack to get around the fact that the structure of Rustam's lookup table doesn't map nicely
    // on to my TLO library functions. The whole Agent and ValueFunction structure of Rustam's code needs to be refactored at some point
    // Unlike the SafetyFirstLookupTable this doesn't need to reorder the objective values
    private void getActionValues(int state)
    {
		for (int a=0; a<numberOfActions; a++)
		{
			// copy the primary-reward values + accumulated primary reward into the first field
			thisStateValues[a][0] = valueFunction.get(0)[a][state] + summedPrimaryReward;
			// copy the impact-reward into the second field
			thisStateValues[a][1] = valueFunction.get(1)[a][state] + summedImpact;
			// ignore the performance-reward values as we shouldn't have access to them anyway
		}
    }
    
    public void setAccumulatedReward(double accumulatedReward)
    {
    	summedPrimaryReward = accumulatedReward;
    }
    
    public void setAccumulatedImpact(double accumulatedImpact)
    {
    	summedImpact = accumulatedImpact;
    }

    @Override
    public int chooseGreedyAction(int state) 
    {
    	getActionValues(state);
    	int greedy = TLO.greedyAction(thisStateValues, thresholds); 
    	// JUST FOR DEBUGGING
    	/*if (state==30 || state==31)
    	{
    		printCurrentStateValues(state);
    		System.out.println("Greedy action\t" + greedy);
    	}*/
    	return greedy;
    }
    
    // returns true if action is amongst the greedy actions for the specified
    // state, otherwise false
    public boolean isGreedy(int state, int action)
    {  
    	getActionValues(state);
    	int best = TLO.greedyAction(thisStateValues, thresholds); 
    	// this action is greedy if it is TLO-equal to the greedily selected action
    	return (TLO.compare(thisStateValues[action], thisStateValues[best], thresholds)==0);
    }
       
    // softmax selection based on tournament score (i.e. the number of actions which each action TLO-dominates)
    protected int softmaxTournament(double temperature, int state)
    {
    	int best = chooseGreedyAction(state); // as a side-effect this will also set up the Q-values array
    	double scores[] = TLO.getDominanceScore(thisStateValues,thresholds);
    	return Softmax.getAction(scores,temperature,best);
    }
    
    // softmax selection based on each action's additive epsilon score
    protected int softmaxAdditiveEpsilon(double temperature, int state)
    {
    	int best = chooseGreedyAction(state); // as a side-effect this will also set up the Q-values array
    	double scores[] = TLO.getInverseAdditiveEpsilonScore(thisStateValues,best);
    	return Softmax.getAction(scores,temperature,best);
    }    

    public double[] getThresholds() {
        return thresholds;
    }

    public void setThresholds(double[] thresholds) {
        this.thresholds = thresholds;
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