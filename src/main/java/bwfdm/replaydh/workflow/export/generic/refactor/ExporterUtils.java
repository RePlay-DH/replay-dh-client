package bwfdm.replaydh.workflow.export.generic.refactor;

import org.swordapp.client.UriRegistry;

public class ExporterUtils {

	/**
	 * Get a file extension (without a dot) from the file name 
	 * (e.g. "txt", "zip", * ...)
	 * 
	 * @param fileName {@link String} with the file name
	 * 
	 * @return {@link String}
	 */
	public static String getFileExtension(String fileName) {
		String extension = "";
		int i = fileName.lastIndexOf('.');
		if(i>0) {
			extension = fileName.substring(i+1);
		}
		return extension;
	}

	/**
	 * Get package format basing on the file name. 
	 * E.g. {@code UriRegistry.PACKAGE_SIMPLE_ZIP} or {@code UriRegistry.PACKAGE_BINARY}
	 * 
	 * @param fileName {@link String} with the file name
	 * 
	 * @return {@link String}
	 */
	public static String getPackageFormat(String fileName) {
		String extension = getFileExtension(fileName);

		if(extension.toLowerCase().equals("zip")) {
			return UriRegistry.PACKAGE_SIMPLE_ZIP;
		}
		return UriRegistry.PACKAGE_BINARY;
	}
	
}
