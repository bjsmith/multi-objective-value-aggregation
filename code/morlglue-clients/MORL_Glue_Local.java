
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.channels.ClosedByInterruptException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.util.AgentLoader;
import org.rlcommunity.rlglue.codec.util.EnvironmentLoader;

import agents.*;
import env.*;
import experiments.*;


public class MORL_Glue_Local
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
	public static void runExperiments(String outpath) {
		// comment out agents/environments/experiments that you don't want to run (at least one needed per list)
		Map<String, AgentGenerator> agents = new HashMap<String, AgentGenerator>(){{
				//put("SFLLA", new ThreadGenerator(){public Thread getThread(String[] args) {return new Thread() {public void run() {Agg_Agent2.main(new String[] {"SFLLA"});}};}});
				//put("LELA", new ThreadGenerator(){public Thread getThread(String[] args) {return new Thread() {public void run() {Agg_Agent.main(new String[] {"LELA"});}};}});
				//put("SFMLA", new ThreadGenerator(){public Thread getThread(String[] args) {return new Thread() {public void run() {Agg_Agent.main(new String[] {"SFMLA"});}};}});
				//put("MIN", new ThreadGenerator(){public Thread getThread(String[] args) {return new Thread() {public void run() {try {MIN_Agent a = new MIN_Agent();a.main();}catch(Exception e){}}};}});				
				put("Linear", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new SideEffectLinearWeightedAgent();}});
				//put("SingleObjective", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new SideEffectSingleObjectiveAgent();}});
				//put("TLO_A", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new SafetyFirstMOAgent();}});
				//put("TLO_P", new AgentGenerator(){public AgentInterface getAgent(String[] args) {return new SatisficingMOAgent();}});
		}};

		Map<String, EnvGenerator> envs = new HashMap<String, EnvGenerator>(){{
				//put("BreakableBottles", new ThreadGenerator(){public Thread getThread(String[] args) {return new Thread() {public void run() {BreakableBottlesSideEffectsV2.main(new String[] {"SFLLA"});}};}});
			//put("BreakableBottles", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {return new BreakableBottlesSideEffectsV2();}});
			//put("UnbreakableBottles", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {return new UnbreakableBottlesSideEffectsV2();}});
			put("Sokoban", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {return new SokobanSideEffects();}});
			//put("Doors", new EnvGenerator(){public EnvironmentInterface getEnv(String[] args) {return new Doors();}});
				//put("Sokoban", new ThreadGenerator(){public Thread getThread(String[] args) {return new Thread() {public void run() {SokobanSideEffects.main(null);}};}});
				//put("Doors", new ThreadGenerator(){public Thread getThread(String[] args) {return new Thread() {public void run() {try {Doors.main(null);}catch(Exception e){}}};}});
		}};

		//Map<String, ThreadGenerator> experiments = new HashMap<String, ThreadGenerator>(){{
		//		put("Standard", new ThreadGenerator(){public Thread getThread(String[] args) {return new Thread() {public void run() {SideEffectExperimentWithExcelOutput.main(args);}};}});
		//}};
		
		System.out.println("SAVING TO PATH: "+outpath);
		System.out.println("NUMBER OF AGENTS: "+agents.size());
		System.out.println("NUMBER OF ENVIRONMENTS: "+envs.size());

	    for(String astring : agents.keySet()) {
	    	for(String envstring : envs.keySet()) {
	    		AgentGenerator atg = agents.get(astring);
	    		EnvGenerator etg = envs.get(envstring);
	    		EnvironmentInterface env = etg.getEnv(new String[] {});
	    		AgentInterface agent = atg.getAgent(new String[] {});
	    		LocalExperiment.main(agent, env, new String[] {outpath});
	    	}
	    }
	}
	

	public static void main(String[] args) 
	{		
		String outpath = "data";
		File theDir = new File(outpath);
		if (!theDir.exists()){
		    theDir.mkdirs();
		}
		runExperiments(outpath);
	}
	
}