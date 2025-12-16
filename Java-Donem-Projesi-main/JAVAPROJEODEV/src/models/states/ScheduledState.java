package models.states;

public class ScheduledState implements AppointmentState {
    @Override
    public String getStatus() { return "SCHEDULED"; }
}
