package tools.valuefunction;

public class LELA_UtilityFunction implements UtilityFunction {

	@Override
	public double[] apply(double a[]) {
		double[] out = new double[a.length];
		
		for(int i=0;i<a.length;i++) {
			
			out[i] = -Math.exp(-a[i]) + a[i] + 1;
		}
		
		return out;
	}	
}
