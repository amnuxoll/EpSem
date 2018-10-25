package agents.juno;

public class JunoConfiguration {
    private double bailSlider;
    private boolean canBail;

    public static JunoConfiguration DEFAULT=
            new JunoConfiguration(false, 0);

    public JunoConfiguration(boolean canBail, double bailSlider){
        this.bailSlider= bailSlider;
        this.canBail= canBail;
    }

    public double getBailSlider(){
        return bailSlider;
    }

    public boolean getCanBail(){
        return canBail;
    }
}
