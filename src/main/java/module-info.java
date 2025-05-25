module org.example.sortalghoritms {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;


    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires java.management;
    requires jdk.management;
    requires org.json;

    // Hibernate & JPA
    requires org.hibernate.orm.core;
    requires jakarta.persistence;
    requires java.persistence;
    requires java.naming;
    requires java.desktop;

    // Deschidere pentru JavaFX
    opens com.ucv to javafx.graphics;
    opens com.ucv.controller to javafx.fxml;

    // Deschidere pentru Hibernate
    opens com.ucv.model to org.hibernate.orm.core, jakarta.persistence;

    exports com.ucv.controller;
}
