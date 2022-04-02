module pcb.pcbanalyse {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens pcb.pcb to javafx.fxml;
    exports pcb.pcb;
}