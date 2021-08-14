package tools.valuefunction;

public class ELA_UtilityFunction implements UtilityFunction {
	// at scale ~ 2 equally good as TLO_A for Sokoban
	// tradeoff at scale ~ 5 where R^P is pushed whereas R^A and R^* drop
	// offset deteriorates function at values above ~1  and below 1e-4
	public double scale = 4;
	public double offset = 0.0;
	
	@Override
	public double[] apply(double a[]) {
		double[] out = new double[a.length];
		
		for(int i=0;i<a.length;i++) {
			
			out[i] = -Math.exp(-(a[i] - offset)/scale) + 1;
		}
		
		return out;
	}
}
