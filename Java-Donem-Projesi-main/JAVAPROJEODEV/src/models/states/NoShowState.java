package models.states;

public class NoShowState implements AppointmentState {
    @Override
    public String getStatus() { return "NO_SHOW"; }
}
