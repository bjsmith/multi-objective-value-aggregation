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

import tools.hypervolume.HVCalculator;

import java.io.*;

// Written by Peter Vamplew Aug 2015, based on the RL_Glue SkeletonExperiment class by Brian Tannner
public class HypervolumeExperiment {

    private int whichEpisode = 0;
    private int numObjectives;
    private double rewardPerEpisode[][][][][];
    private double weightsSet[][] = {{0.05, 0.95},{0.1,0.9},{0.15,0.85},{0.2,0.8},{0.25,0.75},{0.9,0.1}};
    private final int NUM_THRESHOLD_VARIATIONS = weightsSet.length;
    private final int NUM_TRIALS = 5;
    private final int NUM_EPISODES_PER_TRIAL = 10000;
    private final int MAX_EPISODE_LENGTH = 10000;
    private double referencePoint[] = {0, -30}; // reference point for hypervolume calculations - set according to the environment
    
    /* Run One Episode of length maximum cutOff*/
    private Reward runEpisode(int stepLimit) {
        int terminal = RLGlue.RL_episode(stepLimit);

        int totalSteps = RLGlue.RL_num_steps();
        Reward totalReward = RLGlue.RL_return();
        return totalReward;
    }

    public void runExperiment() {
    	// get task details and set up data structures to store reward history
        String taskSpec = RLGlue.RL_init();
        TaskSpecVRLGLUE3 theTaskSpec = new TaskSpecVRLGLUE3(taskSpec);
        numObjectives = theTaskSpec.getNumOfObjectives();
        double rewardPerEpisode[][][][] = new double[NUM_TRIALS][NUM_EPISODES_PER_TRIAL][NUM_THRESHOLD_VARIATIONS][numObjectives];
        double accumulatedHyperVolumeAcrossTrials[] = new double[NUM_EPISODES_PER_TRIAL];
        
        // run the trials
        for (int trial=0; trial<NUM_TRIALS; trial++)
        {
        	System.out.println("Trial " + trial);
        	for (int weightIndex=0; weightIndex<NUM_THRESHOLD_VARIATIONS; weightIndex++)
        	{
        		System.out.println("Trial: " + trial + " Weight variation #: " + weightIndex);
                String agentMessageString = "change_weights";
    			for (int obj=0; obj<numObjectives; obj++)
    			{
    				agentMessageString = agentMessageString + " " + weightsSet[weightIndex][obj];
    			}
                RLGlue.RL_agent_message(agentMessageString);
                RLGlue.RL_agent_message("start_new_trial");
        		for (int episodeNum=0; episodeNum<NUM_EPISODES_PER_TRIAL; episodeNum++)
        		{
        			Reward episodeReward = runEpisode(MAX_EPISODE_LENGTH);
        			for (int obj=0; obj<numObjectives; obj++)
        			{
        				rewardPerEpisode[trial][episodeNum][weightIndex][obj] = episodeReward.doubleArray[obj];
        			}
        		}
        		for (int episodeNum=0; episodeNum<NUM_EPISODES_PER_TRIAL; episodeNum++)
        		{
        			accumulatedHyperVolumeAcrossTrials[episodeNum] += HVCalculator.getVolumeFromArray(rewardPerEpisode[trial][episodeNum], referencePoint);		
        		}
        	}
        	
        }
        RLGlue.RL_cleanup();
        // save results
        try
        {	
        	PrintWriter outFile = new PrintWriter (new File("results.csv"));
        	// save the results from each trial
            for (int trial=0; trial<NUM_TRIALS; trial++)
            {
        		for (int episodeNum=0; episodeNum<NUM_EPISODES_PER_TRIAL; episodeNum++)
        		{
        			outFile.print("Trial #" + trial + "," + episodeNum +",");
        			for (int weightIndex=0; weightIndex<NUM_THRESHOLD_VARIATIONS; weightIndex++)
                    {
            			for (int obj=0; obj<numObjectives; obj++)
            			{
            				outFile.print("," + rewardPerEpisode[trial][episodeNum][weightIndex][obj]);
            			}
            			outFile.print(",");
            		}
            		outFile.println();
            	}
        		outFile.println();
            }  
            // save the summary of the hypervolume values            
            outFile.println("Mean hypervolume per episode over all trials");
    		for (int episodeNum=0; episodeNum<NUM_EPISODES_PER_TRIAL; episodeNum++)
    		{
    			outFile.println(episodeNum + "," + accumulatedHyperVolumeAcrossTrials[episodeNum]/NUM_TRIALS); 
    		}
            outFile.close();
        }
        catch (IOException e)
        {
        	System.out.println("Error saving to file");
        }
        
        
        System.out.println("Experiment finished");

    }

    public static void main(String[] args) {
        HypervolumeExperiment theExperiment = new HypervolumeExperiment();
        theExperiment.runExperiment();
    }
}
