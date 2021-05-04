// Designed for debugging purposes - run this using the UserControlledAgent to test new Environments

package experiments;

import org.rlcommunity.rlglue.codec.RLGlue;
import org.rlcommunity.rlglue.codec.types.Observation_action;
import org.rlcommunity.rlglue.codec.types.Reward;
import org.rlcommunity.rlglue.codec.types.Reward_observation_action_terminal;

/**
 *
 * @author Brian Tanner
 */
public class DebuggingExperiment {

    private int whichEpisode = 0;

    /* Run One Episode of length maximum cutOff*/
    private void runEpisode(int stepLimit) {
        int terminal = RLGlue.RL_episode(stepLimit);

        int totalSteps = RLGlue.RL_num_steps();
        Reward totalReward = RLGlue.RL_return();
        whichEpisode++;
    }

    public void runExperiment() {
        String taskSpec = RLGlue.RL_init();

        //argument to the runEpisode method is the number of time steps for the episode
        runEpisode(100);
        runEpisode(100);
        runEpisode(100);
        runEpisode(100);
        runEpisode(100);
        runEpisode(100);
        runEpisode(100);
        runEpisode(100);

        RLGlue.RL_cleanup();
    }

    public static void main(String[] args) {
        DebuggingExperiment theExperiment = new DebuggingExperiment();
        theExperiment.runExperiment();
    }
}
