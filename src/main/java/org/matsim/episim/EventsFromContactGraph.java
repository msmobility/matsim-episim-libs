package org.matsim.episim;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FrameInputStream;
import net.jpountz.lz4.LZ4FrameOutputStream;
import net.jpountz.xxhash.XXHashFactory;
import org.apache.commons.compress.archivers.*;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.episim.data.ContactGraph;
import org.matsim.episim.data.EpisimContainer;
import org.matsim.episim.data.EpisimEvent;
import org.matsim.episim.data.EpisimEventProvider;

import javax.inject.Inject;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.util.*;

/**
 * Provider that reads and re-plays pre computed events from binary file.
 */
public class EventsFromContactGraph implements EpisimEventProvider {

	private static final Logger log = LogManager.getLogger(EventsFromContactGraph.class);

	/**
	 * Graph for each day.
	 */
	private final Map<DayOfWeek, ContactGraph> events = new EnumMap<>(DayOfWeek.class);

	private final EpisimConfigGroup episimConfig;
	private final Int2ObjectMap<Id<Person>> persons = new Int2ObjectOpenHashMap<>();
	private final Int2ObjectMap<EpisimContainer> container = new Int2ObjectOpenHashMap<>();

	/**
	 * Flag if init was already called.
	 */
	private boolean init = false;

	/**
	 * Write file with contact graph from MATSim scenario.
	 */
	public static void writeGraph(Path output,
								  EventsFromMATSimScenario provider,
								  Collection<EpisimConfigGroup.InfectionParams> params) throws IOException {

		// output must be a zip file
		if (!output.toString().endsWith(".tar.lz4"))
			output = Path.of(output.toString() + ".tar.lz4");


		try (var f = new LZ4FrameOutputStream(Files.newOutputStream(output), LZ4FrameOutputStream.BLOCKSIZE.SIZE_4MB, -1L,
				LZ4Factory.fastestInstance().highCompressor(), XXHashFactory.fastestInstance().hash32(), LZ4FrameOutputStream.FLG.Bits.BLOCK_INDEPENDENCE)) {

			ArchiveOutputStream archive = new ArchiveStreamFactory()
					.createArchiveOutputStream("tar", f);

			Int2ObjectMap<Id<Person>> persons = new Int2ObjectOpenHashMap<>();
			EpisimUtils.writeTarArchiveEntry(archive, "persons", out -> {
				Set<Id<Person>> ids = provider.getPersonIds();
				out.writeInt(ids.size());
				for (Id<Person> p : ids) {
					out.writeInt(p.index());
					EpisimUtils.writeChars(out, p.toString());
					persons.put(p.index(), p);
				}
			});

			Int2ObjectMap<EpisimContainer> container = new Int2ObjectOpenHashMap<>();
			EpisimUtils.writeTarArchiveEntry(archive, "container", out -> {
				Set<EpisimContainer> all = provider.getContainer();
				out.writeInt(provider.getContainer().size());
				for (EpisimContainer c : all) {
					out.writeInt(c.getContainerId().index());
					c.write(out);
					container.put(c.getContainerId().index(), c);
				}
			});

			DayOfWeek[] daysOfWeek = DayOfWeek.values();
			for (int i = 0; i < daysOfWeek.length; i++) {
				DayOfWeek day = daysOfWeek[i];

				// last previous must have been the same for events to be the same
				if (i > 0 && provider.haveSameEvents(day.minus(1), day) && provider.haveSameEvents(day.minus(2), day))
					continue;

				List<EpisimEvent> events = new ArrayList<>();

				for (EpisimEvent event : provider.forDay(day)) {
					events.add(EpisimEvent.copy(event));
				}

				ContactGraph g = new ContactGraph(events, params, persons, container);

				log.info("Writing {}, size: {} MB", day, g.getSize() / (1024 * 1024));

				TarArchiveEntry entry = new TarArchiveEntry(day.toString());
				entry.setSize(g.getSize());
				archive.putArchiveEntry(entry);

				WritableByteChannel out = Channels.newChannel(archive);
				g.write(out);

				archive.closeArchiveEntry();
			}

			archive.finish();
			archive.close();

		} catch (ArchiveException e) {
			throw new IOException(e);
		}
	}

	@Inject
	public EventsFromContactGraph(Config config) {
		this.episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup.class);
		this.init();
	}

	@Override
	public synchronized void init() {

		if (init) return;

		log.info("Init events from contact graph");
		init = true;

		try (var in = new LZ4FrameInputStream(Files.newInputStream(Path.of(episimConfig.getInputGraphFile())))) {

			ArchiveInputStream archive = new ArchiveStreamFactory().createArchiveInputStream("tar", in);

			ArchiveEntry entry;
			while ((entry = archive.getNextEntry()) != null) {

				if (entry.getName().equals("persons")) {
					DataInputStream data = new DataInputStream(archive);
					int n = data.readInt();
					for (int i = 0; i < n; i++) {
						int idx = data.readInt();
						persons.put(idx, Id.createPersonId(EpisimUtils.readChars(data)));
					}

				} else if (entry.getName().equals("container")) {
					DataInputStream data = new DataInputStream(archive);
					int n = data.readInt();
					for (int i = 0; i < n; i++) {
						int idx = data.readInt();
						container.put(idx, EpisimContainer.read(data));
					}
				} else {

					DayOfWeek day = DayOfWeek.valueOf(entry.getName());
					ContactGraph g = new ContactGraph(archive, episimConfig.getInfectionParams(), persons, container);

					log.info("Read graph for {}, size: {}MB", day, g.getSize() / (1024 * 1024));
					events.put(day, g);

				}
			}

		} catch (IOException | ArchiveException e) {
			throw new IllegalStateException("Could not read input event graph", e);
		}
	}

	@Override
	public Collection<Id<Person>> getPersonIds() {
		return persons.values();
	}

	@Override
	public Collection<EpisimContainer> getContainer() {
		return container.values();
	}

	@Override
	public Iterable<EpisimEvent> forDay(DayOfWeek day) {

		if (events.containsKey(day))
			return events.get(day);

		// find previous day with existing events
		for (int i = day.ordinal(); i >= 0; i--) {

			DayOfWeek prevDay = DayOfWeek.values()[i];
			if (events.containsKey(prevDay))
				return events.get(prevDay);

		}

		throw new IllegalStateException("No events for day: " + day);
	}
}
