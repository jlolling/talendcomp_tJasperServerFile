package de.jlo.talendcomp.jasperrepo;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

public class Util {

    public static String buildListAsString(Collection<? extends Object> keys, boolean sql) {
        StringBuilder sb = new StringBuilder();
        boolean firstLoop = true;
        for (Object key : keys) {
            if (key instanceof String) {
                if (firstLoop) {
                    firstLoop = false;
                } else {
                    sb.append(",");
                }
                if (sql) {
                    sb.append("'");
                }
                if (sql) {
                    sb.append(((String) key).trim().replace("'", "''"));
                } else {
                    sb.append(((String) key).trim());
                }
                if (sql) {
                    sb.append("'");
                }
            } else if (key != null) {
                if (firstLoop) {
                    firstLoop = false;
                } else {
                    sb.append(",");
                }
                sb.append(String.valueOf(key));
            }
        }
        return sb.toString();
    }

    public static String buildSQLInClause(Set<? extends Object> keys, String noKeysReplacement) {
        StringBuilder sb = new StringBuilder();
        boolean firstLoop = true;
        for (Object key : keys) {
            if (key instanceof String) {
                if (firstLoop) {
                    firstLoop = false;
                    sb.append(" in (");
                } else {
                    sb.append(",");
                }
                sb.append("'");
                sb.append(((String) key).trim().replace("'", "''"));
                sb.append("'");
            } else if (key != null) {
                if (firstLoop) {
                    firstLoop = false;
                    sb.append(" in (");
                } else {
                    sb.append(",");
                }
                sb.append(String.valueOf(key));
            }
        }
        if (firstLoop == false) {
            sb.append(") ");
        } else if (noKeysReplacement != null) {
        	sb.append(noKeysReplacement);
        } else {
            sb.append(" is not null and 1=0 "); // a dummy condition to enforce a
            // no-selection filter
        }
        return sb.toString();
    }

	public static Locale createLocale(String locale) {
		int p = locale.indexOf('_');
		String language = locale;
		String country = "";
		if (p > 0) {
			language = locale.substring(0, p);
			country = locale.substring(p);
		}
		return new Locale(language, country);
	}
	
	/**
	 * returns true if the string is not filled or contains "null"
	 * @param s the string
	 * @returns true if empty 
	 */
	public static boolean isEmpty(String s) {
		if (s == null) {
			return true;
		}
		if (s.trim().isEmpty()) {
			return true;
		}
		if (s.trim().equalsIgnoreCase("null")) {
			return true;
		}
		return false;
	}

	public static String getRelativePath(String fullPath, String basePath) {
		if (fullPath == null || fullPath.trim().isEmpty()) {
			return null;
		}
		if (basePath == null || basePath.trim().isEmpty()) {
			return fullPath;
		}
		// normalize path
		fullPath = fullPath.replaceAll("\\\\", "/").trim();
		fullPath = fullPath.replaceAll("[/]{2,}", "/").trim();
		fullPath = fullPath.replaceAll("/./", "/").trim();
		basePath = basePath.replaceAll("\\\\", "/").trim();
		basePath = basePath.replaceAll("[/]{2,}", "/").trim();
		basePath = basePath.replaceAll("/./", "/").trim();
		if (basePath.endsWith("/")) {
			basePath = basePath.substring(0, basePath.length() - 1);
		}
		int pos = fullPath.indexOf(basePath);
		if (pos == -1) {
			throw new IllegalArgumentException("fullPath does not contains basePath!");
		}
		return fullPath.substring(pos + basePath.length() + 1);
	}

	public static String getParentUri(String uri) {
		if (uri != null) {
			String folderUri = "";
			int pos = uri.lastIndexOf('/');
			if (pos == -1) {
				throw new IllegalArgumentException("uri must contain an / (minimum at start)!");
			} else if (pos > 0 && pos == (uri.length() - 1)) {
				throw new IllegalArgumentException("uri cannot have an / at the end!");
			}
			if (pos == 0) {
				folderUri = "/";
			} else {
				folderUri = uri.substring(0, pos);
			}
			return folderUri;
		} else {
			return null;
		}
	}

	public static String getResourceId(String uri) {
		if (uri != null) {
			int pos = uri.lastIndexOf('/');
			if (pos == -1) {
				throw new IllegalArgumentException("uri must contain an / (minimum at start)!");
			} else if (pos > 0 && pos == (uri.length() - 1)) {
				throw new IllegalArgumentException("uri cannot have an / at the end!");
			}
			return uri.substring(pos + 1);
		} else {
			return null;
		}
	}

	public static String buildResourceId(String name) {
		if (name == null) {
			throw new IllegalArgumentException("buildResourceId failed: name cannot be null");
		}
		name = name.replace(' ', '_');
		name = name.replace('#', '_');
		name = name.replace(':', '_');
		name = name.replace('[', '_');
		name = name.replace(']', '_');
		return name;
	}
	
}
