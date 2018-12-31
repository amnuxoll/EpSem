package agents.juno;

public class JunoConfiguration {
    //region Static JunoConfigurations
    public static JunoConfiguration DEFAULT = new JunoConfiguration(false, 0, -1);
    //endregion

    //region Class Variables
    private double bailSlider;
    private boolean canBail;
    //maturity to start bailing
    private double maturity;
    //endregion

    //region Constructors
    public JunoConfiguration(boolean canBail, double bailSlider, double maturity){
        this.bailSlider= bailSlider;
        this.canBail= canBail;
        this.maturity= maturity;
    }
    //endregion

    //region Public Methods
    public double getBailSlider(){
        return bailSlider;
    }

    public boolean getCanBail(){
        return canBail;
    }

    public double getMaturity() {
        return maturity;
    }
    //endregion
}
