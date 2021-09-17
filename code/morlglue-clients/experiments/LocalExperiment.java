// Modified from TLOExplorationExperiment in Dec 2018 to support initial experiments
// with AI safety side-effective sensitive agents

package experiments;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Hashtable;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.RLGlue;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.types.Reward;

import tools.valuefunction.TLO_LookupTable;
import tools.spreadsheet.*;

public class LocalExperiment 
{
    
    // helper class that stores experimental settings
    public static class ExperimentSettings {
    	public String NAME = "Test";
    	public String OUTPATH = "data";
    	public String FORMAT = "csv";
    	public String AGENT = "";
    	public String ENV = "";
    	public double ALPHA = 0.1;
	    public double LAMBDA = 0.95;
	    public double GAMMA = 1.0;
	    public int NUM_TRIALS = 1;
		
		//options are: EGREEDY = 0; SOFTMAX_TOURNAMENT = 1; SOFTMAX_ADDITIVE_EPSILON = 2;
		public int EXPLORATION = TLO_LookupTable.SOFTMAX_TOURNAMENT;
	    
	    public int EXPLORATION_PARAMETER = 10; 
    	public int NUM_ONLINE_EPISODES_PER_TRIAL = 5000;
    	public int NUM_OFFLINE_EPISODES_PER_TRIAL = 100;
    	public int MAX_EPISODE_LENGTH = 1000;
    	
    	public Hashtable<String, String> additional_settings = new Hashtable<String, String>();

    	
    	public ExperimentSettings(String name, String outpath, String format, String agent, String env, double alpha, double lambda, double gamma,
    			int num_trials, int exploration, int exploration_param,
    			int num_online_per_trial, int num_offline_per_trial, int max_episode_length) {
    		this.NAME = name;
    		this.OUTPATH = outpath;
    		this.FORMAT = format;
    		this.AGENT = agent;
    		this.ENV = env;
    		this.ALPHA = alpha;
    		this.LAMBDA = lambda;
    		this.GAMMA = gamma;
    		this.NUM_TRIALS = num_trials;
    		this.EXPLORATION = exploration;
    		this.EXPLORATION_PARAMETER = exploration_param;
    		this.NUM_ONLINE_EPISODES_PER_TRIAL = num_online_per_trial;
    		this.NUM_OFFLINE_EPISODES_PER_TRIAL = num_offline_per_trial;
    		this.MAX_EPISODE_LENGTH = max_episode_length;
    	}
    }
    
	// helper class to build experiment settings
	public static class ExperimentBuilder {
		private String NAME = "Test";
		private String OUTPATH = "data";
		private String FORMAT = "csv";
		private String AGENT = "";
		private String ENV = "";
		private double ALPHA = 0.1;
		private double LAMBDA = 0.95;
		private double GAMMA = 1.0;
		private int NUM_TRIALS = 1;
		
		//options are: EGREEDY = 0; SOFTMAX_TOURNAMENT = 1; SOFTMAX_ADDITIVE_EPSILON = 2;
		private int EXPLORATION = TLO_LookupTable.SOFTMAX_TOURNAMENT;
		
		private int EXPLORATION_PARAMETER = 10; 
		private int NUM_ONLINE_EPISODES_PER_TRIAL = 5000;
		private int NUM_OFFLINE_EPISODES_PER_TRIAL = 1;
		private int MAX_EPISODE_LENGTH = 1000;
    	
    	public ExperimentBuilder() {}
    	
    	public ExperimentSettings buildExperiment() {
    		return new ExperimentSettings(NAME, OUTPATH, FORMAT, AGENT, ENV, ALPHA, LAMBDA, GAMMA, NUM_TRIALS, EXPLORATION,
    				EXPLORATION_PARAMETER, NUM_ONLINE_EPISODES_PER_TRIAL, NUM_OFFLINE_EPISODES_PER_TRIAL,
    				MAX_EPISODE_LENGTH);
    	}
    	
