package tools.valuefunction;

import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward;
import tools.approximators.CMACApproximator;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;

public class CMACValueFunction {
    int numberOfActions;
    int numberOfObjectives;
    double[] weights;
    double[] errors;

    //int numberOfFeatures=6000000;
    //int numberOfTilings=6;
    //double tileSize=0.1;
  
    int numberOfFeatures=131072;
    int numberOfTilings=32;
    double tileSize=0.25;
    
    Random r;


    protected ArrayList< ArrayList<CMACApproximator> > valueFunction = null;

    public CMACValueFunction(int numberOfActions, int numberOfObjectives, double[] weights) {
        this.numberOfActions = numberOfActions;
        this.numberOfObjectives = numberOfObjectives;
        this.weights = weights;
        errors = new double[numberOfObjectives];
        valueFunction = new ArrayList<>();
        
      //Getting the runtime reference from the system
        int mb = 1024*1024; //Dean
		Runtime runtime = Runtime.getRuntime(); //Dean
		System.out.println("##### Heap utilisation statistics [MB] #####");
		

        for (int i = 0; i < numberOfObjectives; i++) {
            ArrayList<CMACApproximator> approximators = new ArrayList<>();
            for (int j = 0; j < numberOfActions; j++) {

        		//Print used memory
        		System.out.println("Used Memory:" + (runtime.totalMemory() - runtime.freeMemory()) / mb);
        		
        		//Print free memory
        		System.out.println("Free Memory:" + runtime.freeMemory() / mb);
        		
        		//Print total available memory
        		System.out.println("Total Memory:" + runtime.totalMemory() / mb);
        		
        		//Print Maximum available memory
        		System.out.println("Maximum Memory:" + runtime.maxMemory() / mb);
            	System.out.println("i:" + i+ " j:" +j); //Dean

                approximators.add( new CMACApproximator( numberOfFeatures, numberOfTilings, tileSize ) );
            }
            valueFunction.add( approximators );
        }
       
        r = new Random();
        //Temp code for pausing output -- Dean
        try{
    		BufferedReader br = 
                          new BufferedReader(new InputStreamReader(System.in));
     
    		String input;
     
    		input=br.readLine();
    		System.out.println(input);
    		     
    	}catch(IOException io){
    		io.printStackTrace();
    	}	
        //END Temp code for pausing output -- Dean
    }

    public int chooseGreedyAction(Observation observation) {
        ArrayList<Integer> bestActions = new ArrayList<>();
        double bestValue = 0.0;

        for (int i = 0; i < numberOfObjectives; i++) {
            bestValue += weights[i] * valueFunction.get(i).get(0).getQValue( observation );
        }
        bestActions.add(0);

        for (int a = 1; a < numberOfActions; a++) {

            double scalarValue = 0.0;

            for (int i = 0; i < numberOfObjectives; i++) {
                scalarValue += weights[i] * valueFunction.get(i).get(a).getQValue( observation );
            }

            if ( scalarValue >= bestValue ) {
                if ( scalarValue > bestValue ) {
                    bestActions.clear();
                    bestActions.add(a);
                    bestValue = scalarValue;
                } else {
                    bestActions.add(a);
                }
            }

        }

        if (bestActions.size() > 1) {
            return bestActions.get(r.nextInt(bestActions.size()));
        } else {
            return bestActions.get(0);
        }

    }


    public double[] getQValues(int action, Observation observation) {
        double[] result = new double[ numberOfObjectives ];
        for (int i = 0; i < numberOfObjectives; i++) {
            result[i] = valueFunction.get(i).get(action).getQValue( observation );
        }
        return result;
    }


    public void calculateErrors(int prevAction, Observation prevObservation, int greedyAction, Observation newObservation, double gamma, Reward reward) {
        for (int i = 0; i < numberOfObjectives; i++) {
            ArrayList<CMACApproximator> approximators = valueFunction.get(i);

            double thisQ = approximators.get(prevAction).getQValue(prevObservation);
            double maxQ = approximators.get(greedyAction).getQValue(newObservation);

            errors[i] =  reward.doubleArray[i] + gamma * maxQ - thisQ;
        }
    }
    public void calculateTerminalErrors(int prevAction, Observation prevObservation, Reward reward) {
        for (int i = 0; i < numberOfObjectives; i++) {
            ArrayList<CMACApproximator> approximators = valueFunction.get(i);

            double thisQ = approximators.get(prevAction).getQValue(prevObservation);

            errors[i] =  reward.doubleArray[i] - thisQ;
        }
    }

    public void update(int action, Observation state, double alpha) {

        for (int i = 0; i < numberOfObjectives; i++) {
            ArrayList<CMACApproximator> approximators = valueFunction.get(i);
            approximators.get(action).setQValue(state,alpha*errors[i]);
        }
    }
    
    public void saveValueFunction(String theFileName) {
        try {
            DataOutputStream DO = new DataOutputStream(new FileOutputStream(new File(theFileName)));
            for (int i = 0; i < numberOfObjectives; i++) {        
            	ArrayList<CMACApproximator> approximators = valueFunction.get(i);
                for (int a = 0; a < numberOfActions; a++) {
                	CMACApproximator approxmator = approximators.get(a);                	                	                	                	                	                	
                    for (int s = 0; s < numberOfFeatures; s++) {                    	
                        DO.writeDouble( approxmator.getCellValue(s) );
                    }
                }                
            }
            DO.close();
        } catch (FileNotFoundException ex) {
            System.err.println("Problem saving value function to file: " + theFileName + " :: " + ex);
        } catch (IOException ex) {
            System.err.println("Problem writing value function to file:: " + ex);
        }
    }    
    public void loadValueFunction(String theFileName) {
        try {
            DataInputStream DI = new DataInputStream(new FileInputStream(new File(theFileName)));
            for (int i = 0; i < numberOfObjectives; i++) {        
            	ArrayList<CMACApproximator> approximators = valueFunction.get(i);
                for (int a = 0; a < numberOfActions; a++) {
                	CMACApproximator approxmator = approximators.get(a);                	                	                	                	                	                	
                    for (int s = 0; s < numberOfFeatures; s++) {                    	
                        approxmator.setCellValue(s,DI.readDouble());
                    }
                }                
            }
            DI.close();
        } catch (FileNotFoundException ex) {
            System.err.println("Problem loading value function from file: " + theFileName + " :: " + ex);
        } catch (IOException ex) {
            System.err.println("Problem reading value function from file:: " + ex);
        }
    }
}
