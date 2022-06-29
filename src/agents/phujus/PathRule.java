package agents.phujus;

import framework.SensorData;

import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;

/**
 * class PathRule
 *
 * Describes a rule that describes the expected outcome of a sequence of paths.
 *
 */
public class PathRule extends Rule {

    private HashSet<PathRule> lhs = new HashSet<>();  //can be empty but not null
    private Vector<TreeNode> rhs;  //must contain at least one TreeNode

    /** This variable contains a list of actions that this PathRule represents.
     *  Since a PathRule's LHS has multiple PathRules, it actually represents
     *  multiple paths.  This variable stores only the portion of the path
     *  that all the lhs PathRules agree upon.  Notably if paths are different
     *  lengths, but otherwise match, the longer is used.
     *  Examples:
     *          lhs            flat
     *          abaa, bbaa  -> baa
     *          abaab, a    -> abaab
     *          a, ba, baa  -> a
     */
    private String flat;

    /** ctor
     *
     * note:  initLHS may be null
     * */
    public PathRule(PhuJusAgent initAgent, Collection<PathRule> initLHS, Vector<TreeNode> initRHS) {
        super(initAgent);

        if (initLHS != null) {
            this.lhs = new HashSet<>(initLHS);
        }

        //must make a copy because initRHS is often PJA.pathTraversedSoFar which changes
        this.rhs = new Vector<>(initRHS);

        updateFlat();
    }//ctor

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
        String result = rhs.lastElement().getPathStr();
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
        for(int i = 0; i < this.rhs.size(); ++i) {
            TreeNode myNode = this.rhs.get(i);
            TreeNode otherNode = matRHS.get(i);
            //note:  a perfect match isn't required: nearEquals is used.
            if (! myNode.nearEquals(otherNode)) return false;
        }

        return true;
    }//rhsMatch

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
    public SensorData getRHSExternal() { return this.rhs.lastElement().getCurrExternal(); }
    public String getFlat() { return this.flat; }

}//class PathRule
