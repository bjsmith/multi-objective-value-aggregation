package agents;

import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward;
import org.rlcommunity.rlglue.codec.util.AgentLoader;

import tools.staterep.DummyStateConverter;
import tools.staterep.interfaces.StateConverter;
import tools.traces.StateActionDiscrete;
import tools.valuefunction.WSLookupTable;
import tools.valuefunction.interfaces.ActionSelector;
import tools.valuefunction.interfaces.ValueFunction;


public abstract class SteeringAgent implements AgentInterface {

    ArrayList<ValueFunction> valueFunctions = null;
    ArrayList<Stack> tracingStacks = null;

    private final boolean debugtrace = false;
    private final boolean debugTerminalOnly = false;
    
    private final double initQValues[]={0,0};
    
    private int numActions = 0;
    private boolean policyFrozen = false;
    private Random random;

    private int numStates = 0;

    double alpha = 0.9;
    double startingEpsilon = 0.2; // use .9 for DST and .2 for LinkedRings
    double epsilonLinearDecay = startingEpsilon / 5000; // set this to the inverse of the max number of episodes/time-steps for which exploration should occur
    double epsilon;
    double gamma = 0.9; // use 1.0 for DST, 0.9 for LinkedRings
    double lambda = 0.95;
    int numObjectives;
    final int MAX_STACK_SIZE = 20;

    int numPolicies = 4; 
    double [][]weights = null; // weights stored for each policy, per objective
    double [][]paretoEstimate = null; // estimated Pareto-front values per policy
    int []numPolicyVisits = null; // number of times this policy has been active, for use in updating the estimated Pareto-front values for non-episodic tasks
    

    double []totalEpisodeReward = null;
    double []goalVector = null;
    //double []originalGoalVector;
    double []averageRewards = null;
    double []targetVector;
    final double weightedAverageDecay = 0.99;
    //final int dynamicGoalStartTime = 10000; // goal will not be dynamically modified before this many time-steps have occurred - this is to allow the average reward to stabilise 
    final int actualAverageTime = 100; // number of steps/episodes to use actual average rather than exponential weighted ave
    int numOfSteps = 0;
    int numEpisodes = 0;

    int bestPolicy = 0;
    int bestAction = 0;

    StateConverter stateConverter = null;

    /* --------------------------------------------------------------------------------------------------------------------------------
	This section defines the code which is common to all steering agents, whether they are episodic or non-episodic 
	or using Q or W steering
    ----------------------------------------------------------------------------------------------------------------------------------*/
    
    @Override
    public void agent_init(String taskSpecification) 
    {
        TaskSpecVRLGLUE3 theTaskSpec = new TaskSpecVRLGLUE3(taskSpecification);

        numActions = theTaskSpec.getDiscreteActionRange(0).getMax() + 1;
        numStates = theTaskSpec.getDiscreteObservationRange(0).getMax()+1;
        numObjectives = theTaskSpec.getNumOfObjectives();

        // ** TO DO ** still need to figure out a general way of setting these - for the moment they only work for 2 objectives
        weights = new double[numPolicies][numObjectives];
        double smallAngle = 0.01;
        double degreeRange = Math.PI/2-2 * smallAngle;
        double degreeChange = degreeRange/(numPolicies-1);
        double angle = smallAngle;
        for (int i=0; i<numPolicies; i++)
        {
        	weights[i][0]=Math.sin(angle);
        	weights[i][1]=Math.cos(angle);
        	angle += degreeChange;
        }
        // set up data-structures for storing estimates of Pareto front points
        paretoEstimate = new double[numPolicies][numObjectives];
        for (int i=0; i<numPolicies; i++)
        {
        	for (int j=0; j<numObjectives; j++)
        	{
        		paretoEstimate[i][j] = 0.0;
        	}
        }
        numPolicyVisits = new int[numPolicies];
        for (int i=0; i<numPolicies; i++)
        {
        	numPolicyVisits[i] = 0;
        }       

        valueFunctions = new ArrayList<>();
        tracingStacks = new ArrayList<>();

        for(int i = 0 ; i < numPolicies ; i++) {
            ValueFunction vf = new WSLookupTable( numObjectives, numActions, numStates, 0, weights[i] );
            valueFunctions.add( vf );
            tracingStacks.add( new Stack<StateActionDiscrete>() );
        }

        random = new Random();

        //set the model of converting MDP observation to an int state representation
        stateConverter = new DummyStateConverter();
        StateActionDiscrete.setStateConverter( stateConverter );

        totalEpisodeReward = new double[numObjectives];
        goalVector = new double[numObjectives];
        //originalGoalVector = new double[numObjectives];  
        averageRewards = new double[numObjectives];
        targetVector = new double[numObjectives];
        resetForNewTrial();
    }
    
