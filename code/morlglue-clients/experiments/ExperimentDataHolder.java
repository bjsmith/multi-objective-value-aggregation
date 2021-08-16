// Basically just a container for storing data related to experiments, along with some functionality for writing this out to a file
// Can be used either for storing the results of one run, or as a summary of results across multiple runs
// Written by Peter Vamplew Nov 2015 to assist in the TLO exploration experiments
package experiments;

import java.io.*; // needed for file io

public class ExperimentDataHolder 
{
	public static final int ONLINE = 0;
	public static final int OFFLINE = 1;
	public static final int CALC_MEAN = 0;
	public static final int CALC_MIN = 1;
	public static final int CALC_MAX = 2;
	// define different scalar error metrics - for now its just the additive epsilon
	public static final int ADDITIVE_EPSILON = 0;
	public static final int LINEAR_WEIGHTED_SUM = 1;
	private String SCALAR_METRIC_NAME[] = {"Additive epsilon", "Linear weighted sum"};	
	
	private double episodeReward[][][]; // store the rewards on a per-episode basis across all online and offline episodes
	private double episodeScalarMetric[][]; // store the scalarised value on a per-episode basis
	private double meanEpisodeReward[][]; // store the mean over all online episodes, and over all offline episodes
	private double scalarisedMeanEpisodeReward[] = {0.0, 0.0}; // the scalarised value of the mean reward over all online episodes, and over all offline episodes
	private double meanEpisodeScalarMetric[] = {0.0, 0.0}; // the mean of the scalarised per-episode reward - this is probably less useful than the scalarisedMeanEpisodeReward but we may as well calculate it
	private int episodeCount[] = {0, 0};

	private int numObjectives;
	private int numUpdatesPerformed;
	private int metric;
	
	public ExperimentDataHolder(int numObjectives, int numOnlineEpisodes, int numOfflineEpisodes, int metric)
	{
		// set up per-episode data structures
		episodeReward = new double[2][][];
		episodeReward[ONLINE] = new double[numOnlineEpisodes][numObjectives];
		episodeReward[OFFLINE] = new double[numOfflineEpisodes][numObjectives];		
		episodeScalarMetric = new double[2][];
		episodeScalarMetric[ONLINE] = new double[numOnlineEpisodes];
		episodeScalarMetric[OFFLINE] = new double[numOfflineEpisodes];
		// set up the summary data-structures
		meanEpisodeReward = new double[2][numObjectives];
		// store remaining settings
		this.numObjectives = numObjectives;
		this.metric = metric;
		numUpdatesPerformed = 0;
	}
	
	// Clears data ready for a new run - only needs to clear the meanEpisodeReward, meanEpisodeScalarMetric and episodeCount values as the others will be overwritten
	// We are assuming that the experiment will always run all of the online and offline episodes before using any of the per-episode data
	public void clearData()
	{
		for (int i=0; i<2; i++)
		{
			for (int j=0; j<numObjectives; j++)
			{
				meanEpisodeReward[i][j] = 0.0;
			}
			meanEpisodeScalarMetric[i] = 0.0;
			episodeCount[i] = 0;
		}
		numUpdatesPerformed = 0;
	}
	
	// sets the data for the current episode, and updates relevant episodeCount and meanEpisodeReward values
	// The int parameter o indicates if this is an online or offline episode - should be one of the static final values
	public void setEpisodeData(int o, double thisReward[], double metricParameters[])
	{
		for (int i=0; i<thisReward.length; i++)
		{
			episodeReward[o][episodeCount[o]][i] = thisReward[i];
			meanEpisodeReward[o][i] = (meanEpisodeReward[o][i]*episodeCount[o] + thisReward[i])/(episodeCount[o]+1);
		}
		// calculate the metric for this individual episode
		double thisMetric = calculateMetric(thisReward, metricParameters);
		episodeScalarMetric[o][episodeCount[o]] = thisMetric;
		meanEpisodeScalarMetric[o] = (meanEpisodeScalarMetric[o]*episodeCount[o] + thisMetric)/(episodeCount[o]+1);
		// update the metric over all episodes up to this point
		scalarisedMeanEpisodeReward[o] = calculateMetric(meanEpisodeReward[o], metricParameters);
		episodeCount[o]++;
	}
	
