// Modified from TLOExplorationExperiment in Dec 2018 to support initial experiments
// with AI safety side-effective sensitive agents

package experiments;

import org.rlcommunity.rlglue.codec.RLGlue;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.types.Observation_action;
import org.rlcommunity.rlglue.codec.types.Reward;
import org.rlcommunity.rlglue.codec.types.Reward_observation_action_terminal;

import tools.valuefunction.TLO_LookupTable;
import tools.spreadsheet.*;
import agents.TLO_Agent;

import java.io.*;

public class SideEffectExperimentWithExcelOutput 
{

    private int whichEpisode = 0;
    private int numObjectives;

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
    //private int EXPLORATION = TLO_LookupTable.SOFTMAX_ADDITIVE_EPSILON;
    //private final String METHOD_PREFIX = "SOFTMAX_E";
    //private final String PARAM_CHANGE_STRING = "set_softmax_parameters";
    //private double EXPLORATION_PARAMETER = 10;
    
    // enable this group of declarations for softmax-epsilon exploration		    
    private int EXPLORATION = TLO_LookupTable.SOFTMAX_TOURNAMENT;
    private final String METHOD_PREFIX = "SOFTMAX_T";
    private final String PARAM_CHANGE_STRING = "set_softmax_parameters";
    private double EXPLORATION_PARAMETER = 10; // usually 10
    

	// alter these declarations to match the Environment being used	
    // Settings for the BreakableBottles task
	    //private final String ENVIRONMENT_PREFIX = "Breakable";
	    //private final int NUM_ONLINE_EPISODES_PER_TRIAL = 5000;
	    //private final int NUM_OFFLINE_EPISODES_PER_TRIAL = 100;
	    //private final int MAX_EPISODE_LENGTH = 1000;
    // Settings for the UnbreakableBottles task
	    private final String ENVIRONMENT_PREFIX = "Unbreakable";
	    private final int NUM_ONLINE_EPISODES_PER_TRIAL = 5000;
	    private final int NUM_OFFLINE_EPISODES_PER_TRIAL = 100;
	    private final int MAX_EPISODE_LENGTH = 1000;
    // Settings for the Sokoban task
    	//private final String ENVIRONMENT_PREFIX = "Sokoban";
    	//private final int NUM_ONLINE_EPISODES_PER_TRIAL = 5000;
    	//private final int NUM_OFFLINE_EPISODES_PER_TRIAL = 1;
    	//private final int MAX_EPISODE_LENGTH = 1000;
    // Settings for the Doors task
    	//private final String ENVIRONMENT_PREFIX = "Doors";
    	//private final int NUM_ONLINE_EPISODES_PER_TRIAL = 5000;
    	//private final int NUM_OFFLINE_EPISODES_PER_TRIAL = 1;
    	//private final int MAX_EPISODE_LENGTH = 1000;
	    
	private final String FILENAME_PREFIX = ENVIRONMENT_PREFIX + "-";
	private ExcelWriter excel;
  
