package agents.ndxr;

import java.util.ArrayList;

/**
 * class RuleIndex
 * <p>
 * Stores a set of {#link Rule} objects and indexes them by these properties
 * (in the given order):
 *  - depth
 *  - action
 *  - goal sensor value
 *  - other sensor values (prioritized from least to most wildcarding)
 * <p>
 *  The index is a tree.  Each node in the tree either has child nodes
 *  selected by a property (see list above) or it's a leaf node
 *  that contains a set of Rule objects.
 * <p>
 *  The tree is limited to a maximum size via {@link #MAX_LEAF_NODES}.
 * <p>
 *  The tree tries to keep itself "balanced" in the sense that no leaf node
 *  contains more than 2x as many Rule objects as any other (though this
 *  is ignored when there is a small number of Rules in the index).
 */
public class RuleIndex {
    //This is the maximum number of leaf nodes allowed in the index tree.  It must
    // be large enough to handle any Depth x Action x GoalSensor combo
    //TODO: figure out an ideal size for this based on MAX_NUM_RULES
    public static int MAX_LEAF_NODES = 100;

    /**These two constants control when a leaf gets too big and must be split
     * see {@link #considerSplit()} */
    private static final int MIN_SMALLEST = 2;
    private static final int MAX_SIZE_RATIO = 3;

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

    //To keep the tree balanced, we need to keep track of the largest and
    //smallest leaf node in the tree (where size == number of rules).  The
    //only way I know to do this is to keep a list of them sorted by size
    private static ArrayList<RuleIndex> leaves = new ArrayList<>();

    //Each non-leaf node (except root) has a binary external sensor that it
    //uses to split it's children into two bins.  The index of that sensor
    //is recorded here
    private int splitIndex = -1; //-1 for root node

    /** base ctor creates the root node which indexes based on each rule's depth */
    public RuleIndex() {
        children = new RuleIndex[Rule.MAX_DEPTH + 1];
        for(int i = 0; i <= Rule.MAX_DEPTH; ++i) {
            children[i] = new RuleIndex(this);
        }
    }//root node ctor

    /**
     * this ctor creates a new leaf node
     */
    public RuleIndex(RuleIndex initParent) {
        RuleIndex.numNodes++;
        this.indexDepth = initParent.indexDepth + 1;
        this.rules = new ArrayList<>();
        RuleIndex.leaves.add(0, this);
    }//action RuleIndex ctor

    /**
     * calcSplitIndex
     * <p>
     * is a helper method for {@link #split()}.  It calculates the split index
     * that this node will use.
     */
    private int calcSplitIndex() {
        //At depth 1, always split on goal sensor which, by convention,
        //is always the last sensor
        if (this.indexDepth == 1) {
            return this.rules.get(0).getLHS().length() - 1;
        }

        //TODO: At depth 2+ select sensor that has least wildcarding and also
        //      divides.  For now, hardcode as index 0 to test the code so far
        return 0;
    }//calcSplitIndex


    /**
     * insertLeaf
     * <p>
     * asks this node to insert itself into RuleIndex.leaves in an apporpriate place
     * @param index  Begin the search for a place at this index
     */
    private void insertLeaf(int index) {
        int newIndex = RuleIndex.leaves.size();  //default:  put at end
        for(int i = index; i < RuleIndex.leaves.size(); ++i) {
            RuleIndex ri = RuleIndex.leaves.get(i);
            if (ri.numRules() >= this.numRules()) {
                newIndex = i;
                break;
            }
        }
        RuleIndex.leaves.add(newIndex, this);
    }//insertLeaf

    /**
     * split
     * <p>
     * helper method to turn a leaf node into a non-leaf with two children
     */
    private void split() {
        //bisect based upon a particular sensor
        this.splitIndex = calcSplitIndex();
        this.children = new RuleIndex[2];
        this.children[0] = new RuleIndex(this);
        this.children[1] = new RuleIndex(this);

        //Add the rules to the new nodes.  (Note:  can't use addRule() method
        // as it creates recursion.)
        for(Rule r : this.rules) {
            if (r.getLHS().get(this.splitIndex)) {
                this.children[1].rules.add(r);
            } else {
                this.children[0].rules.add(r);
            }
        }

        //this is no longer a leaf node
        this.rules.clear();
        this.rules = null;
        RuleIndex.leaves.remove(this);

        //Add these children to the leaves list
        this.children[0].insertLeaf(0);
        this.children[1].insertLeaf(0);

    }//split

    /**
     * considerSplit
     * <p>
     * a helper for addRule that considers splitting
     * this node into two.  It also can trigger a rebalance.
     * <p>
     * Caveat:  only call this on leaf nodes!
     */
    private void considerSplit() {

        //Find the smallest leaf node with at least MIN_SMALLEST entries
        RuleIndex smallest = null;
        for(RuleIndex node : RuleIndex.leaves) {
            if (node.rules.size() >= MIN_SMALLEST) {
                smallest = node;
                break;
            }
        }
        if (smallest == null) return;  //too few rules to balance

        //Get the largest leaf node
        RuleIndex largest = RuleIndex.leaves.get(RuleIndex.leaves.size() - 1);

        //split if the size diff is too big
        if ( (largest.numRules() / smallest.numRules() > MAX_SIZE_RATIO) ) {
            split();
        }

        //TODO:  rebalance if too many nodes
    }//considerSplit

    /**
     * adjustRulePos
     * <p>
     * a helper for {@link #addRule} that adjusts the position of this
     * rule in RuleIndex.leaves beacuse it's size has changed
     * <p>
     * Caveat:  only call this on leaf nodes!
     */
    private void adjustRulePos() {
        //Find the rule's index
        int index = -1;
        for(int i = 0; i < RuleIndex.leaves.size(); ++i) {
            if (RuleIndex.leaves.get(i) == this) {
                index = i;
                break;
            }
        }

        //Find its new place
        RuleIndex.leaves.remove(index);
        insertLeaf(index);

    }//adjustRulePos

    /**
     * addRule
     * <p>
     * adds a new Rule to this RuleIndex object.  This may, in turn, trigger
     * a split which may trigger a re-balance.
     */
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
            this.adjustRulePos();
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
            index = (act - 'a');
        }
        this.children[index].addRule(addMe);

    }//addRule

    public boolean isLeaf() { return (this.children == null); }
    public int numRules() {
        if (this.rules == null) return 0;
        else return this.rules.size();
    }


}//class RuleIndex