    private void resetForNewTrial()
    {
    	// reset all measures of rewards, goals etc.
        for(int i = 0; i < numObjectives; i++) {
            totalEpisodeReward[i] = 0.0d;
            averageRewards[i] = 0.0d;
            //goalVector[i] = originalGoalVector[i]; // required if we are using the dynamic goal variant of Q-steering
        }
        numOfSteps = 0;
        numEpisodes = 0;  
        epsilon = startingEpsilon;
        // reset Q-values
        for (int i = 0; i < numPolicies; i++) {
            valueFunctions.get(i).resetQValues(initQValues);
           	for (int j=0; j<numObjectives; j++)
        	{
        		paretoEstimate[i][j] = 0.0;
        	}
        	numPolicyVisits[i] = 0;          	
        } 
    }

    @Override
    public Action agent_start(Observation observation) 
    {
    	//System.out.println("Starting episode " + numEpisodes);
        //clear all traces, reset counter of steps in this episode, reset cumulative reward for this episode
        for(int i=0;i<numPolicies;i++) {
            tracingStacks.get(i).clear();
        }
        numOfSteps = 0;
        for(int i = 0; i < numObjectives; i++) {
            totalEpisodeReward[i] = 0.0;
        } 
        int state = stateConverter.getStateNumber(observation);
        
        // choose random base policy for first time-step, otherwise use the greedy policy which will have already 
        // been determined in agent_end (note: the latter will only happen for episodic tasks)
    	if (numEpisodes==0)
    	{
    		bestPolicy = random.nextInt(numPolicies);
    	}
    	else
    	{
        	// do anything here which is specific to a particular version of steering
        	agent_start_internal(state);
    	}
    	// select an action based on the best policy, and add this to the traces for all policies
        int action = getAction(state, bestPolicy);        
        Action returnAction = new Action(1, 0, 0);
        returnAction.intArray[0] = action;
        StateActionDiscrete sa = new StateActionDiscrete(observation, returnAction);
    	for(int i = 0 ; i < numPolicies ; i++) {
    		tracingStacks.get(i).add(sa);
        }
        return returnAction;
    }

    @Override
    public Action agent_step(Reward reward, Observation observation) 
    {
    	//System.out.print("*\t" + reward.getDouble(0) +"\t" + reward.getDouble(1) +"\t\t");
        numOfSteps++;
        // observe new state, and perform TD updates
        int newState = stateConverter.getStateNumber( observation );
        if (debugtrace) System.out.println("** Moved to state " + newState);
        if (!policyFrozen) {
            for (int i = 0; i < numPolicies; i++) {
                ValueFunction valueFunction = valueFunctions.get(i);

                Stack<StateActionDiscrete> tracingStack = tracingStacks.get(i);
                int greedyAction;

                double currentLambda = lambda;
                for (int j = tracingStack.size() - 1; j >= 0; j--) 
                {
                    //next state action pair in eligibility trace
                    StateActionDiscrete pair = tracingStack.get(j);
                    greedyAction = ((ActionSelector) valueFunction).chooseGreedyAction(newState);
                    int prevAction = pair.getAction().getInt(0);
                    int prevState = stateConverter.getStateNumber(pair.getObservation());
                    if (debugtrace) System.out.println("\tStack level " + j + " State: " + prevState + " Action: " + prevAction);
                    //update first eligibility trace
                    if (j + 1 == tracingStack.size()) {
                        valueFunction.calculateErrors( prevAction, prevState, greedyAction, newState, gamma, reward );
                        //always update first trace with last action
                        valueFunction.update( prevAction, prevState, 1.0, alpha );
                    } 
                    else 
                    {	//update rest of eligibility traces
                        int index = tracingStack.indexOf(pair, j + 1);
                        if (index == -1) {
                            valueFunction.update( prevAction, prevState, currentLambda, alpha );
                        }
                        currentLambda *= lambda;
                    }
                }
            }
        }
        // do whatever details are required for this particular version of steering
        agent_step_internal(reward, newState);
    	// select an action based on the best policy
        int action = getAction(newState, bestPolicy);
        Action returnAction = new Action(1, 0, 0);
        returnAction.intArray[0] = action;
        StateActionDiscrete sa = new StateActionDiscrete(observation, returnAction);
        // update all stacks - if the selected action is not greedy for this policy then clear traces first
    	for (int i = 0 ; i < numPolicies ; i++) 
    	{
    		Stack<StateActionDiscrete> tracingStack = tracingStacks.get(i);
            if (isGreedy(i,newState,action))
            {
    	        if( tracingStack.size() == MAX_STACK_SIZE) 
    	        {
    	            tracingStack.remove(0);
    	        }
            }
            else
            {
            	tracingStack.clear();
            }   		
    		tracingStack.add(sa);
        }
        return returnAction;
    }
    
