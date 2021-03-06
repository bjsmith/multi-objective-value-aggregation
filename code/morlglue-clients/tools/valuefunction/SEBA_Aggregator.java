package tools.valuefunction;

public class SEBA_Aggregator implements Aggregator {

	@Override
	public double[] apply(double a[], double b[]) {
		double[] out = new double[2];
		double f_a_tot = 0.0;
		double f_b_tot = 0.0;		
		
		
		assert a.length == 2;		
		assert a.length == b.length;
			
		
		//first objective in the array is assumed to be an utilitarian objective 
		//and the second objective in the array is assumed to be safety objective
		assert a[1] <= 0;
		assert b[1] <= 0;
		
		if (a[1] < 0) {
			boolean qqq1 = true;
		}
		
		if (b[1] < 0) {
			boolean qqq2 = true;
		}
		
		//NB! The order of array elements is swapped by Vamplew's code 
		//BEFORE aggregator function is called
		//in method private void getActionValues(int state)
		//the first dimension is impact reward
		//the second dimension is goal reward
		f_a_tot = a[1] - a[0] * a[0]; 	 			
		f_b_tot = b[1] - b[0] * b[0];
		
		
		out[0] = f_a_tot/a.length;
		out[1] = f_b_tot/b.length;
		return out;
	}
}
