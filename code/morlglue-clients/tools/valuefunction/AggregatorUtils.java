package tools.valuefunction;

import java.util.ArrayList;
import java.util.Random;


// Implement util functions for aggregating multiple q values with generic aggregators
public abstract class AggregatorUtils {

		
	static Random r = new Random();
	
	// Compare two sets of values based on a specified aggregation function
	public static int compare(double a[], double b[], Aggregator agg)
	{
		
		double[] aggregation = agg.apply(a, b);
		
		if(aggregation[0] > aggregation[1]) {
			return 1;
		} else if(aggregation[0] < aggregation[1])  {
			return -1;
		}
		// TODO maybe throw a coin instead?
		// if we get here the two arrays must be exactly equal so return 0
		return 0;
	}
	
	// Returns the index of the highest-ranked action in the provided array
	public static int greedyAction(double actionValues[][], Aggregator agg)
	{
        ArrayList<Integer> bestActions = new ArrayList<>();        
        bestActions.add(0);

        for (int a = 1; a < actionValues.length; a++) 
        {
        	int compareResult = compare(actionValues[a], actionValues[bestActions.get(0)], agg);
            if (compareResult>0) 
            {
            	bestActions.clear();
            	bestActions.add(a);
            } 
            else if (compareResult==0)
            {
                bestActions.add(a);
            }            
        }
        /*for (int i=0; i<bestActions.size(); i++)
        {
        	System.out.print(bestActions.get(i) + " ");
        }
        System.out.println();*/
        if (bestActions.size() > 1) 
        {
            return bestActions.get(r.nextInt(bestActions.size()));
        } 
        else 
        {
            return bestActions.get(0);
        }		
	}
	
	// Returns a score array with the dominance score of each action (ie the proportion of actions which this action is 
	// equal to our better than according to TLO comparisons)
	public static double[] getDominanceScore(double actionValues[][], Aggregator agg)
	{
		double score[] = new double[actionValues.length];
        for (int a = 0; a < actionValues.length; a++)
        {
        	score[a] = 0;
        }
        for (int a = 0; a < actionValues.length-1; a++) 
        	for (int b=a+1; b<actionValues.length; b++)
        	{
        		int compareResult = compare(actionValues[a], actionValues[b], agg);	
        		if (compareResult>=0)
        			score[a]++;
        		if (compareResult<=0)
        			score[b]++;
        	}
        // scale to the range 0..1
        for (int a = 0; a < actionValues.length; a++)
        {
        	score[a] /= (actionValues.length-1); // numActions -1 as an action is not compared against itself
        }
        return score;
	}
	
	// Returns a score array with the inverse additive-epsilon score for each action (ie 1 - the maximum difference on any objective
	// between this action and the TLO-optimal action)
	public static double[] getInverseAdditiveEpsilonScore(double actionValues[][], int bestIndex)
	{
		int numObjectives = actionValues[0].length;
		double score[] = new double[actionValues.length];
		// first scale the values, so one objective with a wide range can't dominate the results
		double min[] = new double[numObjectives];
		double max[] = new double[numObjectives];
		for (int i=0; i<min.length; i++)
		{
			double tempMin = actionValues[0][i];
			double tempMax = tempMin;
	        for (int a = 1; a < actionValues.length; a++)
	        {
	        	if (actionValues[a][i]<tempMin)
	        		tempMin = actionValues[a][i];
	        	else if (actionValues[a][i]>tempMax)
	        		tempMax = actionValues[a][i];
	        }  
	        min[i] = tempMin;
	        max[i] = tempMax;
		}
        double scaledBest[] = new double[numObjectives];
        for (int i=0; i<scaledBest.length; i++)
        {
        	scaledBest[i] = (actionValues[bestIndex][i]-min[i])/(max[i]-min[i]);
        }
        // now calculate the additive epsilon for each action, scaling as we go
        // finally subtract the additive epsilon from 1 so better solutions get higher scores
        for (int a = 0; a < actionValues.length; a++)
        {
        	score[a] = 0.0;
        	for (int i=0; i<numObjectives; i++)
        	{
        		double diff = scaledBest[i] - (actionValues[a][i]-min[i])/(max[i]-min[i]);
        		if (diff>score[a])
        			score[a] = diff;
        	}
        	score[a] = 1.0 - score[a];
        }
        return score;        	
	}

}