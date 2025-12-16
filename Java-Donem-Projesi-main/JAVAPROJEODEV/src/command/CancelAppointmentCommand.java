package command;

import services.AppointmentService;

public class CancelAppointmentCommand implements Command {
    private AppointmentService appointmentService;
    private String appointmentId;

    public CancelAppointmentCommand(AppointmentService appointmentService, String appointmentId) {
        this.appointmentService = appointmentService;
        this.appointmentId = appointmentId;
    }

    @Override
    public void execute() {
        // Bu komut çalışınca servisteki iptal metodunu çağıracak
        appointmentService.cancelAppointment(appointmentId);
    }
}