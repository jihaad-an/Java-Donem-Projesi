package models.states;

public class CancelledState implements AppointmentState {
    @Override
    public String getStatus() { return "CANCELLED"; }
}
