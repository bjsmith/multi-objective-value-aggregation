package tools.valuefunction;

public class ELA_Aggregator implements Aggregator {
	// at scale ~ 2 equally good as TLO_A for Sokoban
	// tradeoff at scale ~ 5 where R^P is pushed whereas R^A and R^* drop
	// offset deteriorates function at values above ~1  and below 1e-4
	public double scale = 4;
	public double offset = 0.0;
	
	public double[] apply(double a[], double b[]) {
		double[] out = new double[2];
		double[] f_a = new double[a.length];
		double[] f_b = new double[b.length];
		double f_a_tot = 0.0;
		double f_b_tot = 0.0;
		
		for(int i=0;i<a.length;i++) {
			
			f_a[i] = -Math.exp(-(a[i] - offset)/scale) + 1;
			
			f_b[i] = -Math.exp(-(b[i] - offset)/scale) + 1;
			
			f_a_tot += f_a[i];
			f_b_tot += f_b[i];
		}
		out[0] = f_a_tot/a.length;
		out[1] = f_b_tot/b.length;
		return out;
	}
}
