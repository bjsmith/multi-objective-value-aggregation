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


public class MORL_Glue_Local_pilot_eeba_rolf
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
		Map<String, AgentGenerator> agents = new HashMap<String, AgentGenerator>(){{	
			// our agents
//			put("LELA", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"IDENT", "LELA"});}});
//			put("SFMLA", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"IDENT","SFMLA"});}});
//			put("ELA", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"IDENT","ELA"});}});
//			put("SFLLA", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"IDENT","SFLLA"});}});
//			put("MIN", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"MIN"});} });
//			put("SEBA", new AgentGenerator() { public AgentInterface getAgent(String[] args) { return new Agg_Agent(new String[] {"IDENT", "SEBA" }); } });
//			put("EEBA1", new AgentGenerator() { public AgentInterface getAgent(String[] args) { return new Agg_Agent(new String[] {"IDENT", "EEBA"}); } });
//			put("ROLF_EXP_LOG1", new AgentGenerator() { public AgentInterface getAgent(String[] args) { return new Agg_Agent(new String[] {"IDENT", "ROLF_EXP_LOG"}); } });
			// Peter's agents
			put("Linear", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new SideEffectLinearWeightedAgent();}});
//			put("SingleObjective", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new SideEffectSingleObjectiveAgent();}});
//			put("TLO_A", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new SafetyFirstMOAgent();}});
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
		boolean showplots = false;
		
		int num_online = 5000;
		int num_offline = 500;
		int max_episode_length = 1000; //number of TRIALS in EPISODE
		int num_repetitions = 100; //number of times to repeat each exeriment.
		
		String experiment_id = "NoScale";
		String outpath = "data/multirun_n" +num_repetitions + "_eeba_rolf";
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
    			//for (int exp_i=0; exp_i<num_repetitions; exp_i++){
    				
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