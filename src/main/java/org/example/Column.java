package org.example;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
// this annotation can be applied to fields.
@Target(ElementType.FIELD)
// specifies the name of the db column associated with the annotated field
public @interface Column {
    String name();
}
