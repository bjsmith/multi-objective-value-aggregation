import java.io.BufferedReader;
import java.io.InputStream;
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


public class MORL_Glue_Local_RK
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
			//put("ELA", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"ELA"});}});
			//put("SFMLA", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"SFMLA"});}});
			//put("LELA", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"LELA"});}});
			//put("SFLLA", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"SFLLA"});}});
			//put("MIN", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new Agg_Agent(new String[] {"MIN"});}});
			//put("SEBA", new AgentGenerator() { public AgentInterface getAgent(String[] args) { return new Agg_Agent(new String[] { "SEBA" }); } });
			// Peter's agents
			//put("Linear", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new SideEffectLinearWeightedAgent();}});
			//put("SingleObjective", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new SideEffectSingleObjectiveAgent();}});
			put("TLO_A", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new SafetyFirstMOAgent();}});
			//put("TLO_P", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new SatisficingMOAgent();}});
		}};

		// comment out agents that you don't want to run (at least one needed per list)
		Map<String, EnvGenerator> envs = new HashMap<String, EnvGenerator>(){{
			put("BreakableBottles", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {return new BreakableBottlesSideEffectsV2();}});
//			put("UnbreakableBottles", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {return new UnbreakableBottlesSideEffectsV2();}});
//			put("Sokoban", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {return new SokobanSideEffects();}});
//			put("Doors", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {return new Doors();}});
		}};
		
		// define experiment settings
		boolean showplots = true;
		String experiment_id = "TLOA_vs_SFLLA_2";
		String outpath = "data";
		String format = "csv";
		int num_online = 20;
		int num_offline = 10;
		int max_episode_length = 1000;
		int trials = 2;
		
		System.out.println("SAVING TO PATH: "+outpath);
		System.out.println("NUMBER OF AGENTS: "+agents.size());
		System.out.println("NUMBER OF ENVIRONMENTS: "+envs.size());
		
    	for(String envstring : envs.keySet()) {
    		String[] outputfiles = new String[agents.size()];
    		int agentid = 0;
    		for(String astring : agents.keySet()) {
	    		// generator agent and environment
	    		AgentGenerator atg = agents.get(astring);
	    		EnvGenerator etg = envs.get(envstring);
	    		EnvironmentInterface env = etg.getEnv(new String[] {});
	    		AgentInterface agent = atg.getAgent(new String[] {});
	    		
	    		// build experiment settings and run experiment
	    		ExperimentSettings settings = new ExperimentBuilder()
						.name(experiment_id).outpath(outpath).format(format)
						.agent(astring).env(envstring)
						.episodes(num_online, num_offline, max_episode_length).trials(trials)
						.buildExperiment();
	    		String outputfile = LocalExperiment.main(agent, env, settings);
	    		outputfiles[agentid] = outputfile;
	    		agentid ++;
	    	}
    		ExperimentSettings plot_settings = new ExperimentBuilder()
					.name(experiment_id+"_"+envstring).outpath(outpath).format(format).env(envstring)
					.episodes(num_online, num_offline, max_episode_length).trials(trials)
					.buildExperiment();
    		plotting(plot_settings, outputfiles, showplots);
	    }
	    System.out.println("FINISHED ALL EXPERIMENTS.");
	}
	
	public static void main(String[] args) 
	{		
		runExperiments();
	}
	
}