    private void calculateTargetVectorBasic()
    {
    	//System.out.print("Ave reward\t");
    	//for(int i = 0; i < numObjectives; i++)
    		//System.out.print(averageRewards[i]+"\t");
    	//System.out.print("Target\t");
        for(int i = 0; i < numObjectives; i++) 
        {
            targetVector[i] = goalVector[i] - averageRewards[i];
    		//System.out.print(targetVector[i]+"\t");
        }
    }

    // commented out for now - we will probably eventually remove this altogether
    /*private void calculateTargetVectorDynamicGoal()
    {
        // move goal if required to keep ave reward within the dominated quadrant - don't do this before the indicated start time
    	// to allow time for the exponentially weighted average to stabilise
    	if (numOfSteps>dynamicGoalStartTime)
    	{
    		double delta = 0;
	    	for(int i = 0; i < numObjectives; i++) 
	        {
	            if(averageRewards[i] - goalVector[i]>delta)
	            {
	            	delta = averageRewards[i] - goalVector[i];
	            }
	        }
	    	if (delta>0)
	    	{
		    	for(int i = 0; i < numObjectives; i++) 
		        {	
		    		goalVector[i] += 5 * delta;
		        }
	    	}
    	}
        // now calculate target vector
        for(int i = 0; i < numObjectives; i++) 
        {
            targetVector[i] = goalVector[i] - averageRewards[i];
        }
        System.out.print(goalVector[0] + "\t" + goalVector[1] +"\t" + averageRewards[0] +"\t" + averageRewards[1] +"\t" + targetVector[0] +"\t" + targetVector[1] + "\t\t");
    }*/
    



    @Override
    public void agent_cleanup() {
        valueFunctions = null;
        policyFrozen = false;
    }

    private int getAction(int state, int policy) 
    {
        ActionSelector valueFunction = (ActionSelector) valueFunctions.get(policy);
        int action;
        if (!policyFrozen) {
            if (random.nextDouble() < epsilon) {
                action = random.nextInt(numActions);
            } else {
                action = valueFunction.chooseGreedyAction(state);
            }
        } 
        else 
        {
            action = valueFunction.chooseGreedyAction(state);
        }
        return action;
    }
    
    private int getGreedyAction(int state, int policy)
    {
        ActionSelector valueFunction = (ActionSelector) valueFunctions.get(policy);  
        return valueFunction.chooseGreedyAction(state);
    }
    
    // returns true if the specified action is amongst the greedy actions for the 
    // specified state and policy, false otherwise
    private boolean isGreedy(int policy, int state, int action)
    {
        ActionSelector valueFunction = (ActionSelector) valueFunctions.get(policy);
        return valueFunction.isGreedy(state,action);  	
    }

