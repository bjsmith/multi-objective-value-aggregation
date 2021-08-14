package tools.valuefunction;

public class Rolf_EXP_UtilityFunction implements UtilityFunction {
	public double scale = 1;
	public double offset = 0.0;
	
	@Override
	public double[] apply(double a[]) {
		double[] out = new double[a.length];
		
		for(int i=0;i<a.length;i++) {
			
			out[i] = -Math.exp(-(a[i] - offset)/scale);
		}
		
		//NB! need to to -log in the aggregator
		
		return out;
	}
}
