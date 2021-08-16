import tools.valuefunction.Softmax;
public class TestSoftmax {

	public static void main(String[] args) 
	{
		double actionValues[] = {1.8, 25.2, -3.1, 12.3};
		double temperature = 100.0;
		
		while (temperature>=0)
		{
			System.out.print(temperature + "\t");
			System.out.println(Softmax.getAction(actionValues, temperature,0));
			temperature = temperature * 0.9;
		}

	}

}
