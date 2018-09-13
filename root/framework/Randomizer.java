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
        this.random = new Random(System.currentTimeMillis());
    }

    @Override
    public int getRandomNumber(int ceiling) {
        return this.random.nextInt(ceiling);
    }
}