    	public ExperimentBuilder name(String name) { this.NAME = name; return this;}
    	public ExperimentBuilder outpath(String outpath) { this.OUTPATH = outpath; return this;}
    	public ExperimentBuilder format(String format) { this.FORMAT = format; return this;}
    	public ExperimentBuilder agent(String agent) { this.AGENT = agent; return this;}
    	public ExperimentBuilder env(String env) { this.ENV = env; return this;}
    	public ExperimentBuilder alpha(double alpha) { this.ALPHA = alpha; return this;}
    	public ExperimentBuilder lambda(double lambda) { this.LAMBDA = lambda; return this;}
    	public ExperimentBuilder gamma(double gamma) { this.GAMMA = gamma; return this;}
    	public ExperimentBuilder trials(int num_trials) { this.NUM_TRIALS = num_trials; return this;}
    	public ExperimentBuilder exploration(int exploration_type, int exploration_param) {
    		this.EXPLORATION = exploration_type;
    		this.EXPLORATION_PARAMETER = exploration_param;
    		return this;
    	}
    	public ExperimentBuilder episodes(int online, int offline, int max_length) {
    		this.NUM_ONLINE_EPISODES_PER_TRIAL = online;
    		this.NUM_OFFLINE_EPISODES_PER_TRIAL = offline;
    		this.MAX_EPISODE_LENGTH = max_length;
    		return this;
    	}
	}
    
	// descriptions for exploration strategies
    @SuppressWarnings("serial")
	Map<Integer, String> METHOD_PREFIX = new HashMap<Integer, String>(){{
    	put(TLO_LookupTable.EGREEDY, "EGREEDY");
    	put(TLO_LookupTable.SOFTMAX_ADDITIVE_EPSILON, "SOFTMAX_E");
    	put(TLO_LookupTable.SOFTMAX_TOURNAMENT, "SOFTMAY_T");
    }};
	
    // internal members
    private int numObjectives;
    private final String PARAM_CHANGE_STRING = "set_softmax_parameters";
	private ExcelWriter excel;
	private CSVWriter csv;
	private ExperimentSettings settings;
	
	// members holding the RLGlue instances
	public AgentInterface agent;
	public EnvironmentInterface environment;
	public LocalGlueNew LocGlue;
	
	public LocalExperiment(AgentInterface a, EnvironmentInterface e, ExperimentSettings sets) {
		// updates settings
		this.settings = sets;
		// make sure output folder exists
		File theDir = new File(settings.OUTPATH);
		if (!theDir.exists()){
		    theDir.mkdirs();
		}
		// setup RLGlue
		this.agent = a;
		this.environment = e;
    	RLGlue.resetGlueProxy();
		this.LocGlue = new LocalGlueNew(e, a);
		RLGlue.setGlue(this.LocGlue);
	}
  
    // store the data for the most recent Reward. The strings indicates a value or label to be written in the first two columns
    private void saveReward(String labels, Reward r)
    {
    	switch(settings.FORMAT) {
	    	case "csv":
	    		String r0 = Double.toString(r.getDouble(0));
	    		String r1 = Double.toString(r.getDouble(1));
	    		String r2 = Double.toString(r.getDouble(2));
	    		csv.writeLinesRaw(new String[] {labels+","+r0+","+r1+","+r2});
	    		break;
	    	case "excel":
	    		excel.writeNextRowTextAndNumbers(labels, r.doubleArray);
	    		break;
    	}
    }    
	    
    // Run One Episode of length maximum cutOff
    private Reward runEpisode(int episodeId, int stepLimit) {
        int terminal = RLGlue.RL_episode(stepLimit);
        int totalSteps = RLGlue.RL_num_steps();
        Reward totalReward = RLGlue.RL_return();
        if ((episodeId % 1000)==0) {
        		System.out.println("episode "+episodeId+", num steps: "+totalSteps+", total reward: "+Arrays.toString(totalReward.doubleArray));        	
        }
        
        return totalReward;
    }

