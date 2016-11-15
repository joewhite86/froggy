package de.whitefrog.neobase.rest.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.whitefrog.neobase.model.Base;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.model.rest.Filter;
import de.whitefrog.neobase.model.rest.SearchParameter;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.internal.inject.AbstractContainerRequestValueFactory;
import org.glassfish.jersey.server.internal.inject.AbstractValueFactoryProvider;
import org.glassfish.jersey.server.internal.inject.MultivaluedParameterExtractorProvider;
import org.glassfish.jersey.server.model.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.mail.internet.MimeUtility;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

@Singleton
public class SearchParameterResolver extends AbstractValueFactoryProvider {
  private static final Logger logger = LoggerFactory.getLogger(SearchParameterResolver.class);
  private static final ObjectMapper mapper = new ObjectMapper();
  private SearchParameterValueFactory factory = new SearchParameterValueFactory();

  static {
    mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
      .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
      .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
      .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
      .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  public static final String ParameterName = "params";

  private static final class SearchParameterValueFactory extends AbstractContainerRequestValueFactory<SearchParameter> {
    @Context
    private ResourceContext context;
    @Context
    private HttpHeaders headers;

    /**
     * Fetch the Identity object from the request. Since HttpServletRequest is not directly available, we need to get it via
     * the injected {@link ResourceContext}.
     *
     * @return {@link SearchParameter} stored on the request, or NULL if no object was found.
     */
    public SearchParameter provide() {
      HttpServletRequest request = context.getResource(HttpServletRequest.class);
      SearchParameter params;
      // params object serialized as json in header
      if(request.getHeader(ParameterName) != null) {
        params = resolve(request.getHeader(ParameterName));
      } else if(request.getParameter(ParameterName) != null) {
        // .. in query parameter
        params = resolve(request.getParameter(ParameterName));
      } else {
        // .. as query parameters
        Enumeration<String> keys = request.getParameterNames();
        params = new SearchParameter();
        while(keys.hasMoreElements()) {
          String key = keys.nextElement();
          SearchParameterResolver.resolveParameter(params, key, request.getParameter(key));
        }
      }

      if(params.page() != 1 && params.start() == 0) {
        params.start((params.page() - 1) * params.limit());
      }

      // helper for older libraries
      if(request.getParameter("q") != null) {
        params.query(request.getParameter("q"));
      } else if(request.getParameter("query") != null) {
        params.query(request.getParameter("query"));
      }

      if(params.fields().isEmpty()) {
        params.fields(Base.AllFields);
      }

      // preceed language header, if set
      // TODO: doesn't work when Accept-Language: de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4,fr;q=0.2
//            Locale locale = headers.getAcceptableLanguages().get(0);
//            if(!locale.equals(new Locale("*", ""))) {
//                params.locale(locale);
//            }

      return params;
    }
  }

  @Inject
  public SearchParameterResolver(MultivaluedParameterExtractorProvider mpep, ServiceLocator injector) {
    super(mpep, injector, Parameter.Source.UNKNOWN);
  }

  @Override
  public AbstractContainerRequestValueFactory<?> createValueFactory(Parameter parameter) {
    Class<?> classType = parameter.getRawType();
    SearchParam annotation = parameter.getAnnotation(SearchParam.class);
    if(annotation != null && classType.isAssignableFrom(SearchParameter.class)) {
      return factory;
    }

    return null;
  }

  public static SearchParameter resolve(String params) {
    try {
      params = MimeUtility.decodeText(params);
    } catch(UnsupportedEncodingException e) {
      logger.error(e.getMessage(), e);
    }
    try {
      if(params.startsWith("{")) {
        return mapper.readValue(params, SearchParameter.class);
      } else {
        SearchParameter searchParamter = new SearchParameter();
        String[] splits = params.split(";");
        for(String split : splits) {
          String[] splitted = split.split(":");
          resolveParameter(searchParamter, splitted[0], splitted[1]);
        }
        return searchParamter;
      }
    } catch(IOException | ArrayIndexOutOfBoundsException e) {
      logger.error(e.getMessage(), e);
      throw new RuntimeException(params + " is not parsable");
    }
  }

  public static SearchParameter resolveParameter(SearchParameter params, String key, String value) {
    String[] split;
    switch(key) {
      case "q":
        params.query(value);
        break;
      case "ids":
        split = value.split(",");
        List<Long> ids = new ArrayList<>(split.length);
        for(String s : split) ids.add(Long.parseLong(s));
        params.ids(ids);
        break;
      case "uuids":
        split = value.split(",");
        params.uuids(Arrays.asList(split));
        break;
      case "count":
        params.count(true);
        break;
      case "start":
        params.start(Integer.parseInt(value));
        break;
      case "limit":
        params.limit(Integer.parseInt(value));
        break;
      case "page":
        params.page(Integer.parseInt(value));
        break;
      case "locale":
        params.locale(new Locale(value));
        break;
      case "depth":
        params.depth(Integer.parseInt(value));
        break;
      case "order":
        resolveOrder(params, value);
        break;
      case "filter":
      case "filters":
        resolveFilter(params, value);
        break;
      case "fields":
        params.fields(resolveFields(value));
        break;
      case "returns":
      case "return":
        params.returns(value);
        break;
    }
    return params;
  }

  private static FieldList resolveFields(String value) {
    List<String> fields = Arrays.asList(StringUtils.split(value, ","));
    return FieldList.parseFields(fields, true);
  }

  private static void resolveOrder(SearchParameter params, String value) {
    String[] splits = value.split(",");
    for(String split : splits) {
      SearchParameter.SortOrder dir = SearchParameter.SortOrder.ASC;
      if(split.startsWith("-")) {
        dir = SearchParameter.SortOrder.DESC;
        split = split.substring(1);
      }
      // " " needs to be captured too since "+" is converted to " " on URIs
      else if(split.startsWith("+") || split.startsWith(" ")) {
        split = split.substring(1);
      }
      params.orderBy(split, dir);
    }
  }

  private static void resolveFilter(SearchParameter params, String filterString) {
    String[] splits = filterString.split(",");
    for(String split : splits) {
      String[] splitted = split.split(":");
      String field = splitted[0];
      String value = splitted[1];
      Filter filter;
      if(value.startsWith("!")) {
        filter = new Filter.NotEquals(guessType(value.substring(1)));
      } else if(value.startsWith("<")) {
        if(value.substring(1, 2).equals("=")) {
          filter = new Filter.LessThan(Long.parseLong(value.substring(2)));
          ((Filter.LessThan) filter).setIncluding(true);
        } else {
          filter = new Filter.LessThan(Long.parseLong(value.substring(1)));
          ((Filter.LessThan) filter).setIncluding(false);
        }
      } else if(value.startsWith(">")) {
        if(value.substring(1, 2).equals("=")) {
          filter = new Filter.GreaterThan(Long.parseLong(value.substring(2)));
          ((Filter.GreaterThan) filter).setIncluding(true);
        } else {
          filter = new Filter.GreaterThan(Long.parseLong(value.substring(1)));
          ((Filter.GreaterThan) filter).setIncluding(false);
        }
      } else if(value.startsWith("(") && value.contains("-") && value.endsWith(")")) {
        String[] range = value.substring(1, value.length() - 1).split("-");
        filter = new Filter.Range(Long.parseLong(range[0]), Long.parseLong(range[1]));
      } else if(value.startsWith("=")) {
        filter = new Filter.Equals(guessType(value.substring(1)));
      } else {
        filter = new Filter.Equals(guessType(value));
      }
      params.filter(field, filter);
    }
  }

  private static Object guessType(String value) {
    try {
      return Long.parseLong(value);
    } catch(NumberFormatException e) {
      if(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
        return Boolean.parseBoolean(value);
      } else if(value.equalsIgnoreCase("null")) {
        return null;
      } else {
        return value;
      }
    }
  }
}
