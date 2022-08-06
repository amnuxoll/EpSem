package agents.phujus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

/**
 * class PathRule
 *
 * Describes a rule that describes the expected outcome of a sequence of paths.
 *
 */
public class PathRule extends Rule {

//region Instance Variables

    /** lhs is a set of existing PathRules.  One of which must precede this one */
    private HashSet<PathRule> lhs = new HashSet<>();  //can be empty but not null
    /** rhs is a path */
    private final Vector<TreeNode> rhs;  //must contain at least one TreeNode

    /** Each time the agent follows a path that matches this one new PathRule may get
     * created that is a more specific version of this one.  Such PathRules are
     * initially "nascent" (i.e., not added to PJA.pathRules list for use right
     * away) as long as this rule has been effective.  However, if this rule
     * has mixed effectiveness, the nascent Rules can be activated to effectively
     * override this one.  Nascent rules are stored in two sets (below)
     * dpeending upon whether they are an example of this rule being correct
     * or incorrect.  */
    HashSet<PathRule> positive = new HashSet<>();
    HashSet<PathRule> negative = new HashSet<>();

    /** a nascent rule is only being used as an example and is not in PJA.pathRules */
    boolean isNascent = false;

    /** a rule that comes into conflict (both positive and negative examples) is
     * "split", meaning it's nascent, example rules are added to PJA.pathRules.
     */
    private boolean hasSplit = false;

//endregion Instance Variables

//region ctors and initialization

    /** ctor for lhs init from given PathRule */
    public PathRule(PhuJusAgent initAgent, PathRule initLHS, Vector<TreeNode> initRHS) {
        super(initAgent);
        if (initLHS != null) this.lhs.add(initLHS);

        //must make a copy because initRHS is often PJA.pathTraversedSoFar which changes
        this.rhs = new Vector<>(initRHS);
    }



//endregion ctors and initialization

    //TODO:  Should rhsMatch and lhsMatch be doing partial matches??
    // The nearEquals() method lets us dodge this for now but not sure we should.

    /**
     * rhsMatch
     *
     * Determines if a given Vector<TreeNode> matches this rule's rhs.
     */
    public boolean rhsMatch(Vector<TreeNode> matRHS) {
        if (matRHS.size() != this.rhs.size()) return false;
        for(int i = this.rhs.size() - 1; i >= 0; --i) {
            TreeNode myNode = this.rhs.get(i);
            TreeNode otherNode = matRHS.get(i);
            //note:  a perfect match isn't required: nearEquals is used.
            if (! myNode.nearEquals(otherNode)) return false;
        }

        return true;
    }//rhsMatch

    /** convenience overload for comparing to a rule */
    public boolean rhsMatch(PathRule other) {
        return rhsMatch(other.rhs);
    }

    /**
     * lhsMatch
     *
     * compares the lhs of a given PathRule to this one
     *
     * @return a match score:
     *              -1 (no match)
     *               0 (wildcard match)
     *              +N (this many PathRules in common)
     */
    public int lhsMatch(PathRule other) {
        //empty LHS is treated as a wildcard (base PathRule)
        if (this.lhs.size() == 0) return 0;
        if ( (other == null) || (other.lhs.size() == 0) ) return 0;

        //Any overlap is a match
        int matches = 0;
        for(PathRule thisPR : this.lhs) {
            for(PathRule otherPR : other.lhs) {
                if (thisPR.getId() == otherPR.getId()) matches++;
            }
        }

        //if no matches, then they are incompatible
        if (matches == 0) return -1;
        return matches;
    }//lhsMatch

    /**
     * replaceAll
     *
     * replaces all uses of one TFRule in this PathRule with another
     *
     */
    public void replaceAll(TFRule removeMe, TFRule replacement) {
        //Replace all uses on the RHS
        for(TreeNode tn : this.rhs) {
            for(HashSet<TFRule> subset : tn.getCurrInternal()) {
                if (subset.contains(removeMe)) {
                    subset.remove(removeMe);
                    if (replacement != null) {
                        subset.add(replacement);
                    }
                    break;  //a rule shouldn't appear at more than 1 time depth
                }
            }
        }

        //replace all uses in nascent rules' rhs
        if (!this.isNascent  && !this.hasSplit) {
            for(PathRule pr : this.positive) {
                pr.replaceAll(removeMe, replacement);
            }
            for(PathRule pr : this.negative) {
                pr.replaceAll(removeMe, replacement);
            }
        }

    }//replaceAll


    /**
     * insertIntoCategoryList
     *
     * is a helper for {@link #lhsCategories(HashSet)} that adds a PathRule to a category.
     */
    private void insertIntoCategoryList(HashMap<Integer, HashSet<PathRule>> result, PathRule pr, int id) {
        HashSet<PathRule> val;
        if (result.containsKey(id)) {
            val = result.get(id);
        } else {
            val = new HashSet<>();
        }
        val.add(pr);
        result.put(id, val);
    }

