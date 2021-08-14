package tools.valuefunction;

public class GranularUtilityFunction implements UtilityFunction {

	private UtilityFunction actualUtilityFunction;
	private double[] granularities;
	
	public GranularUtilityFunction(UtilityFunction actualUtilityFunction, double[] granularities)
	{
		this.actualUtilityFunction = actualUtilityFunction;
		this.granularities = granularities;
	}
	
	@Override
	public double[] apply(double a[]) {
		
		//NB! granularity is applied BEFORE the nonlinear transformation
		
		double[] granularA = new double[a.length];
		
		for(int i=0;i<a.length;i++) {
			
			//still make copy of the input for maintaining same interface as other utility functions
			//so that the caller can be sure that output array can be safely modified without input array being modified
			if (granularities[i] <= 0) 	//granularity disabled
			{
				granularA[i] = a[i];
			}
			else 
			{
				granularA[i] = Math.round(a[i] / granularities[i]) * granularities[i];
			}	
		}
		
		double[] out = actualUtilityFunction.apply(granularA);
		
		return out;
	}

}
