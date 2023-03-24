package agents.ndxr;

import framework.Action;
import framework.SensorData;

import java.util.Vector;

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
    //TODO: figure out an ideal size for this based on MAX_NUM_RULES + number of possible index combos
    public static int MAX_LEAF_NODES = 100;

    /**These two constants control when a leaf gets too big and must be split
     * see {@link #considerSplit()} */
    //TODO:  have these be calculated based upon MAX_LEAF_NODES & MAX_NUM_RULES?
    private static final int MIN_SMALLEST = 2;
    private static final int MAX_SIZE_RATIO = 2;

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
    private Vector<Rule> rules = null;

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
    private static final Vector<RuleIndex> leaves = new Vector<>();

    //The timestep when this.brother was last updated for all rules
    private static int lastBrotherUpdate = 0;

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
        this.rules = null;
        this.splitIndex = ACTION_INDEX;  //depth 1 node
        Action[] actions = agent.getActionList();
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
     * @param initParent  the parent node of this new leaf
     * @param initExtSensorIndex  the split index of this new leaf
     *
     */
    public RuleIndex(RuleIndex initParent, int initExtSensorIndex) {
        RuleIndex.numLeafNodes++;
        this.indexDepth = initParent.indexDepth + 1;
        this.rules = new Vector<>();
        this.splitIndex = initExtSensorIndex;
    }//action RuleIndex ctor

    /**
     * calcSplitIndex
     * <p>
     * is a helper method for {@link #split()}.  It calculates the split index
     * that this node will use.  If no index can be found, {@link #TBD_INDEX}
     * is returned instead.
     */
    private int calcSplitIndex() {
        //At depth 2, always split on RHS goal sensor which, by convention,
        //is always the last sensor in that set
        int sensorSize = agent.getCurrExternal().size();
        if (this.indexDepth == 2) {
            return 2*sensorSize - 1;
        }

        //TODO: At depth 3+ select sensor that has the most contradiction among the rules.
        //      For now, use the sensor that creates the most even split
        int evenSplit = this.rules.size() / 2;
        int bestGap = 2*sensorSize; //best gap away from even split seen so far
        int result = TBD_INDEX;
        for(int i = 0; i < 2*sensorSize - 1; ++i) {
            int currSplit = 0;
            //count the zeroes
            for(Rule r : this.rules) {
                int bit = getBit(r, i);
                if (bit == 0) {
                    currSplit++;
                }
            }

            //ideal split?
            int gap = Math.abs(currSplit - evenSplit);
            if (gap == 0) {
                result = i;
                break;
            }

            //skip gaps that are completely uneven
            if ((currSplit == 0) || (currSplit == sensorSize)) continue;

            //best gap so far?
            if (gap < bestGap) {
                bestGap = gap;
                result = i;
            }
        }
        return result;
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
        CondSet cset = r.getLHS(); //default LHS
        if (sensorIndex >= cset.size()) { //detect RHS
            sensorIndex -= cset.size();
            cset = r.getRHS();
        }
        return cset.getBit(sensorIndex);
    }//getBit

    /**
     * split
     * <p>
     * helper method to turn a leaf node into a non-leaf with two children
     */
    private void split() {
        //bisect based upon a particular sensor
        this.splitIndex = calcSplitIndex();

        //DEBUG
        if (this.splitIndex == TBD_INDEX) {
            calcSplitIndex();
        }


        if (this.splitIndex == TBD_INDEX) return;
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
     * findRuleBin          <!-- RECURSIVE -->
     * <p>
     * finds which bin in this (sub)tree should contain a given rule.
     * The given rule need not already be present in the bin.
     * <p>
     * Important:  Don't call this method if the needed bin isn't in this (sub)tree!
     */
    private RuleIndex findRuleBin(Rule findMe) {
        //Base Case: if this is a leaf node just return it
        if (isLeaf()) {
            return this;
        }

        //depth 0:  use rule depth
        if (indexDepth == 0) {
            RuleIndex child = this.children[findMe.getDepth()];
            return child.findRuleBin(findMe);
        }

        //depth 1:  use rule's action as index
        if (indexDepth == 1) {
            // We assume actions are a, b, c, etc.  Should be ok for now.
            int childIndex = findMe.getAction() - 'a';
            RuleIndex child = this.children[childIndex];
            return child.findRuleBin(findMe);
        }

        //Depth 2+:  Find the correct child node with the splitIndex
        int childIndex = getBit(findMe, this.splitIndex);
        return this.children[childIndex].findRuleBin(findMe);
    }//findRuleBin

    /**
     * addRule
     * <p>
     * adds a new Rule to this RuleIndex object.  This may, in turn, trigger
     * a split which may trigger a re-balance.
     */
    public void addRule(Rule addMe) {
        RuleIndex bin = findRuleBin(addMe);
        bin.rules.add(addMe);
        bin.adjustRulePos();
        bin.considerSplit();
    }//addRule

    /**
     * updateBrothers         <!-- RECURSIVE -->
     * <p>
     * updates this.brother for all rules in this (sub)tree of the index
     */
    public void updateBrothers() {
        //Recursive case:  non-leaf node
        if (this.children != null) {
            for (RuleIndex ri : this.children) {
                ri.updateBrothers();
            }
            return;
        }

        //Base Case:  leaf node
        if (this.rules != null) {
            //reset all brothers
            for(Rule r : this.rules) {
                r.setBrother(null, 0.0);
            }

            //Perform N^2 comparisons (ouch) to find best match
            for(int i = 0; i < this.rules.size() - 1; ++i) {
                Rule r1 = this.rules.get(i);
                double bestMatch = r1.getBrotherScore();
                for(int j = i + 1; j < this.rules.size(); ++j) {
                    Rule r2 = this.rules.get(j);
                    double score = r1.matchScore(r2);
                    if (score > bestMatch) {
                        r1.setBrother(r2, score);
                        if (r2.getBrotherScore() < score) r2.setBrother(r1, score);
                        bestMatch = score;
                    }
                }//for
            }//for
        }//if base case

    }//updateBrothers

    /**
     * bestBrothers             <!-- RECURSIVE -->
     * <p>
     * is a helper method for {@link #reduce}.  It finds the rule in this
     * tree whose brother is the most similar to it
     */
    public Rule bestBrothers() {
        Rule bestRule = null;
        double bestScore = -1.0;
        //Recursive case:  non-leaf
        if (this.children != null) {
            for(RuleIndex child : this.children) {
                Rule cand = child.bestBrothers();
                if ((cand != null) && (cand.getBrotherScore() > bestScore)) {
                    bestScore = cand.getBrotherScore();
                    bestRule = cand;
                }
            }
        }

        //Base Case:  leaf node
        if (this.rules != null) {
            for(Rule r : this.rules) {
                double score = r.getBrotherScore();
                if (score > bestScore) {
                    bestScore = score;
                    bestRule = r;
                }
            }
        }

        return bestRule;
    }//bestBrothers

    /**
     * removeRule
     * <p>
     * removes all trace of a given rule from this index and replaces with another
     * Note: 'this' must be the RuleIndex leaf node that contains the given rules
     *
     * @param removeMe rule to remove
     * @param replacement rule that is replacing it
     */
    public void replaceRule(Rule removeMe, Rule replacement) {
        this.rules.remove(removeMe);

        //remove any case where the given rule is a brother
        for(Rule r : this.rules) {
            Rule bro = r.getBrother();
            if ( (bro != null) && (bro.getId() == removeMe.getId()) ) {
                r.setBrother(null, 0.0);
            }
        }
        updateBrothers();  //local update to this index only

        //replace this rule as a LHS internal sensor
        //get the rule index at next depth
        int desiredDepth = removeMe.getDepth() + 1;
        if (desiredDepth > Rule.MAX_DEPTH) return;
        RuleIndex searchStart = agent.getRules().children[desiredDepth];

        //Search all descendent nodes (iterative instead of recursive today.  idk why)
        Vector<RuleIndex> toCheck = new Vector<>();
        toCheck.add(searchStart);
        while(toCheck.size() > 0) {
            RuleIndex curr = toCheck.remove(0);
            if (curr.children != null) {
                for(RuleIndex ri : curr.children) {
                    toCheck.add(ri);
                }
            }
            else if (curr.rules != null) {
                for(Rule r : curr.rules) {
                    r.replacePrevRule(removeMe, replacement);
                }
            }
        }//while


    }//replaceRule

    /**
     * replaceInRuleList
     *
     * is a helper method for {@link #reduce}. It replaces r2 with r1 in a given vector
     */
    public static void replaceInRuleList(Rule r1, Rule r2, Vector<Rule> prevs) {
        for(int i = 0; i < prevs.size(); ++i) {
            Rule r = prevs.get(i);
            if (r.getId() == r2.getId()) {
                prevs.remove(i);
                prevs.insertElementAt(r1, i);
            }
        }
    }//replaceInRuleList

    /**
     * reduce
     * <p>
     * merges the two most similar rules in the index
     *
     * @param timeStep the current timeStep
     *
     * @return true if a rule was successfully removed
     */
    public boolean reduce(int timeStep) {
        //do a full update if needed
        if ((this.indexDepth == 0) && (RuleIndex.lastBrotherUpdate < timeStep)) {
            updateBrothers();
            RuleIndex.lastBrotherUpdate = timeStep;
        }

        //merge the closest matching rules
        Rule r1 = this.bestBrothers();
        Rule r2 = r1.getBrother();

        if (r2 == null) {
            return false;  //no matching rules to cull
        }

        //DEBUG: REMOVE
        if (r2.getId() == 41) {
            boolean stop = true;
        }

        //DEBUG
        System.out.println("REMOVING rule: " + r2.verboseString());
        System.out.println("  merged with: " + r1.verboseString());

        r1.mergeWith(r2);

        //DEBUG: REMOVE
        System.out.println("       to get: " + r1.verboseString());

        //remove the old rule from the index
        RuleIndex bin = findRuleBin(r2);
        bin.replaceRule(r2, r1);

        //Replace the old rule in all PathRules
        for(PathRule pr : agent.getPathRules()) {
            replaceInRuleList(r1, r2, pr.getRHS());
        }

        //Replace the old rule from the agent's prevInternal
        Vector< Vector<Rule> > allPrev = agent.getPrevInternal();
        for(Vector<Rule> prevs : allPrev) {
            replaceInRuleList(r1, r2, prevs);
        }

        //Replace the old rule in the agent's curr internal
        Vector<Rule> currs = agent.getCurrInternal();
        replaceInRuleList(r1, r2, currs);

        //Clean up the old rule's data to encourage garbage collection
        r2.cleanup();


        return true;
    }//reduce


    /**
     * class MatchResult
     * <p>
     * contains rule and its match score.  Used as a result by
     * {@link #findMatches} and its helpers.
     */
    public static class MatchResult {
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
     * @param currExtBits may be empty if you only want to match LHS
     */
    private void matchHelper(Vector<MatchResult> results, Vector<Rule> prevInternal,
                             CondSet prevExtBits, CondSet currExtBits) {
        //Recursive case:  non-leaf node
        if (this.children != null) {
            //Retrieve the experience's bit that this rule splits on
            int bitIndex = this.splitIndex;
            int bit = -1;  //default is neither 0 nor 1
            //retrieve bit from LHS
            if (bitIndex < prevExtBits.size()) {
                bit = prevExtBits.getBit(bitIndex);
            }
            //retrieve bit from RHS
            else if (currExtBits.size() > (bitIndex - prevExtBits.size())) {
                bitIndex -= prevExtBits.size();
                bit = currExtBits.getBit(bitIndex);
            }

            //Recurse into matching child(ren)
            if (bit == 0) {
                this.children[0].matchHelper(results, prevInternal, prevExtBits, currExtBits);
            }
            else {
                this.children[1].matchHelper(results, prevInternal, prevExtBits, currExtBits);
            }
        }//non-leaf

        //Base Case:  leaf node
        else if ((this.rules != null) && (this.rules.size() > 0)) {
            for(Rule r : this.rules) {
                double score = r.matchScore(prevInternal, prevExtBits, currExtBits);
                MatchResult mr = new MatchResult(r, score);
                results.add(mr);

                //DEBUG: print the match results
                //System.out.println("Matching bin " + this + ": " + r + " match score: " + score);
            }//for each rule in this node
        }//else if leaf node
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
                                    Vector<Rule> list, Vector<Rule> found) {
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
    public static String ruleIdListStr(Vector<Rule> list) {
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
     * @return the list of rules found and their associated match scores
     *         (in sorted order by index depth)
     */
    public Vector<MatchResult> findMatches(Vector<Rule> prevInternal,
                                           CondSet prevExtBits,
                                           char act,
                                           CondSet currExtBits) {
        Vector<MatchResult> results = new Vector<>();  //accumulates matching rules
        if (this.indexDepth != 0) {
            System.err.println("ERROR:  findMatches called on non-root node.");
            return results;
        }

        //Find matches at each indexDepth
        int piIndex = 0;  //declare this here so it doesn't get reset to 0 in the loop
        Vector<Rule> lilPI = new Vector<>();
        for(int depth = 0; depth <= Rule.MAX_DEPTH; ++depth) {
            //Find the subtree for this depth and action
            int actIndex = act - 'a';
            RuleIndex d2node = this.children[depth].children[actIndex];

            //Find the members of prevInternal at this depth
            piIndex = extractRulesOfDepth(piIndex, depth - 1, prevInternal, lilPI);

            //Retrieve the matching rules
            d2node.matchHelper(results, lilPI, prevExtBits, currExtBits);
        }//for each depth

        return results;
    }//findMatches

    /**
     * findMatches
     * <p>
     * convenience version of {@link #findMatches(Vector, CondSet, char, CondSet)}
     * that can handle SensorData and/or null inputs
     * <p>
     * @param prevInternal must be sorted by indexDepth.  If the value is
     *                     'null' then only level 0 rules can be found.
     * @param currExternal if this parameter is null, then only the LHS
     *                     of rules need to match.
     */
    public Vector<MatchResult> findMatches(Vector<Rule> prevInternal,
                                           SensorData prevExternal,
                                           char act,
                                           SensorData currExternal) {
        //Convert the SensorData to CondSet for matching
        if (prevExternal == null) prevExternal = SensorData.createEmpty();
        CondSet prevExtBits = new CondSet(prevExternal);
        if (currExternal == null) currExternal = SensorData.createEmpty();
        CondSet currExtBits = new CondSet(currExternal);

        return findMatches(prevInternal, prevExtBits, act, currExtBits);

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
        if (r == null) return "<no-rule>";
        int depth = r.getDepth();
        int actIndex = r.getAction() - 'a';
        RuleIndex node = agent.getRules().children[depth].children[actIndex];

        //Generate starting sensors string (all wildcards)
        //Note: JDK 11 has String.repeat() method for this.  Ok to use?
        StringBuilder sbSensors = new StringBuilder();
        for(int i = 0; i < r.getBitsLen(); ++i) {
            sbSensors.append('.');
        }

        //Fill in the known bits from this node and any parents
        while(node != this) {
            int bit = getBit(r, node.splitIndex);
            sbSensors.replace(node.splitIndex, node.splitIndex + 1, "" + bit);

            //DEBUG
            if (node.children == null) {
                break;  //should not happen
            }

            node = node.children[bit];
        }//while

        //Create the result
        int halfway = r.getBitsLen() / 2;
        String lhs = sbSensors.substring(0, halfway);
        String rhs = sbSensors.substring(halfway);
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
        System.out.print(("+-" + this).indent(this.indexDepth * 2));

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
