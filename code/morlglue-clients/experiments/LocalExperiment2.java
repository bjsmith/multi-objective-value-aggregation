// Modified from TLOExplorationExperiment in Dec 2018 to support initial experiments
// with AI safety side-effective sensitive agents

package experiments;

import java.lang.reflect.Field;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.LocalGlue;
import org.rlcommunity.rlglue.codec.RLGlue;
import org.rlcommunity.rlglue.codec.RLGlueInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.types.Observation_action;
import org.rlcommunity.rlglue.codec.types.Reward;
import org.rlcommunity.rlglue.codec.types.Reward_observation_action_terminal;

import agents.SkeletonAgent;
import env.SkeletonEnvironment2;
import tools.valuefunction.TLO_LookupTable;
import tools.spreadsheet.*;

public class LocalExperiment2 
{

    private int whichEpisode = 0;

	public AgentInterface agent;
	public EnvironmentInterface environment;
	public LocalGlue LocGlue;
	
	public LocalExperiment2(AgentInterface a, EnvironmentInterface e, String[] args) {
		this.agent = a;
		this.environment = e;
		this.LocGlue = new LocalGlue(e, a);
		RLGlue.setGlue(this.LocGlue);

	}

    /* Run One Episode of length maximum cutOff*/
    private void runEpisode(int stepLimit) {
        int terminal = RLGlue.RL_episode(stepLimit);

        int totalSteps = RLGlue.RL_num_steps();
        Reward totalReward = RLGlue.RL_return();

        System.out.println("Episode " + whichEpisode + "\t " + totalSteps + " steps \t" + totalReward + " total reward\t " + terminal + " natural end");

        whichEpisode++;
        
    	LocalGlue cc = this.LocGlue;
    	try {
	    	Field f1 = cc.getClass().getDeclaredField("totalReward");
	    	f1.setAccessible(true);
	    	//f1.set(cc, "reflecting on life");
	    	Reward lgReward = (Reward) f1.get(cc);
	    	System.out.println("LocalGlue reward: "+lgReward);
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    }

    public void runExperiment(String[] args) {
        System.out.println("\n\nExperiment starting up!");
        String taskSpec = RLGlue.RL_init();
        System.out.println("RL_init called, the environment sent task spec: " + taskSpec);

        System.out.println("\n\n----------Sending some sample messages----------");

        /*Talk to the agent and environment a bit...*/
        String responseMessage = RLGlue.RL_agent_message("what is your name?");
        System.out.println("Agent responded to \"what is your name?\" with: " + responseMessage);

        responseMessage = RLGlue.RL_agent_message("If at first you don't succeed; call it version 1.0");
        System.out.println("Agent responded to \"If at first you don't succeed; call it version 1.0  \" with: " + responseMessage + "\n");

        responseMessage = RLGlue.RL_env_message("what is your name?");
        System.out.println("Environment responded to \"what is your name?\" with: " + responseMessage);
        responseMessage = RLGlue.RL_env_message("If at first you don't succeed; call it version 1.0");
        System.out.println("Environment responded to \"If at first you don't succeed; call it version 1.0  \" with: " + responseMessage);

        System.out.println("\n\n----------Running a few episodes----------");
        runEpisode(100);
        runEpisode(100);
        runEpisode(100);
        runEpisode(100);
        runEpisode(100);
        runEpisode(1);
        /* Remember that stepLimit of 0 means there is no limit at all!*/
        runEpisode(0);
        RLGlue.RL_cleanup();

        System.out.println("\n\n----------Stepping through an episode----------");
        /*We could also start over and do another experiment */
        taskSpec = RLGlue.RL_init();

        /*We could run one step at a time instead of one episode at a time */
        /*Start the episode */
        Observation_action startResponse = RLGlue.RL_start();

        int firstObservation = startResponse.o.intArray[0];
        int firstAction = startResponse.a.intArray[0];
        System.out.println("First observation and action were: " + firstObservation + " and: " + firstAction);

        /*Run one step */
        Reward_observation_action_terminal stepResponse = RLGlue.RL_step();

        /*Run until the episode ends*/
        while (stepResponse.terminal != 1) {
            stepResponse = RLGlue.RL_step();
            if (stepResponse.terminal != 1) {
                /*Could optionally print state,action pairs */
                /*printf("(%d,%d) ",stepResponse.o.intArray[0],stepResponse.a.intArray[0]);*/
            }
        }

        System.out.println("\n\n----------Summary----------");

        int totalSteps = RLGlue.RL_num_steps();
        Reward totalReward = RLGlue.RL_return();
        System.out.println("It ran for " + totalSteps + " steps, total reward was: " + totalReward);
        RLGlue.RL_cleanup();


    }
    
    public static void main(String[] args) {
    	SkeletonAgent sa = new SkeletonAgent();
    	SkeletonEnvironment2 se = new SkeletonEnvironment2();
        LocalExperiment2 theExperiment = new LocalExperiment2(sa, se, args);
        theExperiment.runExperiment(args);
    }

}

