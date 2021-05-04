// A modification of the SideEffectSingleObjectiveAgent code to implement a simple version of the
// relative reachability agent of Krakovna et al. IMPORTANT - this can only be used with environments which provide
// a no-op action (these all have Noop appended to the end of their name).

package agents;

import java.math.BigDecimal;
import java.util.Random;
import java.util.Stack;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward;
import org.rlcommunity.rlglue.codec.util.AgentLoader;

import tools.hypervolume.Point;
import tools.staterep.DummyStateConverter;
import tools.staterep.interfaces.StateConverter;
import tools.traces.StateActionDiscrete;
import tools.valuefunction.TLO_LookupTable;
import tools.valuefunction.WSLookupTable;
import tools.valuefunction.interfaces.ActionSelector;
import tools.valuefunction.interfaces.ValueFunction;


public class LearningRelativeReachabilityAgent implements AgentInterface {

    WSLookupTable vf = null;
    Stack<StateActionDiscrete> tracingStack = null;

    private boolean policyFrozen = false;
    private Random random;

    private int numActions = 0;
    private int numStates = 0;
    int numOfObjectives;

    private final double initQValues[]={0,0,0};
    double alpha;
    double gamma;
    double lambda;
    int explorationStrategy; // flag used to indicate which type of exploration strategy is being used
    //if using eGreedy exploration
    double startingEpsilon;
    double epsilonLinearDecay;
    double epsilon;
    // if using softmax selection
    double startingTemperature;
    double temperatureDecayRatio;
    double temperature;
    final int MAX_STACK_SIZE = 20;

    int numOfSteps;
    int numEpisodes;
    
    // extra data structures, variables and parameters needed for relative reachability
    // for now the rr parameters have just been hardwired into this code
    double beta = 100;				// weight used when combining the primary and RR reward terms
    double gammaReachability = 0.99;	// discounting term applied to reachability
    double reachability[][]; 	// state x state - measure of discounted reachability of the second state from the first state
    int noopTransitionCount[][]; 		// state x state - how often taking a_noop has transitioned from first state to second state
    int noopVisits[];			// for each state, how many times no-op has been selected in that state
    int noopIndex; 				// the index of the no-op action - will be the last action provided by the environment
    Random noopSampler = new Random();
    

    StateConverter stateConverter = null;

    @Override
    public void agent_init(String taskSpecification) {
    	System.out.println("LearningRelativeReachabilityAgent launched");
        TaskSpecVRLGLUE3 theTaskSpec = new TaskSpecVRLGLUE3(taskSpecification);

        numActions = theTaskSpec.getDiscreteActionRange(0).getMax() + 1;
        numStates = theTaskSpec.getDiscreteObservationRange(0).getMax()+1;
        numOfObjectives = theTaskSpec.getNumOfObjectives();

        double[] weights = new double[numOfObjectives];
        // force all zero weights, except for the first objective - we will be merging the actual primary objective and
        // the RR-based shaping reward into this objective within the agent code
        weights[0]=1;
        for (int i=1; i<numOfObjectives; i++)
        	weights[i]=0;
        vf = new WSLookupTable( numOfObjectives, numActions, numStates, 0, weights );

        random = new Random();
        tracingStack = new Stack<>();
        // set up relative reachability data structures and variables
        noopIndex = numActions - 1;
        reachability = new double[numStates][numStates];
        noopTransitionCount = new int[numStates][numStates];
        noopVisits = new int[numStates];

        //set the model of converting MDP observation to an int state representation
        stateConverter = new DummyStateConverter();
        StateActionDiscrete.setStateConverter( stateConverter );
        resetForNewTrial();
    }
    
    private void resetForNewTrial()
    {
    	policyFrozen = false;
        numOfSteps = 0;
        numEpisodes = 0;  
        epsilon = startingEpsilon;
        temperature = startingTemperature;
        // initialise the relative reachability data structures
        for (int x=0; x<numStates; x++)
        {
        	noopVisits[x]=0;
        	for (int y=0; y<numStates; y++)
        	{
        		noopTransitionCount[x][y] = 0;
        		reachability[x][y] = (x==y ? 1 : 0);
        	}
        }
        // reset Q-values
        vf.resetQValues(initQValues);     
    }

    @Override
    public Action agent_start(Observation observation) {
    	//System.out.println("Starting episode " + numEpisodes + " Epsilon = " + epsilon);
        tracingStack.clear();
        int state = stateConverter.getStateNumber( observation );
        int action = getAction(state);

        Action returnAction = new Action(1, 0, 0);
        returnAction.intArray[0] = action;
        tracingStack.add(new StateActionDiscrete(observation, returnAction)); // put executed action on the stack

        return returnAction;

    }
    
