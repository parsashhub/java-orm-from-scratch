package org.example;

import java.lang.annotation.*;

// this annotation will be available at runtime.
@Retention(RetentionPolicy.RUNTIME)
// this annotation can be applied to classes, interfaces, or enums
@Target(ElementType.TYPE)
// defines a custom annotation named Entity with one element tableName,
// which specifies the name of the db table.
public @interface Entity {
    String tableName();
}
