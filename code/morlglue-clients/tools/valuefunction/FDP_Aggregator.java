package tools.valuefunction;

public class FDP_Aggregator implements Aggregator {

	public double[] apply(double a[], double b[]) {
		double[] out = new double[2];
		double[] f_a = new double[a.length];
		double[] f_b = new double[b.length];
		double f_a_tot = 0.0;
		double f_b_tot = 0.0;
		
		
		assert a.length == b.length;
		
		
		double sum_a = 0.0; 
		double sum_b = 0.0;
		for(int i=0;i<a.length;i++) {
			
			sum_a += a[i];
			sum_b += b[i];
		}
		
		double mean_a = sum_a / a.length;
		double mean_b = sum_b / b.length;
		
		
		for(int i=0;i<a.length;i++) {
			
			f_a[i] = a[i] - (mean_a - a[i]) * (mean_a - a[i]);			
			f_b[i] = b[i] - (mean_b - a[i]) * (mean_b - b[i]);
			
			f_a_tot += f_a[i];
			f_b_tot += f_b[i];
		}
		out[0] = f_a_tot/a.length;
		out[1] = f_b_tot/b.length;
		return out;
	}
}
