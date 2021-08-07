package tools.valuefunction;

public class SUM_LOG_Aggregator implements Aggregator {

	@Override
	public double[] apply(double a[], double b[]) {
		double[] out = new double[2];
		
		
		assert a.length == b.length;
		
		
		double sum_a = 0.0; 
		double sum_b = 0.0;
		for(int i=0;i<a.length;i++) {
			
			sum_a += a[i];
			sum_b += b[i];
		}
		
		out[0] = -Math.log(sum_a / a.length);
		out[1] = -Math.log(sum_b / b.length);
		
		return out;
	}

}
