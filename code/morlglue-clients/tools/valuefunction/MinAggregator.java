package tools.valuefunction;

public class MinAggregator implements Aggregator {

	@Override
	public double[] apply(double a[], double b[]) {
		double[] out = new double[2];
		double min_a = a[0];
		double min_b = b[0];
		
		for(int i=1;i<a.length;i++) {
			if(a[i] < min_a) {
				min_a = a[i];
			} 
			if(b[i] < min_b) {
				min_b = b[i];
			}
		}
		out[0] = min_a;
		out[1] = min_b;
		return out;
	}

}
