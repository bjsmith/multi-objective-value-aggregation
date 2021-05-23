package tools.valuefunction;

public class SEBA_UtilityFunction implements UtilityFunction {

	public double[] apply(double a[]) {
		double[] out = new double[a.length];
		
		for(int i=0;i<a.length;i++) {
			
			//NB! The order of array elements is LATER swapped by Vamplew's code
			//in method private void getActionValues(int state)
			//but at the current stage the array elements are still in the original order of reward dimensions
			// - The first dimension (at index 0) is the goal reward.
			// - The second dimension (at index 1) is the impact reward.
			
			out[0] = a[0];
			out[1] = -a[1] * a[1];
		}
		
		return out;
	}
}