	// Returns the appropriate updated value for the type of update requested.
	private double updateValue(double currentValue, double newValue, int operation)
	{
		if (numUpdatesPerformed==0) // no prior values so just use the newValue regardless of the required operation
		{
			return newValue;
		}
		else if (operation==CALC_MEAN)
		{
			return (numUpdatesPerformed*currentValue + newValue)/(numUpdatesPerformed+1);
		}
		else if (operation==CALC_MIN)
		{
			return Math.min(currentValue, newValue);
		}
		else // must be CALC_MAX
		{
			return Math.max(currentValue,  newValue);
		}
	}
	
	
	// Updates all the scalar metric values based on the requested operator and the data stored in the provided object
	public void updateAllMetrics(ExperimentDataHolder newData, int operation)
	{
		for (int o=0; o<2; o++)
		{
			for (int ep=0; ep<episodeScalarMetric[o].length; ep++)
			{
				episodeScalarMetric[o][ep] = updateValue(episodeScalarMetric[o][ep],newData.episodeScalarMetric[o][ep],operation);	
			}
			meanEpisodeScalarMetric[o] = updateValue(meanEpisodeScalarMetric[o],newData.meanEpisodeScalarMetric[o],operation);
			scalarisedMeanEpisodeReward[o] = updateValue(scalarisedMeanEpisodeReward[o],newData.scalarisedMeanEpisodeReward[o],operation);
		}
		numUpdatesPerformed++;
	}
	
	// Writes all of the data out to the provided BufferedWriter in CSV format to facilitate importing into Excel
	public void saveData(BufferedWriter b)
	{
		try
		{
			b.write(SCALAR_METRIC_NAME[metric] + ",Scalarised mean episodic reward,Mean of scalarised per-episode rewards");
			for (int i=0; i<numObjectives; i++)
				b.write(",Objective " + (i+1));
			b.write("\nOnline:," + scalarisedMeanEpisodeReward[ONLINE] + "," + meanEpisodeScalarMetric[ONLINE]);
			for (int i=0; i<numObjectives; i++)
				b.write("," + meanEpisodeReward[ONLINE][i]);
			b.write("\nOffline:," + scalarisedMeanEpisodeReward[OFFLINE] + "," + meanEpisodeScalarMetric[OFFLINE]);
			for (int i=0; i<numObjectives; i++)
				b.write("," + meanEpisodeReward[OFFLINE][i]);
			b.write("\nPer-episode results\n");
			b.write("Episode #," + SCALAR_METRIC_NAME[metric]);
			for (int i=0; i<numObjectives; i++)
			{
				b.write(",Objective " + (i+1));
			}
			b.newLine();
			for (int o=0; o<2; o++)
			{
				b.write((o==0 ? "Online episodes\n" : "Offline episodes\n"));
				for (int ep=0; ep<episodeScalarMetric[o].length; ep++)
				{
					b.write((ep+1) + "," + episodeScalarMetric[o][ep]);
					for (int i=0; i<numObjectives; i++)
					{
						b.write("," + episodeReward[o][ep][i]);
					}	
					b.newLine();
				}
			}
		}
		catch (IOException e)
		{
			System.out.println("Something went wrong writing to file " + e);
		}
	}
	
	// Writes out just the online and offline summary metrics to the next two columns of the specified BufferWriter - no new line
	public void saveSummary(BufferedWriter b)
	{
		try
		{
			b.write("," + scalarisedMeanEpisodeReward[ONLINE] + "," + scalarisedMeanEpisodeReward[OFFLINE]);
		}
		catch (IOException e)
		{
			System.out.println("Something went wrong writing to file " + e);
		}
	}
	
	// private functions from here down
	
    // Provides a scalar measure of the fitness of the most recent reward by calculating the additive-epsilon measure relative to the target policy's reward
    private double additiveEpsilon(double thisReward[], double target[])
    {
    	double score = 0.0;
    	for (int i=0; i<target.length; i++)
    	{
    		double diff = target[i] - thisReward[i];
    		if (diff>score)
    		{
    			score = diff;
    		}
    	}
    	return score;
    }
    
    // Provides a scalar measure of the fitness of the most recent reward using a linear
    // weighting of the objectives
    private double linearWeightedSum(double thisReward[], double weights[])
    {
    	double sum = 0.0;
    	for (int i=0; i<weights.length; i++)
    	{
    		sum += weights[i] * thisReward[i];
    	}
    	return sum;
    }
    
    // Calls the appropriate sub-method for the selected error metric
    private double calculateMetric(double thisReward[], double metricParameters[])
    {
    	switch (metric)
    	{
    		case ADDITIVE_EPSILON: return additiveEpsilon(thisReward, metricParameters);
    		case LINEAR_WEIGHTED_SUM: return linearWeightedSum(thisReward, metricParameters);
    		default: return -999;
    	}
    }    
    
    
	
}
