/*
 * Unless expressly otherwise stated, code from this project is licensed under the MIT license [https://opensource.org/licenses/MIT].
 *
 * Copyright (c) <2018> <Markus Gärtner, Volodymyr Kushnarenko, Florian Fritze, Sibylle Hermann and Uli Hahn>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package bwfdm.replaydh.utils;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Iterator;
import java.util.Locale;
import java.util.function.IntConsumer;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHException;
import bwfdm.replaydh.core.RDHProperty;
import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.schema.IdentifierType;
import bwfdm.replaydh.workflow.schema.IdentifierType.Uniqueness;


/**
 * Additional util methods, which are not implemented in the basic Java functionality.
 *
 * @author vk
 */
public class RDHUtils {

	public static void checkState(String msg, boolean condition) {
		if(!condition)
			throw new IllegalStateException(msg);
	}

	public static void checkArgument(String msg, boolean condition) {
		if(!condition)
			throw new IllegalArgumentException(msg);
	}

	public static void cleanDirectory(final Path root) throws IOException {
		Files.walkFileTree(root, new FileVisitor<Path>() {

			@Override
			public FileVisitResult preVisitDirectory(Path dir,
					BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file,
					BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc)
					throws IOException {
				throw exc;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc)
					throws IOException {
				if(!Files.isSameFile(root, dir)) {
					Files.delete(dir);
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}


	public static Path normalize(Path path) {
		return path.normalize().toAbsolutePath();
	}


	final private static char[] hexSymbols = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexSymbols[v >>> 4];
	        hexChars[j * 2 + 1] = hexSymbols[v & 0x0F];
	    }
	    return new String(hexChars);
	}

	public static void bytesToHex(byte[] bytes, IntConsumer drain) {
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        drain.accept(hexSymbols[v >>> 4]);
	        drain.accept(hexSymbols[v & 0x0F]);
	    }
	}

    private static final DateTimeFormatter WORKFLOW_DATE_TIME;
//    	= DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US);

    static {
    	WORKFLOW_DATE_TIME = new DateTimeFormatterBuilder()
    			.parseStrict()
    			.appendValue(ChronoField.YEAR, 4)
    			.appendLiteral('-')
    			.appendValue(ChronoField.MONTH_OF_YEAR, 2)
    			.appendLiteral('-')
    			.appendValue(ChronoField.DAY_OF_MONTH, 2)
    			.appendLiteral(' ')
    			.appendValue(ChronoField.HOUR_OF_DAY, 2)
    			.appendLiteral(':')
    			.appendValue(ChronoField.MINUTE_OF_HOUR, 2)
    			.appendLiteral(':')
    			.appendValue(ChronoField.SECOND_OF_MINUTE, 2)
    			.toFormatter(Locale.US);
    }

    public static String formatTimestamp(LocalDateTime time) {
    	return WORKFLOW_DATE_TIME.format(time);
    }

    public static LocalDateTime parseTimestamp(String s) {
    	return WORKFLOW_DATE_TIME.parse(s, LocalDateTime::from);
    }

    public static <T extends Object> T firstSet(T...options) {
    	for(T item : options) {
    		if(item!=null) {
    			return item;
    		}
    	}

    	return null;
    }

    public static final int DEFAULT_PATH_STRING_LENGTH_LIMIT = 50;

	public static String toPathString(Path file, int limit) {
		file = file.toAbsolutePath();

		String path = file.toString();

		if(path.length()>limit && file.getNameCount()>3) {
			StringBuilder left = new StringBuilder(30);
			StringBuilder right = new StringBuilder(30);
			char SEP = File.separatorChar;

			Path root = file.getRoot();
			if(root!=null) {
				left.append(root.toString()).append(SEP);
			}

			int leftIndex = 0;
			int rightIndex = file.getNameCount()-1;

			boolean useRight = true;

			while(left.length()+right.length()<limit && rightIndex>leftIndex) {
				if(useRight) {
					right.insert(0, file.getName(rightIndex--).toString()).insert(0, SEP);
				} else {
					left.append(file.getName(leftIndex++).toString()).append(SEP);
				}

				useRight = !useRight;
			}

			path = left.append("...").append(right).toString();
		}

		return path;
	}

    /**
     * Tries to find a context ({@code String} value) that can be used to globally
     * disambiguate the given identifier. This relies heavily on the {@link Uniqueness}
     * of the identifier itself.
     *
     * Current strategy:
     *
     * <ol>
     * <li>For {@link Uniqueness#GLOBALLY_UNIQUE globally unique} identifiers, return {@code null}.</li>
     * <li>For {@link Uniqueness#ENVIRONMENT_UNIQUE institute unique} identifiers, use the
     * {@link RDHProperty#CLIENT_ORGANIZATION} property of the given {@code environment}.</li>
     * <li>For {@link Uniqueness#LOCALLY_UNIQUE locally unique} identifiers, use the current {@link RDHEnvironment#getWorkspace() workspace} path.</li>
     * <li>For all other types or in case any of the above steps encountered a problem
     * (e.g. no workspace defined yet) this method will throw an {@link RDHException exception}.</li>
     * </ol>
     *
     * @param identifier
     * @param environment
     * @return
     *
     * @throws RDHException if not enough data is provided through settings or the current state of the
     * given environment to properly create a disambiguation context.
     */
    public static String createDisambiguationContext(Identifier identifier, RDHEnvironment environment) {
    	requireNonNull(identifier);
    	requireNonNull(environment);

    	IdentifierType type = identifier.getType();
    	if(type==null)
    		throw new RDHException("Unknown identifier type for disambugation: "+identifier.getType());

    	Uniqueness uniqueness = type.getUniqueness();

    	switch (uniqueness) {
    	// No disambiguation required for globally unique identifiers
		case GLOBALLY_UNIQUE: return null;

		case ENVIRONMENT_UNIQUE: {
			String context = environment.getProperty(RDHProperty.CLIENT_ORGANIZATION);
			if(context==null)
				throw new RDHException("No '"+RDHProperty.CLIENT_ORGANIZATION.getKey()+"' property defined for disambiguation");

			return context;
		}

		case LOCALLY_UNIQUE: {
			Path workspace = environment.getWorkspacePath();
			if(workspace==null)
				throw new RDHException("No workspace available for disambiguation");
			return workspace.toAbsolutePath().toString();
		}

		default:
			throw new RDHException("Unable to disambiguate identifier of type: "+type.getStringValue());
		}
    }

    /**
     * Concatenates all the identities associated with the given {@link Identifiable} to a human
     * readable string.
     *
     * @param identifiable
     * @return
     */
    public static String getCompleteDisplayName(Identifiable identifiable) {
    	StringBuilder sb = new StringBuilder();
    	sb.append('[');

    	for(Iterator<Identifier> it=identifiable.getIdentifiers().iterator(); it.hasNext();) {
    		Identifier identifier = it.next();
    		sb.append(identifier.getType()).append('=').append(identifier.getId());
    		if(it.hasNext()) {
    			sb.append(", ");
    		}
    	}

    	sb.append(']');

    	return sb.toString();
    }

	public static String replaceNotAllowedCharacters(final String str) {
		String output = new String(str);
	
		// Needed for Apache server!
		output = output.replace("\\", "/");
		output = output.replace(" ", "_"); //TODO: replace with "%20" ??
		
		return output;
	}
}