    public String runExperiment() {
    	
    	// set up data structures to store reward history
        String taskSpec = RLGlue.RL_init();
        System.out.println("Task: "+taskSpec);
        TaskSpecVRLGLUE3 theTaskSpec = new TaskSpecVRLGLUE3(taskSpec);
        numObjectives = theTaskSpec.getNumOfObjectives();
    	
    	// configure agent params
        String agentMessageString = "set_learning_parameters" + " " + settings.ALPHA + " " + settings.LAMBDA + " " + settings.GAMMA + " " + settings.EXPLORATION;
        RLGlue.RL_agent_message(agentMessageString);
        RLGlue.RL_agent_message(PARAM_CHANGE_STRING + " " + settings.EXPLORATION_PARAMETER + " " + settings.NUM_ONLINE_EPISODES_PER_TRIAL);
        
        
        // get agent 
        String agentName = RLGlue.RL_agent_message("get_agent_name");
    	String envName = RLGlue.RL_env_message("get env name");
    	
    	// create excel sheet
    	//the colon characters weren't being outputted correctly so I removed them.
    	String timeStamp = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss").format(new java.util.Date());
    	final String fileName = settings.OUTPATH + "/" + settings.ENV + "("+ envName + ")"+"-"+settings.AGENT+ "(" + agentName + ")" + "-"+METHOD_PREFIX.get(settings.EXPLORATION)+settings.EXPLORATION_PARAMETER+"-alpha"+settings.ALPHA+"-lambda"+settings.LAMBDA + "-dt" + timeStamp;
    	
    	if(settings.FORMAT.equals("excel")) {
    		excel = new JxlExcelWriter(fileName);
    	} else if (settings.FORMAT.equals("csv")) {
    		csv = new CSVWriter(fileName);
    		csv.writeLinesRaw(new String[] {"Description,Episode number,R^P,R^A,R^*"});

    	}
    	System.out.println("**********************************************");
    	System.out.println("RUNNING " + " WITH "+settings.AGENT+"("+agentName+") in  "+envName);
        
        // run the trials
        for (int trial=0; trial<settings.NUM_TRIALS; trial++)
        {
        	switch(settings.FORMAT) {
		    	case "csv":
					//csv.writeLinesRaw(new String[] {"Trial:"+trial+",0,0,0,0"});
		    		break;
		    	case "excel":
		        	// start new excel sheet and include header row
		        	excel.moveToNewSheet("Trial"+trial, trial);
		        	excel.writeNextRowText(" &Episode number&R^P&R^A&R^*");
		    		break;
        	}

        	// run the trial and save the results to the spreadsheet
        	System.out.println("Trial " + trial);
            RLGlue.RL_agent_message("start_new_trial");
    		for (int episodeNum=0; episodeNum<settings.NUM_ONLINE_EPISODES_PER_TRIAL; episodeNum++)
    		{
    			//System.out.println("Before running episode");
    			Reward er = runEpisode(episodeNum, settings.MAX_EPISODE_LENGTH);
    			//System.out.println("After running episode");
    			if(settings.FORMAT.equals("excel")) {
    				saveReward("Online&"+(1+episodeNum),er);
    			} else if (settings.FORMAT.equals("csv")){
    				saveReward("Online,"+(1+episodeNum),er);
    			}
    		}
            RLGlue.RL_agent_message("freeze_learning");		// turn off learning and exploration for offline assessment of the final policy    		
            for (int episodeNum=0; episodeNum<settings.NUM_OFFLINE_EPISODES_PER_TRIAL; episodeNum++)
    		{
    			// turn on debugging for the final offline run
            	if (episodeNum==settings.NUM_OFFLINE_EPISODES_PER_TRIAL-1)
            	{
	            	//RLGlue.RL_env_message("start-debugging");
	    			//RLGlue.RL_agent_message("start-debugging");
            	}
            	if(settings.FORMAT.equals("excel")) {
            		saveReward("Offline&"+(1+episodeNum),runEpisode(episodeNum,settings.MAX_EPISODE_LENGTH));
            	} else if(settings.FORMAT.equals("csv")){
            		saveReward("Offline,"+(1+episodeNum),runEpisode(episodeNum,settings.MAX_EPISODE_LENGTH));
            	}
    		}
        	RLGlue.RL_env_message("stop-debugging");
			RLGlue.RL_agent_message("stop-debugging");           
            switch(settings.FORMAT) {
		    	case "csv":
		    		//csv.writeLinesRaw(new String[] {"finsihed trial "+trial});
		    		break;
		    	case "excel":
		            // add two rows at the end of the worksheet to summarise the means over all online and offline episodes
		            String formulas = "AVERAGE(" + excel.getAddress(2,1) + ":" + excel.getAddress(2,settings.NUM_ONLINE_EPISODES_PER_TRIAL) + ")"
		            					+ "&AVERAGE(" + excel.getAddress(3,1) + ":" + excel.getAddress(3,settings.NUM_ONLINE_EPISODES_PER_TRIAL) + ")"
		            					+ "&AVERAGE(" + excel.getAddress(4,1) + ":" + excel.getAddress(4,settings.NUM_ONLINE_EPISODES_PER_TRIAL) + ")";
		            excel.writeNextRowTextAndFormula("Mean over all online episodes& ", formulas);
		            formulas = "AVERAGE(" + excel.getAddress(2,settings.NUM_ONLINE_EPISODES_PER_TRIAL+1) + ":" + excel.getAddress(2,settings.NUM_ONLINE_EPISODES_PER_TRIAL+settings.NUM_OFFLINE_EPISODES_PER_TRIAL) + ")"         
		            			+ "&AVERAGE(" + excel.getAddress(3,settings.NUM_ONLINE_EPISODES_PER_TRIAL+1) + ":" + excel.getAddress(3,settings.NUM_ONLINE_EPISODES_PER_TRIAL+settings.NUM_OFFLINE_EPISODES_PER_TRIAL) + ")"
		            			+ "&AVERAGE(" + excel.getAddress(4,settings.NUM_ONLINE_EPISODES_PER_TRIAL+1) + ":" + excel.getAddress(4,settings.NUM_ONLINE_EPISODES_PER_TRIAL+settings.NUM_OFFLINE_EPISODES_PER_TRIAL) + ")";
		            excel.writeNextRowTextAndFormula("Mean over all offline episodes& ", formulas);
		    		break;
            }

        }
        
        switch(settings.FORMAT) {
	    	case "csv:":
	    		break;
	    	case "excel":
		        // make summary sheet - the +2 on the number of rows is to capture the online and offline means as well as the individual episode results
		        excel.makeSummarySheet(settings.NUM_TRIALS, "R^P&R^A&R^*", 2, 1, numObjectives, settings.NUM_ONLINE_EPISODES_PER_TRIAL+settings.NUM_OFFLINE_EPISODES_PER_TRIAL+2);           
		        // make another sheet which collates the online and off-line per episode means across all trials, for later use in doing t-tests
		        excel.moveToNewSheet("Collated", settings.NUM_TRIALS+1); // put this after the summary sheet
		        excel.writeNextRowText("Trial&R^P Online mean&R^A Online mean&R^* Online mean&R^P Offline mean&R^A Offline mean&R^* Offline mean");
		        final int ONLINE_ROW = settings.NUM_ONLINE_EPISODES_PER_TRIAL+settings.NUM_OFFLINE_EPISODES_PER_TRIAL+1;
		        final int OFFLINE_ROW = ONLINE_ROW+1;
		        for (int i=0; i<settings.NUM_TRIALS; i++)
		        {
		        	String text = Integer.toString(i);
		        	String lookups = excel.getAddress(i,2,ONLINE_ROW) + "&" + excel.getAddress(i,3,ONLINE_ROW) + "&" + excel.getAddress(i,4,ONLINE_ROW) + "&" 
		        					+ excel.getAddress(i,2,OFFLINE_ROW) + "&" + excel.getAddress(i,3,OFFLINE_ROW) + "&" + excel.getAddress(i,4,OFFLINE_ROW);
		        	excel.writeNextRowTextAndFormula(text, lookups);
		        }
		        
		        //TODO write experiment settings into excel file
		        
		        excel.closeFile();
		        break;
        }
        
        RLGlue.RL_cleanup();
        System.out.println("********************************************** Experiment finished");
        return fileName;
    }

	public static String main(AgentInterface agent, EnvironmentInterface env, ExperimentSettings settings) {
		LocalExperiment locExp = new LocalExperiment(agent, env, settings);
		String outputfile = locExp.runExperiment();
		return outputfile;
	}
}

