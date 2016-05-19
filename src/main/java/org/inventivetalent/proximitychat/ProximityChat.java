package org.inventivetalent.proximitychat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.inventivetalent.pluginannotations.PluginAnnotations;
import org.inventivetalent.pluginannotations.config.ConfigValue;

import java.util.*;

public class ProximityChat extends JavaPlugin implements Listener {

	final Random random = new Random();

	@ConfigValue(path = "alphabet") String alphabet;
	@ConfigValue(path = "distort") boolean distort = true;

	@ConfigValue(path = "radius.min") int minRadius = 8;
	@ConfigValue(path = "radius.max") int maxRadius = 32;

	int minRadiusSquared;
	int maxRadiusSquared;

	@Override
	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, this);
		saveDefaultConfig();
		PluginAnnotations.loadAll(this, this);
		minRadiusSquared = minRadius * minRadius;
		maxRadiusSquared = maxRadius * maxRadius;
	}

	@EventHandler(priority = EventPriority.LOW)
	public void on(AsyncPlayerChatEvent event) {
		if (event.isCancelled()) { return; }
		Set<Player> originalReceivers = new HashSet<>();// Players who receive the original message
		Map<Player, String> distortedReceivers = new HashMap<>();// Players who receive a distorted message
		for (Iterator<Player> iterator = event.getRecipients().iterator(); iterator.hasNext(); ) {
			Player player = iterator.next();
			if (!player.getWorld().getUID().equals(event.getPlayer().getWorld().getUID())) {
				iterator.remove();
				continue;
			}
			double distance = player.getLocation().distanceSquared(event.getPlayer().getLocation());
			if (distance > maxRadiusSquared) {
				iterator.remove();
			} else if (distort) {// Only use separate messages if we are actually changing something
				if (distance > minRadiusSquared) {
					distortedReceivers.put(player, distortMessage(event.getMessage(), random.nextInt(maxRadius - minRadius), maxRadius - minRadius));
				} else {
					originalReceivers.add(player);
				}
			}
		}

		if (distort && !originalReceivers.isEmpty() && !distortedReceivers.isEmpty()) {
			event.setCancelled(true);
			for (Player player : originalReceivers) {
				player.sendMessage(String.format(event.getFormat(), player.getDisplayName(), event.getMessage()));
			}
			for (Map.Entry<Player, String> entry : distortedReceivers.entrySet()) {
				entry.getKey().sendMessage(String.format(event.getFormat(), entry.getKey().getDisplayName(), entry.getValue()));
			}
		}
	}

	String distortMessage(String original, int chance, int max) {
		char[] originalChars = original.toCharArray();
		StringBuilder distortedBuilder = new StringBuilder();

		for (int i = 0; i < originalChars.length; i++) {
			boolean distort = !Character.isWhitespace(originalChars[i]) && random.nextInt(max) < chance;
			if (distort) {
				char replaced = alphabet.charAt(random.nextInt(alphabet.length()));
				distortedBuilder.append(replaced);
			} else {
				distortedBuilder.append(originalChars[i]);
			}
		}

		return distortedBuilder.toString();
	}

}
