// Extends the TLO_LookupTable to support EOVF/MOX-style exploration
// There is an extra objective which indicates underexplored states. It is optimistically initialised, receives a reward of 0 on all steps, and
// is discounted (even if the other objectives aren't) so its values will decay to zero over time.
// This objective is added on to each of the other objectives before TLO action-selection is performed.
// Will usually be used only with greedy action selection (ie egreedy with epsilon set to zero) but the other options have been retained for flexibility

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

import org.rlcommunity.rlglue.codec.types.Reward;

import tools.valuefunction.interfaces.ActionSelector;
import tools.valuefunction.interfaces.LookupTable;

import tools.valuefunction.AggregatorUtils;

public class TLO_EOVF_LookupTable extends LookupTable implements ActionSelector 
{
	
	// constants to label the different exploration strategies
	public static final int EGREEDY = 0;
	public static final int SOFTMAX_TOURNAMENT = 1;
	public static final int SOFTMAX_ADDITIVE_EPSILON = 2;
	
    
    Random r = null;
    int explorationStrategy = 0; // default is egreedy
    double thresholds[];
    double thisStateValues[][];
    private int numActualObjectives;
    private int eovfIndex;
    private double eovfGamma;

    public TLO_EOVF_LookupTable(int numberOfObjectives, int numberOfActions, int numberOfStates, int initValue, double thresholds[], double eovfGamma) 
    {
        super(numberOfObjectives+1, numberOfActions, numberOfStates, initValue);
        r = new Random(AggregatorUtils.random.nextInt());    
        this.thresholds = thresholds;
        numActualObjectives = numberOfObjectives;
        eovfIndex = numberOfObjectives;
        thisStateValues = new double[numberOfActions][numActualObjectives];
        this.eovfGamma = eovfGamma;

    }
    
    // set the exploration strategy
    public void setExplorationStrategy(int ex)
    {
    	explorationStrategy = ex;
    }
    
    public double[] getThresholds() {
        return thresholds;
    }

    public void setThresholds(double[] thresholds) {
        this.thresholds = thresholds;
    }
    
    // over-riding the error calculation code from the superclass so as to account for the exploration objective updating
    // actual objectives are updated as per normal Q-learning, but the exploratory objective uses the action for the next state which is greedy
    // with respect to exploration to determine its TD error
    public void calculateErrors(int action, int previousState, int greedyAction, int newState, double gamma, Reward reward) 
    {
    	double[][] qValues;
    	// first calculate errors for the actual objectives as normal
        for (int i = 0; i < numActualObjectives; i++) {
            qValues = valueFunction.get(i);
            
            double thisQ = qValues[ action ][ previousState ];                        
            double maxQ = qValues[ greedyAction ][ newState ];
            
            double err = getRewardForThisObjective(reward, i) + gamma * maxQ - thisQ;
            
            errors[i] = err;
        }
        qValues = valueFunction.get(eovfIndex);
        errors[eovfIndex] = 0 + eovfGamma * qValues[greedyAction][newState] - qValues[action][previousState]; // hard-wired 0 reward for the eovf objective

        //int greedyExploratoryAction = chooseGreedyExploratoryAction(newState);
        //errors[eovfIndex] = 0 + eovfGamma * qValues[greedyExploratoryAction][newState] - qValues[action][previousState]; // hard-wired 0 reward for the eovf objective
        
    }
    
    @Override
    public void calculateTerminalErrors(int action, int previousState, double gamma, Reward reward) 
    {
    	double[][] qValues;
        for (int i = 0; i < numActualObjectives; i++) {
            qValues = valueFunction.get(i);
            
            double thisQ = qValues[ action ][ previousState ];                        
            
            errors[i] =  getRewardForThisObjective(reward, i) - thisQ;
        }    
        qValues = valueFunction.get(eovfIndex);
        errors[eovfIndex] = 0 - qValues[action][previousState];     // hard-wired 0 reward for the eovf objective
    }
    
