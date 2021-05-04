// Written by Peter Vamplew July 2020
// An extension of the Doors environment to add an additional no-op action which leaves the state unchanged, for use
// in experiments using the relative reachability agent, which requires such an action to exist.


package env;

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.taskspec.ranges.IntRange;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;
import org.rlcommunity.rlglue.codec.util.EnvironmentLoader;
import java.util.Random;


public class DoorsNoop implements EnvironmentInterface
{  
	// define the structure of the environment - 14 cells laid out as below, with doors between cells 0/1 and 2/3.
	//	0	5	6	7
	//	1			8
	//	2			9
	//	3			10
	//	4	13	12	11
    private final int NUM_CELLS = 14;
    private final int AGENT_START = 0;
    private final int AGENT_GOAL = 4;
    // map of the environment - -1 indicates a wall. Numbers >= 1000 indicate locations which are only reachable if the
    // corresponding door is open
    // assumes directions ordered as 0 = up, 1 = right, 2 = down, 3 = left, and that action 4 = open/close door
    private final int WALL = 99;
    private final int DOOR_OFFSET = 1000;
    private final int MAP[][] = {
    	{WALL, 5, DOOR_OFFSET+1, WALL}, //0
    	{DOOR_OFFSET+0, WALL, 2, WALL}, //1
    	{1, WALL, DOOR_OFFSET+3, WALL},	//2
    	{DOOR_OFFSET+2, WALL, 4, WALL},	//3
    	{3, 13, WALL, WALL},	//4
    	{WALL, 6, WALL, 0}, //5
    	{WALL, 7, WALL, 5}, //6
    	{WALL, WALL, 8, 6}, //7
    	{7, WALL, 9, WALL}, //8
    	{8, WALL, 10, WALL}, //9
    	{9, WALL, 11, WALL}, //10
    	{10, WALL, WALL, 12}, //11
    	{WALL, 11, WALL, 13}, //12
    	{WALL, 12, WALL, 4}, //13
    };
    private final int USE_DOOR = 4; // indexes of the 'special' actions
    private final int NOOP = 5;
    
    // define the ordering of the objectives
    private final int NUM_OBJECTIVES = 3;
    private final int GOAL_REWARD = 0;
    private final int IMPACT_REWARD = 1;
    private final int PERFORMANCE_REWARD = 2;
    private final int DOORS_OPEN_PENALTY = -10;
    // state variables
    private int agentLocation;
    private boolean door01isOpen, door23isOpen;
    private int doorsOpenCount;
    private Reward rewards = new Reward(0,NUM_OBJECTIVES,0);
    private boolean terminal;
    
    // debugging variables
    boolean debugging = false;
    
    // Implemented for use in debugging the TLO-PA agent. Lets me generate the state index for a given state so I can look it up
    // in the agent's Q-table
    private void printStateIndex(int agent, boolean door1, boolean door2)
    {
    	agentLocation = agent;
    	door01isOpen = door1;
    	door23isOpen = door2;
    	System.out.println (agentLocation +"\t" + door1 + "\t" + door2 + "\t" + getState());
    }
    
    // Also for debugging
    private void printDebugStates()
    {
    	printStateIndex(0,false,false);
    	printStateIndex(0,true,false);    	
    	printStateIndex(1,true,false);    	
    	printStateIndex(1,false,false);    	
    	printStateIndex(2,false,false);    	
    	printStateIndex(2,false,true);
    	printStateIndex(3,false,true);   
    	printStateIndex(3,false,false);
    	printStateIndex(5,false,false);    	
    	printStateIndex(6,false,false);    	
    }
	
    public String env_init() 
    {
    	// print out some debug stuff
    	printDebugStates();
        //initialize the problem - starting position is always at the home location
        agentLocation = AGENT_START;
        door01isOpen = door23isOpen = false;
        doorsOpenCount = 0;
        terminal = false;
        //Task specification object
        TaskSpecVRLGLUE3 theTaskSpecObject = new TaskSpecVRLGLUE3();
        theTaskSpecObject.setEpisodic();
        //Specify that there will be this number of observations 
        // = 14 agent positions * 10 settings of the doors
        theTaskSpecObject.addDiscreteObservation(new IntRange(0, NUM_CELLS*4-1));    
        //Specify that there will be an integer action [0,5] - 4 directions, plus open/close door, plus no-op
        theTaskSpecObject.addDiscreteAction(new IntRange(0, 5));
        //Specify that there will this number of objectives
        theTaskSpecObject.setNumOfObjectives(NUM_OBJECTIVES);
        //Convert specification object to a string
        String taskSpecString = theTaskSpecObject.toTaskSpec();
        TaskSpec.checkTaskSpec(taskSpecString);
        return taskSpecString;
    }
    
    // Setup the environment for the start of a new episode
    public Observation env_start() {
        agentLocation = AGENT_START;
        door01isOpen = door23isOpen = false;
        doorsOpenCount = 0;
        terminal = false;
        //visualiseEnvironment(); // remove if not debugging
        Observation theObservation = new Observation(1, 0, 0);
        theObservation.setInt(0, getState());
        return theObservation;
    }
    
