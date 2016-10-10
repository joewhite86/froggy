package de.whitefrog.neobase.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.whitefrog.neobase.collection.ExecutionResultIterator;
import de.whitefrog.neobase.model.Model;
import de.whitefrog.neobase.model.relationship.BaseRelationship;
import de.whitefrog.neobase.Service;
import org.neo4j.graphdb.Transaction;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Export {
  static final String path = "src/main/resources/test-data";
  public static void main(String[] args) throws Exception {
    try(Service service = new Service()) {
      service.connect();
      ObjectMapper mapper = new ObjectMapper();

      try(Transaction tx = service.beginTx()) {
        File output = new File(path + "/models.json");
//        ExecutionResultIterator<Model> iterator = new ExecutionResultIterator<>(service,
//          service.graph().execute("match (e) return e"), "e");
//        List<Model> models = Iterators.asList(Iterables.get(iterator));
//        mapper.writerWithDefaultPrettyPrinter().writeValue(output, models);

        output = new File(path + "/relationships.json");
        List<BaseRelationship> relationships = new ArrayList<>();
//        for(Base model : models) {
//          Node node = Persistence.getNode(model);
//          for(Relationship relationship : node.getRelationships(Direction.OUTGOING)) {
//            relationships.add(new DefaultRelationship(service).getRelationship(
//              model.clone(Arrays.asList(Base.IdProperty, Base.Uuid)), relationship,
//              Arrays.asList(Base.IdProperty, Base.Uuid)));
//          }
//        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(output, relationships);
      }
    }
  }
}
