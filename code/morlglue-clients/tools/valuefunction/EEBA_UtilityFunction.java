package tools.valuefunction;

public class EEBA_UtilityFunction implements UtilityFunction {

	public double[] apply(double a[]) {
		double[] out = new double[a.length];
			
		//NB! The order of array elements will be LATER swapped by Vamplew's code
		//in method private void getActionValues(int state)
		//but at the current stage the array elements are still in the original order of reward dimensions
		// - The first dimension (at index 0) is the goal reward.
		// - The second dimension (at index 1) is the impact reward.
		
		out[0] = a[0];
		out[1] = -Math.exp(-a[1]) + 1;
			
		return out;
	}
}
