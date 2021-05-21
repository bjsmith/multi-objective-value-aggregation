import java.util.HashMap;
import java.util.Map;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.EnvironmentInterface;

import agents.*;
import env.*;
import experiments.*;
import experiments.LocalExperiment.ExperimentSettings;
import experiments.LocalExperiment.ExperimentBuilder;


public class MORL_Glue_Local_SD_BJS2
{
	
	public static interface AgentGenerator
	{
		public AgentInterface getAgent(String[] args);
	}
	
	public static interface EnvGenerator
	{
		public EnvironmentInterface getEnv(String[] args);
	}
	
	// Runs all combinations of (agent, environment, experiment)
	@SuppressWarnings("serial")
	public static void runExperiments() {
		//double primaryRewardThreshold = 1000; // use high value here to get lex-pa (for tlo-p or tlo-pa use the per envt thresholds below)
		// For UnbreakableBottles
//	    	double primaryRewardThreshold = -50; // sets threshold on the acceptable minimum level of performance on the primary reward
//	    	double minPrimaryReward = -1000; // the lowest reward obtainable
//	    	double maxPrimaryReward = 44;	// the highest reward obtainable
	    // For BreakableBottles
//	    	double primaryRewardThreshold = -500; // sets threshold on the acceptable minimum level of performance on the primary reward
//	    	double minPrimaryReward = -1000; // the lowest reward obtainable
//	    	double maxPrimaryReward = 44;	// the highest reward obtainable   
	    // For Sokoban and Doors
	    	double primaryRewardThreshold = -500; // sets threshold on the acceptable minimum level of performance on the primary reward
	    	double minPrimaryReward = -1000; // the lowest reward obtainable
	    	double maxPrimaryReward = 50;	// the highest reward obtainable
	    	
		// comment out agents that you don't want to run (at least one needed per list)
		Map<String, AgentGenerator> agents = new HashMap<String, AgentGenerator>(){{	
			// our agents
			//put("ELA", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"ELA"});}});
			//put("SFMLA", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"SFMLA"});}});
			//put("LELA", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"LELA"});}});
			//put("SFLLA", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"SFLLA"});}});
			//put("MIN", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"MIN"});}});
			//put("SEBA", new AgentGenerator() { public AgentInterface getAgent(String[] args) { return new Agg_Agent(new String[] { "SEBA" }); } });
			// Peter's agents
			//put("Linear", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new SideEffectLinearWeightedAgent();}});
			//put("SingleObjective", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new SideEffectSingleObjectiveAgent();}});
			
			put("TLO_P", new AgentGenerator(){public AgentInterface getAgent(String[] args) {
				double safetyThreshold = 1000; //use high value if you want to 'switch off' thresholding (ie to get TLO-P rather than TLO-PA)

				SatisficingMOAgent agent = new SatisficingMOAgent();
				agent.safetyThreshold = safetyThreshold;
				agent.primaryRewardThreshold = primaryRewardThreshold;
				agent.minPrimaryReward = minPrimaryReward;
				agent.maxPrimaryReward = maxPrimaryReward;
				return agent;
				}}
			);
			
			put("TLO_PA", new AgentGenerator(){public AgentInterface getAgent(String[] args) {
	            //TLO-PA
	        	double safetyThreshold = -0.1; //use high value if you want to 'switch off' thresholding (ie to get TLO-P rather than TLO-PA)
				SatisficingMOAgent agent = new SatisficingMOAgent();
				agent.safetyThreshold = safetyThreshold;
				agent.primaryRewardThreshold = primaryRewardThreshold;
				agent.minPrimaryReward = minPrimaryReward;
				agent.maxPrimaryReward = maxPrimaryReward;
				return agent;
				}}
			);
			
			put("TLO_A", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new SafetyFirstMOAgent();}});
			
			
			

			
		}};

		// comment out agents that you don't want to run (at least one needed per list)
		Map<String, EnvGenerator> envs = new HashMap<String, EnvGenerator>(){{
//			put("BreakableBottles", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {return new BreakableBottlesSideEffectsV2();}});
//			put("UnbreakableBottles", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {return new UnbreakableBottlesSideEffectsV2();}});
			put("Sokoban", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {return new SokobanSideEffects();}});
			put("Doors", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {return new Doors();}});
		}};
		
		// define experiment settings
		String experiment_id = "Test";
		String outpath = "data";
		
		System.out.println("SAVING TO PATH: "+outpath);
		System.out.println("NUMBER OF AGENTS: "+agents.size());
		System.out.println("NUMBER OF ENVIRONMENTS: "+envs.size());

	    for(String astring : agents.keySet()) {
	    	for(String envstring : envs.keySet()) {
	    		// generator agent and environment
	    		AgentGenerator atg = agents.get(astring);
	    		EnvGenerator etg = envs.get(envstring);
	    		EnvironmentInterface env = etg.getEnv(new String[] {});
	    		AgentInterface agent = atg.getAgent(new String[] {});
	    		
	    		// build experiment settings and run experiment
	    		ExperimentSettings settings = new ExperimentBuilder()
						.name(experiment_id).outpath(outpath)
						.agent(astring).env(envstring)
						.buildExperiment();
	    		LocalExperiment.main(agent, env, settings);
	    	}
	    }
	    System.out.println("FINISHED ALL EXPERIMENTS.");
	}
	
	public static void main(String[] args) 
	{		
		runExperiments();
	}
	
}