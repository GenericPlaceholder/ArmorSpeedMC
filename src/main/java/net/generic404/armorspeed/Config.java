package net.generic404.armorspeed;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class Config {
	private final ArrayList<String> keys;
	private final ArrayList<String> entries;

	private Config() {
		keys = new ArrayList<>();
		entries = new ArrayList<>();
	}

	public static Config parseBytes(byte[] bytes) {
		var out = new Config();

		// get index of every newline char
		var indexes = new HashSet<Integer>();
		for (int i = 0; i < bytes.length; i++) {
			if ((char) bytes[i] == '\n') {
				indexes.add(i);
			}
		}

		// get line strings
		var lines = new String[indexes.size() + 1];
		{
			var index = 0;
			var builder = new StringBuilder();
			for (int i = 0; i < bytes.length; i++) {
				if (indexes.contains(i)) {
					lines[index] = builder.toString();
					builder = new StringBuilder();
					index++;
				} else {
					builder.append((char) bytes[i]);
				}
			}
			lines[index] = builder.toString();
		}

		var headers = new ArrayList<String>();
		for (var str : lines) {
			if (str == null) {
				continue;
			}

			if (str.contains(":")) {
				var split = str.split(":");
				if (split.length > 1 && !split[1].isEmpty()) {
					headers.remove(str.split(":")[1].strip());
				}
				if (!split[0].isEmpty()) {
					headers.add(str.split(":")[0].strip());
				}
			} else {
				var split = str.split("=");
				if (split.length > 1) {
					StringBuilder key = new StringBuilder(split[0].strip());

					if (!headers.isEmpty()) {
						for (int i = headers.size() - 1; i >= 0; i--) {
							var header = headers.get(i);
							if (!header.isEmpty()) {
								key.insert(0, header + '.');
							}
						}
					}

					out.keys.add(key.toString());
					out.entries.add(split[1].strip());
				}
			}
		}

		return out;
	}

	// this is for writing the config to a file, it returns text data as bytes
	public byte[] getBytes() {
		var lines = new ArrayList<String>();
		var indents = new ArrayList<Integer>();
		var headers = new ArrayList<String>();
		int indent = 0;

		for (var str : keys) {
			var out = new String(str);

			for (String header : headers) { // forward for loop for stripping string of headers in the order of appearance
				if (out.startsWith(header)) {
					out = out.split(header + '.', 2)[1];
				}
			}
			for (int i = headers.size()-1; i >= 0; i--) { // reverse for loop for removing headers based on order of appearance
				var header = headers.get(i);
				if (!str.contains(header)) {
					indent--;
					lines.add(':'+header);
					indents.add(indent);
					headers.remove(i);
					i = headers.size();
				}
			}

			while (out.contains(".")) {
				var split = out.split("\\.",2);

				headers.add(split[0]);
				lines.add(split[0]+":");
				indents.add(indent);
				indent++;

				out = split[1];
			}

			var value = getValue(str);
			out = out.concat(" = ").concat(value);

			lines.add(out);
			indents.add(indent);
		}
		for (var str : headers) {
			indent--;
			lines.add(':'+str);
			indents.add(indent);
		}

		int length = 0;
		for (var str : lines) {
			if (length != 0) {
				length++;
			}
			length += str.length();
		}

		var out = "";
		for (int i = 0; i < lines.size(); i++) {
			StringBuilder str = new StringBuilder(lines.get(i));

			for (int n = 0; n < indents.get(i); n++) {
				str.insert(0, '\t');
			}

			if (!out.isEmpty()) {
				out = out.concat("\n");
			}
			out = out.concat(str.toString());
		}

		return out.getBytes();
	}

	public String[] getKeys() {
		return keys.toArray(new String[0]);
	}
	public boolean hasKey(String key) {
		return keys.contains(key);
	}

	public String getValue(String key) {
		return keys.contains(key)?
				entries.get(keys.indexOf(key)):
				null;
	}
	public void setValue(String key, Object value) {
		if (keys.contains(key)) {
			var index = keys.indexOf(key);
			entries.remove(index);
			entries.add(index, String.valueOf(value));
		} else {
			keys.add(key);
			entries.add(String.valueOf(value));
		}
	}

	/**
	 * Projects the priority config's existing values onto keys (or creating them if they don't exist) of a surface config.
	 * Preserves key order from surface.
	 * @param surface The config to be projected onto.
	 * @param projected The config to be projected onto the surface config.
	 * @return A new config with the combined keys and values of both surface and projected, with projected having key priority.
	 */
	public static Config project(Config surface, Config projected) {
		var out = new Builder();

		var keys = new HashSet<String>();
		keys.addAll(surface.keys);
		keys.addAll(projected.keys);

		for (var str : keys) {
			if (projected.hasKey(str)) { // if projected value exists, write entry and continue
				out.addEntry(str,projected.getValue(str));
			} else if (surface.hasKey(str)) { // if surface value exists, write entry and continue
				out.addEntry(str,surface.getValue(str));
			}
		}

		return out.build();
	}


	public static Builder getBuilder() {
		return new Builder();
	}

	public static class Builder {
		private final ArrayList<String> entries;

		private Builder() {
			entries = new ArrayList<>();
		}

		public Builder addEntry(String key, String value) {
			entries.add(key);
			entries.add(value);
			return this;
		}

		public Config build() {
			var out = new Config();

			for (int i = 0; i < entries.size(); i += 2) {
				out.keys.add(entries.get(i));
				out.entries.add(entries.get(i+1));
			}

			return out;
		}
	}



	// armorspeed methods
	public static String CONFIG_FILE_NAME = "armorspeed.cfg";

	public static Config getDefaultConfig() {
		return Config.getBuilder()
				.addEntry("Debuff","0.01")
				.addEntry("Shield debuff","0.05")
				.addEntry("Speed offset","0.1")
				.addEntry("Affect mobs","true")
				.build();
	}

	public static Config loadConfig() throws IOException {
		var path = FabricLoader.getInstance().getConfigDir();
		var file = new File(path.toFile(),CONFIG_FILE_NAME);

		var configDefault = getDefaultConfig();

		// if the config file doesn't exist, return the default config and create the config file
		if (!file.exists()) {
			var os = new FileOutputStream(file);
			os.write(configDefault.getBytes());
			os.close();
			return configDefault;
		}

		// get config from file
		var is = new FileInputStream(file);
		var configFile = parseBytes(is.readAllBytes());
		is.close();

		// return the projected config
		return Config.project(configDefault,configFile);
	}

	public static String[] reloadConfig(Config configNew, Config configOld) {
		var out = new ArrayList<String>();

		try { // DEBUFF_AMOUNT : Debuff
			Armorspeed.DEBUFF_AMOUNT = Float.parseFloat(configNew.getValue("Debuff"));
		} catch (Exception e) {
			out.add("Debuff");
			Armorspeed.DEBUFF_AMOUNT = Float.parseFloat(configOld.getValue("Debuff"));
		}
		try { // DEBUFF_AMOUNT_SHIELD : Shield debuff
			Armorspeed.DEBUFF_AMOUNT_SHIELD = Float.parseFloat(configNew.getValue("Shield debuff"));
		} catch (Exception e) {
			out.add("Shield debuff");
			Armorspeed.DEBUFF_AMOUNT_SHIELD = Float.parseFloat(configOld.getValue("Shield debuff"));
		}
		try { // DEBUFF_OFFSET : Speed offset
			Armorspeed.DEBUFF_OFFSET = Float.parseFloat(configNew.getValue("Speed offset"));
		} catch (Exception e) {
			out.add("Speed offset");
			Armorspeed.DEBUFF_OFFSET = Float.parseFloat(configOld.getValue("Speed offset"));
		}
		try { // AFFECT_MOBS : Affect mobs
			Armorspeed.AFFECT_MOBS = Boolean.parseBoolean(configNew.getValue("Affect mobs"));
		} catch (Exception e) {
			out.add("Affect mobs");
			Armorspeed.AFFECT_MOBS = Boolean.parseBoolean(configOld.getValue("Affect mobs"));
		}

		return out.isEmpty()?null:out.toArray(new String[0]);
	}


	/** Currently loaded config */
	public static Config CONFIG;
	/** Default config */
	public static Config CONFIG_DEFAULT;

	public static void reloadConfigSafe(MinecraftServer server) {
		// Load new config from file
		Config configNew = null;
		try {
			configNew = Config.loadConfig();
		} catch (Exception e) {
			Armorspeed.LOGGER.error("Failed to read config file.");
			e.printStackTrace(System.err);
		}

		// Reload the config and log if any entries are invalid.
		var err = Config.reloadConfig(configNew,CONFIG_DEFAULT);
		if (err != null) {
			Armorspeed.LOGGER.warn("Found invalid entries in config. Entries: {}", Arrays.toString(err));
		} else {
			Armorspeed.LOGGER.info("Reloaded config.");
		}
	}

	public static void init() {
		CONFIG_DEFAULT = Config.getDefaultConfig();
		try {
			CONFIG = Config.loadConfig();
		} catch (Exception e) {
			Armorspeed.LOGGER.error("Failed to load config. Loading defaults.");
			CONFIG = CONFIG_DEFAULT;
		}

		ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, t) -> Config.reloadConfigSafe(server));
	}
}
