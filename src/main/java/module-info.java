module com.project.cdiem {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.microsoft.sqlserver.jdbc;

    opens com.project.controller to javafx.fxml;
    exports com.project;
}
