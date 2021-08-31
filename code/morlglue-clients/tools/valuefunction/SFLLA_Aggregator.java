package tools.valuefunction;

public class SFLLA_Aggregator implements Aggregator {
	// at scale ~ 0.5-0.8 behaves like TLO_A; transition at around 0.9 to being better at R^P but failing in safety
	// offset at 0.9 (scale = 0.8) learning becomes spikey; with (0.1, 0.3) interesting fail have way through learning happens
	// (scale, offset): (0.5, 0) (0,17, 0.05) work as well as TLO_A in all environments!
	public double scale = 1;
	public double offset = 0.;

	@Override
	public double[] apply(double a[], double b[]) {
		double[] out = new double[2];
		double[] f_a = new double[a.length];
		double[] f_b = new double[b.length];
		double f_a_tot = 0.0;
		double f_b_tot = 0.0;
		
		for(int i=0;i<a.length;i++) {
			if(a[i] < 0) {
				f_a[i] = -Math.exp(-(a[i] - offset)/scale) + 1;
			} else {
				f_a[i] = Math.log((a[i] - offset)/scale + 1);
			}
			
			if(b[i] < 0) {
				f_b[i] = -Math.exp(-(b[i] - offset)/scale) + 1;
			} else {
				f_b[i] = Math.log((b[i] - offset)/scale + 1);
			}
			
			f_a_tot += f_a[i];
			f_b_tot += f_b[i];
		}
		out[0] = f_a_tot/a.length;
		out[1] = f_b_tot/b.length;
		return out;
	}
	
}
