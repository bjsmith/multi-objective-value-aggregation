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


public class WSteeringTabularEpisodic extends SteeringAgent 
{
	
    protected void agent_start_internal(int state)
    {
    	agent_start_internal_episodic(state);
    }

    protected void agent_step_internal(Reward reward, int newState)
    {
    	agent_step_internal_episodic(reward);
    }
    
    public void agent_end(Reward reward)
    {
    	agent_end_episodic(reward);
    }
    

    protected int chooseGreedyPolicy(double[] targetVector, int state)
    {
    	return chooseGreedyPolicy_w(targetVector, state);
    }

    public static void main(String[] args) {
    	System.out.println("Launching W-Steering Tabular Episodic agent");
        AgentLoader theLoader = new AgentLoader( new WSteeringTabularEpisodic() );
        theLoader.run();

    }


}
