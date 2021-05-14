
import agents.*;
import env.*;
import experiments.*;


public class MORL_Glue_Driver_RK
{
	
	public static interface ThreadGenerator
	{
		public Thread getThread();
	}
	
	public static Process startServer() {
		Process server = null;
		// try to launch the MORL_Glue server
		Runtime rt = Runtime.getRuntime();
		try {
			server = rt.exec(new String[] {"wine","../morlglue-server/morlglue_x86.exe"}); // Linux, MacOS
			//server = rt.exec("..\\morlglue-server\\morlglue_x86.exe"); // Windows
			System.out.println("Launching server");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return server;
	}
	
	// Runs all combinations of (agent, environment, experiment)
	public static void runExperiments() {
		// comment out agents/environments/experiments that you don't want to run (at least one needed per list)
		ThreadGenerator[] agents = new ThreadGenerator[]{
				//new ThreadGenerator(){public Thread getThread() {return new Thread() {public void run() {Agg_Agent.main(new String[] {"SFLLA"});}};}},
				new ThreadGenerator(){public Thread getThread() {return new Thread() {public void run() {SideEffectSingleObjectiveAgent.main(null);}};}},
				new ThreadGenerator(){public Thread getThread() {return new Thread() {public void run() {SideEffectLinearWeightedAgent.main(null);}};}},
				new ThreadGenerator(){public Thread getThread() {return new Thread() {public void run() {SafetyFirstMOAgent.main(null);}};}},
				new ThreadGenerator(){public Thread getThread() {return new Thread() {public void run() {SatisficingMOAgent.main(null);}};}}
		};

		ThreadGenerator[] envs = new ThreadGenerator[]{
				new ThreadGenerator(){public Thread getThread() {return new Thread() {public void run() {BreakableBottlesSideEffectsV2.main(new String[] {"SFLLA"});}};}},
				new ThreadGenerator(){public Thread getThread() {return new Thread() {public void run() {UnbreakableBottlesSideEffectsV2.main(null);}};}},
				new ThreadGenerator(){public Thread getThread() {return new Thread() {public void run() {SokobanSideEffects.main(null);}};}},
				new ThreadGenerator(){public Thread getThread() {return new Thread() {public void run() {Doors.main(null);}};}}
		};

		ThreadGenerator[] experiments = new ThreadGenerator[]{
				new ThreadGenerator(){public Thread getThread() {return new Thread() {public void run() {SideEffectExperimentWithExcelOutput.main(new String[] {"SFLLA"});}};}}
		};
		int combo = 0;
		System.out.println("NUMBER OF AGENTS "+agents.length);
		System.out.println("NUMBER OF ENVIRONMENTS "+envs.length);
		System.out.println("NUMBER OF EXPERIMENTS "+experiments.length);

	    for (ThreadGenerator extg : experiments) {
		    for(ThreadGenerator atg : agents) {
		    	for(ThreadGenerator etg : envs) {
		    		System.out.println("COMBO "+combo+" RUNNING");
		    		Process server = startServer();
		    		Thread agent = atg.getThread();
		    		agent.start();
		    		Thread env = etg.getThread();
		    		env.start();
		    		Thread experiment = extg.getThread();
		    		experiment.start();
		    		try {
		    			experiment.join();
		    			Thread.sleep(1000);
			    		//agent.interrupt();
			    		//env.interrupt();
			    		//server.destroy();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
		    		combo += 1;
		    	}
		    }
	    }
	    //System.exit(0); 
	}

	public static void main(String[] args) 
	{		
		runExperiments();
	}
	
}