    // use a 'shortest path' (actually highest value) algo to update reachability based on the most recent transition
    private void updateReachabilityModel(int beforeState, int afterState)
    {
    	if (beforeState!=afterState)
    	{
    		reachability[beforeState][afterState]=gammaReachability; // we now know we can get from beforeState to afterState in one step (assumes deterministic transitions)
    	}
    	for (int x=0; x<numStates; x++)
    	{
        	for (int y=0; y<numStates; y++)
        	{		
        		reachability[x][y] = Math.max(reachability[x][y], reachability[x][beforeState] * gammaReachability * reachability[afterState][y]); 
    		}	
    	}
    }
    
    // Calculate the relative reachability between the baseline state and the state being evaluated
    private double relativeReachability(int baselineState, int evalState)
    {
    	double drr = 0.0;
    	for (int x=0; x<numStates; x++)
    	{
    		drr += Math.max(0, reachability[baselineState][x] - reachability[evalState][x]);
    	}
    	return drr/numStates;
    }
    
    // Returns the state produced by executing no-op from the currentState, based off the count of previously observed state transitions
    private int sampleNoopState(int currentState)
    {
    	if (noopVisits[currentState]==0) // no prior data so use a uniform random distribution
    	{
    		return noopSampler.nextInt(numStates);
    	}
    	else
    	{
    		int index = 0;
    		int sum = noopTransitionCount[currentState][0];
    		int randSelection = noopSampler.nextInt(noopVisits[currentState])+1; // 1..number of occurrences of this state
    		while (sum<randSelection)
    		{
    			index++;
    			sum += noopTransitionCount[currentState][index];
    		}
    		return index;
    	}
    }
    

    @Override
    public Action agent_step(Reward reward, Observation observation) 
    {
        numOfSteps++;

        int state = stateConverter.getStateNumber( observation );
        int action;
        int greedyAction = ((ActionSelector)vf).chooseGreedyAction(state);

        if (!policyFrozen) {
            double currentLambda = lambda;
            for (int i = tracingStack.size() - 1; i >= 0; i--) {
                StateActionDiscrete pair = tracingStack.get(i);

                int prevAction = pair.getAction().getInt(0);
                int prevState = stateConverter.getStateNumber(pair.getObservation());

                if (i + 1 == tracingStack.size()) // this is the most recent action so all the updates happen here
                {
                	updateReachabilityModel(prevState, state);
                	if (prevAction == noopIndex) // update no-op related counts
                	{
                		noopTransitionCount[prevState][state]++;
                		noopVisits[prevState]++;
                	}
                	else // if the most recent action wasn't a no-op, then calc relative reachability and use that to shape the reward
                	{
                		int noopAfterState = sampleNoopState(prevState);
                		double drr = relativeReachability(noopAfterState, state);
                		reward.setDouble(0, reward.getDouble(0)-beta*drr); // subtract the scaled rr penalty off the primary reward
                	}       	
                    vf.calculateErrors(prevAction, prevState, greedyAction, state, gamma, reward);
                    vf.update(prevAction, prevState, 1.0, alpha);
                } 
                else {
                	// if there is no more recent entry for this state-action pair then update it
                	// this is to implement replacing rather than accumulating traces
                    int index = tracingStack.indexOf(pair, i + 1);
                    if (index == -1) {
                        vf.update(prevAction, prevState, currentLambda, alpha);
                    }
                    currentLambda *= lambda;
                }
            }
            action = getAction(state);
        } else {// if frozen, don't learn and follow greedy policy
            action = greedyAction;
        }

        Action returnAction = new Action(1, 0, 0);
        returnAction.intArray[0] = action;
        // clear trace if this action is not greedy, otherwise trim stack if neccesary
        if (isGreedy(state,action))
        {
	        if( tracingStack.size() == MAX_STACK_SIZE ) 
	        {
	            tracingStack.remove(0);
	        }
        }
        else
        {
        	tracingStack.clear();
        }
        // in either case, can now add this state-action to the trace stack
        tracingStack.add( new StateActionDiscrete(observation, returnAction ) );

        return returnAction;
    }

    @Override
    // Note - there are no reachability calculations included here. As Glue doesn't provide any state information when we reach a terminal,
    // we can't actually do any updating of the reachability matrix or noop model. It shouldn't matter as the shaping reward isn't really
    // relevant on these terminal transitions anyway
    public void agent_end(Reward reward) 
    {
  	  	numOfSteps++;
  	  	numEpisodes++;
  	  	epsilon -= epsilonLinearDecay;
  	  	temperature *= temperatureDecayRatio;
        if (!policyFrozen) {
            double currentLambda = lambda;
            for (int i = tracingStack.size() - 1; i >= 0; i--) {
                StateActionDiscrete pair = tracingStack.get(i);

                int prevAction = pair.getAction().getInt(0);
                int prevState = stateConverter.getStateNumber(pair.getObservation());

                if (i + 1 == tracingStack.size()) 
                {
                    vf.calculateTerminalErrors(prevAction, prevState, gamma, reward);
                    vf.update(prevAction, prevState, 1.0, alpha);
                } 
                else 
                {
                	// if there is no more recent entry for this state-action pair then update it
                	// this is to implement replacing rather than accumulating traces
                    int index = tracingStack.indexOf(pair, i + 1);
                    if (index == -1) {
                        vf.update(prevAction, prevState, currentLambda, alpha);
                    }
                    currentLambda *= lambda;
                }
            }
        }
    }