    @Override
    public String agent_message(String message) {
        if (message.equals("freeze learning")) {
            policyFrozen = true;
            System.out.println("Learning has been freezed");
            return "message understood, policy frozen";
        }
        if (message.equals("unfreeze learning")) {
            policyFrozen = false;
            System.out.println("Learning has been unfreezed");
            return "message understood, policy unfrozen";
        }

        if (message.equals("get_best_policy")) {
            return Integer.toString(bestPolicy);
        }
        if (message.equals("get_average_reward0")) {
            return Double.toString(averageRewards[0]);
        }
        if (message.equals("get_average_reward1")) {
            return Double.toString(averageRewards[1]);
        }
        if (message.equals("start_new_trial")){
        	resetForNewTrial();
            //System.out.println("New trial started: Q-values and other variables reset");
            return "New trial started: Q-values and other variables reset";
        }


        if (message.startsWith("save_value_function")) {
            String[] parts = message.split(" ");
            for(int i = 0 ; i < numPolicies ; i++) {
                WSLookupTable valueFunction = (WSLookupTable)valueFunctions.get(i);
                valueFunction.saveValueFunction( parts[1] + "_Policy" + i );
            }
            return "Value functions are saved";
        }

        if (message.startsWith("change_target_point")) {

            String[] parts = message.split(" ");

            goalVector[0] = Double.valueOf(parts[1]).doubleValue();
            goalVector[1] = Double.valueOf(parts[2]).doubleValue();
            System.out.println("Target point has been changed to (" + goalVector[0] + "," + goalVector[1] + ")");
            return "message understood, weights are changed";
        }

        return "The steering agent does not understand your message.";

    }
    
    private void updateSteeringVectors(int numMeasurements, double latestRewards[])
    {
    	// update calculation of averageReward, calculate target vector and choose best policy
        // We initially use an actual average for a small number of time-steps to establish a reasonable base-line, then use this as
        // an initial value for an exponentially weighted average
    	//System.out.print("numMeasurements\t"+numMeasurements+"\treward");
    	//for (int i=0; i<numObjectives; i++)
    		//System.out.print(latestRewards[i]+"\t");
		if (numMeasurements<actualAverageTime)
		{
			for(int i = 0; i < numObjectives; i++)
	        {
	        	averageRewards[i] = (averageRewards[i] * (numMeasurements-1) + latestRewards[i]) / numMeasurements; 
	        }
		}
		else
		{
			// calculate exponentially weighted moving average
	        for(int i = 0; i < numObjectives; i++)
	        {
	        	averageRewards[i] = averageRewards[i] * weightedAverageDecay + latestRewards[i] * (1-weightedAverageDecay);
	        }    			
		}
        calculateTargetVectorBasic();
		//calculateTargetVectorDynamicGoal();         	
    }
    
    /* --------------------------------------------------------------------------------------------------------------------------------
	This section defines the code which varies between steering agents. Each concrete class based on SteeringAgent should over-ride the
	abstract methods below to contain a call to the appropriate variant of that method
    ----------------------------------------------------------------------------------------------------------------------------------*/
 
    // over-ride to call either agent_start_internal_episodic or agent_start_internal_non_episodic
    protected abstract void agent_start_internal(int state); 
    
    // over-ride to call either agent_step_internal_episodic or agent_step_internal_non_episodic
    protected abstract void agent_step_internal(Reward reward, int newState); 
    
    // over-ride to call either agent_end_episodic or agent_end_non_episodic
    public abstract void agent_end(Reward reward);
    
    //over-ride to call either chooseGreedyPolicy_Q or chooseGreedyPolicy_w
    protected abstract int chooseGreedyPolicy(double[] targetVector, int state);
 
    // updates estimates of Pareto front value for each policy
    protected void agent_start_internal_episodic(int state)
    {
    	for (int i=0; i<numPolicies; i++)
    	{
    		// find the best action for this policy, so we can use its Q-values in updating the estimate
    		int action = getGreedyAction(state, i);
            ValueFunction valueFunction = valueFunctions.get(i);
    		for (int j=0; j<numObjectives; j++)
    		{
    			if (numEpisodes<actualAverageTime) // use cumulative average during early episodes
    			{
    				paretoEstimate[i][j] = (paretoEstimate[i][j] * (numEpisodes-1) + valueFunction.getQValues(action,state)[j]) / numEpisodes; 
    			}
    			else // then exponentially weighted average after that
    			{
    				paretoEstimate[i][j] = paretoEstimate[i][j] * weightedAverageDecay + valueFunction.getQValues(action,state)[j] * (1-weightedAverageDecay);   			
    			}
    		}
    	}	
    	/*System.out.print("Pareto\t");
    	// print out the current estimates
    	for (int i=0; i<numPolicies; i++)
    	{
    		for (int j=0; j<numObjectives; j++)
    		{
    			System.out.print(paretoEstimate[i][j] + "\t");
    		}
    	}
    	System.out.println();*/
    	// choose the best policy for this episode
	    bestPolicy = chooseGreedyPolicy(targetVector,state);
    }
 
