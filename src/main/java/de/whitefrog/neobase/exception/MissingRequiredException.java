package de.whitefrog.neobase.exception;

import de.whitefrog.neobase.model.Base;
import de.whitefrog.neobase.model.relationship.Relationship;

import java.lang.reflect.Field;

public class MissingRequiredException extends RuntimeException {
  private Base model;
  private Relationship relationship;
  private final Field field;

  public MissingRequiredException(String msg) {
    super(msg);
    this.model = null;
    this.field = null;
  }
  public MissingRequiredException(Base model, Field field) {
    super("The value for the field \"" + field.getName() + "\" is missing on " + model);
    this.model = model;
    this.field = field;
  }
  public MissingRequiredException(Relationship model, Field field) {
    super("The value for the field \"" + field.getName() + "\" is missing on " + model);
    this.relationship = model;
    this.field = field;
  }

  public MissingRequiredException(Base model, String field) {
    super("The value for the field \"" + field + "\" is missing on " + model);
    this.model = model;
    this.field = null;
  }

  public Base getModel() {
    return model;
  }

  public Field getField() {
    return field;
  }
}
