package modules;

/**
 * Created by sr3897c on 10-05-17.

 * ln(P/(1-P) ) = ( 1.67261 +   -0.44853  x CPU    +  0.01098  x Boolean_Value_Accuracy  +       5.41480 x Boolean_Value_SNR
 */
public class Logit {


    private static final double intercept = 1.67261, cpuUsgCoeff = -0.44853, acracyChngCoeff = 0.01098, snrChngCoeff = 5.41480;
    public float probability;
    //Constructor
    protected Logit( float cpuUsage, int accuracyChanged,  int snrChanged) {



        double logit = (intercept + cpuUsage * cpuUsgCoeff + accuracyChanged * acracyChngCoeff + snrChanged * snrChngCoeff);
        probability = (float) (1 / (1 + Math.exp(-logit)));
        //return logit;
    }
}
