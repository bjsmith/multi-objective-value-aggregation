import java.util.HashMap;
import java.util.Map;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.EnvironmentInterface;

import agents.*;
import env.*;
import experiments.*;
import experiments.LocalExperiment.ExperimentSettings;
import experiments.LocalExperiment.ExperimentBuilder;


public class MORL_Glue_Local_gen_vary_UAPA extends MORL_Glue_Local_Base
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
			put("SEBA", new AgentGenerator() { public AgentInterface getAgent(String[] args) { return new Agg_Agent(new String[] {"IDENT", "SEBA" }); } });
			// Peter's agents
//			put("Linear", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new SideEffectLinearWeightedAgent();}});
//			put("SingleObjective", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new SideEffectSingleObjectiveAgent();}});
			put("TLO_A", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new SafetyFirstMOAgent();}});
		}};

		// comment out agents that you don't want to run (at least one needed per list)
		Map<String, EnvGenerator> envs = new HashMap<String, EnvGenerator>(){{
//			put("UnbreakableBottles", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {return new UnbreakableBottlesSideEffectsV2();}});
			double[][] modifiers = {
					{1,1},
					{1,0.01},
					{1,0.1},
					{1,10},
					{1,100},
					{0.01,1},
					{0.1,1},
					{10,1},
					{100,1}
			};
			//String[] base_envs = {"BreakableBottles","UnbreakableBottles"};
			for (double[] mset: modifiers) {
				String env_name = "BreakableBottles";
				if (mset[0]!=1) {
					env_name += "rew" + mset[0];
				}
				if (mset[1]!=1) {
					env_name += "pen" + mset[1];
				}
				put(env_name, new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {
					return new BreakableBottlesSideEffectsV2(mset[0],mset[1]);
					
					}});
				
				env_name = "UnbreakableBottles";
				if (mset[0]!=1) {
					env_name += "rew" + mset[0];
				}
				if (mset[1]!=1) {
					env_name += "pen" + mset[1];
				}
				put(env_name, new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {
					return new UnbreakableBottlesSideEffectsV2(mset[0],mset[1]);
					
					}});
				
				env_name = "Sokoban";
				if (mset[0]!=1) {
					env_name += "rew" + mset[0];
				}
				if (mset[1]!=1) {
					env_name += "pen" + mset[1];
				}
				put(env_name, new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {
					return new SokobanSideEffects(mset[0],mset[0],mset[1]);
					
					}});
				
				env_name = "Doors";
				if (mset[0]!=1) {
					env_name += "rew" + mset[0];
				}
				if (mset[1]!=1) {
					env_name += "pen" + mset[1];
				}
				put(env_name, new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {
					return new Doors(mset[0],mset[0],mset[1]);
					
					}});	
				}
				
		}};
		
		// define experiment settings
		String experiment_id = "NoScale";
		String outpath = "data";
		int num_online = 5000;
		int num_offline = 500;
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
	    		//settings.additional_settings.put("PenaltyScale",env.)
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