package agents.phujus;

import framework.SensorData;

import java.util.HashSet;
import java.util.Vector;

/**
 * class PathRule
 *
 * Describes a rule that looks like this:
 *   external sensors + path + external sensors + path -> external sensors
 *
 *
 */
public class PathRule {

    private SensorData firstExternal;
    private SensorData secondExternal;
    private SensorData rhsExternal = null;

    //A path is described by a single TreeNode that was the last node in the path
    private TreeNode firstPath;
    private TreeNode secondPath;

    public PathRule(SensorData initFirstExt, TreeNode initFirstPath,
                    SensorData initSecondExt, TreeNode initSecondPath) {
        this.firstExternal = initFirstExt;
        this.firstPath = initFirstPath;
        this.secondExternal = initSecondExt;
        this.secondPath = initSecondPath;
    }

    public void setRHS(SensorData initRHS) {
        this.rhsExternal = initRHS;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(TreeNode.extToString(this.firstExternal));
        result.append(firstPath.getPathStr());
        result.append("|");
        result.append(TreeNode.extToString(this.secondExternal));
        result.append(secondPath.getPathStr());
        result.append(" -> ");
        if (this.rhsExternal == null) {
            result.append("null");
        } else {
            result.append(TreeNode.extToString(this.rhsExternal));
        }

        return result.toString();
    }//toString

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof PathRule)) return false;
        return obj.toString().equals(this.toString());
    }

    /**
     * @return 'true' if this PathRule matches a given set of inputs
     */
    public boolean matches(Vector<TreeNode> path1, Vector<TreeNode> path2) {
        if (! path1.firstElement().getCurrExternal().equals(this.firstExternal)) return false;
        if (! path1.lastElement().getPathStr().equals(this.firstPath.getPathStr())) return false;
        if (! path1.lastElement().getCurrExternal().equals(this.secondExternal)) return false;
        if (! path2.lastElement().getPathStr().equals(this.secondPath.getPathStr())) return false;
        return true;
    }
}//class PathRule
