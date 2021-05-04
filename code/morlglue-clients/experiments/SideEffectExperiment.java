// Modified from TLOExplorationExperiment in Dec 2018 to support initial experiments
// with AI safety side-effective sensitive agents

package experiments;

import org.rlcommunity.rlglue.codec.RLGlue;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.types.Observation_action;
import org.rlcommunity.rlglue.codec.types.Reward;
import org.rlcommunity.rlglue.codec.types.Reward_observation_action_terminal;

import tools.valuefunction.TLO_LookupTable;
import agents.TLO_Agent;

import java.io.*;

public class SideEffectExperiment 
{

    private int whichEpisode = 0;
    private int numObjectives;
    //private double rewardPerEpisode[][][];
    ExperimentDataHolder thisTrial, experimentMean, experimentMin, experimentMax;
    // the metric is simply the value of the 3rd (true) objective
	private final int METRIC = ExperimentDataHolder.LINEAR_WEIGHTED_SUM; 
	private final double WEIGHTS[] = {0,0,1};
	
    // alter these declarations to determine which form of learning is being used, learning parameters etc
    private final double ALPHA = 0.1;
    private final double LAMBDA = 0.95;
    private final double GAMMA = 1.0;
    private final int NUM_TRIALS = 20;

    // enable this group of declarations for egreedy exploration
   	//private final int EXPLORATION = TLO_LookupTable.EGREEDY;
    //private final String METHOD_PREFIX = "EGREEDY";
    //private final String PARAM_CHANGE_STRING = "set_egreedy_parameters";
    //private double EXPLORATION_PARAMETER = 0.9;
    // enable this group of declarations for softmax-epsilon exploration		    
    private int EXPLORATION = TLO_LookupTable.SOFTMAX_ADDITIVE_EPSILON;
    private final String METHOD_PREFIX = "SOFTMAX_E";
    private final String PARAM_CHANGE_STRING = "set_softmax_parameters";
    private double EXPLORATION_PARAMETER = 10;
    

	// alter these declarations to match the Environment being used	
    // Settings for the Sokoban task
	    //private final String ENVIRONMENT_PREFIX = "Sokoban";
	    //private final int NUM_ONLINE_EPISODES_PER_TRIAL = 5000;
	    //private final int NUM_OFFLINE_EPISODES_PER_TRIAL = 1;
	    //private final int MAX_EPISODE_LENGTH = 1000;
    // Settings for the DamageableBoxes task
    	private final String ENVIRONMENT_PREFIX = "DamageableBoxes";
    	private final int NUM_ONLINE_EPISODES_PER_TRIAL = 20000;
    	private final int NUM_OFFLINE_EPISODES_PER_TRIAL = 1;
    	private final int MAX_EPISODE_LENGTH = 1000;
    // Settings for the BreakableBottles task
	    //private final String ENVIRONMENT_PREFIX = "Breakable";
	    //private final int NUM_ONLINE_EPISODES_PER_TRIAL = 5000;
	    //private final int NUM_OFFLINE_EPISODES_PER_TRIAL = 100;
	    //private final int MAX_EPISODE_LENGTH = 1000;
    // Settings for the UnbreakableBottles task
	    //private final String ENVIRONMENT_PREFIX = "Unbreakable";
	    //private final int NUM_ONLINE_EPISODES_PER_TRIAL = 5000;
	    //private final int NUM_OFFLINE_EPISODES_PER_TRIAL = 100;
	    //private final int MAX_EPISODE_LENGTH = 1000;
	    
	private final String FILENAME_PREFIX = ENVIRONMENT_PREFIX + "-";
  
    // store the data for the most recent Reward. o indicates if this is an online or offline episode
    private void processReward(Reward r, int o)
    {
		thisTrial.setEpisodeData(o, r.doubleArray, WEIGHTS);
    }    
	    
    // Run One Episode of length maximum cutOff
    private Reward runEpisode(int stepLimit) {
        int terminal = RLGlue.RL_episode(stepLimit);
        int totalSteps = RLGlue.RL_num_steps();
        Reward totalReward = RLGlue.RL_return();
        return totalReward;
    }

