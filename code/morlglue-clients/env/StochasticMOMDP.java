// Written by Peter Vamplew Sept 2020
// Implements the Stochastic MOMDP benchmark environment as described in Vamplew et al's paper on
// stochastic MOMDPs and value-based MORL
package env;

import java.util.Random;

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.taskspec.ranges.IntRange;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;
import org.rlcommunity.rlglue.codec.util.EnvironmentLoader;


public class StochasticMOMDP implements EnvironmentInterface
{  
	private int currentState; 
	private Reward rewards;
    private Random r = new Random(471);
    private double[][] STATE_1_REWARDS = {{1.5, 10},{1.7,7},{2.5,6},{3.3,5},{3.7,0}};
	
    public String env_init() 
    {
        //initialize the starting position, and an object to hold the reward
        currentState = 0;
        rewards = new Reward(0,2,0);
        //Task specification object
        TaskSpecVRLGLUE3 theTaskSpecObject = new TaskSpecVRLGLUE3();
        theTaskSpecObject.setContinuing();
        //Specify that there will be this number of observations (ie just 2 states)
        theTaskSpecObject.addDiscreteObservation(new IntRange(0, 1));              
        //Specify that there will be five actions
        theTaskSpecObject.addDiscreteAction(new IntRange(0, 4));
        //Specify that there will be this number of objectives
        theTaskSpecObject.setNumOfObjectives(2);
        //Convert specification object to a string
        String taskSpecString = theTaskSpecObject.toTaskSpec();
        TaskSpec.checkTaskSpec(taskSpecString);
        return taskSpecString;
    }
    
    // Setup the environment for the start of a new episode
    public Observation env_start() {
        currentState = 0;       
        Observation theObservation = new Observation(1, 0, 0);
        theObservation.setInt(0, currentState);
        return theObservation;
    }
    
    // Execute the specified action, update environmental state and return the reward and new observation
    public Reward_observation_terminal env_step(Action action) 
    {
        updatePositionAndReward( action.getInt(0));
        // set up new Observation
        Reward_observation_terminal RewardObs = new Reward_observation_terminal();
        Observation theObservation = new Observation(1, 0, 0);
        theObservation.setInt(0, currentState);
        RewardObs.setObservation(theObservation);
        RewardObs.setTerminal(currentState>1); // end once we have taken an action in state 1
        // setup new rewards
        RewardObs.setReward(rewards);
        return RewardObs;
    }

    public void env_cleanup() 
    {
    }

    public String env_message(String message) 
    {
        throw new UnsupportedOperationException(message + " is not supported by the LinkedRings environment.");
    }
    
    
    // update the agent's position within the environment based on the specified action, and stores the associated reward values
    public void updatePositionAndReward(int theAction) 
    {
    	if (currentState==0) // in starting state, so all actions have the same stochastic outcome
    	{
    		currentState=1;
    		if (r.nextDouble()<=0.5) // reward is (1,0)
    		{
    			rewards.setDouble(0, 1);
    			rewards.setDouble(1,0);
        	}
    		else
    		{
    			rewards.setDouble(0, 3);
    			rewards.setDouble(1,0);
        	}
        }
        //  must be in state 1, so reward depends on action and we move to a terminal state
        else
        {
        	currentState=2; // move to terminal state
        	// set rewards based on choice of action
        	rewards.setDouble(0, STATE_1_REWARDS[theAction][0]);
        	rewards.setDouble(1, STATE_1_REWARDS[theAction][1]);
        }     
    }   

    public static void main(String[] args) 
    {
        EnvironmentLoader theLoader = new EnvironmentLoader(new StochasticMOMDP());
        theLoader.run();
    }


}

