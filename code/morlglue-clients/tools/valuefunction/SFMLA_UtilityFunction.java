package tools.valuefunction;

public class SFMLA_UtilityFunction implements UtilityFunction {

	public double[] apply(double a[]) {
		double[] out = new double[a.length];
		
		for(int i=0;i<a.length;i++) {
			
			if(a[i] < 0) {
				out[i] = 2*a[i];
			} else {
				out[i] = a[i];
			}
		}
		
		return out;
	}

}
