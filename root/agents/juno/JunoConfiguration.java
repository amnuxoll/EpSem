package agents.juno;

public class JunoConfiguration {
    private double bailSlider;
    private boolean canBail;
    //maturity to start bailing
    private double maturity;

    public static JunoConfiguration DEFAULT=
            new JunoConfiguration(false, 0, -1);

    public JunoConfiguration(boolean canBail, double bailSlider, double maturity){
        this.bailSlider= bailSlider;
        this.canBail= canBail;
        this.maturity= maturity;
    }

    public double getBailSlider(){
        return bailSlider;
    }

    public boolean getCanBail(){
        return canBail;
    }

    public double getMaturity() {
        return maturity;
    }
}
