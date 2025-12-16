package models.states;

public class CompletedState implements AppointmentState {
    @Override
    public String getStatus() { return "COMPLETED"; }
}
