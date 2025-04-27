module org.example.sortalghoritms {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires java.management;
    requires jdk.management;
    requires org.json;

    opens org.example.sortalghoritms to javafx.fxml;
    exports org.example.sortalghoritms;
}