package de.whitefrog.froggy.exception;

/**
 * Thrown when a repository failed to initialize.
 * This can happen when a wrong model name is passed or 
 * the packages are not properly registered in the service. 
 */
public class RepositoryInstantiationException extends FroggyException {
  public RepositoryInstantiationException(String message) { super(message); }
  public RepositoryInstantiationException(Throwable cause) {
      super(cause);
  }
  
  public RepositoryInstantiationException(String s, Throwable cause) {
        super(s, cause);
    }
}