    // no need to do anything specific in agent_start for non_episodic tasks
    protected void agent_start_internal_non_episodic(int state)
    {
    	
    }
    
    protected void agent_step_internal_episodic(Reward reward)
    {  
        for(int i = 0; i < numObjectives; i++) {
            totalEpisodeReward[i] += reward.getDouble(i);
        }    
    }
    
    protected void agent_step_internal_non_episodic(Reward reward, int newState)
    {
        epsilon -= epsilonLinearDecay; // for non-episodic problems we decay the epsilon on every time-step
    	double rewards[] = new double[numObjectives];
		for(int i = 0; i < numObjectives; i++)
        {
			rewards[i] = reward.getDouble(i);
			System.out.print(rewards[i]+"\t");
        }
		System.out.println();
    	updateSteeringVectors(numOfSteps, rewards);
    	bestPolicy = chooseGreedyPolicy(targetVector, newState);
    	// update estimated Pareto value for the current policy
    	numPolicyVisits[bestPolicy]++;
    	// find the best action for this policy, so we can use its Q-values in updating the estimate
    	int action = getGreedyAction(newState, bestPolicy);
        ValueFunction valueFunction = valueFunctions.get(bestPolicy);
    	for (int j=0; j<numObjectives; j++)
    	{
    		// convert the Q-value estimate of discounted future reward to an estimate of average per-step reward by adjusting for gamma
    		double adjustedValue = valueFunction.getQValues(action,newState)[j] * (1-gamma);
    		if (numPolicyVisits[bestPolicy]<actualAverageTime) // use cumulative average during early episodes
    		{
    			paretoEstimate[bestPolicy][j] = (paretoEstimate[bestPolicy][j] * (numPolicyVisits[bestPolicy]-1) + adjustedValue) / numPolicyVisits[bestPolicy]; 
    		}
    		else // then exponentially weighted average after that
    		{
    			paretoEstimate[bestPolicy][j] = paretoEstimate[bestPolicy][j] * weightedAverageDecay + adjustedValue * (1-weightedAverageDecay);   			
    		}
    	}
    	/*System.out.print("Pareto\t" + bestPolicy + "\t");
    	// print out the current estimates
    	for (int i=0; i<numPolicies; i++)
    	{
    		for (int j=0; j<numObjectives; j++)
    		{
    			System.out.print(paretoEstimate[i][j] + "\t");
    		}
    	}*/
    }
    
    // no need for agent_end_non_episodic as it is never called for non-episodic tasks
    protected void agent_end_non_episodic() 
    {
    	System.out.println("End-NE");
    }
    
    // update state-action values based on the reward received on reaching a terminal state
    // update all the steering vectors based on the reward for this episode - these will be used to choose
    // the greedy base policy when the next episode starts
    protected void agent_end_episodic(Reward reward) 
    {
  	  	numOfSteps++;
        numEpisodes++;
        epsilon -= epsilonLinearDecay;
        for(int i = 0; i < numObjectives; i++) {
            totalEpisodeReward[i] += reward.getDouble(i);
        }  
        // update state-action values
        if (!policyFrozen) {
            for (int i = 0; i < numPolicies; i++) {
                ValueFunction valueFunction = valueFunctions.get(i);

                Stack<StateActionDiscrete> tracingStack = tracingStacks.get(i);

                double currentLambda = lambda;
                for (int j = tracingStack.size() - 1; j >= 0; j--) {
                    //next state action pair in eligibility trace
                    StateActionDiscrete pair = tracingStack.get(j);

                    int prevAction = pair.getAction().getInt(0);
                    int prevState = stateConverter.getStateNumber(pair.getObservation());
                    
                    //update first eligibility trace
                    if (j + 1 == tracingStack.size()) {
                        valueFunction.calculateTerminalErrors( prevAction, prevState, gamma, reward );
                        valueFunction.update( prevAction, prevState, 1.0, alpha );
                    } else 
                    {
                  	  //update rest of eligibility traces
                        int index = tracingStack.indexOf(pair, j + 1);
                        if (index == -1) {
                            valueFunction.update( prevAction, prevState, currentLambda, alpha );
                        }
                        currentLambda *= lambda;
                    }
                }
            }
        } 
        //update average-reward and target vectors
    	updateSteeringVectors(numEpisodes, totalEpisodeReward);
    	//System.out.println(); 
    }

