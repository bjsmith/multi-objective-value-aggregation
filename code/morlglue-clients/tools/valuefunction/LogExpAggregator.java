package tools.valuefunction;

public class LogExpAggregator implements Aggregator {

	public double[] apply(double a[], double b[]) {
		double[] out = new double[2];
		double[] f_a = new double[a.length];
		double[] f_b = new double[b.length];
		double f_a_tot = 0.0;
		double f_b_tot = 0.0;
		for(int i=0;i<a.length;i++) {
			double f_ai = 0.0;
			double f_bi = 0.0;
			
			if(a[i] < 0) {
				f_a[i] = -Math.exp(-a[i]) + 1;
			} else {
				f_a[i] = Math.log(a[i] + 1);
			}
			
			if(b[i] < 0) {
				f_b[i] = -Math.exp(-b[i]) + 1;
			} else {
				f_b[i] = Math.log(b[i] + 1);
			}
			
			f_a_tot += f_ai;
			f_b_tot += f_bi;
		}
		out[0] = f_a_tot;
		out[1] = f_b_tot;
		return out;
	}
	
}
