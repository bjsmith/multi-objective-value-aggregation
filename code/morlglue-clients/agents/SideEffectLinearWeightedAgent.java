// A quick-and-dirty conversion of the WSAgent code to implement linear weighted
// Q-learning as a baseline for the AI safety side-effects research project. This version is based on
// SideEffectsSingleObjectiveAgent. It received the standard objectives plus, as the final objective,
// the true reward. This is ignored by the agent by the simple expedient of setting the final objective weight to 0, while 
// the other weights are set to sum to 1. Ugly and inefficient, but time is pressing.

// IMPORTANT: Weights are fixed in the constructor - need to edit that if you want to try alternative weights.

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


public class SideEffectLinearWeightedAgent implements AgentInterface {

    WSLookupTable vf = null;
    Stack<StateActionDiscrete> tracingStack = null;

    private boolean policyFrozen = false;
    private Random random;

    private int numActions = 0;
    private int numStates = 0;
    int numOfObjectives;
    private final double objective1Weight = 0.1; // other weights are set to be equal to each other, and so all weights sum to 1.

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

    StateConverter stateConverter = null;

    @Override
    public void agent_init(String taskSpecification) {
    	System.out.println("SideEffectSingleObjectiveAgent launched");
        TaskSpecVRLGLUE3 theTaskSpec = new TaskSpecVRLGLUE3(taskSpecification);

        numActions = theTaskSpec.getDiscreteActionRange(0).getMax() + 1;
        numStates = theTaskSpec.getDiscreteObservationRange(0).getMax()+1;
        numOfObjectives = theTaskSpec.getNumOfObjectives();

        double[] weights = new double[numOfObjectives];
        // force all zero weights, except for the first objective
        weights[0]=objective1Weight;
        weights[numOfObjectives-1]=0.0; // force agent to ignore the final reward as that is the true reward which it shouldn't know
        double otherWeights = (1.0-objective1Weight)/(numOfObjectives-2); // calc others weights so as to sum to 1.0 overall
        for (int i=1; i<numOfObjectives-1; i++)
        	weights[i]=otherWeights;
        vf = new WSLookupTable( numOfObjectives, numActions, numStates, 0, weights );

        random = new Random();
        tracingStack = new Stack<>();

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

                if (i + 1 == tracingStack.size()) // this is the most recent action
                {
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
    		return "Linear";
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
        System.out.println("SideEffectsLinearWeightAgent - unknown message: " + message);
        return "SideEffectLinearWeightedAgent does not understand your message.";
    }

    public static void main(String[] args) {
        AgentLoader theLoader = new AgentLoader( new SideEffectLinearWeightedAgent() );
        theLoader.run();

    }


}
