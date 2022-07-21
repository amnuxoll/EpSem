package agents.phujus;

import framework.SensorData;

import java.util.Collection;
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

    private HashSet<PathRule> lhs = new HashSet<>();  //can be empty but not null
    private Vector<TreeNode> rhs;  //must contain at least one TreeNode

    /** This variable contains a list of actions that this PathRule represents.
     *  Since a PathRule's LHS has multiple PathRules, it actually represents
     *  multiple paths.  This variable stores only the portion of the path
     *  that all the lhs PathRules agree upon.  Notably if paths are different
     *  lengths, but otherwise match, the longer is used.
     *  Examples:
     *          lhs            flat    //TODO: add extern
     *          abaa, bbaa  -> baa
     *          abaab, a    -> abaab
     *          a, ba, baa  -> a
     */
    private String flat;

    /** Each time this PathRule is used to evaluate a path a new PathRule may get
     * created that is a more specific version of this one.  Such PathRules might
     * remain "nascent" (i.e., not added to PJA.pathRules list for use right
     * away) as long as this rule has been effective.  However, if this rule
     * has mixed effectiveness, the nascent Rules can be activated to effectively
     * override this one.  These "spawn" rules are stored in two sets (below)
     * dpeending upon whether they are an example of this rule being correct
     * or incorrect.  */

    HashSet<PathRule> positive = new HashSet<>();
    HashSet<PathRule> negative = new HashSet<>();
    /** a nascent rule is only being used as an example and is not in PJA.pathRules */
    boolean isNascent = false;
    /** a rule that comes into conflict (both positive and negative examples) is
     * "split", meaning it's example rules are merged and added to PJA.pathRules.
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

        updateFlat();
    }



//endregion ctors and initialization



    /** calculates how many chars of two given string match (counting from the end)
     * Examples:  overlap("fop", "bop") == 2
     *            overlap("pof", "pob") == 0;
     */
    public static int overlapLen(String s1, String s2) {
        int s1len = s1.length();
        int s2len = s2.length();
        int shortest = Math.min(s1len, s2len);
        int pos = 1;
        int result = 0;
        while(pos <= shortest) {
            char c1 = s1.charAt(s1len - pos);
            char c2 = s2.charAt(s2len - pos);
            if (c1 != c2) break;
            result++;
            pos++;
        }//while

        return result;
    }//overlapLen

    /**
     * genFlat
     *
     * generates the flat version of a given lhs and rhs of a PathRule.
     * See the comment above this.flat for details.

     * Note: This method is public/static because it's also used by PJA.
     */
    public static String genFlat(HashSet<PathRule> lhs, Vector<TreeNode> rhs) {
        //TODO:  I don't like appending the final external sensors as I've
        // done here as it means the agent will be brittle with random
        // external sensors.  A TF-IDF partial matching is needed or, as
        // per MaRz, just assume all paths go to a Goal/Reward state.
        // I don't like the latter either since we will eventually be in FSMs
        // that are large enough that you have to plan multiple paths to
        // get to a goal. So...partial matching is the best solution I see atm.
        String result = rhs.lastElement().getPathStr() + rhs.lastElement().getCurrExternal().toStringShort();
        if ((lhs != null) && (lhs.size() > 0)) {

            //Gather all the flats and find the longest (we need it below)
            String longest = "";
            int llen = 0;
            String[] flats = new String[lhs.size()];
            int i = 0;
            for(PathRule pr : lhs) {
                flats[i] = pr.getFlat();
                int flen = flats[i].length();
                if (flen > llen) {
                    longest = flats[i];
                    llen = flen;
                }
                i++;
            }

            //Generate a string from the intersection of lhs PathRules
            StringBuilder combo = new StringBuilder(longest);
            for (String flat : flats) {
                int overlap = overlapLen(combo.toString(), flat);
                if ( (overlap < combo.length()) && (overlap < flat.length()) ) {
                    combo.delete(0, combo.length() - overlap);
                    if (combo.length() == 0) break;
                }
            }//for

            result = combo + result;
        }//if

        return result;
    }//genFlat

    /**
     * updateFlat
     *
     * should be called each time a PathRule is modified so that the flat
     * representation is kept up to date.
     *
     * @return the new flat
     */
    public String updateFlat() {
        this.flat = genFlat(this.lhs, this.rhs);
        return this.flat;
    }//updateFlat



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
        if (other.lhs.size() == 0) return 0;

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
     * insertIntoCategoryList
     *
     * is a helper for {@link #lhsCategories(HashSet)} that adds a PathRule to a category.
     */
    private void insertIntoCategoryList(HashMap<Integer, HashSet<PathRule>> result, PathRule pr, int id) {
        HashSet<PathRule> val = null;
        if (result.containsKey(id)) {
            val = result.get(id);
        } else {
            val = new HashSet<PathRule>();
        }
        val.add(pr);
        result.put(id, val);
    }

    /**
     * lhsCategories
     *
     * groups a given set of PathRules by by LHS items they have in common
     *
     *
     */
    private HashMap<Integer, HashSet<PathRule> > lhsCategories(HashSet<PathRule> list) {
        HashMap<Integer, HashSet<PathRule> > result = new HashMap<Integer, HashSet<PathRule> >();

        for(PathRule pr : list) {
            if (pr.lhs.size() == 0) {
                //Any empty LHS is indexed by -1
                insertIntoCategoryList(result, pr, -1);
            } else {
                //For each PathRule that is in the LHS of the given list:
                //add an entry to 'result' to record that
                for (PathRule lhsItem : pr.lhs) {
                    int id = lhsItem.ruleId;
                    insertIntoCategoryList(result, pr, id);
                }
            }
        }//for


        return result;
    }//lhsCategories

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
        HashSet<PathRule> largest = null;
        int largestSize = -1;
        Integer largestKey = -1;
        for(Integer key : cats.keySet()) {
            HashSet<PathRule> cat = cats.get(key);
            if (cat.size() > largestSize) {
                largestSize = cat.size();
                largest = cat;
                largestKey = key;
            }
        }

        //If there are 2+ PathRules then they need to be merged
        PathRule merged = null;
        if (largestSize == 1) {
            merged = (PathRule)largest.toArray()[0];  // no merging necessary
            merged.isNascent = false;
        } else {
            //Create a new LHS that is the union of all the LHS of the category
            HashSet<PathRule> unionLHS = new HashSet<>();
            for (PathRule pr : largest) {
                unionLHS.addAll(pr.lhs);
            }

            //Because they share a common ancestor, the RHS path of all these
            // rules must be the same.  However, the internal sensors in the
            // path can be different. Create a new RHS that has the intersection
            // of internal sensors in each TreeNode of the path
            Vector<TreeNode> intersectRHS = null;
            for (PathRule pr : largest) {
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
                    intersectTN.getCurrInternal().retainAll(prTN.getCurrInternal());
                }
            }//for

            //Now create the merged PathRule
            merged = new PathRule(this.agent, null, intersectRHS);
            merged.lhs = unionLHS;
            merged.positive = largest;
        }//else (merge required)

        //Add this merged rule to the agent
        this.agent.addRule(merged);

        //Remove all the newly merged rules from the categories
        cats.remove(largestKey);
        for(PathRule largePR : largest) {
            for(HashSet<PathRule> cat : cats.values()) {
                if (cat.contains(largePR)) cat.remove(largePR);
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

        //If any ids appearin both positive & negative then omit it
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
        HashSet<PathRule> mergedPos = mergeByCategory(posCats);
        HashSet<PathRule> mergedNeg = mergeByCategory(negCats);
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
        result.append(" pos:" + this.positive.size() + " neg:" + this.negative.size());

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
        if (this.ruleId == other.ruleId) return true;

        //If they match, consider them equal
        int matchLen = this.matchLen(other.lhs, other.rhs);
        return (matchLen == this.length());
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
    public SensorData getRHSExternal() { return this.rhs.lastElement().getCurrExternal(); }
    public String getFlat() { return this.flat; }
    public void addExample(PathRule pr, boolean correct) {
        pr.isNascent = true;
        if (correct) this.positive.add(pr);
        else this.negative.add(pr);
        //TODO: enforce a max size for this.positive and this.negative?
        //      perhaps better to globally remove PRs that aren't getting used
    }
    public boolean hasConflict() { return ((this.positive.size() > 0) && (this.negative.size() > 0)); }
    public boolean hasSplit() { return this.hasSplit; }

}//class PathRule
