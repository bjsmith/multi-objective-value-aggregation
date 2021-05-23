package tools.valuefunction;

public class Identity_UtilityFunction implements UtilityFunction {

	public double[] apply(double a[]) {
		double[] out = new double[a.length];
		
		for(int i=0;i<a.length;i++) {
			
			//still make copy of the input for maintaining same interface as other utility functions
			//so that the caller can be sure that output array can be safely modified without input array being modified
			out[i] = a[i]; 	
		}
		
		return out;
	}

}
