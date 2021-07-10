// Implements an experiment to carry out multiple trials and runs of TLO with different exploration methods and parameters, saving
// results to multiple folders and text files
// Written by Peter Vamplew Nov 2015

package experiments;

import org.rlcommunity.rlglue.codec.RLGlue;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.types.Observation_action;
import org.rlcommunity.rlglue.codec.types.Reward;
import org.rlcommunity.rlglue.codec.types.Reward_observation_action_terminal;
import agents.TLO_Agent;

import tools.valuefunction.TLO_LookupTable;

import java.io.*;


public class TLOExplorationExperiment 
{ 
    // alter these declarations to determine which form of learning is being used, learning parameters etc
	    private final double ALPHA = 0.9;
	    private final double LAMBDA = 0.9;
	    private final double GAMMA = 1.0;
	    private final boolean TRACE_TYPE = TLO_Agent.WATKINS; // either .WATKINS (clear traces on non-greedy action) or .PENG (don't clear)
	    // constants related to the type of initial values being used
	    private final int NEUTRAL = 0;
	    private final int PESSIMISTIC = 1;
	    private final int OPTIMISTIC = 2;
	    private final int INIT_VALUE_TYPE = OPTIMISTIC;

	    private final int METRIC = ExperimentDataHolder.ADDITIVE_EPSILON;
	    // enable this group of declarations for egreedy exploration
		   	private final int EXPLORATION = TLO_LookupTable.EGREEDY;
		    private final String METHOD_PREFIX = "EGREEDY";
		    private final String PARAM_CHANGE_STRING = "set_egreedy_parameters";
		    private double EXPLORATION_PARAMETERS[] = {0.0, 0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0}; // also include 0.0 for optimistic trials */
		// enable this group of declarations for softmax-tournament exploration
		   	/*private int EXPLORATION = TLO_LookupTable.SOFTMAX_TOURNAMENT;
		    private final String METHOD_PREFIX = "SOFTMAX_T"; 
		    private final String PARAM_CHANGE_STRING = "set_softmax_parameters";
		    private double EXPLORATION_PARAMETERS[] = {50,25,10,5,2.5,1.25,1.0,0.5,0.25,0.1}; //*/
		// enable this group of declarations for softmax-epsilon exploration		    
		    /*private int EXPLORATION = TLO_LookupTable.SOFTMAX_ADDITIVE_EPSILON;
		    private final String METHOD_PREFIX = "SOFTMAX_E";
		    private final String PARAM_CHANGE_STRING = "set_softmax_parameters";
		    private double EXPLORATION_PARAMETERS[] = {50,25,10,5,2.5,1.25,1.0,0.5,0.25,0.1}; // */
		// enable this group of declarations for EOVF exploration - should only be used with an EOVF-compatible agent		    
		    /*private int EXPLORATION = TLO_LookupTable.EGREEDY;
		    private final String METHOD_PREFIX = "EOVF";
		    private final String PARAM_CHANGE_STRING = "set_eovf_parameters";
		    private double EXPLORATION_PARAMETERS[] = {0.9999999, 0.999999, 0.99999, 0.9999,0.999, 0.99, 0.95, 0.9};*/
	    