    /**
     * lhsCategories
     *
     * groups a given set of PathRules by LHS items that they have in common
     *
     *
     */
    private HashMap<Integer, HashSet<PathRule> > lhsCategories(HashSet<PathRule> list) {
        HashMap<Integer, HashSet<PathRule> > result = new HashMap<>();

        for(PathRule pr : list) {
            if (pr.lhs.size() == 0) {
                //Any empty LHS is indexed by -1
                insertIntoCategoryList(result, pr, -1);
            } else {
                //For each PathRule that is in the LHS of the given list:
                //add an entry to 'result' to record that
                for (PathRule lhsItem : pr.lhs) {
                    insertIntoCategoryList(result, pr, lhsItem.ruleId);
                }
            }
        }//for


        return result;
    }//lhsCategories

    /** merges all the rules in a specified category to create a single PathRule
     * that is consistent with them all */
    private PathRule mergeCategory(HashSet<PathRule> cat) {
        PathRule result;
        //If there is only one rule in a category then no merging needed
        if (cat.size() == 1) {
            result = (PathRule) cat.toArray()[0];
            result.isNascent = false;
        }
        //If there are 2+ PathRules then they need to be merged
        else {
            //Create a new LHS that is the union of all the LHS of the category
            HashSet<PathRule> unionLHS = new HashSet<>();
            for (PathRule pr : cat) {
                unionLHS.addAll(pr.lhs);
            }

            //Because they share a common ancestor, the RHS path of all these
            // rules must be the same.  However, the internal sensors in the
            // path can be different. Create a new RHS that has the intersection
            // of internal sensors in each TreeNode of the path
            Vector<TreeNode> intersectRHS = null;
            for (PathRule pr : cat) {
                //Initialize at first iteration as it's tricky to do before the loop starts
                if (intersectRHS == null) {
                    intersectRHS = new Vector<>(pr.rhs);
                    continue;
                }

                //Update the intersection with this PR.  I.e., any internal
                // sensor missing in pr is removed from intersectRHS.
                for (int i = 0; i < intersectRHS.size(); ++i) {
                    TreeNode intersectTN = intersectRHS.get(i);
                    TreeNode prTN = pr.rhs.get(i);
                    int shallowest = Math.min(intersectTN.getCurrInternal().size(),
                                              prTN.getCurrInternal().size());
                    for(int depth = 0; depth < shallowest; ++depth) {
                        intersectTN.getCurrInternal(depth).retainAll(prTN.getCurrInternal(depth));
                    }
                }
            }//for

            //Now create the merged PathRule
            result = new PathRule(this.agent, null, intersectRHS);
            result.lhs = unionLHS;
            result.positive = cat;
        }//else (merge required)
        return result;
    }//mergeCategory

    /** removes all the PathRules from a given category list that are in a
     * given category */
    private void removeCategory(HashMap<Integer, HashSet<PathRule>> cats, Integer catKey) {
        HashSet<PathRule> toRemove = cats.get(catKey);
        cats.remove(catKey);
        for(PathRule largePR : toRemove) {
            for(HashSet<PathRule> cat : cats.values()) {
                cat.remove(largePR);
            }
        }

        //Clean up any empty categories
        Vector<Integer> removeMe = new Vector<>();
        for(Integer catId : cats.keySet()) {
            if (cats.get(catId).size() == 0) {
                removeMe.add(catId);
            }
        }
        for(Integer catId : removeMe) {
            cats.remove(catId);
        }
    }//removeCategory


    /**
     * mergeByCategory
     *
     * merges a given categorized set of PathRules into a smaller set.
     * With a lot more code, it's possible to create more optimal sets
     * but this is faster, less bug prone, and likely right most of the time.
     *
     * CAVEAT:  This method is recursive and also destroys the given category list
     *
     * @param cats  the categorized set (likely generated from {@link #lhsCategories}
     *
     * @return the merged rules or null if there are no rules in the set
     */
    private HashSet<PathRule> mergeByCategory(HashMap<Integer, HashSet<PathRule>> cats) {
        if (cats.size() == 0) return null; //base case

        //Find the largest category
        HashSet<PathRule> largestCat = null;
        int largestSize = -1;
        Integer largestKey = -1;
        for(Integer key : cats.keySet()) {
            HashSet<PathRule> cat = cats.get(key);
            if (cat.size() > largestSize) {
                largestSize = cat.size();
                largestCat = cat;
                largestKey = key;
            }
        }

        //Merge this largest category
        PathRule merged = mergeCategory(largestCat);

        //Add this merged rule to the agent
        this.agent.addRule(merged);

        //Remove all the newly merged rules from the categories
        removeCategory(cats, largestKey);

        //Now make a recursive call (yikes!) to get the remaining merged rules
        HashSet<PathRule> result = mergeByCategory(cats);
        if (result == null) {
            result = new HashSet<>();
        }

        result.add(merged);
        return result;

    }//mergeByCategory

