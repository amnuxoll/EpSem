package environments.fsm;

import framework.Move;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class FSMTransitionTableTest {
    //region Constructor Tests
    @Test
    public void constructorNullTransitionsThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMTransitionTable(null));
    }

    @Test
    public void constructorValidatesTransitionTableNotAllTransitionsContainSameMoveCount() {
        HashMap<Move, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Move("a"), 1);
        transitionSet1.put(new Move("b"), 1);
        transitionSet1.put(new Move("c"), 1);
        HashMap<Move, Integer> transitionSet2 = new HashMap<>();
        transitionSet2.put(new Move("a"), 1);
        transitionSet2.put(new Move("b"), 1);
        HashMap<Move, Integer>[] transitionTable = new HashMap[] {
                transitionSet1,
                transitionSet2
        };
        assertThrows(IllegalArgumentException.class, () -> new FSMTransitionTable(transitionTable));
    }

    @Test
    public void constructorValidatesTransitionTableNotAllTransitionsContainSameMoves() {
        HashMap<Move, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Move("a"), 1);
        transitionSet1.put(new Move("b"), 1);
        transitionSet1.put(new Move("c"), 1);
        HashMap<Move, Integer> transitionSet2 = new HashMap<>();
        transitionSet2.put(new Move("a"), 1);
        transitionSet2.put(new Move("b"), 1);
        transitionSet2.put(new Move("d"), 1);
        HashMap<Move, Integer>[] transitionTable = new HashMap[] {
                transitionSet1,
                transitionSet2
        };
        assertThrows(IllegalArgumentException.class, () -> new FSMTransitionTable(transitionTable));
    }
    //endregion
}
