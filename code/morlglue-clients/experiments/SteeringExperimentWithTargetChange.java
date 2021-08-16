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

// Written by Peter Vamplew Dec 2014, based on the RL_Glue SkeletonExperiment class by Brian Tannner
// This is a variant of the original SteeringExperiment, in which the choice of target point is changed part-way through learning, to test the ability of
// the steering agent to adapt to the new target - this is simulating potential interaction with the user during the learning process
public class SteeringExperimentWithTargetChange{

    private int whichEpisode = 0;
    private int numObjectives;
    private double rewardPerEpisode[][][][];

    private double targetPoints[][] = {{20,-1},{20,-1},{20,-1}}; //DST-Mixed objective maxima  
    private double switchedPoints[][] = {{5.90613846605345,-2.08618845137974},{16.6770131525498,-10.6133764670363},{20.2078262852212,-16.2126521144337}}; //DST-Mixed optimistic targets + 1
    private final int NUM_TARGET_POINTS = targetPoints.length;
    private final int NUM_TRIALS = 10;
    private final int NUM_EPISODES_PER_TRIAL = 5100; // 5100 for DST, 1 for LinkedRings
    private int MAX_EPISODE_LENGTH = 1000; //1000 for DST, 5101 for LinkedRings
    private final int NUM_EPISODES_BEFORE_SWITCHING = 2000;
    
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
        rewardPerEpisode = new double[NUM_TRIALS][NUM_TARGET_POINTS][NUM_EPISODES_PER_TRIAL][numObjectives];
        
        // run the trials
        for (int trial=0; trial<NUM_TRIALS; trial++)
        {
        	System.out.println("Trial " + trial);
        	for (int targetPoint=0; targetPoint<NUM_TARGET_POINTS; targetPoint++)
        	{
        		System.out.println("Trial: " + trial + " Target #: " + targetPoint);
                String agentMessageString = "change_target_point";
    			for (int obj=0; obj<numObjectives; obj++)
    			{
    				agentMessageString = agentMessageString + " " + targetPoints[targetPoint][obj];
    			}
                RLGlue.RL_agent_message(agentMessageString);
                RLGlue.RL_agent_message("start_new_trial");
        		for (int episodeNum=0; episodeNum<NUM_EPISODES_PER_TRIAL; episodeNum++)
        		{
        			Reward episodeReward = runEpisode(MAX_EPISODE_LENGTH);
        			for (int obj=0; obj<numObjectives; obj++)
        			{
        				rewardPerEpisode[trial][targetPoint][episodeNum][obj] = episodeReward.doubleArray[obj];
        			}
                    if (episodeNum==NUM_EPISODES_BEFORE_SWITCHING-1) // -1 because episode numbering starts at 0
                    {
                		System.out.println("Trial: " + trial + " Switched Target #: " + targetPoint);
                    	// change to the switched target point, but don't reset any other settings - retain prior learning
	                    agentMessageString = "change_target_point";
	        			for (int obj=0; obj<numObjectives; obj++)
	        			{
	        				agentMessageString = agentMessageString + " " + switchedPoints[targetPoint][obj];
	        			}
	                    RLGlue.RL_agent_message(agentMessageString);
                    }
        		}
        	}
        }
        RLGlue.RL_cleanup();
        
        // save results
        try
        {	
        	PrintWriter outFile = new PrintWriter (new File("results.csv"));
            for (int trial=0; trial<NUM_TRIALS; trial++)
            {
        		for (int episodeNum=0; episodeNum<NUM_EPISODES_PER_TRIAL; episodeNum++)
        		{
        			outFile.print("Trial #" + trial + "," + episodeNum +",");
                	for (int targetPoint=0; targetPoint<NUM_TARGET_POINTS; targetPoint++)
                    {
            			for (int obj=0; obj<numObjectives; obj++)
            			{
            				outFile.print("," + rewardPerEpisode[trial][targetPoint][episodeNum][obj]);
            			}
            			outFile.print(",");
            		}
            		outFile.println();
            	}
        		outFile.println();
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
        SteeringExperimentWithTargetChange theExperiment = new SteeringExperimentWithTargetChange();
        theExperiment.runExperiment();
    }
}
