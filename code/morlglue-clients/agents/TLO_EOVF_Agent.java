// A modification of the TLO_Agent to use an additional exploration objective (the EOVF/MOX approach)

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
import tools.valuefunction.TLO_EOVF_LookupTable;
import tools.valuefunction.interfaces.ActionSelector;
import tools.valuefunction.interfaces.ValueFunction;


public class TLO_EOVF_Agent implements AgentInterface {

    TLO_EOVF_LookupTable vf = null;
    Stack<StateActionDiscrete> tracingStack = null;

    private boolean clearTraces; 
    public static final boolean WATKINS = true; // use Watkin's traces (clear on non-greedy actions)
    public static final boolean PENG = false; // use Peng's traces (don't clear traces on non-greedy actions)
    private final double EOVF_INIT = 200; // change this on a per-environment basis
    private boolean policyFrozen = false;
    //private Random random;

    private int numActions = 0;
    private int numStates = 0;
    int numOfObjectives;

    private double initQValues[];
    double thresholds[];
    double alpha;
    double lambda; 
    double gamma;

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
    private boolean fubar = false; // used for debugging -set this to true if something is going wrong
    private int lastState;
    private int stateRepetitionCounter;
    private final int STATE_REPETITION_THRESHOLD = 50;

    StateConverter stateConverter = null;

    @Override
    public void agent_init(String taskSpecification) 
    {
    	System.out.println("TLO EOVF Q-learning agent launched");
        TaskSpecVRLGLUE3 theTaskSpec = new TaskSpecVRLGLUE3(taskSpecification);

        numActions = theTaskSpec.getDiscreteActionRange(0).getMax() + 1;
        numStates = theTaskSpec.getDiscreteObservationRange(0).getMax()+1;
        numOfObjectives = theTaskSpec.getNumOfObjectives();
        initQValues = new double[numOfObjectives+1]; // add 1 because we need an initial value for the EOVF objective as well
        initQValues[numOfObjectives] = EOVF_INIT;

        thresholds = new double[numOfObjectives-1];
        // default to all thresholds set at 0
        for (int i=0; i<numOfObjectives-1; i++)
        	thresholds[i]=0.0;
        vf = new TLO_EOVF_LookupTable(numOfObjectives, numActions, numStates, 0, thresholds, 0.9); // 0.9 is a default setting for the EOVF gamma value

        //random = new Random(AggregatorUtils.random.nextInt());
        tracingStack = new Stack<>();
        startingEpsilon = 0; // default - just use greedy exploration in combination with the EOVF objective

        //set the model of converting MDP observation to an int state representation
        stateConverter = new DummyStateConverter();
        StateActionDiscrete.setStateConverter( stateConverter );
        resetForNewTrial();

    }
    
    private void resetForNewTrial()
    {
        numOfSteps = 0;
        numEpisodes = 0;  
        epsilon = startingEpsilon;
        temperature = startingTemperature;
        // reset Q-values
        vf.resetQValues(initQValues); 
        policyFrozen = false;
        fubar = false;
    }
    
    // provided for debugging purposes
    private void printState(int state)
    {
    	System.out.print("State " + state + ": ");
    	for (int a=0; a<numActions; a++)
    	{
    		System.out.print("A" + a);
    		if (vf.isGreedy(state, a))
    		{
    			System.out.print("G");
    		}
    		if (vf.isExploratoryGreedy(state, a))
    		{
    			System.out.print("E");
    		} 
    		System.out.print(" ");
    		vf.printQValues(state, a);
    	}
    	System.out.println();
    }

    @Override
    public Action agent_start(Observation observation) {
    	//System.out.println("Starting episode " + numEpisodes + " Temperature = " + temperature);
    	if (fubar) System.out.println();
    	numOfSteps = 0;
        tracingStack.clear();
        int state = stateConverter.getStateNumber( observation );
    	//System.out.print("0 ");printState(state);
        int action = getAction(state);

        Action returnAction = new Action(1, 0, 0);
        returnAction.intArray[0] = action;
        tracingStack.add(new StateActionDiscrete(observation, returnAction)); // put executed action on the stack
        lastState = state;
        stateRepetitionCounter = 0;
        return returnAction;

    }

    @Override
    public Action agent_step(Reward reward, Observation observation) 
    {
        numOfSteps++;

        int state = stateConverter.getStateNumber( observation );
        /*if (state==lastState)
        {
        	stateRepetitionCounter++;
        	if (stateRepetitionCounter>STATE_REPETITION_THRESHOLD)
        	{
        		System.out.print(numOfSteps + " ");
        		vf.printCurrentStateValues(state);
        		System.out.println("Reward: " + reward.doubleArray[0] + " " + reward.doubleArray[1] + " " + reward.doubleArray[2] + " " + reward.doubleArray[3]);
        	}
        	if (stateRepetitionCounter>100)
        	{
        		System.out.println("Going to debug mode");
        	}
        }
        else
        {
        	stateRepetitionCounter=0;
        }
        lastState = state;
        if (fubar)
        {
        	System.out.print(state + " ");
        }
  	  	if (numOfSteps>=998)
  	  	{
  	  		fubar = true;
  	  	}*/
        int action;
        int greedyAction = ((ActionSelector)vf).chooseGreedyAction(state);

        if (!policyFrozen) {
            //double currentLambda = lambda;
        	int stepsBack = 0;
            for (int i = tracingStack.size() - 1; i >= 0; i--) {
                StateActionDiscrete pair = tracingStack.get(i);

                int prevAction = pair.getAction().getInt(0);
                int prevState = stateConverter.getStateNumber(pair.getObservation());

                if (i + 1 == tracingStack.size()) // this is the most recent action
                {
                    vf.calculateErrors(prevAction, prevState, greedyAction, state, gamma, reward);
                    vf.update(prevAction, prevState, stepsBack, lambda, alpha, gamma);
                } 
                else {
                	// if there is no more recent entry for this state-action pair then update it
                	// this is to implement replacing rather than accumulating traces
                    int index = tracingStack.indexOf(pair, i + 1);
                    if (index == -1) {
                    	vf.update(prevAction, prevState, stepsBack, lambda, alpha, gamma);
                    }
                    //currentLambda *= lambda;
                }
                stepsBack++;
            }
        } 
        //System.out.print(numOfSteps + " ");printState(state);
        action = getAction(state);
        Action returnAction = new Action(1, 0, 0);
        returnAction.intArray[0] = action;
        // clear trace if this action is not greedy and we are using Watkin's approach to traces, otherwise trim stack if neccesary
        if (clearTraces && !isGreedy(state,action))
        {
        	tracingStack.clear();
        }
        else
        {
	        if( tracingStack.size() == MAX_STACK_SIZE ) 
	        {
	            tracingStack.remove(0);
	        }
        }
        // in either case, can now add this state-action to the trace stack
        tracingStack.add( new StateActionDiscrete(observation, returnAction ) );

        return returnAction;
    }