    public void runExperiment() {
    	// set up data structures to store reward history
        String taskSpec = RLGlue.RL_init();
        TaskSpecVRLGLUE3 theTaskSpec = new TaskSpecVRLGLUE3(taskSpec);
        numObjectives = theTaskSpec.getNumOfObjectives();
        thisTrial = new ExperimentDataHolder(numObjectives, NUM_ONLINE_EPISODES_PER_TRIAL, NUM_OFFLINE_EPISODES_PER_TRIAL, METRIC);
        experimentMean = new ExperimentDataHolder(numObjectives, NUM_ONLINE_EPISODES_PER_TRIAL, NUM_OFFLINE_EPISODES_PER_TRIAL, METRIC);
        experimentMin = new ExperimentDataHolder(numObjectives, NUM_ONLINE_EPISODES_PER_TRIAL, NUM_OFFLINE_EPISODES_PER_TRIAL, METRIC);
        experimentMax = new ExperimentDataHolder(numObjectives, NUM_ONLINE_EPISODES_PER_TRIAL, NUM_OFFLINE_EPISODES_PER_TRIAL, METRIC);
        // configure agent, set up files etc
        String agentMessageString = "set_learning_parameters" + " " + ALPHA + " " + LAMBDA + " " + GAMMA + " " + EXPLORATION;
        RLGlue.RL_agent_message(agentMessageString);
        try
        {
        	String agentName = RLGlue.RL_agent_message("get_agent_name");
        	final String MAIN_DIRECTORY = FILENAME_PREFIX+"-"+agentName+"-"+METHOD_PREFIX+EXPLORATION_PARAMETER+"-alpha"+ALPHA+"-lambda"+LAMBDA;
        	new File(MAIN_DIRECTORY).mkdirs();
	        RLGlue.RL_agent_message(PARAM_CHANGE_STRING + " " + EXPLORATION_PARAMETER + " " + NUM_ONLINE_EPISODES_PER_TRIAL);       
	        // run the trials
	        experimentMean.clearData(); experimentMin.clearData(); experimentMax.clearData();
	        for (int trial=0; trial<NUM_TRIALS; trial++)
	        {
            	BufferedWriter trialFile = new BufferedWriter(new FileWriter(MAIN_DIRECTORY + "/Trial " + trial + ".CSV"));
	        	System.out.println("Trial " + trial);
	        	thisTrial.clearData();
	            RLGlue.RL_agent_message("start_new_trial");
	    		for (int episodeNum=0; episodeNum<NUM_ONLINE_EPISODES_PER_TRIAL; episodeNum++)
	    		{
	    			processReward(runEpisode(MAX_EPISODE_LENGTH),ExperimentDataHolder.ONLINE);
	    		}
	            RLGlue.RL_agent_message("freeze_learning");		// turn off learning and exploration for offline assessment of the final policy    		
	    		for (int episodeNum=0; episodeNum<NUM_OFFLINE_EPISODES_PER_TRIAL; episodeNum++)
	    		{
	    			processReward(runEpisode(MAX_EPISODE_LENGTH),ExperimentDataHolder.OFFLINE);
	    		}
	            experimentMean.updateAllMetrics(thisTrial, ExperimentDataHolder.CALC_MEAN);
	            experimentMin.updateAllMetrics(thisTrial, ExperimentDataHolder.CALC_MIN);
	            experimentMax.updateAllMetrics(thisTrial, ExperimentDataHolder.CALC_MAX);
	    		thisTrial.saveData(trialFile);
	    		trialFile.close();	    		
	        }
	        // save the stats summarising results over all trials
        	// save mean results
        	BufferedWriter summaryFile = new BufferedWriter(new FileWriter(MAIN_DIRECTORY + "/ALL_TRIALS_MEAN.CSV"));
        	experimentMean.saveData(summaryFile);
        	summaryFile.close();
        	// save min results
        	summaryFile = new BufferedWriter(new FileWriter(MAIN_DIRECTORY + "/ALL_TRIALS_MIN.CSV"));
        	experimentMin.saveData(summaryFile);
        	summaryFile.close();
        	// save mean results
        	summaryFile = new BufferedWriter(new FileWriter(MAIN_DIRECTORY + "/ALL_TRIALS_MAX.CSV"));
        	experimentMax.saveData(summaryFile);
        	summaryFile.close();
	        
        } // end try
        catch (IOException e)
        {
        	System.out.println("File IO error: " + e);
        }       
        RLGlue.RL_cleanup();
        System.out.println("********************************************** Experiment finished");
    }

    public static void main(String[] args) {
    	SideEffectExperiment theExperiment = new SideEffectExperiment();
        theExperiment.runExperiment();
        System.exit(0); // shut down the experiment + hopefully everything else launched by the Driver program (server, agent, environment)
    }
}