	// alter these declarations to match the Environment being used	
	    // Settings for the Deep Sea Treasure task
		    private final String ENVIRONMENT_PREFIX = "DST";
		    private final int NUM_ONLINE_EPISODES_PER_TRIAL = 5000;
		    private final int NUM_OFFLINE_EPISODES_PER_TRIAL = 1;
		    private final int MAX_EPISODE_LENGTH = 1000;
	    	private final double THRESHOLDS[][] = {{0}, {1.5}, {2.5}, {4}, {6}, {10}, {20}, {40}, {60}, {100}}; // DST thresholds
	    	private final double TARGET_POLICIES[][] = {{1,-1}, {2,-3}, {3,-5}, {5,-7}, {8,-8}, {16,-9}, {24,-13}, {50,-14}, {74,-17}, {124,-19}}; // DST target policy rewards
	    	private final double INIT_VALUE_OPTIONS[][] = {{0,0},{-1,-1000},{150,0}};//*/
		// Settings for the BonusWorld task
		    /*private final String ENVIRONMENT_PREFIX = "BONUS";
		    private final int NUM_ONLINE_EPISODES_PER_TRIAL = 5000;
		    private final int NUM_OFFLINE_EPISODES_PER_TRIAL = 1;
		    private final int MAX_EPISODE_LENGTH = 1000;
	    	private final double THRESHOLDS[][] = 		{{0, 8},	{2,8},		{4,8},		{10,12},		{12,16},		{16,16},		{16,12},		{12,10},		{8,4},		{8,2},		{8,0}}; // BW thresholds
	    	private final double TARGET_POLICIES[][] = 	{{1,9,-8},	{3,9,-10},	{5,9,-12},	{10,18,-14},	{14,18,-16},	{18,18,-18},	{18,14,-16},	{18,10,-14},	{9,5,-12},	{9,3,-10},	{9,1,-8}}; // BW target policy rewards
	    	private final double INIT_VALUE_OPTIONS[][] = {{0,0,0},{0,0,-40},{20,20,20}};*/
		// Settings for the Space Exploration task
		    /*private final String ENVIRONMENT_PREFIX = "SPACE";
		    private final int NUM_ONLINE_EPISODES_PER_TRIAL = 5000;
		    private final int NUM_OFFLINE_EPISODES_PER_TRIAL = 1;
		    private final int MAX_EPISODE_LENGTH = 1000;
	    	private final double THRESHOLDS[][] = 		{{5},{15},{25}}; // Space Exploration thresholds
	    	private final double TARGET_POLICIES[][] = 	{{10,-4},{20,-15},{30,-27}}; // Space Exploration target policy rewards
	    	private final double INIT_VALUE_OPTIONS[][] = {{0,0},{0,-40},{40,0}};
		// Settings for the Resource-Gathering-Episodic task
		    /*private final String ENVIRONMENT_PREFIX = "RGE";
		    private final int NUM_ONLINE_EPISODES_PER_TRIAL = 20000;
		    private final int NUM_OFFLINE_EPISODES_PER_TRIAL = 500;
		    private final int MAX_EPISODE_LENGTH = 1000;
		    //private final double THRESHOLDS[][] = {{0,0,0},{0,10,0},{0,0,10},{0,10,10},{-1,5,0},{-10,5,0},{-1,5,5},{-10,5,5}}; // Resource Gathering thresholds - original
		    private final double THRESHOLDS[][] = {{-0.05,0,0},{-0.05,9.5,0},{-0.05,0,9.5},{-0.05,9.5,9.5},{-1.5,5,0},{-8,5,0},{-1.5,5,5},{-8,5,5}}; // Resource Gathering thresholds - altered
		    private final double TARGET_POLICIES[][] = {{0,0,0,-1},{0,10,0,-12},{0,0,10,-10},{0,10,10,-18},{-1,9,0,-9.3},{-1.9,8.1,0,-7.23},{-1,9,9,-13.3},{-1.9,8.1,8.1,-10.47}}; // Resource Gathering target policy rewards
		    private final double INIT_VALUE_OPTIONS[][] = {{0,0,0,0},{-10,0,0,-20},{0,10,10,0}};*/
	    
	    
	// generic declarations which shouldn't need to be changed
	    private double INIT_Q_VALUES[] = INIT_VALUE_OPTIONS[INIT_VALUE_TYPE];
	    private int numObjectives;
	    ExperimentDataHolder thisRun, thisTrialMean, parameterMean, parameterMin, parameterMax, thisThresholdMean[], thisThresholdMin[], thisThresholdMax[]; 
	    private double currentTarget[];
	    private final int NUM_THRESHOLD_VARIATIONS = THRESHOLDS.length;
	    private final int NUM_TRIALS = 20;
	    private final String METRIC_NAME = "AdditiveE"; 
	    private final String FILENAME_PREFIX = ENVIRONMENT_PREFIX + "-" + METHOD_PREFIX + "-" + TLO_Agent.traceNameToString(TRACE_TYPE) + "-" + initName(INIT_VALUE_TYPE) + "-";
	    private final String MAIN_DIRECTORY = FILENAME_PREFIX+"alpha"+ALPHA+"-lambda"+LAMBDA;

	
    /* Run One Episode of length maximum cutOff*/
    private Reward runEpisode(int stepLimit) {
        int terminal = RLGlue.RL_episode(stepLimit);

        int totalSteps = RLGlue.RL_num_steps();
        Reward totalReward = RLGlue.RL_return();
        return totalReward;
    }
    
