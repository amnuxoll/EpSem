package agents.phujus;

import framework.SensorData;

import java.util.HashSet;
import java.util.Objects;

/**
 * BaseRule
 *
 * describes the prior probability that a particular action will take the
 * agent to a particular outcome
 */
public class BaseRule extends Rule {

    /**
     * class ExtCond
     *
     * Tracks external conditions (LHS or RHS)
     */
    public static class ExtCond extends Confidence implements Comparable<ExtCond> {
        public String sName;  //sensor name
        public boolean val;   //sensor value

        public ExtCond(String initSName, boolean initVal) {
            this.sName = initSName;
            this.val = initVal;
        }

        /** ExtConds are equal if they have the same sName and val */
        @Override
        public boolean equals(Object o) {
            if (! (o instanceof ExtCond)) return false;
            ExtCond other = (ExtCond) o;
            return (this.sName.equals(other.sName)) && (this.val == other.val);
        }

        @Override
        public String toString() { return sName + "=" + val; }

        @Override
        public int compareTo(ExtCond o) { return this.sName.compareTo(o.sName); }

        @Override
        public int hashCode() { return Objects.hash(sName, val); }
    }//class ExtCond

    protected char action;

    //The RHS of the rule consists of a set of external sensor values that were
    // present when the rule was created
    protected final HashSet<ExtCond> rhsExternal;

    public BaseRule(PhuJusAgent agent) {
        super(agent);
        this.action = agent.getPrevAction();
        this.rhsExternal = initExternal(agent.getCurrExternal());
    }



    /** converts from SensorData to HashSet<ExtCond> */
    protected HashSet<ExtCond> initExternal(SensorData sData) {
        HashSet<ExtCond> result = new HashSet<>();
        for(String sName : sData.getSensorNames()) {
            result.add(new ExtCond(sName, (Boolean)sData.getSensor(sName)));
        }

        return result;
    }//initExternal
}
