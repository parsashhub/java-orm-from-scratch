package org.example;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
// it is used to mark a field as the primary key of the entity.
public @interface Id { }
