package de.whitefrog.frogr.repository;

import de.whitefrog.frogr.Service;
import de.whitefrog.frogr.cypher.QueryBuilder;
import de.whitefrog.frogr.exception.MissingRequiredException;
import de.whitefrog.frogr.exception.PersistException;
import de.whitefrog.frogr.helper.ReflectionUtil;
import de.whitefrog.frogr.model.*;
import de.whitefrog.frogr.persistence.AnnotationDescriptor;
import de.whitefrog.frogr.service.Search;
import org.apache.commons.collections.CollectionUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.PropertyContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Base repository for relationship and entities.
 * Provides most basic functionality like searching and saving.
 */
public abstract class BaseRepository<T extends Base> implements Repository<T> {
  private final Logger logger;
  @Inject
  private Service service;
  private String type;
  protected Class<?> modelClass;

  public BaseRepository() {
    this.logger = LoggerFactory.getLogger(getClass());
    this.type = getClass().getSimpleName().substring(0, getClass().getSimpleName().indexOf("Repository"));
  }
  public BaseRepository(String type) {
    this.logger = LoggerFactory.getLogger(getClass());
    this.type = type;
  }

  @Override
  public T createModel(PropertyContainer node) {
    return createModel(node, new FieldList());
  }

  @Override
  public boolean contains(T model) {
    return model.getId() != -1 && find(model.getId()) != null;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public Class<?> getModelClass() {
    if(modelClass == null) {
      modelClass = service.persistence().cache().getModel(getType());
    }

    return modelClass;
  }

  Set<String> getModelInterfaces(Class<?> clazz) {
    Set<String> output = new HashSet<>();
    Class<?>[] interfaces = clazz.getInterfaces();
    for(Class<?> i: interfaces) {
      if(Model.class.isAssignableFrom(i) && !i.equals(Model.class)) {
        output.add(i.getSimpleName());
        output.addAll(getModelInterfaces(i));
      }
    }
    return output;
  }
  
  public Logger logger() {
    return logger;
  }

  public T fetch(T tag, String... fields) {
    return fetch(tag, FieldList.parseFields(fields));
  }

  @Override
  public T fetch(T tag, FieldList fields) {
    return fetch(tag, false, fields);
  }

  @Override
  public T fetch(T tag, boolean refetch, FieldList fields) {
    service.persistence().fetch(tag, fields, refetch);
    return tag;
  }

  @Override
  public T find(long id, String... fields) {
    return search().ids(id).fields(fields).single();
  }

  @Override
  public T findByUuid(String uuid, String... fields) {
    return search().filter(Entity.Uuid, uuid).fields(fields).single();
  }

  @Override
  public GraphDatabaseService graph() {
    return service().graph();
  }

  public void initialize() {}

  @Override
  public QueryBuilder queryBuilder() {
    return new QueryBuilder(this);
  }
  
  @Override
  public String queryIdentifier() {
    return getType().toLowerCase();
  }

  @Override
  public void save(T model) throws PersistException {
    save(new SaveContext<>(this, model));
  }

  @Override
  @SafeVarargs
  public final void save(T... entities) throws PersistException {
    for(T entity : entities) {
      save(entity);
    }
  }
  
  @Override
  public Search search() {
    return new Search(this);
  }

  @Override
  public Service service() {
    return service;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void sort(List<T> list, List<SearchParameter.OrderBy> orderBy) {
    if(orderBy != null && !list.isEmpty()) {
      Class<? extends Base> clazz = list.get(0).getClass();
      final List<Field> orderedFields = new ArrayList<>(orderBy.size());
      for(SearchParameter.OrderBy order : orderBy) {
        try {
          final Field field = ReflectionUtil.getSuperField(clazz, order.field());
          final String dir = order.dir();
          if(!field.isAccessible()) field.setAccessible(true);
          list.sort((o1, o2) -> {
            try {
              // just proceed if the already ordered fields aren't equal
              if(!orderedFields.isEmpty()) {
                boolean inOrder = false;
                for(Field orderedField : orderedFields) {
                  Object val1 = orderedField.get(o1);
                  Object val2 = orderedField.get(o2);
                  if(val1 != val2 && (val1 == null || val2 == null)) {
                    inOrder = true;
                    break;
                  }
                  int compare = val1 == null? 1: ((Comparable) val1).compareTo(val2);
                  if(compare != 0) {
                    inOrder = true;
                    break;
                  }
                }
                if(inOrder) return 0;
              }
              Object val1 = field.get(o1);
              Object val2 = field.get(o2);
              if(dir.equalsIgnoreCase("asc")) {
                if(val1 == null) return -1;
                else if(val2 == null) return 1;
                return ((Comparable) val1).compareTo(val2);
              }
              else {
                if(val2 == null) return -1;
                else if(val1 == null) return 1;
                return ((Comparable) val2).compareTo(val1);
              }
            } catch(IllegalAccessException e) {
              logger.error("field " + field.getName() + ", used for sorting, is not accessible");
            }
            return 0;
          });
          orderedFields.add(field);
        } catch(NoSuchFieldException e) {
          logger.warn("couldn't sort by field " + order.field() + ", field does not exist on class " + clazz.getName());
        }
      }
    }
  }

  public void validateModel(SaveContext<T> context) {
    context.fieldMap().forEach(f -> {
      if(context.model().getCheckedFields().contains(f.getName())) return;
      AnnotationDescriptor annotations = service.persistence().cache().fieldAnnotations(context.model().getClass(), f.getName());
      // check if required fields are set
      if(!context.model().getPersisted() && annotations.required) {
        try {
          Object value = f.field().get(context.model());
          if(value == null || (value instanceof String && ((String) value).isEmpty())) {
            throw new MissingRequiredException(context.model(), f.field());
          }
        } catch(IllegalAccessException e) {
          logger.error(e.getMessage(), e);
        }
      }
      Set<ConstraintViolation<T>> violations = service().validator().validateProperty(context.model(), f.getName());
      for(ConstraintViolation<T> violation : violations) {
        logger.error(violation.getPropertyPath().toString() + " " + violation.getMessage());
      }
      if(CollectionUtils.isNotEmpty(violations)) {
        throw new javax.validation.ConstraintViolationException("violations storing " + context.model(), violations);
      }
    });
  }

  @Override
  public void dispose() {
    
  }
}
