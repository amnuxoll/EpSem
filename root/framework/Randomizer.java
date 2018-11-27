package framework;

import java.util.Random;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class Randomizer implements IRandomizer {
    private Random random;

    public Randomizer() {
        this(System.currentTimeMillis());
    }

    public Randomizer(long seed){
        this.random= new Random(seed);
    }

    @Override
    public int getRandomNumber(int ceiling) {
        return this.random.nextInt(ceiling);
    }
}
