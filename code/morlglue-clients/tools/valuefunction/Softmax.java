// Implements a softmax function for use in exploration
package tools.valuefunction;
import java.util.Random;

public abstract class Softmax 
{
	//static Random random = new Random(AggregatorUtils.random.nextInt());	//comment-out: let there be only one global base / static random generator so that if that global generator is reset then this one here also becomes reset
	static Random random = AggregatorUtils.random;
	
	// Performs softmax selection. Should an error occur in the calculations as temperature gets too low, we detect this
	// and simply return the greedy action instead
	public static int getAction(double actionValues[], double temperature, int greedyAction)
	{
		int numActions = actionValues.length;
		double sumOfSoftmaxTerms[] = new double[numActions];
		sumOfSoftmaxTerms[0] = Math.exp(actionValues[0]/temperature);
		for (int a=1; a<numActions; a++)
		{
			sumOfSoftmaxTerms[a] = Math.exp(actionValues[a]/temperature) + sumOfSoftmaxTerms[a-1];
			if (Double.isInfinite(sumOfSoftmaxTerms[a]))
			{
				return greedyAction;
			}
		}
		double nextRandom = random.nextDouble();
		int selectedAction = 0;
		while((sumOfSoftmaxTerms[selectedAction]/sumOfSoftmaxTerms[numActions-1])<nextRandom)
		{
			selectedAction++;
		}
		return selectedAction;
	}
	
	// Performs tournament (ranking) based softmax - only the ordering of the actionValues is used rather than the actual values.
	// provided for compatibility with the softmax-tournament method used by my multiobjective RL methods. Should an error occur in the calculations as temperature gets too low, we detect this
	// and simply return the greedy action instead
	public static int getTournamentAction(double actionValues[], double temperature, int greedyAction)
	{
		// first calculate the ranking score for each action
		double score[] = new double[actionValues.length];
	    for (int a = 0; a < actionValues.length; a++)
	    {
	    	score[a] = 0;
	    }
	    for (int a = 0; a < actionValues.length-1; a++) 
	    {
	    	for (int b=a+1; b<actionValues.length; b++)
	    	{
	    		if (actionValues[a]>actionValues[b])
	    			score[a]++;
	    		else if (actionValues[a]<actionValues[b])
	    			score[b]++;
	    	}
		}
	    // scale to the range 0..1
	    for (int a = 0; a < actionValues.length; a++)
	    {
	    	score[a] /= (actionValues.length-1); // numActions -1 as an action is not compared against itself
	    }
	    // now do softmax selection on those scores
	    return getAction(score, temperature, greedyAction);
	}
	
}
