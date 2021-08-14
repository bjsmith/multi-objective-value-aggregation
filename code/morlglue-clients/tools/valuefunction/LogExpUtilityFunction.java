package tools.valuefunction;

public class LogExpUtilityFunction implements UtilityFunction {

	@Override
	public double[] apply(double a[]) {
		double[] out = new double[a.length];
		
		for(int i=0;i<a.length;i++) {
			
			if(a[i] < 0) {
				out[i] = -Math.exp(-a[i]) + 1;
			} else {
				out[i] = Math.log(a[i] + 1);
			}
		}
		
		return out;
	}
	
}
