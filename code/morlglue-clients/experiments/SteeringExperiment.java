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
public class SteeringExperiment {

    private int whichEpisode = 0;
    private int numObjectives;
    private double rewardPerEpisode[][][][];
    // various targets for the DST-Mixed problem
    //private double targetPoints[][] = {{5.09386153394655,-3.91381154862026},{15.2229868474502,-11.9866235329637},{18.2921737147788,-16.7873478855663}}; // pess -1
    //private double targetPoints[][] = {{5.29693076697328,-3.45690577431013},{15.5864934237251,-11.6433117664819},{18.7710868573894,-16.6436739427832}}; // pess -.5
    //private double targetPoints[][] = {{5.5,-3},{15.95,-11.3},{19.25,-16.5}}; // neutral targets
    //private double targetPoints[][] = {{5.70306923302672,-2.54309422568987},{16.3135065762749,-10.9566882335181},{19.7289131426106,-16.3563260572168}}; // optimistic +.5
    //private double targetPoints[][] = {{5.90613846605345,-2.08618845137974},{16.6770131525498,-10.6133764670363},{20.2078262852212,-16.2126521144337}}; // optimistic +1
    //private double targetPoints[][] = {{6.10920769908017,-1.62928267706961},{17.0405197288247,-10.2700647005544},{20.6867394278317,-16.0689781716505}}; // optimistic +1.5
    //private double targetPoints[][] = {{6.3122769321069,-1.17237690275949},{17.4040263050996,-9.92675293407257},{21.1656525704423,-15.9253042288673}}; // optimistic +2
    //private double targetPoints[][] = {{20,-1}}; //DST-Mixed objective maxima  
    
    // various targets for the Linked Rings problem
    //private double targetPoints[][] = {{-0.707106781186548,1.29289321881345},{0.292893218813452,0.292893218813452},{1.29289321881345,-0.707106781186548}}; //Linked Rings pessimistic targets -1
    //private double targetPoints[][] = {{-0.353553390593274,1.64644660940673},{0.646446609406726,0.646446609406726},{1.64644660940673,-0.353553390593274}}; //Linked Rings pessimistic targets -.5
    private double targetPoints[][] = {{0,2},{1,1},{2,0}}; //Linked Rings neutral targets
    //private double targetPoints[][] = {{0.353553390593274,2.35355339059327},{1.35355339059327,1.35355339059327},{2.35355339059327,0.353553390593274}}; //Linked Rings optimistic targets + .5
    //private double targetPoints[][] = {{0.707106781186548,2.70710678118655},{1.70710678118655,1.70710678118655},{2.70710678118655,0.707106781186548}}; //Linked Rings optimistic targets + 1
    //private double targetPoints[][] = {{1.06066017177982,3.06066017177982},{2.06066017177982,2.06066017177982},{3.06066017177982,1.06066017177982}}; //Linked Rings optimistic targets + 1.5
    //private double targetPoints[][] = {{1.4142135623731,3.41421356237309},{2.41421356237309,2.41421356237309},{3.41421356237309,1.4142135623731}}; //Linked Rings optimistic targets + 2
    //private double targetPoints[][] = {{3,3}}; // Linked Rings objective maxima      
    
    // various targets for the NonRecurrentRings problem
    //private double targetPoints[][] = {{-0.957106781186548,0.542893218813452},{-0.207106781186548,-0.207106781186548},{0.542893218813452,-0.957106781186548}}; //NonRecurrentRings pessimistic targets -1
    //private double targetPoints[][] = {{-0.603553390593274,0.896446609406726},{0.146446609406726,0.146446609406726},{0.896446609406726,-0.603553390593274}}; //NonRecurrentRings pessimistic targets -.5
    //private double targetPoints[][] = {{-0.25,1.25},{0.5,0.5},{1.25,-0.25}}; //NonRecurrentRings neutral targets
    //private double targetPoints[][] = {{0.103553390593274,1.60355339059327},{0.853553390593274,0.853553390593274},{1.60355339059327,0.103553390593274}}; //NonRecurrentRings optimistic targets + .5
    //private double targetPoints[][] = {{0.457106781186548,1.95710678118655},{1.20710678118655,1.20710678118655},{1.95710678118655,0.457106781186548}}; //NonRecurrentRings optimistic targets + 1
    //private double targetPoints[][] = {{0.810660171779821,2.31066017177982},{1.56066017177982,1.56066017177982},{2.31066017177982,0.810660171779821}}; //NonRecurrentRings optimistic targets + 1.5
    //private double targetPoints[][] = {{1.1642135623731,2.66421356237309},{1.9142135623731,1.9142135623731},{2.66421356237309,1.1642135623731}}; //NonRecurrentRings optimistic targets + 2
    //private double targetPoints[][] = {{2,2}}; // Non-recurrent rings maximal point
    
    private final int NUM_TARGET_POINTS = targetPoints.length;
    private final int NUM_TRIALS = 10;
    private final int NUM_EPISODES_PER_TRIAL = 1;//5100; // 5100 for DST, 1 for LinkedRings
    private final int MAX_EPISODE_LENGTH = 5101; //1000 for DST, 5101 for LinkedRings
    
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
        	//System.out.println("Trial " + trial);
        	for (int targetPoint=0; targetPoint<NUM_TARGET_POINTS; targetPoint++)
        	{
        		//System.out.println("Trial: " + trial + " Target #: " + targetPoint);
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
        		}
        		//RLGlue.RL_agent_message("save_value_function ringsQ");
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
        SteeringExperiment theExperiment = new SteeringExperiment();
        theExperiment.runExperiment();
    }
}
