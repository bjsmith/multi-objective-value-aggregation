// OK, this is an odd one! This is a dummy agent used to debug the performance of two agents which should be producing equivalent
// behaviour (ie when one agent is a refinement of the other, as in when SafetyFirstMOAgent was extended to SatisficingMOAgent).
// It creates an instance of each agent, and then acts as a channel to pass messages to/from the RL_Glue server code, sending the same
// data to each agent. At the point where their behaviour starts to diverge, it gets them to produce a dump of all relevant
// information.

// NOTE: I realised what the issue was with SatisficingMOAgent just before I finished this code, so while this code looks complete I have
// never actually tested it.

package agents;

import java.math.BigDecimal;
import java.util.ArrayList;
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
import tools.valuefunction.SafetyFirstLookupTable;
import tools.valuefunction.TLO_LookupTable;
import tools.valuefunction.interfaces.ActionSelector;
import tools.valuefunction.interfaces.ValueFunction;


public class ComparisonAgentForDebugging implements AgentInterface {

	SafetyFirstMOAgent agent1 = new SafetyFirstMOAgent();
	SatisficingMOAgent agent2 = new SatisficingMOAgent();
	
    double impactThreshold = -0.1; // sets threshold on the acceptable level of environmental disruption
	
	SafetyFirstLookupTable vf = null;
    Stack<StateActionDiscrete> tracingStack = null;

    private boolean policyFrozen = false;
    private boolean debugging = false;
    private Random random;

    private int numActions = 0;
    private int numStates = 0;
    int numOfObjectives;

    private final double initQValues[]={0,0,0};
    int explorationStrategy; // flag used to indicate which type of exploration strategy is being used
    //if using eGreedy exploration
    double startingEpsilon;
    double epsilonLinearDecay;
    double epsilon;
    // if using softmax selection
    double startingTemperature;
    double temperatureDecayRatio;
    double temperature;
    
    double alpha;
    double gamma;
    double lambda;
    final int MAX_STACK_SIZE = 20;

    int numOfSteps;
    int numEpisodes;
    double accumulatedImpact; // sum the impact reward received so far

    StateConverter stateConverter = null;

    @Override
    public void agent_init(String taskSpecification) {
    	agent1.agent_init(taskSpecification);
    	agent2.agent_init(taskSpecification);
    }
    
    

    @Override
    public Action agent_start(Observation observation) {
    	Action action1 = agent1.agent_start(observation);
    	Action action2 = agent2.agent_start(observation);
    	if (action1.getInt(0)!=action2.getInt(0)) // the agents disagree
    	{
    		agent1.dumpInfo(observation, action1, action2);
    		agent2.dumpInfo(observation, action2, action1);
    	}
        return action1;
    }

    @Override
    public Action agent_step(Reward reward, Observation observation) 
    {
    	Action action1 = agent1.agent_step(reward, observation);
    	Action action2 = agent2.agent_step(reward, observation);
    	if (action1.getInt(0)!=action2.getInt(0)) // the agents disagree
    	{
    		agent1.dumpInfo(observation, action1, action2);
    		agent2.dumpInfo(observation, action2, action1);
    	}
        return action1;
    }

    @Override
    public void agent_end(Reward reward) 
    {
    	agent1.agent_end(reward);
    	agent2.agent_end(reward);
    }

    @Override
    public void agent_cleanup() {
    	agent1.agent_cleanup();
    	agent2.agent_cleanup();
    }
    
    

    @Override
    public String agent_message(String message) {
    	if (message.equals("get_agent_name"))
    	{
    		return "Comparison";
    	}
    	else
    	{
    		String string1 = agent1.agent_message(message);
    		String string2 = agent2.agent_message(message);
            return string1 + "\n" + string2;    		
    	}
    }

    public static void main(String[] args) {
        AgentLoader theLoader = new AgentLoader( new ComparisonAgentForDebugging() );
        theLoader.run();

    }


}
