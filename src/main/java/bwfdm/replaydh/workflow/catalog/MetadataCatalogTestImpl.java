package bwfdm.replaydh.workflow.catalog;

import java.util.ArrayList;
import java.util.List;

import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.impl.DefaultPerson;
import bwfdm.replaydh.workflow.impl.DefaultResource;
import bwfdm.replaydh.workflow.impl.DefaultTool;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * 
 * @author Florian Fritze
 *
 */
public class MetadataCatalogTestImpl implements MetadataCatalog {

	
	public MetadataCatalogTestImpl(WorkflowSchema schema) {
		super();
		// TODO Auto-generated constructor stub
		this.schema=schema;
	}
	
	private WorkflowSchema schema;
	private DefaultTool tool;
	private DefaultResource resource;
	private DefaultResource output;
	private DefaultPerson person;

	@Override
	public Result query(QuerySettings settings, String fragment) throws CatalogException {
		SimpleResult result = new SimpleResult();
		List<String> strings = new ArrayList<>();
		strings.add(tool.getEnvironment());
		strings.add(tool.getParameters());
		strings.add(tool.getIdentifier(schema.getDefaultNameVersionIdentifierType()).getId());
		return result;
	}

	@Override
	public Result query(QuerySettings settings, List<Constraint> constraints) throws CatalogException {
		SimpleResult result = new SimpleResult();
		if(constraints.get(0).getKey().equals("type")) {
			constraints.get(0).getValue().equals("dataset/analysis");
			result.add(output);
		}
		return result;
	}

	@Override
	public List<String> suggest(QuerySettings settings, Identifiable context, String key, String valuePrefix)
			throws CatalogException {
		List<String> suggestions = new ArrayList<>();
		suggestions.add("1A");
		suggestions.add("1B");
		suggestions.add("1C");
		return suggestions;
	}

	public void createObjects() {
		
		tool = DefaultTool.withSettings("-v -file path/to/my/dir/model.xml", System.getProperty("os.arch"));
		tool.addIdentifier(new Identifier(schema.getDefaultNameVersionIdentifierType(), "myTool-v1"));
		tool.setResourceType("software/parser");
		
		resource = DefaultResource.withResourceType("dataset/model");
		resource.addIdentifier(new Identifier(schema.getDefaultNameVersionIdentifierType(), "model1"));
		resource.addIdentifier(new Identifier(schema.getDefaultPathIdentifierType(), "path/to/model/dir/file.xml"));
		
		output = DefaultResource.withResourceType("dataset/analysis");
		output.addIdentifier(new Identifier(schema.getDefaultPathIdentifierType(), "output/dir/result.xml"));
		
		person = DefaultPerson.withRole("annotator");
		person.addIdentifier(new Identifier(schema.getDefaultNameIdentifierType(), "约翰 "));
	}
}
