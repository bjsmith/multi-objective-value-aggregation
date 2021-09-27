import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.EnvironmentInterface;

import agents.*;
import env.*;
import experiments.*;
import experiments.LocalExperiment.ExperimentSettings;
import experiments.LocalExperiment.ExperimentBuilder;
import tools.valuefunction.AggregatorUtils;


public class MORL_Glue_Local_pilot_granularity
{
	public static interface AgentGenerator
	{
		public AgentInterface getAgent(String[] args);
	}
	
	public static interface EnvGenerator
	{
		public EnvironmentInterface getEnv(String[] args);
	}
	
	public static void plotting(ExperimentSettings settings, String[] files, boolean showplots) {
		Runtime rt = Runtime.getRuntime();
		String pythonPath = "/home/robert/miniconda3/envs/nnq/bin/python"; // replace with your Python path that has numpy, pandas and matplotlib
		try {
			ArrayList<String> command = new ArrayList<String>(Arrays.asList(pythonPath, "learning_plot.py", "--name",
					settings.NAME, "--path", settings.OUTPATH,
					"--num_online", Integer.toString(settings.NUM_ONLINE_EPISODES_PER_TRIAL),"--num_offline",
					Integer.toString(settings.NUM_OFFLINE_EPISODES_PER_TRIAL), "--timestamp",
					"--title", settings.ENV, "--format", settings.FORMAT));//, "--files"));
			if(showplots) {
				command.add("--show");
			}
			command.add("--files");
			command.addAll(new ArrayList<String>(Arrays.asList(files)));
			String[] cmd_ar = new String[command.size()];
	        cmd_ar = command.toArray(cmd_ar);
	        
			System.out.print("Running ");
			for(String part : cmd_ar) {
				System.out.print(part+" ");
			}
			// run plotting script
			Process p = rt.exec(cmd_ar);
	        String cmdOutput = null;
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader errorInput = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            // read the output from the command
            while ((cmdOutput = stdInput.readLine()) != null) {
                System.out.println(cmdOutput);
            }
            while ((cmdOutput = errorInput.readLine()) != null) {
                System.out.println(cmdOutput);
            }
			System.out.println("FINISHED PLOTTING.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	// Runs all combinations of (agent, environment, experiment)
	@SuppressWarnings("serial")
	public static void runExperiments() {
		// comment out agents that you don't want to run (at least one needed per list)
		double[][] granularities = {
				
				{0.0, 0.0},	//no granularity
				
				//no granularity for reward
				{0, 0.01},
				{0, 0.1},
				{0, 1},
				{0, 10},
				{0, 100},
				
				//no granularity for penalty
				{0.01, 0},
				{0.1, 0},
				{1, 0},
				{10, 0},
				{100, 0},
				
				//granularity for both objectives
				{0.01, 0.01},
				{0.1, 0.1},
				{1, 1},
				{10, 10},
				{100, 100},
		};
		
		for (double[] granularity_set: granularities) {
		
			Map<String, AgentGenerator> agents = new HashMap<String, AgentGenerator>(){{	
				// our agents

				
				String agent_name_sufix = "";
				agent_name_sufix += "rew_gran" + granularity_set[0];
				agent_name_sufix += "pen_gran" + granularity_set[1];
							
				
	//			put("MIN1" + agent_name_sufix, new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"IDENT", "MIN"}, granularity_set[0], granularity_set[1]);}});
			
			
				//Using Q value transformation function during aggregation
	//			put("LELA" + agent_name_sufix, new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"IDENT", "LELA"}, granularity_set[0], granularity_set[1]);}});
				put("SFMLA1" + agent_name_sufix, new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"IDENT","SFMLA"}, granularity_set[0], granularity_set[1]);}});
				put("ELA1" + agent_name_sufix, new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"IDENT","ELA"}, granularity_set[0], granularity_set[1]);}});
				put("SFLLA1" + agent_name_sufix, new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"IDENT","SFLLA"}, granularity_set[0], granularity_set[1]);}});
				put("SEBA1" + agent_name_sufix, new AgentGenerator() { public AgentInterface getAgent(String[] args) { return new Agg_Agent(new String[] {"IDENT", "SEBA" }, granularity_set[0], granularity_set[1]); } });
				put("EEBA1" + agent_name_sufix, new AgentGenerator() { public AgentInterface getAgent(String[] args) { return new Agg_Agent(new String[] {"IDENT", "EEBA"}, granularity_set[0], granularity_set[1]); } });
				put("ROLF_EXP_LOG1" + agent_name_sufix, new AgentGenerator() { public AgentInterface getAgent(String[] args) { return new Agg_Agent(new String[] {"IDENT", "ROLF_EXP_LOG"}, granularity_set[0], granularity_set[1]); } });

			
				// Peter's agents - NB! no granularity is applied to them
	//			put("Peter_Linear", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new SideEffectLinearWeightedAgent();}});
	//			put("SingleObjective", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new SideEffectSingleObjectiveAgent();}});
				put("TLO_A", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new SafetyFirstMOAgent();}});

			
				//Using trivial linear agent
				put("LIN_SUM" + agent_name_sufix, new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"IDENT", "SUM"}, granularity_set[0], granularity_set[1]);}});
			
			
				//Using reward to utility transformation function (in other words, "utility function") near agent entry point
				put("LELA2" + agent_name_sufix, new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"LELA", "SUM"}, granularity_set[0], granularity_set[1]);}});
				put("SFMLA2" + agent_name_sufix, new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"SFMLA", "SUM"}, granularity_set[0], granularity_set[1]);}});
				put("ELA2" + agent_name_sufix, new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"ELA", "SUM"}, granularity_set[0], granularity_set[1]);}});
				put("SFLLA2" + agent_name_sufix, new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"SFLLA", "SUM"}, granularity_set[0], granularity_set[1]);}});
				put("SEBA2" + agent_name_sufix, new AgentGenerator() { public AgentInterface getAgent(String[] args) { return new Agg_Agent(new String[] {"SEBA", "SUM"}, granularity_set[0], granularity_set[1]); } });
				put("EEBA2" + agent_name_sufix, new AgentGenerator() { public AgentInterface getAgent(String[] args) { return new Agg_Agent(new String[] {"EEBA", "SUM"}, granularity_set[0], granularity_set[1]); } });
				put("ROLF_EXP_LOG2" + agent_name_sufix, new AgentGenerator() { public AgentInterface getAgent(String[] args) { return new Agg_Agent(new String[] {"ROLF_EXP", "SUM_LOG"}, granularity_set[0], granularity_set[1]); } });
			
			}};

			// comment out agents that you don't want to run (at least one needed per list)
			Map<String, EnvGenerator> envs = new HashMap<String, EnvGenerator>(){{
	//			put("UnbreakableBottles", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {return new UnbreakableBottlesSideEffectsV2();}});
				double[][] modifiers = {
					
					//{0.01,0.01},
					//{0.1,0.1},
					{1,1}/*,					
					{10,10},
					{100,100},
					
					{1,0.01},
					{1,0.1},
					{1,10},
					{1,100},
					
					{0.01,1},
					{0.1,1},
					{10,1},
					{100,1}
					*/
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
			boolean showplots = false;
		
			int num_online = 5000;
			int num_offline = 500;
			int max_episode_length = 1000; //number of TRIALS in EPISODE
			int num_repetitions = 100; //number of times to repeat each exeriment.
		
			String experiment_id = "NoScale";
			String outpath = "data/multirun_n" +num_repetitions + "_eeba_rolf_attempt2";
			String format = "csv";
		
			System.out.println("SAVING TO PATH: "+outpath);
			System.out.println("NUMBER OF AGENTS: "+agents.size());
			System.out.println("NUMBER OF ENVIRONMENTS: "+envs.size());

			long startTime = System.nanoTime();
				
	    	for(String envstring : envs.keySet()) {
	    		experiment_id = "Test " + envstring;
	    		String[] outputfiles = new String[agents.size()];
	    		int runid = 0;
	    		for(String astring : agents.keySet()) {
	    			
	    			//make each experiment repeatable independent from previously run experiments in the same process
	    	    	//NB! reset random generator outside of the trials loop so that the trials will still be different
	    	    	AggregatorUtils.ResetRandomGenerator();
	    	    	
		    		// generator agent and environment
		    		AgentGenerator atg = agents.get(astring);
		    		EnvGenerator etg = envs.get(envstring);
		    		EnvironmentInterface env = etg.getEnv(new String[] {});
		    		AgentInterface agent = atg.getAgent(new String[] {});
	    		
		    		// build experiment settings and run experiment
		    		ExperimentSettings settings = new ExperimentBuilder()
							.name(experiment_id).outpath(outpath).format("csv").trials(num_repetitions)
							.agent(astring).env(envstring)
							.episodes(num_online, num_offline, max_episode_length)
							.buildExperiment();

		    		//settings.additional_settings.put("PenaltyScale",env.)
		    		String outputfile = LocalExperiment.main(agent, env, settings);
		    		outputfiles[runid] = outputfile;
		    		runid ++;
	    			//}

		    	}
	    		ExperimentSettings plot_settings = new ExperimentBuilder()
						.name(experiment_id).outpath(outpath).env(envstring).format("csv").trials(num_repetitions)
						.episodes(num_online, num_offline, max_episode_length)
						.buildExperiment();
	    		//plotting(plot_settings, outputfiles, showplots);
		    }
			
		} 	//for (double[] granularity_set: granularities) {
		
	    System.out.println("FINISHED ALL EXPERIMENTS.");
	    long endTime   = System.nanoTime();
		double totalTime = (endTime - startTime) / 1000000000.0;
		System.out.println("TOTAL TIME = "+totalTime+" seconds");

	}
	
	public static void main(String[] args) 
	{		
		runExperiments();
	}
	
}