    @Override
    public void agent_cleanup() {
        vf = null;
        policyFrozen = false;
    }

  
    private int getAction(int state) {
        ActionSelector valueFunction = (ActionSelector) vf;
        int action;
        if (!policyFrozen)
        {
        	switch (explorationStrategy)
        	{
	        	case TLO_LookupTable.EGREEDY: 
	        		action = valueFunction.choosePossiblyExploratoryAction(epsilon, state); 
	        		break;
	        	case TLO_LookupTable.SOFTMAX_TOURNAMENT: 
	        		
	        	case TLO_LookupTable.SOFTMAX_ADDITIVE_EPSILON : 
	        		action = valueFunction.choosePossiblyExploratoryAction(temperature, state);
	        		break;
	        	default:
	        		action = -1; // this should never happen - if it does we'll return an invalid value to force the program to halt
        	}
        } 
        else 
        {
        	action = valueFunction.chooseGreedyAction(state);
        }
        return action;
    }
    
    // returns true if the specified action is amongst the greedy actions for the 
    // specified state, false otherwise
    private boolean isGreedy(int state, int action)
    {
        ActionSelector valueFunction = (ActionSelector) vf;
        return valueFunction.isGreedy(state,action);  	
    }

    @Override
    public String agent_message(String message) {
    	if (message.equals("get_agent_name"))
    	{
    		return "LRR";
    	}
        if (message.equals("freeze_learning")) {
            policyFrozen = true;
            System.out.println("Learning has been freezed");
            return "message understood, policy frozen";
        }
        if (message.startsWith("change_weights")){
            System.out.print("SideEffectSingleObjectiveAgent: Weights can not be changed");
            return "SideEffectSingleObjectiveAgent: Weights can not be changed";
        }
        if (message.startsWith("set_learning_parameters")){
        	System.out.println(message);
        	String[] parts = message.split(" ");
        	alpha = Double.valueOf(parts[1]).doubleValue();
        	lambda = Double.valueOf(parts[2]).doubleValue(); 
        	gamma = Double.valueOf(parts[3]).doubleValue();
        	explorationStrategy = Integer.valueOf(parts[4]).intValue();
        	vf.setExplorationStrategy(explorationStrategy);
        	System.out.print("Alpha = " + alpha + " Lambda = " + lambda + " Gamma = " + gamma + " exploration = " + TLO_LookupTable.explorationStrategyToString(explorationStrategy));
            System.out.println();
            return "Learning parameters set";
        }
        if (message.startsWith("set_egreedy_parameters")){
        	String[] parts = message.split(" ");
        	startingEpsilon = Double.valueOf(parts[1]).doubleValue();
        	epsilonLinearDecay = startingEpsilon / Double.valueOf(parts[2]).doubleValue(); // 2nd param is number of online episodes over which e should decay to 0
            System.out.println("Starting epsilon changed to " + startingEpsilon);
            return "egreedy parameters changed";
        }
        if (message.startsWith("set_softmax_parameters")){
        	String[] parts = message.split(" ");
        	startingTemperature = Double.valueOf(parts[1]).doubleValue();
        	int numEpisodes =  Integer.valueOf(parts[2]).intValue(); // 2nd param is number of online episodes over which temperature should decay to 0.01
        	temperatureDecayRatio = Math.pow(0.01/startingTemperature,1.0/numEpisodes);
            System.out.println("Starting temperature changed to " + startingTemperature + " Decay ratio = " + temperatureDecayRatio);
            return "softmax parameters changed";
        } 
        if (message.equals("start_new_trial")){
        	resetForNewTrial();
            System.out.println("New trial started: Q-values and other variables reset");
            return "New trial started: Q-values and other variables reset";
        }
        if (message.equals("start-debugging")){
            System.out.println("Debugging triggered");
            /*System.out.print(" ,");
            for (int i=0; i<numStates; i++)
            {
            	System.out.print(i+",");
            }
            System.out.println();
        	for (int x=0; x<numStates; x++)
        	{
            	System.out.print(x+",");
    	        	for (int y=0; y<numStates; y++)
    	        	{	
    	        		System.out.print(reachability[x][y]+",");
            		}
    	        System.out.println();
        	}
            System.out.println("RR(0,5)=" + relativeReachability(0, 5));
            System.out.println("RR(5,10)=" + relativeReachability(5, 10));
            System.out.println("RR(20,21)=" + relativeReachability(20, 21));*/
            vf.printQValues(0);
            vf.printQValues(5);
            vf.printQValues(10);
            return "Debugging triggered - printing out final reachability values";
        }
        System.out.println("LearningRelativeReachabilityAgent - unknown message: " + message);
        return "LearningRelativeReachabilityAgent does not understand your message.";
    }

    public static void main(String[] args) {
        AgentLoader theLoader = new AgentLoader( new LearningRelativeReachabilityAgent() );
        theLoader.run();

    }


}
