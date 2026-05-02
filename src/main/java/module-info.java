/**
 * CDIEM desktop application module.
 * Exposes the JavaFX application and opens controllers so FXML can inject UI fields.
 */
module com.project.cdiem {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.microsoft.sqlserver.jdbc;

    opens com.project.controller to javafx.fxml;
    exports com.project;
}
