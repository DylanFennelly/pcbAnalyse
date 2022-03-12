module pcb.pcbanalyse {
    requires javafx.controls;
    requires javafx.fxml;


    opens pcb.pcb to javafx.fxml;
    exports pcb.pcb;
}