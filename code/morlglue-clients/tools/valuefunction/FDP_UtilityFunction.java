package tools.valuefunction;

public class FDP_UtilityFunction implements UtilityFunction {

	@Override
	public double[] apply(double a[]) {
		double[] out = new double[a.length];
		
		double sum_a = 0.0; 
		for(int i=0;i<a.length;i++) {
			
			sum_a += a[i];
		}
		
		double mean_a = sum_a / a.length;
		
		
		for(int i=0;i<a.length;i++) {
			
			out[i] = a[i] - (mean_a - a[i]) * (mean_a - a[i]);
		}
		
		return out;
	}
}