    // this is a specialised overloaded version of the update function to deal with
    // the fact that eovf has its own gamma value - steps specifies how far back in time
    // this state and action pair is
    public void update(int action, int state, int steps, double lambda, double alpha, double gamma) {
        //System.out.println("\t\tUpdate - state,action " + state + ", " + action);  
    	double trace = Math.pow(lambda*gamma, steps);
        for (int i = 0; i < numberOfObjectives; i++) 
        {
            double[][] qValues = valueFunction.get(i);
            double thisQ = qValues[ action ][ state ];
            if (i==eovfIndex)
            {
            	trace = Math.pow(lambda*eovfGamma, steps); // use the eovf-gamma trace instead
            }
            double newQ = thisQ + alpha * trace * errors[i];
            qValues[ action ][ state ] = newQ;
            //System.out.println(i + ": " + thisQ + " -> " + newQ);
        }        
    }
    
    
    // This is a bit of a hack to get around the fact that the structure of Rustam's lookup table doesn't map nicely
    // on to my TLO library functions. The whole Agent and ValueFunction structure of Rustam's code needs to be refactored at some point
    // Copies the q-values for the current state into the 2 dimensional arraythisStateValues index by [action][objective]
    // In this class, we also use this as the opportunity to add the novelty objective values onto the other objectives
    private void getActionValuesPlusNovelty(int state)
    {
    	//System.out.println("EOVF getActionValues - state " + state);
    	double novelty[][] = valueFunction.get(eovfIndex);
    	for (int obj=0; obj<numActualObjectives; obj++)
    	{
    		double[][] thisObjQ = valueFunction.get(obj);
    		for (int a=0; a<numberOfActions; a++)
    		{
    			thisStateValues[a][obj] = thisObjQ[a][state] + novelty[a][state];
    			//System.out.println(a + ": " + thisObjQ[a][state] + " + " + novelty[a][state] + " = " + thisStateValues[a][obj]);
    		}
    	}    	
    }
    
    // Fill the state values array with the Q-values for the current state, not using the novelty objective
    private void getActionValuesWithoutNovelty(int state)
    {
    	//System.out.println("EOVF getActionValuesWithoutNovelty - state " + state);
    	double novelty[][] = valueFunction.get(eovfIndex);
    	for (int obj=0; obj<numActualObjectives; obj++)
    	{
    		double[][] thisObjQ = valueFunction.get(obj);
    		for (int a=0; a<numberOfActions; a++)
    		{
    			thisStateValues[a][obj] = thisObjQ[a][state];
    			//System.out.println(a + ": " + thisStateValues[a][obj]);
    		}
    	}    	
    }
    
    public void setEovfGamma(double gamma)
    {
    	eovfGamma = gamma;
    	System.out.println("TLOEOVFLT: eovfGamma = " + eovfGamma);
    }
    
    // choose an action greedily without considering the novelty objective
    public int chooseGreedyAction(int state) 
    {
    	getActionValuesWithoutNovelty(state);
    	return TLO.greedyAction(thisStateValues, thresholds); 
    }
    
    // Choose an action greedily, takinginto account the exploratory objective
    public int chooseGreedyExploratoryAction(int state)
    {
    	getActionValuesPlusNovelty(state);
    	return TLO.greedyAction(thisStateValues, thresholds);  
    }
    
    
    
    // returns true if action is amongst the greedy actions for the specified
    // state, otherwise false - only considers the actual objectives, not the novelty factor
    public boolean isGreedy(int state, int action)
    {  
    	getActionValuesWithoutNovelty(state);
    	int best = TLO.greedyAction(thisStateValues, thresholds); 
    	// this action is greedy if it is TLO-equal to the greedily selected action
    	return (TLO.compare(thisStateValues[action], thisStateValues[best], thresholds)==0);
    }
    
    // returns true if action is amongst the greedy actions for the specified
    // state, otherwise false - only considers the effect of the novelty factor
    public boolean isExploratoryGreedy(int state, int action)
    {  
    	getActionValuesPlusNovelty(state);
    	int best = TLO.greedyAction(thisStateValues, thresholds); 
    	// this action is greedy if it is TLO-equal to the greedily selected action
    	return (TLO.compare(thisStateValues[action], thisStateValues[best], thresholds)==0);
    }
    
    // simple eGreedy selection
    private int eGreedy(double epsilon, int state)
    {
    	if (r.nextDouble()<=epsilon)
    		return r.nextInt(numberOfActions);
    	else
    		return chooseGreedyAction(state);
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
    
    // This will use the EOVF novelty action to choose an action
    public int choosePossiblyExploratoryAction(double parameter, int state)
    {
    	return chooseGreedyExploratoryAction(state);    	
    	/*if (explorationStrategy==EGREEDY)
    		return eGreedy(parameter, state);
    	else if (explorationStrategy==SOFTMAX_TOURNAMENT)
    		return softmaxTournament(parameter, state);
    	else if (explorationStrategy==SOFTMAX_ADDITIVE_EPSILON)
    		return softmaxAdditiveEpsilon(parameter, state);
    	else
    	{
    		System.out.println("Error - undefined exploration strategy" + explorationStrategy);
    		return -1; // should cause a crash to halt proceedings
    	}*/
    	
    }
    
    // print out the Q-values, including novelty value for the specified state
    public void printQValues(int state, int action)
    {
    	for (int o=0; o<numberOfObjectives; o++)
    	{
    		System.out.print(valueFunction.get(o)[action][state]+" ");
    	}
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