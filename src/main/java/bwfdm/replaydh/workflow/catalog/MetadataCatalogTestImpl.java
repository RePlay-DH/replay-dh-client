package bwfdm.replaydh.workflow.catalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
		this.schema=schema;
		result = new SimpleResult();
		this.createObjects();
	}
	
	private WorkflowSchema schema;
	private DefaultTool tool;
	private DefaultResource resource;
	private DefaultResource output;
	private DefaultPerson person;
	private SimpleResult result;

	@Override
	public Result query(QuerySettings settings, String fragment) throws CatalogException {
		result.clear();
		Map<Identifiable, List<String>> idValues = new HashMap<>();
		List<String> stringsTool = new ArrayList<>();
		List<String> stringsResource = new ArrayList<>();
		List<String> stringsOutput = new ArrayList<>();
		List<String> stringsPerson = new ArrayList<>();
		stringsTool.add(tool.getEnvironment());
		stringsTool.add(tool.getParameters());
		stringsTool.add(tool.getIdentifier(schema.getDefaultNameVersionIdentifierType()).getId());
		stringsTool.add(tool.getResourceType());
		idValues.put(tool, stringsTool);
		stringsResource.add(resource.getResourceType());
		stringsResource.add(resource.getIdentifier(schema.getDefaultNameVersionIdentifierType()).getId());
		stringsResource.add(resource.getIdentifier(schema.getDefaultPathIdentifierType()).getId());
		idValues.put(resource, stringsResource);
		stringsOutput.add(output.getResourceType());
		stringsOutput.add(output.getIdentifier(schema.getDefaultPathIdentifierType()).getId());
		idValues.put(output, stringsOutput);
		stringsPerson.add(person.getRole());
		stringsPerson.add(person.getIdentifier(schema.getDefaultNameIdentifierType()).getId());
		idValues.put(person, stringsPerson);
		boolean found=false;
		for(String value : idValues.get(tool)) {
			if(value.startsWith(fragment)) {
				if(!(result.isEmpty())) {
					for (Iterator<Identifiable> iter = result.iterator(); iter.hasNext(); ) {
						if(tool.equals(iter.next())) {
							found=true;
							break;
						}
					}
					if(found==false) {
						result.add(tool);
					}
				} else {
					result.add(tool);
				}
			} else if (value.endsWith(fragment)) {
				if(!(result.isEmpty())) {
					for (Iterator<Identifiable> iter = result.iterator(); iter.hasNext();) {
						if (tool.equals(iter.next())) {
							found=true;
							break;
						}
					}
					if(found==false) {
						result.add(tool);
					}
				} else {
					result.add(tool);
				}
			}
		}
		found=false;
		for(String value : idValues.get(resource)) {
			if(value.startsWith(fragment)) {
				if(!(result.isEmpty())) {
					for (Iterator<Identifiable> iter = result.iterator(); iter.hasNext(); ) {
						if(resource.equals(iter.next())) {
							found=true;
							break;
						}
					}
					if(found==false) {
						result.add(resource);
					}
				} else {
					result.add(resource);
				}
			} else if (value.endsWith(fragment)) {
				if(!(result.isEmpty())) {
					for (Iterator<Identifiable> iter = result.iterator(); iter.hasNext();) {
						if(resource.equals(iter.next())) {
							found=true;
							break;
						}
					}
					if(found==false) {
						result.add(resource);
					}
				} else {
					result.add(resource);
				}
			}
		}
		found=false;
		for(String value : idValues.get(output)) {
			if(value.startsWith(fragment)) {
				if(!(result.isEmpty())) {
					for (Iterator<Identifiable> iter = result.iterator(); iter.hasNext(); ) {
						if(output.equals(iter.next())) {
							found=true;
							break;
						}
					}
					if(found==false) {
						result.add(output);
					}
				} else {
					result.add(output);
				}
			} else if (value.endsWith(fragment)) {
				if(!(result.isEmpty())) {
					for (Iterator<Identifiable> iter = result.iterator(); iter.hasNext();) {
						if(output.equals(iter.next())) {
							found=true;
							break;
						}
					}
					if(found==false) {
						result.add(output);
					}
				} else {
					result.add(output);
				}
			}
		}
		found=false;
		for(String value : idValues.get(person)) {
			if(value.startsWith(fragment)) {
				if(!(result.isEmpty())) {
					for (Iterator<Identifiable> iter = result.iterator(); iter.hasNext(); ) {
						if(person.equals(iter.next())) {
							found=true;
							break;
						}
					}
					if(found==false) {
						result.add(person);
					}
				} else {
					result.add(person);
				}
			} else if (value.endsWith(fragment)) {
				if(!(result.isEmpty())) {
					for (Iterator<Identifiable> iter = result.iterator(); iter.hasNext();) {
						if(person.equals(iter.next())) {
							found=true;
							break;
						}
					}
					if(found==false) {
						result.add(person);
					}
				} else {
					result.add(person);
				}
			}
		}
		return result;
	}

	@Override
	public Result query(QuerySettings settings, List<Constraint> constraints) throws CatalogException {
		SimpleResult result = new SimpleResult();
		for(Constraint constraint : constraints) {
			if(constraint.getKey().equals("type")) {
				if (constraint.getValue().equals("dataset/analysis")) {
					/*if(!(result.isEmpty())) {
						for (Iterator<Identifiable> iter = result.iterator(); iter.hasNext(); ) {
							if(!(output.getSystemId().equals(iter.next().getSystemId()))) {
								result.add(output);
							}
						}
					} else {*/
						result.add(output);
					//}
				}
			}
			if(constraint.getKey().equals("parameter")) {
				if (constraint.getValue().equals("-v -file path/to/my/dir/model.xml")) {
					/*if(!(result.isEmpty())) {
						for (Iterator<Identifiable> iter = result.iterator(); iter.hasNext(); ) {
							if(!(tool.getSystemId().equals(iter.next().getSystemId()))) {
								result.add(tool);
							}
						}
					} else {*/
						result.add(tool);
					//}
				}
			}
			if(constraint.getKey().equals("env")) {
				if (constraint.getValue().equals("x86_64")) {
					/*if(!(result.isEmpty())) {
						for (Iterator<Identifiable> iter = result.iterator(); iter.hasNext(); ) {
							if(!(tool.getSystemId().equals(iter.next().getSystemId()))) {
								result.add(tool);
							}
						}
					} else {*/
						result.add(tool);
					//}
				}
			}
			if(constraint.getKey().equals("name")) {
				if (constraint.getValue().equals("Schmidt")) {
					/*if(!(result.isEmpty())) {
						for (Iterator<Identifiable> iter = result.iterator(); iter.hasNext(); ) {
							if(!(tool.getSystemId().equals(iter.next().getSystemId()))) {
								result.add(tool);
							}
						}
					} else {*/
						result.add(person);
					//}
				}
			}
		}
		return result;
	}

	@Override
	public List<String> suggest(QuerySettings settings, Identifiable context, String key, String valuePrefix)
			throws CatalogException {
		List<String> suggestions = new ArrayList<>();
		suggestions.add("hallo1a");
		suggestions.add("hallo1b");
		suggestions.add("hallo1c");
		List<String> results = new ArrayList<>();
		for(String value : suggestions) {
			if(value.startsWith(valuePrefix)) {
				results.add(value);
			}
		}
		return results;
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
		person.addIdentifier(new Identifier(schema.getDefaultNameIdentifierType(), "Schmidt"));
	}
}
