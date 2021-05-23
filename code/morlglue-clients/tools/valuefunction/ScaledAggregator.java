package tools.valuefunction;

public abstract class ScaledAggregator implements Aggregator {
    //accumulator for scaling
	//accumulates value for each objective
    double[] accumulatedSquaredImpact;
    int accumulateIterator;
    boolean scale;
    
    public ScaledAggregator(boolean scale) {
    	this.scale = scale;
    	accumulatedSquaredImpact = new double[2];
    	accumulatedSquaredImpact[0] = 0;
    	accumulatedSquaredImpact[1] = 0;
    }

	@Override
	public double[] apply(double[] a, double[] b) {
		if(this.scale) {
			accumulate_scale_counter(a,b);
		}
		
		return base_apply(a,b);
	}
	
	abstract public double[] base_apply(double[] a, double[] b);
	
	public void accumulate_scale_counter(double[] a, double[] b) {
		
	}
	
	

}
