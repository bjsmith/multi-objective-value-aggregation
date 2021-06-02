import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.EnvironmentInterface;

import experiments.LocalExperiment.ExperimentSettings;

public abstract class MORL_Glue_Local_Base {
	
	public static interface AgentGenerator
	{
		public AgentInterface getAgent(String[] args);
	}
	
	public static interface EnvGenerator
	{
		public EnvironmentInterface getEnv(String[] args);
	}
	
	public static void plotting(ExperimentSettings settings, String[] files) {
		Runtime rt = Runtime.getRuntime();
		String pythonPath = "/Users/benjaminsmith/anaconda/envs/morl_env/bin/python"; // replace with your Python path that has numpy, pandas and matplotlib
		try {
			ArrayList<String> command = new ArrayList<String>(Arrays.asList(pythonPath,"learning_plot.py", "--name",
					settings.NAME, "--path", settings.OUTPATH,
					"--num_online", Integer.toString(settings.NUM_ONLINE_EPISODES_PER_TRIAL),"--num_offline",
					Integer.toString(settings.NUM_OFFLINE_EPISODES_PER_TRIAL), "--timestamp", "--files"));
			command.addAll(new ArrayList<String>(Arrays.asList(files)));
			String[] cmd_ar = new String[command.size()];
	        cmd_ar = command.toArray(cmd_ar);
			System.out.println("Running ");
			for(String part : cmd_ar) {
				System.out.print(part+" ");
			}
			
			Process p = rt.exec(cmd_ar);
	        String cmdOutput = null;
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader errorInput = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            // read the output from the command
            while ((cmdOutput = stdInput.readLine()) != null) {
                System.out.println(cmdOutput);
            }
            while ((cmdOutput = errorInput.readLine()) != null) {
                System.out.println(cmdOutput);
            }
			System.out.println("FINISHED PLOTTING");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