    /**
     * split
     *
     * activates the nascent rules in this.positive/negative.  Those that are similar are combined.
     */
    public void split() {
        HashMap<Integer, HashSet<PathRule> > posCats = lhsCategories(this.positive);
        HashMap<Integer, HashSet<PathRule> > negCats = lhsCategories(this.negative);

        //If any ids appear in both positive & negative then omit them
        Vector<Integer> dups = new Vector<>();
        for(int posId : posCats.keySet()) {
            for(int negId : negCats.keySet()) {
                if (posId == negId) {
                    dups.add(posId);
                }
            }
        }
        for(Integer i : dups) {
            posCats.remove(i);
            negCats.remove(i);
        }

        //Special case:  one or both categories are empty so abort the split
        if ( (posCats.size() == 0) || (negCats.size() == 0)) {
            return;
        }

        //merge rules that have overlap to create as few new rules as possible
        mergeByCategory(posCats);
        mergeByCategory(negCats);
        this.hasSplit = true;
    }//split

    /**
     * matchLen
     *
     * calculates how well this PathRule matches given data.  Since PathRule
     * is a recursive data structure, the match score is how far back into
     * the past the match persists.
     *
     * Caveat:  this is a recursive method!
     *
     * @return the length of the match
     */
    public int matchLen(HashSet<PathRule> matLHS, Vector<TreeNode> matRHS) {
        //Match the RHS
        if (!rhsMatch(matRHS)) return 0;

        //Nasty super-recursive compare (yikes!)
        int bestLen = 0;
        for(PathRule thisPR : this.lhs) {
            for(PathRule otherPR : matLHS) {
                int len = thisPR.matchLen(otherPR.lhs, otherPR.rhs);
                if (len > bestLen) {
                    bestLen = len;
                }
            }
        }

        return 1 + bestLen;
    }//matchLen


    /** helper for toString that just prints the LHS. */
    private void toStringLHS(StringBuilder result) {
        result.append("#");
        result.append(this.ruleId);
        result.append(": (");
        boolean first = true;
        for(PathRule pr : this.lhs) {
            result.append(first ? "" : ",");
            first = false;
            result.append(pr.ruleId);
        }
        result.append(") -> ");
    }//toStringConf

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        toStringLHS(result);

        //print a short version first
        result.append(this.rhs.lastElement().getPathStr());
        result.append(":");
        result.append(this.rhs.lastElement().getCurrExternal().toStringShort());
        result.append(String.format(" ^  conf=%.5f", getConfidence()).replaceAll("0+$", "0"));
        result.append(" pos:");
        result.append(this.positive.size());
        result.append(" neg:");
        result.append(this.negative.size());

        //now print the details
        result.append("  [");
        for(TreeNode node : this.rhs) {
            result.append(node.toString(false));
            result.append("; ");
        }
        result.append("]");

        return result.toString();
    }//toString

    /** a shorter string format designed to be used inline */
    @Override
    public String toStringShort() {
        StringBuilder result = new StringBuilder();
        toStringLHS(result);
        result.append(this.rhs.lastElement().getPathStr());
        result.append(":");
        result.append(this.rhs.lastElement().getCurrExternal().toStringShort());

        return result.toString();
    }//toStringShort


    public boolean equals(Object obj) {
        if (! (obj instanceof PathRule)) return false;
        PathRule other = (PathRule) obj;

        //See if we can save some time:
        return (this.ruleId == other.ruleId);
    }

    /**
     * length
     *
     * Caveat:  this is a recursive method!
     *
     * @return the time depth of this rule
     */
    public int length() {
        //Base Case
        if (this.lhs == null) return 1;

        //Recursive Case
        int bestLen = 0;
        for(PathRule pr : this.lhs) {
            int len = pr.length();
            if (len > bestLen) {
                bestLen = len;
            }
        }
        return 1 + bestLen;
    }//size

    /** get the final sensor data of this path */
    public Vector<TreeNode> cloneRHS() { return new Vector<>(this.rhs); }
    public int lhsSize() { return this.lhs.size(); }
    public boolean lhsContains(PathRule other) { return this.lhs.contains(other); }
    public boolean hasConflict() { return ((this.positive.size() > 0) && (this.negative.size() > 0)); }
    public boolean hasSplit() { return this.hasSplit; }

    public void addExample(PathRule pr, boolean correct) {
        pr.isNascent = true;
        if (correct) {
            this.positive.add(pr);
            //Keep to a max size (arbitrarily 10)
            while (this.positive.size() > 10) {
                PathRule oldest = null;
                for(PathRule ex : this.positive) {
                    if ((oldest == null) || (oldest.ruleId > ex.ruleId)) {
                        oldest = ex;
                    }
                }
                this.positive.remove(oldest);
            }
        }
        else {
            this.negative.add(pr);
            //Keep to a max size (arbitrarily 10)
            while (this.negative.size() > 10) {
                PathRule oldest = null;
                for(PathRule ex : this.negative) {
                    if ((oldest == null) || (oldest.ruleId > ex.ruleId)) {
                        oldest = ex;
                    }
                }
                this.negative.remove(oldest);
            }
        }
        //TODO: enforce a max size for this.positive and this.negative?
        //      perhaps better to globally remove PRs that aren't getting used
    }


}//class PathRule
