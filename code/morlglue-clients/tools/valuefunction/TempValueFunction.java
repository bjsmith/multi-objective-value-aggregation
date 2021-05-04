package tools.valuefunction;

import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward;

import java.util.ArrayList;
import java.util.Random;

public class TempValueFunction {
    protected final int numberOfObjectives;
    protected final int numberOfActions;
    protected final int solarStates;
    protected final int loadStates;
    protected final int batteryStates;
    protected final int futureSolarStates;
    protected final int futureLoadStates;
    double[] weights = null;
    Random random = null;

    protected ArrayList< ArrayList<double[][][][][]> > valueFunction = null;
    protected double[] errors = null;

    public TempValueFunction(int numberOfObjectives, int numberOfActions, int solarStates, int loadStates, int batteryStates, int futureSolarStates, int futureLoadStates, double[] weights) {
        this.numberOfObjectives = numberOfObjectives;
        this.numberOfActions = numberOfActions;
        this.solarStates = solarStates;
        this.loadStates = loadStates;
        this.batteryStates = batteryStates;
        this.futureSolarStates = futureSolarStates;
        this.futureLoadStates = futureLoadStates;

        valueFunction = new ArrayList<>();

        for (int i = 0; i < numberOfObjectives; i++) {
            ArrayList<double[][][][][]> actions = new ArrayList<>();
            for (int j = 0; j < numberOfActions; j++) {
                double[][][][][] array = new double[solarStates][loadStates][batteryStates][futureSolarStates][futureLoadStates];
                actions.add(array);
            }
            valueFunction.add(actions);
        }
        errors = new double[numberOfObjectives];
        this.weights = weights;
        random = new Random();
    }

    public void calculateErrors(int previousAction, Observation prevObservation, int greedyAction, Observation newObservation, double gamma, Reward reward) {
        for (int i = 0; i < numberOfObjectives; i++) {

            ArrayList<double[][][][][]> actions = valueFunction.get(i);

            double[][][][][] qTable =  actions.get(previousAction);
            int solarBin = discretizeBy100(getSolar(prevObservation));
            int loadBin = discretizeBy20(getLoad(prevObservation));
            int battBin = discretizeBy50(getBatteryCharge(prevObservation));
            int fSoloarBin = discretizeBy100(getFutureSolar(prevObservation));
            int fLoadBin = discretizeBy100(getFutureLoad(prevObservation));
            double thisQ = qTable[solarBin][loadBin][battBin][fSoloarBin][fLoadBin];

            qTable =  actions.get(greedyAction);
            solarBin = discretizeBy100(getSolar(newObservation));
            loadBin = discretizeBy20(getLoad(newObservation));
            battBin = discretizeBy50(getBatteryCharge(newObservation));
            fSoloarBin = discretizeBy100(getFutureSolar(newObservation));
            fLoadBin = discretizeBy100(getFutureLoad(newObservation));
            double maxQ = qTable[solarBin][loadBin][battBin][fSoloarBin][fLoadBin];
            System.out.println("maxQ is " + maxQ);

            errors[i] =  reward.doubleArray[i] + gamma * maxQ - thisQ;
        }
    }

    public void update(int previousAction, Observation prevObservation, double alpha) {

        for (int i = 0; i < numberOfObjectives; i++) {
            ArrayList<double[][][][][]> actions = valueFunction.get(i);
            double[][][][][] qTable =  actions.get(previousAction);
            int solarBin = discretizeBy100(getSolar(prevObservation));
            int loadBin = discretizeBy20(getLoad(prevObservation));
            int battBin = discretizeBy50(getBatteryCharge(prevObservation));
            int fSoloarBin = discretizeBy100(getFutureSolar(prevObservation));
            int fLoadBin = discretizeBy100(getFutureLoad(prevObservation));
            double thisQ = qTable[solarBin][loadBin][battBin][fSoloarBin][fLoadBin];


            double newQ = thisQ + alpha * errors[i] ;
            qTable[solarBin][loadBin][battBin][fSoloarBin][fLoadBin] = newQ;
        }
    }

    public int chooseGreedyAction(Observation observation) {
        ArrayList<Integer> bestActions = new ArrayList<>();
        double bestValue = -Double.MAX_VALUE;

        int solarBin = discretizeBy100(getSolar(observation));
        int loadBin = discretizeBy20(getLoad(observation));
        int battBin = discretizeBy50(getBatteryCharge(observation));
        int fSoloarBin = discretizeBy100(getFutureSolar(observation));
        int fLoadBin = discretizeBy100(getFutureLoad(observation));

        for (int a = 0; a < numberOfActions; a++) {
            double scalarValue = 0.0;
            for (int o = 0; o < numberOfObjectives; o++) {
                ArrayList<double[][][][][]> actions = valueFunction.get(o);
                double[][][][][] qTable = actions.get(a);
                double qValue =qTable[solarBin][loadBin][battBin][fSoloarBin][fLoadBin];
                scalarValue += weights[o] * qValue;
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
            return bestActions.get(random.nextInt(bestActions.size()));
        } else {
            return bestActions.get(0);
        }

    }



    private int discretizeBy20(double input) {
        return  (int) Math.floor( input / 20 );
    }
    private int discretizeBy100(double input) {
        return  (int) Math.floor( input / 100 );
    }

    private int discretizeBy50(double input) {
        return  (int) Math.floor( input / 50 );
    }

    private double getSolar(Observation observation) {

        assert observation != null : "Observation object is null";
        double predictedEnergy = observation.getDouble( 0 );
        assert predictedEnergy < 783  && predictedEnergy >= 0 : "Predicted energy is not in a specified range";

        return predictedEnergy;
    }
    private double getLoad(Observation observation) {

        assert observation != null : "Observation object is null";
        double load = observation.getDouble( 1 );
        assert load < 965  && load >= 0 : "Load is not in a specified range";

        return load;
    }
    private double getBatteryCharge(Observation observation) {

        assert observation != null : "Observation object is null";
        double batteryCharge = observation.getDouble( 2 );
        assert batteryCharge <= 2000  && batteryCharge >= 0 : "Battery charge is not in a specified range";

        return batteryCharge;
    }
    private double getFutureSolar(Observation observation) {

        assert observation != null : "Observation object is null";
        double futureSolar = observation.getDouble( 3 );
        assert futureSolar <= 5000  && futureSolar >= 0 : "Battery charge is not in a specified range";

        return futureSolar;
    }
    private double getFutureLoad(Observation observation) {

        assert observation != null : "Observation object is null";
        double futureLoad = observation.getDouble( 4 );
        assert futureLoad <= 6000  && futureLoad >= 0 : "Battery charge is not in a specified range";

        return futureLoad;
    }


}