    // store the data for the most recent Reward. The strings indicates a value or label to be written in the first two columns
    private void saveReward(String labels, Reward r)
    {
    	excel.writeNextRowTextAndNumbers(labels, r.doubleArray);
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
        // configure agent, set up files etc
        String agentMessageString = "set_learning_parameters" + " " + ALPHA + " " + LAMBDA + " " + GAMMA + " " + EXPLORATION;
        RLGlue.RL_agent_message(agentMessageString);
    	String agentName = RLGlue.RL_agent_message("get_agent_name");
    	final String fileName = FILENAME_PREFIX+"-"+agentName+"-"+METHOD_PREFIX+EXPLORATION_PARAMETER+"-alpha"+ALPHA+"-lambda"+LAMBDA;
    	excel = new JxlExcelWriter(fileName);
    	
    	
        RLGlue.RL_agent_message(PARAM_CHANGE_STRING + " " + EXPLORATION_PARAMETER + " " + NUM_ONLINE_EPISODES_PER_TRIAL);       
        // run the trials
        for (int trial=0; trial<NUM_TRIALS; trial++)
        {
        	// start new excel sheet and include header row
        	excel.moveToNewSheet("Trial"+trial, trial);
        	excel.writeNextRowText(" &Episode number&R^P&R^A&R^*");
        	// run the trial and save the results to the spreadsheet
        	System.out.println("Trial " + trial);
            RLGlue.RL_agent_message("start_new_trial");
    		for (int episodeNum=0; episodeNum<NUM_ONLINE_EPISODES_PER_TRIAL; episodeNum++)
    		{
    			saveReward("Online&"+(1+episodeNum),runEpisode(MAX_EPISODE_LENGTH));
    		}
            RLGlue.RL_agent_message("freeze_learning");		// turn off learning and exploration for offline assessment of the final policy    		
            for (int episodeNum=0; episodeNum<NUM_OFFLINE_EPISODES_PER_TRIAL; episodeNum++)
    		{
    			// turn on debugging for the final offline run
            	if (episodeNum==NUM_OFFLINE_EPISODES_PER_TRIAL-1)
            	{
	            	//RLGlue.RL_env_message("start-debugging");
	    			//RLGlue.RL_agent_message("start-debugging");
            	}
    			saveReward("Offline&"+(1+episodeNum),runEpisode(MAX_EPISODE_LENGTH));
    		}
        	RLGlue.RL_env_message("stop-debugging");
			RLGlue.RL_agent_message("stop-debugging");           
            // add two rows at the end of the worksheet to summarise the means over all online and offline episodes
            String formulas = "AVERAGE(" + excel.getAddress(2,1) + ":" + excel.getAddress(2,NUM_ONLINE_EPISODES_PER_TRIAL) + ")"
            					+ "&AVERAGE(" + excel.getAddress(3,1) + ":" + excel.getAddress(3,NUM_ONLINE_EPISODES_PER_TRIAL) + ")"
            					+ "&AVERAGE(" + excel.getAddress(4,1) + ":" + excel.getAddress(4,NUM_ONLINE_EPISODES_PER_TRIAL) + ")";
            excel.writeNextRowTextAndFormula("Mean over all online episodes& ", formulas);
            formulas = "AVERAGE(" + excel.getAddress(2,NUM_ONLINE_EPISODES_PER_TRIAL+1) + ":" + excel.getAddress(2,NUM_ONLINE_EPISODES_PER_TRIAL+NUM_OFFLINE_EPISODES_PER_TRIAL) + ")"         
            			+ "&AVERAGE(" + excel.getAddress(3,NUM_ONLINE_EPISODES_PER_TRIAL+1) + ":" + excel.getAddress(3,NUM_ONLINE_EPISODES_PER_TRIAL+NUM_OFFLINE_EPISODES_PER_TRIAL) + ")"
            			+ "&AVERAGE(" + excel.getAddress(4,NUM_ONLINE_EPISODES_PER_TRIAL+1) + ":" + excel.getAddress(4,NUM_ONLINE_EPISODES_PER_TRIAL+NUM_OFFLINE_EPISODES_PER_TRIAL) + ")";
            excel.writeNextRowTextAndFormula("Mean over all offline episodes& ", formulas);
        }
        // make summary sheet - the +2 on the number of rows is to capture the online and offline means as well as the individual episode results
        excel.makeSummarySheet(NUM_TRIALS, "R^P&R^A&R^*", 2, 1, numObjectives, NUM_ONLINE_EPISODES_PER_TRIAL+NUM_OFFLINE_EPISODES_PER_TRIAL+2);           
        // make another sheet which collates the online and off-line per episode means across all trials, for later use in doing t-tests
        excel.moveToNewSheet("Collated", NUM_TRIALS+1); // put this after the summary sheet
        excel.writeNextRowText("Trial&R^P Online mean&R^A Online mean&R^* Online mean&R^P Offline mean&R^A Offline mean&R^* Offline mean");
        final int ONLINE_ROW = NUM_ONLINE_EPISODES_PER_TRIAL+NUM_OFFLINE_EPISODES_PER_TRIAL+1;
        final int OFFLINE_ROW = ONLINE_ROW+1;
        for (int i=0; i<NUM_TRIALS; i++)
        {
        	String text = Integer.toString(i);
        	String lookups = excel.getAddress(i,2,ONLINE_ROW) + "&" + excel.getAddress(i,3,ONLINE_ROW) + "&" + excel.getAddress(i,4,ONLINE_ROW) + "&" 
        					+ excel.getAddress(i,2,OFFLINE_ROW) + "&" + excel.getAddress(i,3,OFFLINE_ROW) + "&" + excel.getAddress(i,4,OFFLINE_ROW);
        	excel.writeNextRowTextAndFormula(text, lookups);
        }
        excel.closeFile();
        RLGlue.RL_cleanup();
        System.out.println("********************************************** Experiment finished");
    }

    public static void main(String[] args) {
    	SideEffectExperimentWithExcelOutput theExperiment = new SideEffectExperimentWithExcelOutput();
        theExperiment.runExperiment();
        System.exit(0); // shut down the experiment + hopefully everything else launched by the Driver program (server, agent, environment)
    }
}

