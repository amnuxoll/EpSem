package agents.ndxr;

import java.util.ArrayList;

/**
 * class RuleIndex
 *
 * Stores a set of {#link Rule} objects and indexes them by these properties
 * (in the given order):
 *  - depth
 *  - action
 *  - goal sensor value
 *  - other sensor values (prioritized from least to most wildcarding)
 *
 *  The index is a tree.  Each node in the tree either has child nodes
 *  selected by a property (see list above) or it's a leaf node
 *  that contains a set of Rule objects.
 *
 *  The tree is limited to a maximum size via {@link #MAX_NODES}.
 *
 *  The tree tries to keep itself "balanced" in the sense that no leaf node
 *  contains more than 2x as many Rule objects as any other (though this
 *  is ignored when there is a small number of Rules in the index).
 */
public class RuleIndex {
    //This is the maximum number of nodes allowed in the index tree.  It must be large enough
    //to handle any Depth x Action x GoalSensor combo
    //TODO: figure out an ideal size for this based on MAX_NUM_RULES
    public static int MAX_NODES = 100;

    //Keep track of how many nodes have been created
    private static int numNodes = 1;

    //The index is organized as a tree and each RuleIndex object is one
    //node in the tree.  This variable tracks the node's depth so that
    //it can know what to index itself with.  Warning:  do not confuse this
    //with the depth of the rule!
    private int indexDepth = 0;  //0 == root node

    //If this node is a non-leaf node then it's child nodes are stored here.
    //Otherwise this variable stays null
    private RuleIndex[] children = null;

    //If the node is a leaf node then it contains Rule objects here.  Otherwise
    //this variable stays null
    private ArrayList<Rule> rules = null;

    //At depths 2 and higher, the node is indexed by a particular external
    //sensor, specified here as an index into {@link Rule.lhs}
    private int extSensorIndex = -1;

    //Keep track of which leaf node has the most rules inside
    private static RuleIndex largest = null;

    //Keep track of which leaf node has the fewest rules inside (excluding those
    //below a certain minimum).  This value can be null at first
    private static RuleIndex smallest = null;
    private static final int MIN_SMALLEST = 3;

    /** base ctor creates the root node which indexes based on each rule's depth */
    public RuleIndex() {
        children = new RuleIndex[Rule.MAX_DEPTH + 1];
        for(int i = 0; i <= Rule.MAX_DEPTH; ++i) {
            children[i] = new RuleIndex(this);
        }
        this.largest = children[0];
        this.smallest = null;
    }//root node ctor

    /**
     * this ctor creates a new leaf node
     */
    public RuleIndex(RuleIndex initParent) {
        RuleIndex.numNodes++;
        this.indexDepth = initParent.indexDepth + 1;
        this.rules = new ArrayList<>();
    }//action RuleIndex ctor

    /**
     * split
     *
     * helper method to turn a leaf node into a non-leaf with two children
     */
    private void split() {
        //At depth 1, always split on goal sensor
        //TODO:  STOPPED HERE
    }//split

    /**
     * considerSplit
     *
     * a helper for addRule that considers splitting
     * this node into two.  It also can trigger a rebalance.
     */
    private void considerSplit() {

        //Is this the first smallest rule?
        if ((RuleIndex.smallest == null) && (this.numRules() >= MIN_SMALLEST)) {
            RuleIndex.smallest = this;
        }

        //Is this the new largest rule?
        if ( (RuleIndex.largest != this)
                && (this.numRules() > RuleIndex.largest.numRules()) ) {
            RuleIndex.largest = this;

            //split if this node is too big
            if ( (RuleIndex.smallest != null)
                && (RuleIndex.largest.numRules() / RuleIndex.smallest.numRules() > 5) ) {
                split();
            }


            //TODO:  rebalance if too many nodes

        }
    }//considerSplit

    public void addRule(Rule addMe) {
        //root:  use rule depth
        if (indexDepth == 0) {
            RuleIndex child = this.children[addMe.getDepth()];
            child.addRule(addMe);
            return;
        }

        //If this is a leaf node just add the rule
        if (isLeaf()) {
            this.rules.add(addMe);
            this.considerSplit();
            return;
        }

        //Find the correct child node and recurse
        int index = this.extSensorIndex;
        //at depth 1 use action as the index instead
        if (indexDepth == 1) {
            //Take advantage of the fact that action is a lowercase letter
            //(We may not get away with this in the long term.)
            char act = addMe.getAction();
            index = (int)(act - 'a');
        }
        this.children[index].addRule(addMe);

    }//addRule

    public boolean isLeaf() { return (this.children == null); }
    public int numRules() {
        if (this.rules == null) return 0;
        else return this.rules.size();
    }


}//class RuleIndex