    // select the best policy using Q-steering's policy metric (dot product of Q-value and steering vector)
    protected int chooseGreedyPolicy_Q(double[] targetVector, int state) {

        //System.out.print("State\t" + state + "\t");
    	//System.out.print(" Ave reward =\t" + averageRewards[0] + "," + averageRewards[1] + "\tTarget =\t" + targetVector[0] + "," + targetVector[1] + "\t");
        int bestPolicy;
        ArrayList<Integer> bestPolicies = new ArrayList<>();
        double bestDotProduct = -Double.MAX_VALUE;
        for (int i = 0; i < numPolicies; i++) {
            ValueFunction valueFunction = valueFunctions.get(i);
            // debugging only - delete once fixed
            	/*for (int j=0; j<numActions; j++)
            	{
            		for (int k=0; k<numObjectives; k++)
            		{
            			System.out.print(valueFunction.getQValues(j,state)[k] + "\t");
            		}
            	}*/
            int selectedAction = ( (ActionSelector)valueFunction ).chooseGreedyAction(state);
            //System.out.print("a*\t" + selectedAction + "\t");
            //obtain a vector of expected future rewards
            double[] futureRewards = valueFunction.getQValues( selectedAction , state );

            //calculate dot product
            double dotProduct = 0.0;
            for(int j=0 ; j<targetVector.length; j++) {
                dotProduct += targetVector[j] * futureRewards[j];
                //System.out.print(futureRewards[j]+"\t");
            }
            //System.out.print(dotProduct+"\t\t");
            if ( dotProduct >= bestDotProduct ) {
                if ( dotProduct > bestDotProduct ) {
                    bestPolicies.clear();
                    bestPolicies.add(i);
                    bestDotProduct = dotProduct;
                } else {
                    bestPolicies.add(i);
                }
            }
        }
        if (bestPolicies.size() > 1) {
            bestPolicy =  bestPolicies.get(random.nextInt(bestPolicies.size()));
        } else {
            bestPolicy =  bestPolicies.get(0);
        }
        //System.out.print(bestPolicy);
        return bestPolicy;
    }
    
    // select the best policy using w-steering's policy metric (distance between policy weights and steering vector)
    protected int chooseGreedyPolicy_w(double[] targetVector, int state) {

        int bestPolicy;
        ArrayList<Integer> bestPolicies = new ArrayList<>();
        double bestDistanceSquared = Double.MAX_VALUE;
    	//System.out.println(" Ave reward = " + averageRewards[0] + "," + averageRewards[1] + " Target = " + targetVector[0] + "," + targetVector[1] + ",");
    	// for each policy, find the distance between it and the targetVector - choose the policy with minimum distance
    	for (int i = 0; i < numPolicies; i++) 
        {
            double distanceSquared = 0.0;
            for(int j=0 ; j<targetVector.length; j++) 
            {
            	double difference = targetVector[j]- weights[i][j];
            	distanceSquared += difference * difference;
            }
            if (distanceSquared <= bestDistanceSquared) {
                if (distanceSquared < bestDistanceSquared ) {
                    bestPolicies.clear();
                    bestPolicies.add(i);
                    bestDistanceSquared = distanceSquared;
                } 
                else 
                {
                    bestPolicies.add(i);
                }
            }
        }
        if (bestPolicies.size() > 1) {
            bestPolicy =  bestPolicies.get(random.nextInt(bestPolicies.size()));
        } else 
        {
            bestPolicy =  bestPolicies.get(0);
        }
        //System.out.println(" Best policy = " + bestPolicy);
        return bestPolicy;
    } 

}
