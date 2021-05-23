import java.util.HashMap;
import java.util.Map;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.EnvironmentInterface;

import agents.*;
import env.*;
import experiments.*;
import experiments.LocalExperiment.ExperimentSettings;
import experiments.LocalExperiment.ExperimentBuilder;


public class MORL_Glue_Local_TLO_A_S_BJS extends MORL_Glue_Local_Base
{
	
	// Runs all combinations of (agent, environment, experiment)
	@SuppressWarnings("serial")
	public static void runExperiments() {
		// comment out agents that you don't want to run (at least one needed per list)
		Map<String, AgentGenerator> agents = new HashMap<String, AgentGenerator>(){{	
			// our agents
			put("LELA", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"IDENT", "LELA"});}});
			put("SFMLA", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"IDENT","SFMLA"});}});
			put("ELA", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"IDENT","ELA"});}});
			put("SFLLA", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"IDENT","SFLLA"});}});
//			put("MIN", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"MIN"});} });
//			put("SEBA", new AgentGenerator() { public AgentInterface getAgent(String[] args) { return new Agg_Agent(new String[] { "SEBA" }); } });
			// Peter's agents
//			put("Linear", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new SideEffectLinearWeightedAgent();}});
//			put("SingleObjective", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new SideEffectSingleObjectiveAgent();}});
			put("TLO_A", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new SafetyFirstMOAgent();}});
		}};

		// comment out agents that you don't want to run (at least one needed per list)
		Map<String, EnvGenerator> envs = new HashMap<String, EnvGenerator>(){{
//			put("BreakableBottles", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {return new BreakableBottlesSideEffectsV2();}});
//			put("UnbreakableBottles", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {return new UnbreakableBottlesSideEffectsV2();}});
			
			put("Sokoban", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {
				return new SokobanSideEffects(1,1,1);
				
				}});
			
			put("SokobanTimeUsePenalty10", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {
				return new SokobanSideEffects(10,1,1);
				
				}});
			put("SokobanReward10", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {
				return new SokobanSideEffects(1,10,1);
				
				}});
			put("SokobanBoxPositionPenalty10", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {
				return new SokobanSideEffects(1,1,10);
				}});

			put("SokobanTimeUsePenalty2", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {
				return new SokobanSideEffects(2,1,1);
				
				}});
			put("SokobanReward2", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {
				return new SokobanSideEffects(1,2,1);
				
				}});
			put("SokobanBoxPositionPenalty2", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {
				return new SokobanSideEffects(1,1,2);
				
				}});
			put("SokobanTimeUsePenalty0_5", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {
				return new SokobanSideEffects(0.5,1,1);
				
				}});
			put("SokobanReward0_5", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {
				return new SokobanSideEffects(1,0.5,1);
				
				}});
			put("SokobanBoxPositionPenalty0_5", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {
				return new SokobanSideEffects(1,05,2);
				
				}});

//			put("Doors", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {return new Doors();}});
		}};
		
		// define experiment settings
		String experiment_id = "Test";
		String outpath = "data";
		int num_online = 5000;
		int num_offline = 1;
		int max_episode_length = 1000;
		
		System.out.println("SAVING TO PATH: "+outpath);
		System.out.println("NUMBER OF AGENTS: "+agents.size());
		System.out.println("NUMBER OF ENVIRONMENTS: "+envs.size());
		
    	for(String envstring : envs.keySet()) {
    		experiment_id = "Test " + envstring;
    		String[] outputfiles = new String[agents.size()];
    		int runid = 0;
    		for(String astring : agents.keySet()) {
	    		// generator agent and environment
	    		AgentGenerator atg = agents.get(astring);
	    		EnvGenerator etg = envs.get(envstring);
	    		EnvironmentInterface env = etg.getEnv(new String[] {});
	    		AgentInterface agent = atg.getAgent(new String[] {});
	    		
	    		// build experiment settings and run experiment
	    		ExperimentSettings settings = new ExperimentBuilder()
						.name(experiment_id).outpath(outpath)
						.agent(astring).env(envstring)
						.episodes(num_online, num_offline, max_episode_length)
						.buildExperiment();
	    		String outputfile = LocalExperiment.main(agent, env, settings);
	    		outputfiles[runid] = outputfile;
	    		runid ++;
	    	}
    		ExperimentSettings plot_settings = new ExperimentBuilder()
					.name(experiment_id).outpath(outpath).env(envstring)
					.episodes(num_online, num_offline, max_episode_length)
					.buildExperiment();
    		plotting(plot_settings, outputfiles);
	    }
		
	    System.out.println("FINISHED ALL EXPERIMENTS.");
	}
	
	public static void main(String[] args) 
	{		
		runExperiments();
	}
	
}