/*
 * Copyright 2008 Brian Tanner
 * http://rl-glue-ext.googlecode.com/
 * brian@tannerpages.com
 * http://brian.tannerpages.com
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * 
 *  $Revision: 676 $
 *  $Date: 2009-02-08 18:15:04 -0700 (Sun, 08 Feb 2009) $
 *  $Author: brian@tannerpages.com $
 *  $HeadURL: http://rl-glue-ext.googlecode.com/svn/trunk/projects/codecs/Java/examples/skeleton-sample/SkeletonExperiment.java $
 * 
 */

package experiments;

import org.rlcommunity.rlglue.codec.RLGlue;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.types.Observation_action;
import org.rlcommunity.rlglue.codec.types.Reward;
import org.rlcommunity.rlglue.codec.types.Reward_observation_action_terminal;
import java.io.*;

// Modified from Exploration Experiment in Sept 2018 to support initial experiments
// with AI safety side-effective sensitive agents
public class SideEffectExperiment_Old 
{

    private int whichEpisode = 0;
    private int numObjectives;
    private double rewardPerEpisode[][][];
    private final int NUM_TRIALS = 20;
    private final int NUM_EPISODES_PER_TRIAL = 5000; //5100;
    private final int MAX_EPISODE_LENGTH = 1000;
    
    /* Run One Episode of length maximum cutOff*/
    private Reward runEpisode(int stepLimit) {
        int terminal = RLGlue.RL_episode(stepLimit);

        int totalSteps = RLGlue.RL_num_steps();
        Reward totalReward = RLGlue.RL_return();
        return totalReward;
    }

    public void runExperiment() {
    	// set up data structure to store reward history
        String taskSpec = RLGlue.RL_init();
        TaskSpecVRLGLUE3 theTaskSpec = new TaskSpecVRLGLUE3(taskSpec);
        numObjectives = theTaskSpec.getNumOfObjectives();
        rewardPerEpisode = new double[NUM_TRIALS][NUM_EPISODES_PER_TRIAL][numObjectives];
        
        // run the trials
        for (int trial=0; trial<NUM_TRIALS; trial++)
        {
        	System.out.println("Trial " + trial);
            RLGlue.RL_agent_message("start_new_trial");
			RLGlue.RL_agent_message("stop-debugging");
			//RLGlue.RL_env_message("stop-debugging");
    		for (int episodeNum=0; episodeNum<NUM_EPISODES_PER_TRIAL; episodeNum++)
    		{
    			// this bit is for debugging only
    			/*if (episodeNum==NUM_EPISODES_PER_TRIAL-1)
    			{
    				RLGlue.RL_agent_message("start-debugging");
    				//RLGlue.RL_env_message("start-debugging");
    			}*/
    			Reward episodeReward = runEpisode(MAX_EPISODE_LENGTH);
    			for (int obj=0; obj<numObjectives; obj++)
    			{
    				rewardPerEpisode[trial][episodeNum][obj] = episodeReward.doubleArray[obj];
    			}
    		}
        }
        RLGlue.RL_cleanup();
        
        // save results
        try
        {	
        	PrintWriter outFile = new PrintWriter (new File("results.csv"));
    		for (int episodeNum=0; episodeNum<NUM_EPISODES_PER_TRIAL; episodeNum++)
    		{
                for (int trial=0; trial<NUM_TRIALS; trial++)
                {
        			outFile.print("Trial #" + trial + "," + episodeNum +",");
        			for (int obj=0; obj<numObjectives; obj++)
        			{
        				outFile.print(rewardPerEpisode[trial][episodeNum][obj]+ ",");
        			}
        			outFile.print(",");
            	}
        		outFile.println();
            }  
            outFile.close();
        }
        catch (IOException e)
        {
        	System.out.println("Error saving to file");
        }      
        System.out.println("********************************************** Experiment finished");
    }

    public static void main(String[] args) {
    	SideEffectExperiment_Old theExperiment = new SideEffectExperiment_Old();
        theExperiment.runExperiment();
        System.exit(0); // shut down the experiment + hopefully everything else launched by the Driver program (server, agent, environment)
    }
}
