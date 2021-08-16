package experiments;

import org.rlcommunity.rlglue.codec.RLGlue;
import org.rlcommunity.rlglue.codec.types.Reward;

public class Experiment {

    private int whichEpisode = 0;
    


    public void steeringQValue() throws Exception {
        
        for (int i = 0; i < 10; i++) {
            RLGlue.RL_init();
            runEpisode(800000);
            RLGlue.RL_cleanup();            
        }
        
    }    
    
    public void runExperiment() throws Exception {
        steeringQValue();
    }

    private void runEpisode(int stepLimit) {
        int terminal = RLGlue.RL_episode(stepLimit);

        Reward reward = RLGlue.RL_return();

        System.out.println("Episode " + whichEpisode + "\t " + " first reward \t" + reward.doubleArray[0] + " second reward \t" + reward.doubleArray[1]);

        whichEpisode++;
    }    

    public static void main(String[] args) throws Exception{
        Experiment theExperiment = new Experiment();
        theExperiment.runExperiment();

    }
}
