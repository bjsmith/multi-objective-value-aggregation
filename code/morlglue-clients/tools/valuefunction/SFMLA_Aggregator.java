package tools.valuefunction;

public class SFMLA_Aggregator implements Aggregator {

	public double[] apply(double a[], double b[]) {
		//System.out.println("applying SFMLA");
		double[] out = new double[2];
		double[] f_a = new double[a.length];
		double[] f_b = new double[b.length];
		double f_a_tot = 0.0;
		double f_b_tot = 0.0;
		
		for(int i=0;i<a.length;i++) {
			if(a[i] < 0) {
				f_a[i] = 2*a[i];
			} else {
				f_a[i] = a[i];
			}
			
			if(b[i] < 0) {
				f_b[i] = 2*b[i];
			} else {
				f_b[i] = b[i];
			}
			
			f_a_tot += f_a[i];
			f_b_tot += f_b[i];
		}
		out[0] = f_a_tot/a.length;
		out[1] = f_b_tot/b.length;
		return out;
	}

}
