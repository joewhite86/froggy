package de.whitefrog.neobase.service;

import de.whitefrog.neobase.Service;
import de.whitefrog.neobase.collection.ExecutionResultIterator;
import de.whitefrog.neobase.collection.RelationshipResultIterator;
import de.whitefrog.neobase.cypher.Query;
import de.whitefrog.neobase.exception.RepositoryInstantiationException;
import de.whitefrog.neobase.helper.Streams;
import de.whitefrog.neobase.helper.TimeUtils;
import de.whitefrog.neobase.model.Base;
import de.whitefrog.neobase.model.Model;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.model.rest.Filter;
import de.whitefrog.neobase.model.rest.QueryField;
import de.whitefrog.neobase.model.rest.SearchParameter;
import de.whitefrog.neobase.persistence.FieldDescriptor;
import de.whitefrog.neobase.persistence.Persistence;
import de.whitefrog.neobase.repository.Repository;
import org.apache.commons.collections.CollectionUtils;
import org.neo4j.graphdb.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Search {
  private static final Logger logger = LoggerFactory.getLogger(Search.class);
  private final Service service;
  private final Repository<? extends Base> repository;
  private SearchParameter params;
  
  public Search(Repository<? extends Base> repository) {
    this.repository = repository;
    this.service = repository.service();
    this.params = new SearchParameter();
  }

  public long count() {
    Query query = repository.queryBuilder().buildSimple(params);
    if(!CollectionUtils.isEmpty(params.returns())) {
      query.query(query.query() + " return count(" + params.returns().get(0) + ") as c");
    } else {
      query.query(query.query() + " return count(*) as c");
    }
    long start = 0;

    if(logger.isDebugEnabled()) {
      start = System.nanoTime();
    }

    try {
      Result result = repository.service().graph().execute(query.query(), query.params());
      return (long) result.columnAs("c").next();
    } catch(IllegalStateException e) {
      logger.error("On query: " + query.query(), e);
      throw e;
    } finally {
      if(logger.isDebugEnabled()) {
        logger.debug("\n{}\nQuery: {}\nQueryParams: {}\ntook: {}", params, query.query(), query.params(),
          TimeUtils.formatInterval(System.nanoTime() - start, TimeUnit.NANOSECONDS));
      }
    }
  }

  public Search depth(int depth) {
    params.depth(depth);
    return this;
  }

  private Result execute(SearchParameter params) {
    Query query = repository.queryBuilder().build(params);
    long start = 0;
    
    if(logger.isDebugEnabled()) {
      start = System.nanoTime();
    }
    
    try {
      return repository.service().graph().execute(query.query(), query.params());      
    } catch(IllegalStateException e) {
      logger.error("On query: " + query.query(), e);
      throw e;
    } finally {
      if(logger.isDebugEnabled()) {
        logger.debug("\n{}\nQuery: {}\nQueryParams: {}\ntook: {}", params, query.query(), query.params(),
          TimeUtils.formatInterval(System.nanoTime() - start, TimeUnit.NANOSECONDS));
      }
    }
  }

  public Search fields(String... fields) {
    params.fields(fields);
    return this;
  }

  public Search fields(QueryField... fields) {
    params.fields(fields);
    return this;
  }

  public Search fields(FieldList fields) {
    params.fields(fields);
    return this;
  }

  public Search filter(String property, String value) {
    params.filter(property, value);
    return this;
  }

  public Search filter(Filter filter) {
    params.filter(filter);
    return this;
  }

  public Number sum(String field) {
    Query query = repository.queryBuilder().buildSimple(params);
    query.query(query.query() + " return sum(" + field + ") as c");
    logger.debug(params.toString());
    logger.debug(query.query());
    logger.debug(query.params().toString());

    Result result = repository.service().graph().execute(query.query(), query.params());

    return (Number) result.columnAs("c").next();
  }

  public <T extends Base> List<T> list() {
    return ((Stream<T>) search(params)).collect(Collectors.toList());
  }

  private Stream<? extends Base> search(SearchParameter params) {
    if(CollectionUtils.isEmpty(params.returns()) || 
        (params.returns().size() == 1 && params.returns().contains(repository.queryIdentifier()))) {
      if(!params.isFiltered() && !params.isOrdered() && params.returns() == null && params.page() == 1) {
        if(!CollectionUtils.isEmpty(params.ids())) {
          return params.ids().stream()
            .map(id -> repository.find(id, params.fields()))
            .filter(Objects::nonNull);
        }
        else if(!CollectionUtils.isEmpty(params.uuids())) {
          return params.uuids().stream()
            .map(uuid -> repository.findIndexed(Model.Uuid, uuid, params).findFirst().get())
            .filter(Objects::nonNull);
        }
      }

      Result result = execute(params);
      return Streams.get(new ExecutionResultIterator<>(repository, result, params));
    } else if(params.returns().size() == 1) {
      Result result = execute(params);
      FieldDescriptor descriptor = Persistence.cache().fieldDescriptor(repository.getModelClass(),
        params.returns().get(0));
      if(descriptor.isRelationship()) {
        return Streams.get(new RelationshipResultIterator<>(result, params));
      } else {
        try {
          Repository repository = service.repository(descriptor.baseClass().getSimpleName());
          return Streams.get(new ExecutionResultIterator<>(repository, result, params));
        }
        catch(RepositoryInstantiationException e) {
          return Streams.get(new ExecutionResultIterator<>(service, descriptor.baseClass(), result, params));
        }
      }
    } else {
      // TODO: Handle correctly
      throw new UnsupportedOperationException();
    }
  }

  public <T extends Base> Set<T> set() {
    return ((Stream<T>) search(params)).collect(Collectors.toSet());
  }

  public <T extends Base> Stream<T> stream() {
    return (Stream<T>) search(params);
  }

  public <T extends Base> T single() {
    Optional<T> optional = (Optional<T>) search(params.limit(1)).findFirst();
    return optional.isPresent()? optional.get(): null;
  }

  public Search params(SearchParameter params) {
    this.params = params;
    return this;
  }

  public Search locale(Locale locale) {
    params.locale(locale);
    return this;
  }

  public Search query(String query) {
    params.query(query);
    return this;
  }

  public Search start(int start) {
    params.start(start);
    return this;
  }

  public Search ids(Long... ids) {
    params.ids(ids);
    return this;
  }

  public Search ids(Set<Long> ids) {
    params.ids(ids);
    return this;
  }

  public Search uuids(String... uuids) {
    params.uuids(uuids);
    return this;
  }

  public Search uuids(List<String> uuids) {
    params.uuids(uuids);
    return this;
  }

  public Search uuids(Set<String> uuids) {
    params.uuids(uuids);
    return this;
  }

  public Search limit(int limit) {
    params.limit(limit);
    return this;
  }

  public Search page(int page) {
    params.page(page);
    return this;
  }

  public Search orderBy(String field) {
    params.orderBy(field);
    return this;
  }

  public Search orderBy(String field, SearchParameter.SortOrder dir) {
    params.orderBy(field, dir);
    return this;
  }

  public Search returns(String... fields) {
    params.returns(fields);
    return this;
  }
}