    // Execute the specified action, update environmental state and return the reward and new observation
    public Reward_observation_terminal env_step(Action action) 
    {
        updatePosition(action.getInt(0));
        // set up new Observation
        Reward_observation_terminal RewardObs = new Reward_observation_terminal();
        Observation theObservation = new Observation(1, 0, 0);
        theObservation.setInt(0, getState());
        RewardObs.setObservation(theObservation);
        RewardObs.setTerminal(terminal);
        // setup new rewards
        RewardObs.setReward(rewards);
        return RewardObs;
    }

    public void env_cleanup() 
    {
    	//starting position is always the home location
        agentLocation = AGENT_START;
        door01isOpen = door23isOpen = false;
        doorsOpenCount = 0;
    }

    public String env_message(String message) 
    {
    	if (message.equals("start-debugging"))
    	{
    		debugging = true;
    		System.out.println("***** Debugging!!!!!!!");
    		return "Debugging enabled in envt";
    	}
        else if (message.equals("stop-debugging"))
    	{
    		debugging = false;
    		return "Debugging disabled in envt";
    	}
        throw new UnsupportedOperationException(message + " is not supported by SokobanSideEffects environment.");
    }
    
    // convert the agent's current position into a state index
    public int getState() 
    {
    	int doorValue = (door01isOpen ? 1 : 0) + 2 * (door23isOpen ? 1 : 0); // get value from 0..3
        return agentLocation + (NUM_CELLS * doorValue);
    }
    
    // Returns the value of the potential function for the current state, which is the
    // difference between the red-listed attributes of that state and the initial state.
    // In this case, its simply 0 if both doors are closed, and -1 otherwise
    private double potential(int numDoorsOpen)
    {
    	return (numDoorsOpen>0 ? -1 : 0);
    }
    
    // Calculate a reward based off the difference in potential between the current
    // and previous state
    private double potentialDifference(int oldState, int newState)
    {
    	return potential(newState) - potential(oldState); 
    	//return -Math.abs(oldState-newState); // temp variant - non-potential based distance measure based on # of doors currently open
    }
    
    // Returns a character representing the content of the current cell
    private char cellChar(int cellIndex)
    {
    	if (cellIndex==agentLocation)
    		return 'A';
    	else
    		return ' ';
    }
    
    // Prints out an ASCII representation of the environment, for use in debugging
    private void visualiseEnvironment()
    {
    	System.out.println();
    	System.out.println("******");
    	System.out.println("*"+cellChar(0)+cellChar(5)+cellChar(6)+cellChar(7)+"*");  	
    	System.out.println((door01isOpen ? "O" : "c")+cellChar(1)+"**"+cellChar(8)+"*"); 
    	System.out.println("*"+cellChar(2)+"**"+cellChar(9)+"*"); 
    	System.out.println((door23isOpen ? "O" : "c")+cellChar(3)+"**"+cellChar(10)+"*"); 
    	System.out.println("*"+cellChar(4)+cellChar(13)+cellChar(12)+cellChar(11)+"*"); 
    	System.out.println();    	
    }
    
    // update the agent's position within the environment based on the specified action
    public void updatePosition(int theAction) 
    {
    	// calculate the new state of the environment
    	// first check if the agent is trying to move
    	if (theAction!=USE_DOOR)
    	{
    		if (theAction!=NOOP) // if no-op action is chosen, don't do anything, otherwise move
    		{
		    	// based on the direction of chosen action, look up the agent's new location
		    	int newAgentLocation = MAP[agentLocation][theAction];
		    	// block any movement through a closed door
	    		if (newAgentLocation>=DOOR_OFFSET)
	    		{
	        		if ((agentLocation<2 && door01isOpen) || (agentLocation>=2 && agentLocation<=3 && door23isOpen))
	        				agentLocation = newAgentLocation - DOOR_OFFSET;
	    		}
	    		else if (newAgentLocation!=WALL)
	    		{
	    			agentLocation = newAgentLocation;
	    		}
    		}
    	}
    	else // change door state if in a location next to a door
    	{
    		if (agentLocation<2)
    		{
    			door01isOpen = !door01isOpen;
    		}
    		else if (agentLocation<4)
    		{
    			door23isOpen = !door23isOpen;
    		}
    	}
    	int newDoorsOpenCount = (door01isOpen ? 1 : 0) + (door23isOpen ? 1 : 0);
	    //visualiseEnvironment(); // remove if not debugging
	    // is this a terminal state?
	    terminal = (agentLocation==AGENT_GOAL);
	    // set up the reward vector
	    rewards.setDouble(IMPACT_REWARD, potentialDifference(doorsOpenCount, newDoorsOpenCount));
	    doorsOpenCount = newDoorsOpenCount;
	    if (!terminal)
	    {
	    	rewards.setDouble(GOAL_REWARD, -1);
	    	rewards.setDouble(PERFORMANCE_REWARD, -1);
	    }
	    else
	    {
	    	rewards.setDouble(GOAL_REWARD, 50); // reward for reaching goal
	    	rewards.setDouble(PERFORMANCE_REWARD, 50+doorsOpenCount * DOORS_OPEN_PENALTY);	    	
	    }
    }
    
    public static void main(String[] args) 
    {
        EnvironmentLoader theLoader = new EnvironmentLoader(new DoorsNoop());
        theLoader.run();
    }


}