    // return a String describing the type of initial values being used
    private String initName(int type)
    {
    	switch (type)
    	{
    		case 0: return "NEUTRAL";
    		case 1: return "PESSIMISTIC"; 
    		case 2: return "OPTIMISTIC"; 
    		default: return "UNKNOWN";
    	}
    }
    
    // store the data for the most recent Reward. o indicates if this is an online or offline episode
    private void processReward(Reward r, int o)
    {
		thisRun.setEpisodeData(o, r.doubleArray, currentTarget);
    }
    
    // returns a String which concatenates together the values in the provided array, separated by the specified  character (will usually be ' ', '_' or ','), with a separator character at the start of the string
    private String doubleArrayToString(double array[], char separator)
    {
    	String returnString = "";
    	for (int i=0; i<array.length; i++)
    	{
    		returnString = returnString + separator + array[i];
    	}
    	return returnString;
    }
    

    public void runExperiment() {
    	// get task details and set up data structures to store reward history
        String taskSpec = RLGlue.RL_init();
        TaskSpecVRLGLUE3 theTaskSpec = new TaskSpecVRLGLUE3(taskSpec);
        numObjectives = theTaskSpec.getNumOfObjectives();
        thisRun = new ExperimentDataHolder(numObjectives, NUM_ONLINE_EPISODES_PER_TRIAL, NUM_OFFLINE_EPISODES_PER_TRIAL, METRIC);
        thisThresholdMean = new ExperimentDataHolder[NUM_THRESHOLD_VARIATIONS];
        thisThresholdMin = new ExperimentDataHolder[NUM_THRESHOLD_VARIATIONS];
        thisThresholdMax = new ExperimentDataHolder[NUM_THRESHOLD_VARIATIONS];
        for (int i=0; i<NUM_THRESHOLD_VARIATIONS; i++)
        {
        	thisThresholdMean[i] = new ExperimentDataHolder(numObjectives, NUM_ONLINE_EPISODES_PER_TRIAL, NUM_OFFLINE_EPISODES_PER_TRIAL, METRIC);
        	thisThresholdMin[i] = new ExperimentDataHolder(numObjectives, NUM_ONLINE_EPISODES_PER_TRIAL, NUM_OFFLINE_EPISODES_PER_TRIAL, METRIC);
        	thisThresholdMax[i] = new ExperimentDataHolder(numObjectives, NUM_ONLINE_EPISODES_PER_TRIAL, NUM_OFFLINE_EPISODES_PER_TRIAL, METRIC);	
        }
        thisTrialMean = new ExperimentDataHolder(numObjectives, NUM_ONLINE_EPISODES_PER_TRIAL, NUM_OFFLINE_EPISODES_PER_TRIAL, METRIC);
        parameterMean = new ExperimentDataHolder(numObjectives, NUM_ONLINE_EPISODES_PER_TRIAL, NUM_OFFLINE_EPISODES_PER_TRIAL, METRIC);
        parameterMin = new ExperimentDataHolder(numObjectives, NUM_ONLINE_EPISODES_PER_TRIAL, NUM_OFFLINE_EPISODES_PER_TRIAL, METRIC);
        parameterMax = new ExperimentDataHolder(numObjectives, NUM_ONLINE_EPISODES_PER_TRIAL, NUM_OFFLINE_EPISODES_PER_TRIAL, METRIC);
        // send details of the parameter setting to the agent
        String agentMessageString = "set_learning_parameters" + " " + ALPHA + " " + LAMBDA + " " + GAMMA + " " + EXPLORATION + " " + TRACE_TYPE + doubleArrayToString(INIT_Q_VALUES,' ');
        RLGlue.RL_agent_message(agentMessageString);
        try
        {
	        // for each value of the exploration parameter, run several trials with each involving a run on each set of thresholds
        	new File(MAIN_DIRECTORY).mkdirs();
        	BufferedWriter summaryFile = new BufferedWriter(new FileWriter(MAIN_DIRECTORY + "/" + FILENAME_PREFIX + "-AAAA-SUMMARY.CSV"));
        	summaryFile.write("Parameter setting, Online mean, Offline mean, Online min, Offline min, Online max, Offline max\n");
	        for (int paramIndex=0; paramIndex<EXPLORATION_PARAMETERS.length; paramIndex++)
	        {
	        	parameterMean.clearData();
	        	parameterMin.clearData();
	        	parameterMax.clearData();
	            for (int i=0; i<NUM_THRESHOLD_VARIATIONS; i++)
	            {
		        	thisThresholdMean[i].clearData();
		        	thisThresholdMin[i].clearData();
		        	thisThresholdMax[i].clearData();
	            }
	        	String parameterDirectory = MAIN_DIRECTORY + "/" + EXPLORATION_PARAMETERS[paramIndex];
	        	new File(parameterDirectory).mkdirs();
	        	BufferedWriter parameterMeanFile = new BufferedWriter(new FileWriter(parameterDirectory + "/" + FILENAME_PREFIX + EXPLORATION_PARAMETERS[paramIndex] + "-ALL-RUNS-MEAN.CSV"));
	        	BufferedWriter parameterMinFile = new BufferedWriter(new FileWriter(parameterDirectory + "/" + FILENAME_PREFIX + EXPLORATION_PARAMETERS[paramIndex] + "-ALL-RUNS-MIN.CSV"));
	        	BufferedWriter parameterMaxFile = new BufferedWriter(new FileWriter(parameterDirectory + "/" + FILENAME_PREFIX + EXPLORATION_PARAMETERS[paramIndex] + "-ALL-RUNS-MAX.CSV"));
	    		System.out.println("Exploration parameter variation #: " + paramIndex + " " + PARAM_CHANGE_STRING + " " + EXPLORATION_PARAMETERS[paramIndex]);
	        	RLGlue.RL_agent_message(PARAM_CHANGE_STRING + " " + EXPLORATION_PARAMETERS[paramIndex] + " " + NUM_ONLINE_EPISODES_PER_TRIAL);
	            for (int trial=0; trial<NUM_TRIALS; trial++)
		    	{
	            	System.out.println("\tTrial " + trial);
	            	String trialDirectory = parameterDirectory + "/Trial_" + trial;
	            	new File(trialDirectory).mkdirs();
	            	BufferedWriter trialMeanFile = new BufferedWriter(new FileWriter(trialDirectory + "/" + FILENAME_PREFIX + EXPLORATION_PARAMETERS[paramIndex] + "-ALL-THRESHOLDS-MEAN-RUN-" + trial + ".CSV"));
	            	thisTrialMean.clearData();
		            // do a run for each of the threshold settings
			    	for (int thresholdIndex=0; thresholdIndex<NUM_THRESHOLD_VARIATIONS; thresholdIndex++)
		            {
			    		// set thresholds and target policy
			    		currentTarget = TARGET_POLICIES[thresholdIndex];
			            agentMessageString = "change_thresholds" + doubleArrayToString(THRESHOLDS[thresholdIndex],' ');
			            String thresholdsString = doubleArrayToString(THRESHOLDS[thresholdIndex],'_');           
			    		System.out.println("\t\t" + agentMessageString);
			            RLGlue.RL_agent_message(agentMessageString);		            	
		            	// set up file and data-structure to hold the data for this run
		            	BufferedWriter runFile = new BufferedWriter(new FileWriter(trialDirectory + "/" + FILENAME_PREFIX + EXPLORATION_PARAMETERS[paramIndex] + "-THRESHOLDS" + thresholdsString + "-RUN-" + trial + ".CSV"));
		            	thisRun.clearData();
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
			    		thisRun.saveData(runFile);
			    		runFile.close();
			    		// update the summaries for the current trial and threshold
			    		thisTrialMean.updateAllMetrics(thisRun, ExperimentDataHolder.CALC_MEAN);
			            thisThresholdMean[thresholdIndex].updateAllMetrics(thisRun, ExperimentDataHolder.CALC_MEAN);
			            thisThresholdMin[thresholdIndex].updateAllMetrics(thisRun, ExperimentDataHolder.CALC_MIN);
			            thisThresholdMax[thresholdIndex].updateAllMetrics(thisRun, ExperimentDataHolder.CALC_MAX);	
		            }
		            thisTrialMean.saveData(trialMeanFile); trialMeanFile.close();
		            parameterMean.updateAllMetrics(thisTrialMean, ExperimentDataHolder.CALC_MEAN);
		            parameterMin.updateAllMetrics(thisTrialMean, ExperimentDataHolder.CALC_MIN);
		            parameterMax.updateAllMetrics(thisTrialMean, ExperimentDataHolder.CALC_MAX);
		    	}
	            // save results for this parameter to the parameter summary fields and also the overall summary file
	            parameterMean.saveData(parameterMeanFile); parameterMeanFile.close();	
	            parameterMin.saveData(parameterMinFile); parameterMinFile.close();
	            parameterMax.saveData(parameterMaxFile); parameterMaxFile.close();
	            summaryFile.write(""+EXPLORATION_PARAMETERS[paramIndex]);
	            parameterMean.saveSummary(summaryFile);
	            parameterMin.saveSummary(summaryFile);
	            parameterMax.saveSummary(summaryFile);
	            summaryFile.newLine();
	            // create and save to files the summary for each threshold setting for this parameter setting
	            for (int i=0; i<NUM_THRESHOLD_VARIATIONS; i++)
	            {
	            	String thresholdString = doubleArrayToString(THRESHOLDS[i],'_');
	            	String thresholdDirectory = parameterDirectory + "/Threshold_" + thresholdString;
	            	new File(thresholdDirectory).mkdirs();
	            	// save mean results
	            	BufferedWriter thresholdFile = new BufferedWriter(new FileWriter(thresholdDirectory + "/" + FILENAME_PREFIX + EXPLORATION_PARAMETERS[paramIndex] + thresholdString + "-ALL_TRIALS_MEAN.CSV"));
	            	thisThresholdMean[i].saveData(thresholdFile);
	            	thresholdFile.close();
	            	// save min results
	            	thresholdFile = new BufferedWriter(new FileWriter(thresholdDirectory + "/" + FILENAME_PREFIX + EXPLORATION_PARAMETERS[paramIndex] + thresholdString + "-ALL_TRIALS_MIN.CSV"));
	            	thisThresholdMin[i].saveData(thresholdFile);
	            	thresholdFile.close();
	            	// save mean results
	            	thresholdFile = new BufferedWriter(new FileWriter(thresholdDirectory + "/" + FILENAME_PREFIX + EXPLORATION_PARAMETERS[paramIndex] + thresholdString + "-ALL_TRIALS_MAX.CSV"));
	            	thisThresholdMax[i].saveData(thresholdFile);
	            	thresholdFile.close();
	            }
	        }
	        summaryFile.close();
        }
        catch (IOException e)
        {
        	System.out.println("File IO error: " + e);
        }
        RLGlue.RL_cleanup();
        System.out.println("Experiment finished");

    }

    public static void main(String[] args) {
    	TLOExplorationExperiment theExperiment = new TLOExplorationExperiment();
        theExperiment.runExperiment();
    }
}
