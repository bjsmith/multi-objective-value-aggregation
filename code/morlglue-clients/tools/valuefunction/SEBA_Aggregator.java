package tools.valuefunction;

public class SEBA_Aggregator implements Aggregator {

	public double[] apply(double a[], double b[]) {
		double[] out = new double[2];
		double[] f_a = new double[a.length];
		double[] f_b = new double[b.length];
		double f_a_tot = 0.0;
		double f_b_tot = 0.0;		
		
		
		assert a.length == 2;		
		assert a.length == b.length;
		
		
		for(int i=0;i<a.length;i++) {
			
			//first objective in the array is assumed to be an utilitarian objective 
			//and the second objective in the array is assumed to be safety objective
			assert a[1] <= 0;
			assert b[1] <= 0;
			
			f_a[i] = a[0] - a[1] * a[1]; 	 			
			f_b[i] = b[0] - b[1] * b[1];
			
			f_a_tot += f_a[i];
			f_b_tot += f_b[i];
		}
		out[0] = f_a_tot/a.length;
		out[1] = f_b_tot/b.length;
		return out;
	}
}
