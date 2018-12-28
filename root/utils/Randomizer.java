package utils;

import java.util.Random;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class Randomizer {
    //region Class Variables
    private Random random;
    //endregion

    //region Constructors
    public Randomizer() {
        this(System.currentTimeMillis());
    }

    public Randomizer(long seed){
        this.random= new Random(seed);
    }
    //endregion

    //region Public Methods
    public int getRandomNumber(int ceiling) {
        return this.random.nextInt(ceiling);
    }
    //endregion
}