    @Override
    public void agent_end(Reward reward) 
    {
    	//System.out.println("END EPISODE\n\n");
  	  	numOfSteps++;
  	  	numEpisodes++;

  	  	epsilon -= epsilonLinearDecay;
  	  	temperature *= temperatureDecayRatio;
        if (!policyFrozen) {
        	int stepsBack = 0;
        	//double currentLambda = lambda;
            for (int i = tracingStack.size() - 1; i >= 0; i--) {
                StateActionDiscrete pair = tracingStack.get(i);

                int prevAction = pair.getAction().getInt(0);
                int prevState = stateConverter.getStateNumber(pair.getObservation());

                if (i + 1 == tracingStack.size()) 
                {
                    vf.calculateTerminalErrors(prevAction, prevState, gamma, reward);
                    vf.update(prevAction, prevState, stepsBack, lambda, alpha, gamma);
                } 
                else 
                {
                	// if there is no more recent entry for this state-action pair then update it
                	// this is to implement replacing rather than accumulating traces
                    int index = tracingStack.indexOf(pair, i + 1);
                    if (index == -1) {
                        vf.update(prevAction, prevState, stepsBack, lambda, alpha, gamma);
                    }
                    //currentLambda *= lambda;
                }
                stepsBack++;
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
        if (message.startsWith("set_learning_parameters")){
        	System.out.println(message);
        	String[] parts = message.split(" ");
        	alpha = Double.valueOf(parts[1]).doubleValue();
        	lambda = Double.valueOf(parts[2]).doubleValue(); 
        	gamma = Double.valueOf(parts[3]).doubleValue();
        	explorationStrategy = Integer.valueOf(parts[4]).intValue();
        	clearTraces = Boolean.parseBoolean(parts[5]);
        	vf.setExplorationStrategy(explorationStrategy);
        	System.out.print("Alpha = " + alpha + " Lambda = " + lambda + " Gamma = " + gamma + " exploration = " + TLO_LookupTable.explorationStrategyToString(explorationStrategy) 
        			+ " Clearing traces = " + clearTraces + " Init Q values =");
        	for (int i=0; i<numOfObjectives; i++)
        	{
        		initQValues[i] = Double.valueOf(parts[i+6]).doubleValue();
        		System.out.print(" " + initQValues[i]);
        	}
            System.out.println();
            return "Learning parameters set";
        }	
        if (message.equals("freeze_learning")) {
            policyFrozen = true;
            System.out.println("\t\t\tLearning and exploration has been frozen");
            return "message understood, policy frozen";
        }
        if (message.equals("unfreeze_learning")) {
            policyFrozen = false;
            System.out.println("\t\t\tLearning and exploration has been unfrozen");
            return "message understood, policy frozen";
        }
        if (message.startsWith("change_thresholds")){
            System.out.print("\tThresholds changed to ");
        	String[] parts = message.split(" ");
        	double thresholds[] = new double[numOfObjectives-1];
        	for (int i=0; i< numOfObjectives-1; i++)
        	{
        		thresholds[i] = Double.valueOf(parts[i+1]).doubleValue();
        		System.out.print(thresholds[i] + " ");
        	}
        	vf.setThresholds(thresholds);
            System.out.println();
            return "Thresholds changed";
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
        if (message.startsWith("set_eovf_parameters")){
        	String[] parts = message.split(" ");
        	double eovfGamma = Double.valueOf(parts[1]).doubleValue();
        	int numEpisodes =  Integer.valueOf(parts[2]).intValue(); // ignore this - not relevant for EOVF exploration
        	vf.setEovfGamma(eovfGamma);
            System.out.println("EOVF gamma set to " + eovfGamma);
            return "eovf parameters changed";
        }
        if (message.equals("start_new_trial")){
        	resetForNewTrial();
            System.out.println("\t\t\tNew trial started: Q-values and other variables reset");
            return "New trial started: Q-values and other variables reset";
        }
        

        return "TLO QLearningAgent(Java) does not understand your message.";
    }
    
    // returns a string indicating the type of traces being used
    public static String traceNameToString(boolean traceType)
    {
    	if (traceType)
    	{
    		return "WATKINS";
    	}
    	else
    	{
    		return "PENG";
    	}
    }

    public static void main(String[] args) {
        AgentLoader theLoader = new AgentLoader( new TLO_EOVF_Agent() );
        theLoader.run();

    }


}
