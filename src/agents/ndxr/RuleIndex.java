package agents.ndxr;

import framework.Action;
import framework.SensorData;

import java.util.ArrayList;

/**
 * class RuleIndex
 * <p>
 * Stores a set of {#link Rule} objects and indexes them by these properties
 * (in the given order):
 *  - depth
 *  - action
 *  - rhs goal sensor value
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

    /** these are index constants for top-level nodes.  See extSensorIndex below. */
    private static final int TBD_INDEX = -1;  //for leaf nodes
    private static final int DEPTH_INDEX = -2;
    private static final int ACTION_INDEX = -3;

    //Reference to agent
    static NdxrAgent agent = null;

    //Keep track of how many leaf nodes have been created
    private static int numLeafNodes = 1;

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

    //provides an index to the value upon which a non-leaf node splits as follows:
    // depth 0:  DEPTH_INDEX
    // depth 1:  ACTION_INDEX
    // depth 2:  index of last sensor on RHS (goal sensor)
    // depth 3+: index of a non-goal sensor.  For LHS use the index into Rule.lhs
    //           For RHS, add the length of Rule.lhs.
    private int splitIndex;

    //To keep the tree balanced, we need to keep track of the largest and
    //smallest leaf node in the tree (where size == number of rules).  The
    //only way I know to do this is to keep a list of them sorted by size
    private static ArrayList<RuleIndex> leaves = new ArrayList<>();

    /** base ctor creates the root node which indexes based on each rule's depth
     * and adds child and grandchild nodes for action and goal sensor  */
    public RuleIndex(NdxrAgent initAgent) {
        RuleIndex.agent = initAgent;
        this.splitIndex = DEPTH_INDEX;
        children = new RuleIndex[Rule.MAX_DEPTH + 1];
        //Create a child node for each possible depth
        for(int i = 0; i <= Rule.MAX_DEPTH; ++i) {
            children[i] = new RuleIndex(this);  //level 1 node ctor
        }
    }//root node ctor

    /**
     * this ctor creates a depth 1 node which indexes on the rule's action
     */
    public RuleIndex(RuleIndex initParent) {
        this.indexDepth = 1;
        this.splitIndex = ACTION_INDEX;  //depth 1 node
        Action[] actions = agent.getActions();
        children = new RuleIndex[actions.length];
        //Create a child leaf node for each possible action
        for(int i = 0; i < children.length; ++i) {
            children[i] = new RuleIndex(this, TBD_INDEX);
            RuleIndex.leaves.add(0, children[i]);
        }
    }//action RuleIndex ctor

    /**
     * this ctor creates a new leaf node.
     * <p>
     * Important:  The caller is responsible for inserting this new node into
     *             RuleIndex.leaves as appropriate.
     *
     */
    public RuleIndex(RuleIndex initParent, int initExtSensorIndex) {
        RuleIndex.numLeafNodes++;
        this.indexDepth = initParent.indexDepth + 1;
        this.rules = new ArrayList<>();
        this.splitIndex = initExtSensorIndex;
    }//action RuleIndex ctor

    /**
     * calcSplitIndex
     * <p>
     * is a helper method for {@link #split()}.  It calculates the split index
     * that this node will use.
     */
    private int calcSplitIndex() {
        //At depth 2, always split on RHS goal sensor which, by convention,
        //is always the last sensor in that set
        if (this.indexDepth == 2) {
            int sensorSize = agent.getCurrExternal().size();
            return 2*sensorSize - 1;
        }

        //TODO: At depth 3+ select sensor that has least wildcarding and also
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
     * retrieves the proper bit from a given rule with a given sensorIndex
     * where the sensorIndex spans the range of LHS + RHS (treating them
     * as a single set).
     */
    private int getBit(Rule r, int sensorIndex) {
        int sensorSize = agent.getCurrExternal().size();
        WCBitSet sensors = r.getLHS(); //default LHS
        if (sensorIndex >= sensorSize) { //detect RHS
            sensors = r.getRHS();
            sensorIndex -= sensorSize;
        }

        int retVal = sensors.wcget(sensorIndex);
        if (retVal < 0) retVal = 1;  //treat wildcard as '1' for indexing.  Mistake??
        return retVal;
    }//getBit

    /**
     * split
     * <p>
     * helper method to turn a leaf node into a non-leaf with two children
     */
    private void split() {
        //bisect based upon a particular sensor
        this.splitIndex = calcSplitIndex();
        this.children = new RuleIndex[2];
        this.children[0] = new RuleIndex(this, this.splitIndex);
        this.children[1] = new RuleIndex(this, this.splitIndex);

        //Add the rules to the new nodes.  (Note:  can't use addRule() method
        // as it creates recursion.)

        for(Rule r : this.rules) {
            //Add this rule to the correct bin
            int index = getBit(r, this.splitIndex);
            this.children[index].rules.add(r);
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
            largest.split();
        }

        //TODO:  DEBUG: For now, do extra splits to test splitting and searching more thoroughly
        else if ( (largest.indexDepth < 6) && (largest.numRules() > 4) ) {
            largest.split();
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
     * addRule          <!-- RECURSIVE -->
     * <p>
     * adds a new Rule to this RuleIndex object.  This may, in turn, trigger
     * a split which may trigger a re-balance.
     */
    public void addRule(Rule addMe) {
        //depth 0:  use rule depth
        if (indexDepth == 0) {
            RuleIndex child = this.children[addMe.getDepth()];
            child.addRule(addMe);
            return;
        }

        //depth 1:  use rule's action as index
        if (indexDepth == 1) {
            // We assume actions are a, b, c, etc.  Should be ok for now.
            int childIndex = addMe.getAction() - 'a';
            RuleIndex child = this.children[childIndex];
            child.addRule(addMe);
            return;
        }

        //Base Case: if this is a leaf node so just add the rule
        if (isLeaf()) {
            this.rules.add(addMe);
            this.adjustRulePos();
            this.considerSplit();
            return;
        }

        //Recursive Case:
        //We are at depth 2+ with a non-leaf.  Find the correct child node and recurse.
        //Node that extSensorIndex could ref LHS or RHS as per code below.
        int childIndex = getBit(addMe, this.splitIndex);
        this.children[childIndex].addRule(addMe);

    }//addRule

    /**
     * class MatchResult
     * <p>
     * contains rule and its match score.  Used as a result by
     * {@link #findMatches} and its helpers.
     */
    public class MatchResult {
        public Rule rule;
        public double score;

        public MatchResult(Rule initRule, double initScore) {
            this.rule = initRule;
            this.score = initScore;
        }
    }//class MatchResult

    /**
     * matchHelper           <!-- RECURSIVE -->
     * <p>
     * is a helper method for {@link #findMatches} that finds all matches in this subtree.
     * Note: This method must be called on a node with indexDepth of 2+.
     *
     * @param prevInternal must contain only rules whose depth matches this subtree.
     */
    private void matchHelper(ArrayList<MatchResult> results, ArrayList<Rule> prevInternal,
                             WCBitSet prevExtBits, WCBitSet currExtBits) {
        //Recursive case:  non-leaf node
        if (this.children != null) {
            //Retrieve the experience's bit that this rule splits on
            int bitIndex = this.splitIndex;
            int bit;
            if (bitIndex < Rule.getBitsLen()) {
                bit = prevExtBits.wcget(bitIndex);
            }
            else {
                bitIndex -= Rule.getBitsLen();
                bit = currExtBits.wcget(bitIndex);
            }

            //Recurse into matching child(ren)
            if (bit != 1) {
                this.children[0].matchHelper(results, prevInternal, prevExtBits, currExtBits);
            }
            if (bit != 0) {
                this.children[1].matchHelper(results, prevInternal, prevExtBits, currExtBits);
            }
        }//non-leaf

        //Base Case:  leaf node
        else if ((this.rules != null) && (this.rules.size() > 0)) {
            for(Rule r : this.rules) {
                double score = r.matchScore(prevInternal, prevExtBits, currExtBits);
                if (score > 0.0) {  //TODO:  0.0 is a low bar.  Require a higher min score to match?  Or take the top N?
                    MatchResult mr = new MatchResult(r, score);
                    results.add(mr);
                }
            }

            //DEBUG: print the results
            System.out.println("Matching bin " + this + ":");
        }
    }//matchHelper

    /**
     * extractRulesOfDepth
     * <p>
     * is a helper method for {@link #findMatches} that finds the Rules
     * in a given array list that have a given depth
     *
     * @param index  the search begins at this index.  This list is assumed
     *               to be in sorted order.
     * @param depth  the required depth
     * @param list   the list to search
     * @param found  this list is filled with the matching rules.  Any existing contents are cleared.
     *
     * @return the index where the search stopped (to setup for next depth)
     */
    private int extractRulesOfDepth(int index, int depth,
                                    ArrayList<Rule> list, ArrayList<Rule> found) {
        //reset any detritus from prev code
        found.clear();

        //check for bad depth
        if (( depth < 0) || (depth > Rule.MAX_DEPTH)) return index;

        while(index < list.size()) {
            Rule r = list.get(index);

            //skip rules of too early depth (shouldn't happen?)
            if (r.getDepth() < depth) {
                index++;
            }

            //copy rules of correct depth
            else if (r.getDepth() == depth) {
                found.add(r);
                index++;
            }

            //stop the loop at a later depth
            else {
                break;
            }
        }//while

        return index;
    }//extractRulesOfDepth


    /**
     * ruleIdListStr           <!-- DEBUG -->
     * <p>
     * returns the ids of a list of rules in a parenthetical list
     */
    public static String ruleIdListStr(ArrayList<Rule> list) {
        int count = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for(Rule r : list) {
            if (count > 0) sb.append(",");
            sb.append(r.getId());
            count++;
        }
        sb.append(")");
        return sb.toString();
    }//ruleIdListStr


    /**
     * findMatches
     * <p>
     * searches this rule index for all rules that match the given parameters
     * <p>
     * Note: this method should only be called on the top-level (root) node.
     *
     * @param prevInternal must be sorted by indexDepth.  It also may be null.
     *
     * @return the list of rules found and their associated match scores
     *         (in sorted order by index depth)
     */
    public ArrayList<MatchResult> findMatches(ArrayList<Rule> prevInternal,
                                       SensorData prevExternal,
                                       char act,
                                       SensorData currExternal) {
        ArrayList<MatchResult> results = new ArrayList<>();  //accumulates matching rules
        if (this.indexDepth != 0) {
            System.err.println("ERROR:  findMatches called on non-root node.");
            return results;
        }

        //Convert the SensorData to WCBitSet for matching
        WCBitSet prevExtBits = new WCBitSet();
        if (prevExternal != null) prevExtBits = new WCBitSet(prevExternal.toBitSet());
        WCBitSet currExtBits = new WCBitSet(currExternal.toBitSet());

        //DEBUG:  print the to-be-matched experience
        StringBuilder sb = new StringBuilder();
        System.out.println("Given Experience: ");
        sb.append("  ");
        sb.append(ruleIdListStr(prevInternal));
        sb.append(prevExtBits.bitString(Rule.getBitsLen()));
        sb.append(act);
        sb.append(" -> ");
        sb.append(currExtBits.bitString(Rule.getBitsLen()));
        System.out.println(sb.toString());

        //Find matches at each indexDepth
        int piIndex = 0;  //declare this here so it doesn't get reset to 0 in the loop
        ArrayList<Rule> lilPI = new ArrayList<>();
        for(int depth = 0; depth <= Rule.MAX_DEPTH; ++depth) {
            //Find the subtree for this depth and action
            int actIndex = act - 'a';
            RuleIndex d2node = this.children[depth].children[actIndex];

            //Find the members of prevInternal at this depth
            piIndex = extractRulesOfDepth(piIndex, depth - 1, prevInternal, lilPI);

            //Retrieve the matching rules
            d2node.matchHelper(results, lilPI, prevExtBits, currExtBits);
        }//for each depth

        //DEBUG
        System.out.println("Results: ");
        for(RuleIndex.MatchResult mr : results) {
            System.out.print("  ");
            System.out.print(mr.rule);
            System.out.println(" match score: " + mr.score);
        }

        return results;
    }//findMatches

    /**
     * getRepRule
     * <p>
     * retrieves a single Rule that is in this RuleIndex object.  If the RuleIndex
     * is a non-leaf it recurses to find one.
     *
     * @return the rule or null if none found
     */
    private Rule getRepRule() {
        //Base Case:  leaf node
        if (this.isLeaf()) {
            if (this.rules.size() == 0) {
                return null;  //no rule in this node
            }
            return this.rules.get(0);
        }

        //Recursive Case:  non-leaf node
        for(RuleIndex child : this.children) {
            Rule r = child.getRepRule();
            if (r != null) return r;
        }

        return null; //this sub-tree is empty
    }//getRepRule

    /**
     * buildRepStr
     * <p>
     * is a helper method for toString that builds a representation of
     * a given RuleIndex node that has indexDepth of 2+
     *
     * @return an string in this format: ...1.0.b -> ......0
     */
    private String buildRepStr() {
        //Find the level 2 node that is/contains this one
        Rule r = getRepRule();
        int depth = r.getDepth();
        int actIndex = r.getAction() - 'a';
        RuleIndex node = agent.getRules().children[depth].children[actIndex];

        //Generate starting sensors string (all wildcards)
        //Note: JDK 11 has String.repeat() method for this.  Ok to use?
        StringBuilder sbSensors = new StringBuilder();
        for(int i = 0; i < Rule.getBitsLen() * 2; ++i) {
            sbSensors.append('.');
        }

        //Fill in the known bits from this node any parents
        while(node != this) {
            int bit = getBit(r, node.splitIndex);
            if (bit != -1) {  //should always be true
                sbSensors.replace(node.splitIndex, node.splitIndex + 1, "" + bit);
            }

            node = node.children[bit];
        }

        //Create the result
        String lhs = sbSensors.substring(0, Rule.getBitsLen());
        String rhs = sbSensors.substring(Rule.getBitsLen());
        String result = lhs + r.getAction() + " -> " + rhs;
        return result;

    }//buildRepStr

    @Override
    public String toString() {
        StringBuilder retVal = new StringBuilder();

        //index depth
        retVal.append("(level ");
        retVal.append(this.indexDepth);

        //rule depth
        Rule r = getRepRule();
        if (r != null) {
            if (indexDepth > 0) {
                retVal.append(", rule depth ");
                retVal.append(r.getDepth());
            }

            //action
            if (indexDepth > 1) {
                retVal.append(", action ");
                retVal.append(r.getAction());
            }
        }

        retVal.append(")");

        //build a representation of the rules inside this node
        if (indexDepth > 2) {
            retVal.append(buildRepStr());
        }

        return retVal.toString();

    }//toString

    public boolean isLeaf() { return (this.children == null); }
    public int numRules() {
        if (this.rules == null) return 0;
        else return this.rules.size();
    }

    /**
     * printAll           <!-- RECURSIVE -->
     * <p>
     * prints all the rules in this (sub)tree in a readable format
     */
    public void printAll() {
        //don't print nodes that contain no rules
        if (this.getRepRule() == null) return;

        //print the node, properly indented and formatted as ASCII tree
        System.out.print(("+-" + this.toString()).indent(this.indexDepth * 2));

        //Recursive Case: non-leaf - recurse to children
        if (this.children != null) {
            for(RuleIndex child : this.children) {
                child.printAll();
            }
        }
        //Base Case:  leaf - print all rules
        else if (this.rules != null) {
            for(Rule r : rules) {
                System.out.print(r.toString().indent(2 + this.indexDepth * 2));
            }
        }
    }//printAll


}//class RuleIndex