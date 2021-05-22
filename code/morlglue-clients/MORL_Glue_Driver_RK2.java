//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.lang.reflect.Field;
//import java.lang.reflect.Method;
//import java.nio.channels.ClosedByInterruptException;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//import org.rlcommunity.rlglue.codec.AgentInterface;
//import org.rlcommunity.rlglue.codec.EnvironmentInterface;
//import org.rlcommunity.rlglue.codec.util.AgentLoader;
//import org.rlcommunity.rlglue.codec.util.EnvironmentLoader;
//
//import agents.*;
//import env.*;
//import experiments.*;
//
//
//public class MORL_Glue_Driver_RK2
//{
//	
//	private static void injectEnvironmentVariable(String key, String value)
//            throws Exception {
//
//        Class<?> processEnvironment = Class.forName("java.lang.ProcessEnvironment");
//
//        Field unmodifiableMapField = getAccessibleField(processEnvironment, "theUnmodifiableEnvironment");
//        Object unmodifiableMap = unmodifiableMapField.get(null);
//        injectIntoUnmodifiableMap(key, value, unmodifiableMap);
//
//        Field mapField = getAccessibleField(processEnvironment, "theEnvironment");
//        Map<String, String> map = (Map<String, String>) mapField.get(null);
//        map.put(key, value);
//    }
//
//    private static Field getAccessibleField(Class<?> clazz, String fieldName)
//            throws NoSuchFieldException {
//
//        Field field = clazz.getDeclaredField(fieldName);
//        field.setAccessible(true);
//        return field;
//    }
//
//    private static void injectIntoUnmodifiableMap(String key, String value, Object map)
//            throws ReflectiveOperationException {
//
//        Class unmodifiableMap = Class.forName("java.util.Collections$UnmodifiableMap");
//        Field field = getAccessibleField(unmodifiableMap, "m");
//        Object obj = field.get(map);
//        ((Map<String, String>) obj).put(key, value);
//    }
//	
//	public static interface ThreadGenerator
//	{
//		public Thread getThread(String[] args);
//	}
//	
//	public static Process startServer(String port) {
//		//Setup environment variables
//		Map<String, String> environment = new HashMap<String, String>(System.getenv());
//		environment.put("RLGLUE_PORT", port);
//		String[] envp = new String[environment.size()];
//		int count = 0;
//		for (Map.Entry<String, String> entry : environment.entrySet()) {
//		    envp[count++] = entry.getKey() + "=" + entry.getValue();
//		}
//		
//		// try to launch the MORL_Glue server
//		Process server = null;
//		Runtime rt = Runtime.getRuntime();
//		try {
//			server = rt.exec(new String[] {"wine","../morlglue-server/morlglue_x86.exe"}, envp); // Linux, MacOS
//			//server = rt.exec("..\\morlglue-server\\morlglue_x86.exe"); // Windows
//			System.out.println("Launching server");
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return server;
//	}
//	
//	public static Process startServer2(String port) {
//		ProcessBuilder processBuilder = new ProcessBuilder(new String[] {"wine","../morlglue-server/morlglue_x86.exe"});
//		//processBuilder.environment().put("RLGLUE_PORT", port);
//		Process server = null;
//		try {
//			server = processBuilder.start();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return server;
//	}
//	
//	public static Process stopServer() {
//		Process killer = null;
//		// try to launch the MORL_Glue server
//		Runtime rt = Runtime.getRuntime();
//		try {
//			killer = rt.exec(new String[] {"fuser","-k", "4096/tcp"}); // Linux, MacOS
//			//server = rt.exec("..\\morlglue-server\\morlglue_x86.exe"); // Windows
//			System.out.println("Launching server");
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return killer;
//	}
//	
//	// Runs all combinations of (agent, environment, experiment)
//	public static void runExperiments(String outpath) {
//		// comment out agents/environments/experiments that you don't want to run (at least one needed per list)
//		Map<String, ThreadGenerator> agents = new HashMap<String, ThreadGenerator>(){{
//				//put("SFLLA", new ThreadGenerator(){public Thread getThread(String[] args) {return new Thread() {public void run() {Agg_Agent2.main(new String[] {"SFLLA"});}};}});
//				//put("LELA", new ThreadGenerator(){public Thread getThread(String[] args) {return new Thread() {public void run() {Agg_Agent.main(new String[] {"LELA"});}};}});
//				//put("SFMLA", new ThreadGenerator(){public Thread getThread(String[] args) {return new Thread() {public void run() {Agg_Agent.main(new String[] {"SFMLA"});}};}});
//				//put("MIN", new ThreadGenerator(){public Thread getThread(String[] args) {return new Thread() {public void run() {try {MIN_Agent a = new MIN_Agent();a.main();}catch(Exception e){}}};}});				
//				put("Linear", new ThreadGenerator(){public Thread getThread(String[] args) {return new Thread() {public void run() {SideEffectLinearWeightedAgent.main(args);}};}});
//				put("SingleObjective", new ThreadGenerator(){public Thread getThread(String[] args) {return new Thread() {public void run() {SideEffectSingleObjectiveAgent.main(args);}};}});
//				put("TLO_A", new ThreadGenerator(){public Thread getThread(String[] args) {return new Thread() {public void run() {SafetyFirstMOAgent.main(args);}};}});
//				put("TLO_P", new ThreadGenerator(){public Thread getThread(String[] args) {return new Thread() {public void run() {SatisficingMOAgent.main(args);}};}});
//		}};
//
//		Map<String, ThreadGenerator> envs = new HashMap<String, ThreadGenerator>(){{
//				//put("BreakableBottles", new ThreadGenerator(){public Thread getThread(String[] args) {return new Thread() {public void run() {BreakableBottlesSideEffectsV2.main(new String[] {"SFLLA"});}};}});
//				put("UnbreakableBottles", new ThreadGenerator(){public Thread getThread(String[] args) {return new Thread() {public void run() {UnbreakableBottlesSideEffectsV2.main(args);}};}});
//				//put("Sokoban", new ThreadGenerator(){public Thread getThread(String[] args) {return new Thread() {public void run() {SokobanSideEffects.main(null);}};}});
//				//put("Doors", new ThreadGenerator(){public Thread getThread(String[] args) {return new Thread() {public void run() {try {Doors.main(null);}catch(Exception e){}}};}});
//		}};
//
//		Map<String, ThreadGenerator> experiments = new HashMap<String, ThreadGenerator>(){{
//				put("Standard", new ThreadGenerator(){public Thread getThread(String[] args) {return new Thread() {public void run() {SideEffectExperimentWithExcelOutput.main(args);}};}});
//		}};
//		
//		System.out.println("SAVING TO PATH: "+outpath);
//		System.out.println("NUMBER OF AGENTS: "+agents.size());
//		System.out.println("NUMBER OF ENVIRONMENTS: "+envs.size());
//		System.out.println("NUMBER OF EXPERIMENTS: "+experiments.size());
//		
//		//Map<String, String> programEnv = null;
//		//try {
//		//	programEnv = getModifiableEnvironment();
//		//} catch (Exception e1) {
//		//	e1.printStackTrace();
//		//}
//		//ExecutorService exec = Executors.newFixedThreadPool(10);
//		Process server;
//		int initport = 4096;
//	    for (String exstring : experiments.keySet()) {
//		    for(String astring : agents.keySet()) {
//		    	for(String envstring : envs.keySet()) {
//		    		//System.out.println("HOST: "+System.getenv("RLGLUE_HOST")+", PORT: "+System.getenv("RLGLUE_PORT"));
//		    		String stringport = Integer.toString(initport);
//		    		server = startServer2(stringport);
//		    		try {
//						injectEnvironmentVariable("RLGLUE_PORT", stringport);
//					} catch (Exception e1) {
//						e1.printStackTrace();
//					}
//		    		ThreadGenerator extg = experiments.get(exstring);
//		    		ThreadGenerator atg = agents.get(astring);
//		    		ThreadGenerator etg = envs.get(envstring);
//		    		
//		    		
//		    		Thread env = etg.getThread(new String[] {stringport});
//		    		env.start();
//		    		Thread agent = atg.getThread(new String[] {stringport});
//		    		agent.start();
//		    		Thread experiment = extg.getThread(new String[] {outpath, envstring, astring});
//		    		experiment.start();
//		    		
//		    		try {
//		    			experiment.join();
//		    			//stopServer();
//		    			server.destroyForcibly();
//		    			Thread.sleep(1000);
//		    			System.out.println("experiment running? "+experiment.isAlive());
//			    		agent.interrupt();
//		    			env.interrupt();
//		    			System.out.println("agent running? "+agent.isAlive());
//		    			System.out.println("environment running? "+env.isAlive());
//		    			System.out.println("Server running? "+server.isAlive());
//					} catch (InterruptedException e) {
//						//e.printStackTrace();
//					}
//		    		//initport += 1;
//		    	}
//		    }
//	    }
//	}
//
//	public static void main(String[] args) 
//	{		
//		String outpath = "data";
//		File theDir = new File(outpath);
//		if (!theDir.exists()){
//		    theDir.mkdirs();
//		}
//		runExperiments(outpath);
//	}
//	
//}