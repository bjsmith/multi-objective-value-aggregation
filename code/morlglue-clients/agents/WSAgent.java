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
import tools.valuefunction.WSLookupTable;
import tools.valuefunction.interfaces.ActionSelector;
import tools.valuefunction.interfaces.ValueFunction;


public class WSAgent implements AgentInterface {

    WSLookupTable vf = null;
    Stack<StateActionDiscrete> tracingStack = null;

    private boolean policyFrozen = false;
    private Random random;

    private int numActions = 0;
    private int numStates = 0;
    int numOfObjectives;

    private final double initQValues[]={0,0,0};
    double alpha = 0.5;
    double startingEpsilon = 0.9;
    double epsilonLinearDecay = startingEpsilon / 9999; // set this to the inverse of the max number of episodes - 1
    double epsilon;
    double gamma = 1.0;
    double lambda = 0.95;
    final int MAX_STACK_SIZE = 20;

    int numOfSteps;
    int numEpisodes;

    StateConverter stateConverter = null;

    @Override
    public void agent_init(String taskSpecification) {
    	System.out.println("Linear-weighted Q-learning agent launched");
        TaskSpecVRLGLUE3 theTaskSpec = new TaskSpecVRLGLUE3(taskSpecification);

        numActions = theTaskSpec.getDiscreteActionRange(0).getMax() + 1;
        numStates = theTaskSpec.getDiscreteObservationRange(0).getMax()+1;
        numOfObjectives = theTaskSpec.getNumOfObjectives();

        double[] weights = new double[numOfObjectives];
        // default to even weighting of objectives
        for (int i=0; i<numOfObjectives; i++)
        	weights[i]=1.0/numOfObjectives;
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
        numOfSteps = 0;
        numEpisodes = 0;  
        epsilon = startingEpsilon;
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
        if (!policyFrozen) {
            if (random.nextDouble() < epsilon) {
                action = random.nextInt(numActions);
            } else {
                action = valueFunction.chooseGreedyAction(state);
            }
        } else {
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
        if (message.equals("freeze learning")) {
            policyFrozen = true;
            System.out.println("Learning has been freezed");
            return "message understood, policy frozen";
        }
        if (message.startsWith("change_weights")){
            System.out.print("Weights changed to ");
        	String[] parts = message.split(" ");
        	double weights[] = new double[numOfObjectives];
        	for (int i=0; i< numOfObjectives; i++)
        	{
        		weights[i] = Double.valueOf(parts[i+1]).doubleValue();
        		System.out.print(weights[i] + " ");
        	}
        	vf.setWeights(weights);
            System.out.println();
            return "Weights changed";
        }
        if (message.equals("start_new_trial")){
        	resetForNewTrial();
            System.out.println("New trial started: Q-values and other variables reset");
            return "New trial started: Q-values and other variables reset";
        }

        return "QLearningAgent(Java) does not understand your message.";
    }

    public static void main(String[] args) {
        AgentLoader theLoader = new AgentLoader( new WSAgent() );
        theLoader.run();

    }


}
