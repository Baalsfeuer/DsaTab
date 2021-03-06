package com.dsatab;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.dsatab.activity.DsaTabPreferenceActivity;
import com.dsatab.data.enums.ArmorPosition;
import com.dsatab.data.enums.Position;
import com.dsatab.fragment.AnimalFragment;
import com.dsatab.fragment.BaseFragment;
import com.dsatab.fragment.BodyFragment;
import com.dsatab.fragment.CharacterFragment;
import com.dsatab.fragment.ItemsFragment;
import com.dsatab.fragment.ListableFragment;
import com.dsatab.fragment.MapFragment;
import com.dsatab.fragment.NotesEditFragment;
import com.dsatab.fragment.item.ItemListFragment;

/**
 *  
 */
public class DsaTabConfiguration {

	private Map<Class<? extends BaseFragment>, Integer> tabResourceIds = new HashMap<Class<? extends BaseFragment>, Integer>(
			15);

	private Context context;

	private List<Integer> tabIcons;
	private List<Integer> itemIcons;

	public enum WoundType {
		Standard("Standard"), Trefferzonen("Trefferzonen");

		private String title;

		private WoundType(String t) {
			title = t;
		}

		public String title() {
			return title;
		}
	}

	public enum ArmorType {
		ZonenRuestung("Zonenrüstung"), GesamtRuestung("Gesamte Rüstung");

		private String title;

		private ArmorType(String t) {
			title = t;
		}

		public String title() {
			return title;
		}
	}

	public DsaTabConfiguration(Context context) {
		this.context = context;

		tabResourceIds.put(CharacterFragment.class, R.drawable.tab_character);
		tabResourceIds.put(BodyFragment.class, R.drawable.tab_wound);
		tabResourceIds.put(ListableFragment.class, R.drawable.tab_fight);
		tabResourceIds.put(ItemsFragment.class, R.drawable.tab_items);
		tabResourceIds.put(ItemListFragment.class, R.drawable.tab_items);
		tabResourceIds.put(NotesEditFragment.class, R.drawable.tab_notes);
		tabResourceIds.put(MapFragment.class, R.drawable.tab_map);
		tabResourceIds.put(AnimalFragment.class, R.drawable.tab_animal);

		// DO NOT CHANGE ORDER OF ICONS BECAUSE IT IS stored in tabconfiguration
		// as tabResourceIndex, ALWAYS ADD AT THE LASt POSITION!!!
		tabIcons = Arrays.asList(R.drawable.tab_character, R.drawable.tab_coins, R.drawable.tab_fight,
				R.drawable.tab_items, R.drawable.tab_magic, R.drawable.tab_liturige, R.drawable.tab_map,
				R.drawable.tab_notes, R.drawable.tab_talents, R.drawable.tab_wound, R.drawable.tab_pdf,
				R.drawable.tab_art, R.drawable.tab_animal);

		// DO NOT CHANGE ORDER OF ICONS BECAUSE IT IS stored in tabconfiguration
		// as tabResourceIndex, ALWAYS ADD AT THE LASt POSITION!!!
		itemIcons = Arrays.asList(R.drawable.icon_2hieb, R.drawable.icon_2schwert, R.drawable.icon_armor_chain,
				R.drawable.icon_armor_cloth, R.drawable.icon_armor_metal, R.drawable.icon_armor,
				R.drawable.icon_attack, R.drawable.icon_ausweichen, R.drawable.icon_bags, R.drawable.icon_bow,
				R.drawable.icon_crossbow, R.drawable.icon_diskus, R.drawable.icon_fecht, R.drawable.icon_fist,
				R.drawable.icon_greaves, R.drawable.icon_halberd, R.drawable.icon_hammer, R.drawable.icon_helm_full,
				R.drawable.icon_helm_half, R.drawable.icon_helm, R.drawable.icon_hieb, R.drawable.icon_kettenwaffen,
				R.drawable.icon_longbow, R.drawable.icon_longsword, R.drawable.icon_messer,
				R.drawable.icon_metal_shield, R.drawable.icon_misc_multi, R.drawable.icon_misc, R.drawable.icon_net,
				R.drawable.icon_shield, R.drawable.icon_sling, R.drawable.icon_special, R.drawable.icon_speer,
				R.drawable.icon_stab, R.drawable.icon_steel_armor, R.drawable.icon_sword, R.drawable.icon_whip,
				R.drawable.icon_wurfbeil, R.drawable.icon_wurfdolch);
	}

	public List<Integer> getTabIcons() {
		return tabIcons;
	}

	public List<Integer> getItemIcons() {
		return itemIcons;
	}

	public int getTabResourceId(Class<? extends BaseFragment> clazz) {
		if (tabResourceIds.containsKey(clazz)) {
			return tabResourceIds.get(clazz);
		} else
			return 0;
	}

	public int getIndexOfTabResourceId(int resourceId) {
		return tabIcons.indexOf(resourceId);
	}

	public List<ArmorPosition> getArmorPositions() {
		return Arrays.asList(ArmorPosition.values());
	}

	public List<Position> getWoundPositions() {
		if (DsaTabApplication.getPreferences().getBoolean(DsaTabPreferenceActivity.KEY_HOUSE_RULES_MORE_WOUND_ZONES,
				false)) {
			return Position.WOUND_POSITIONS;
		} else
			return Position.WOUND_POSITIONS;
	}

	public ArmorType getArmorType() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		return ArmorType.valueOf(preferences.getString(DsaTabPreferenceActivity.KEY_ARMOR_TYPE,
				ArmorType.ZonenRuestung.name()));
	}

	public WoundType getWoundType() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		return WoundType.valueOf(preferences.getString(DsaTabPreferenceActivity.KEY_WOUND_TYPE,
				WoundType.Trefferzonen.name()));
	}

}
