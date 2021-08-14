package tools.valuefunction;

public class GranularAggregator implements Aggregator {

	private Aggregator actualAggregator;
	private double[] granularities;
	
	public GranularAggregator(Aggregator actualAggregator, double[] granularities)
	{
		this.actualAggregator = actualAggregator;
		this.granularities = granularities;
	}
	
	@Override
	public double[] apply(double a[], double b[]) {
	
		assert a.length == b.length;
		
		
		//NB! granularity is applied BEFORE the nonlinear transformation
		
		double granularA[] = new double[a.length];
		double granularB[] = new double[b.length];
		
		for(int i=0;i<a.length;i++) {
			
			//still make copy of the input for maintaining same interface as other utility functions
			//so that the caller can be sure that output array can be safely modified without input array being modified
			if (granularities[i] <= 0) 	//granularity disabled
			{
				granularA[i] = a[i]; 
				granularB[i] = b[i];
			}
			else 
			{
				granularA[i] = Math.round(a[i] / granularities[i]) * granularities[i];
				granularB[i] = Math.round(b[i] / granularities[i]) * granularities[i];
			}
		}
		
		double[] out = actualAggregator.apply(granularA, granularB);
		
		return out;
	}

}
