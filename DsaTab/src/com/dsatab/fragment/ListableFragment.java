package com.dsatab.fragment;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.ActionMode.Callback;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.dsatab.DsaTabApplication;
import com.dsatab.R;
import com.dsatab.activity.DsaTabActivity;
import com.dsatab.activity.DsaTabPreferenceActivity;
import com.dsatab.activity.ItemsActivity;
import com.dsatab.activity.ModificatorEditActivity;
import com.dsatab.activity.NotesEditActivity;
import com.dsatab.data.Art;
import com.dsatab.data.Attribute;
import com.dsatab.data.CombatTalent;
import com.dsatab.data.Hero;
import com.dsatab.data.MetaTalent;
import com.dsatab.data.Probe;
import com.dsatab.data.Purse.Currency;
import com.dsatab.data.Spell;
import com.dsatab.data.Talent;
import com.dsatab.data.TalentGroup;
import com.dsatab.data.Value;
import com.dsatab.data.adapter.ListableItemAdapter;
import com.dsatab.data.enums.AttributeType;
import com.dsatab.data.enums.EventCategory;
import com.dsatab.data.enums.FeatureType;
import com.dsatab.data.enums.Hand;
import com.dsatab.data.enums.TalentGroupType;
import com.dsatab.data.enums.TalentType;
import com.dsatab.data.items.DistanceWeapon;
import com.dsatab.data.items.EquippedItem;
import com.dsatab.data.items.Item;
import com.dsatab.data.items.ItemContainer;
import com.dsatab.data.items.Shield;
import com.dsatab.data.items.Weapon;
import com.dsatab.data.listable.FileListable;
import com.dsatab.data.listable.HeaderListItem;
import com.dsatab.data.listable.Listable;
import com.dsatab.data.listable.PurseListable;
import com.dsatab.data.modifier.AbstractModificator;
import com.dsatab.data.modifier.CustomModificator;
import com.dsatab.data.modifier.Modificator;
import com.dsatab.data.notes.Connection;
import com.dsatab.data.notes.Event;
import com.dsatab.data.notes.NotesItem;
import com.dsatab.db.DataManager;
import com.dsatab.util.Debug;
import com.dsatab.util.Util;
import com.dsatab.view.ArtInfoDialog;
import com.dsatab.view.DirectoryChooserDialogHelper;
import com.dsatab.view.DirectoryChooserDialogHelper.Result;
import com.dsatab.view.EquippedItemChooserDialog;
import com.dsatab.view.ListSettings;
import com.dsatab.view.ListSettings.FilterType;
import com.dsatab.view.ListSettings.ListItem;
import com.dsatab.view.ListSettings.ListItemType;
import com.dsatab.view.SpellInfoDialog;
import com.dsatab.view.listener.EditListener;
import com.dsatab.view.listener.HeroInventoryChangedListener;
import com.haarman.listviewanimations.itemmanipulation.AnimateAdapter;
import com.haarman.listviewanimations.itemmanipulation.OnAnimateCallback;
import com.haarman.listviewanimations.view.DynamicListView;

public class ListableFragment extends BaseListFragment implements OnItemClickListener, HeroInventoryChangedListener,
		OnAnimateCallback {

	private static final int MENU_FILTER_GROUP = 97;

	private DynamicListView itemList;
	private ListableItemAdapter listItemAdapter;
	private AnimateAdapter<Listable> animateAdapter;

	protected static final class ModifierActionMode implements ActionMode.Callback {

		private WeakReference<ListView> listView;
		private WeakReference<BaseListFragment> listFragment;

		public ModifierActionMode(BaseListFragment fragment, ListView listView) {
			this.listFragment = new WeakReference<BaseListFragment>(fragment);
			this.listView = new WeakReference<ListView>(listView);
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, com.actionbarsherlock.view.MenuItem item) {
			boolean notifyChanged = false;

			ListView list = listView.get();
			BaseListFragment fragment = listFragment.get();
			if (list == null || fragment == null)
				return false;

			SparseBooleanArray checkedPositions = list.getCheckedItemPositions();
			if (checkedPositions != null) {
				for (int i = checkedPositions.size() - 1; i >= 0; i--) {
					if (checkedPositions.valueAt(i)) {
						Object obj = list.getItemAtPosition(checkedPositions.keyAt(i));

						if (obj instanceof CustomModificator) {
							CustomModificator modificator = (CustomModificator) obj;

							switch (item.getItemId()) {
							case R.id.option_edit:
								Intent intent = new Intent(fragment.getActivity(), ModificatorEditActivity.class);
								intent.putExtra(ModificatorEditActivity.INTENT_ID, modificator.getId());
								intent.putExtra(ModificatorEditActivity.INTENT_NAME, modificator.getModificatorName());
								intent.putExtra(ModificatorEditActivity.INTENT_RULES, modificator.getRules());
								intent.putExtra(ModificatorEditActivity.INTENT_COMMENT, modificator.getComment());
								intent.putExtra(ModificatorEditActivity.INTENT_ACTIVE, modificator.isActive());
								fragment.getActivity().startActivityForResult(intent,
										DsaTabActivity.ACTION_EDIT_MODIFICATOR);
								mode.finish();
								return true;
							case R.id.option_delete:
								DsaTabApplication.getInstance().getHero().removeModificator(modificator);
								break;
							}
						}
					}

				}
				if (notifyChanged) {
					Util.notifyDatasetChanged(list);
				}
			}
			mode.finish();
			return true;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			ListView list = listView.get();
			BaseListFragment fragment = listFragment.get();
			if (list == null || fragment == null)
				return false;

			boolean hasItems = false;
			boolean hasModifiers = false;

			SparseBooleanArray checkedPositions = list.getCheckedItemPositions();
			if (checkedPositions != null) {
				for (int i = checkedPositions.size() - 1; i >= 0; i--) {
					if (checkedPositions.valueAt(i)) {

						Object obj = list.getItemAtPosition(checkedPositions.keyAt(i));

						if (obj instanceof CustomModificator) {
							hasModifiers = true;
						}
						if (obj instanceof EquippedItem) {
							hasItems = true;
						}

					}
				}
			}

			if (hasModifiers && !hasItems) {
				mode.getMenuInflater().inflate(R.menu.modifikator_popupmenu, menu);
			} else if (hasItems && !hasModifiers) {
				mode.getMenuInflater().inflate(R.menu.equipped_item_popupmenu, menu);
			}

			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			ListView list = listView.get();
			BaseListFragment fragment = listFragment.get();
			if (list == null || fragment == null)
				return;

			fragment.mMode = null;
			list.clearChoices();

			Util.notifyDatasetChanged(list);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			ListView list = listView.get();
			BaseListFragment fragment = listFragment.get();
			if (list == null || fragment == null)
				return false;

			MenuItem edit = menu.findItem(R.id.option_edit);

			SparseBooleanArray checkedPositions = list.getCheckedItemPositions();
			if (checkedPositions != null) {
				for (int i = checkedPositions.size() - 1; i >= 0; i--) {
					if (checkedPositions.valueAt(i)) {
						Object obj = list.getItemAtPosition(checkedPositions.keyAt(i));

						if (obj instanceof CustomModificator) {
							if (edit != null && !edit.isEnabled()) {
								edit.setEnabled(true);
								return true;
							} else {
								return false;
							}
						} else {
							if (edit != null && edit.isEnabled()) {
								edit.setEnabled(false);
								return true;
							} else {
								return false;
							}
						}

					}
				}
			}

			return false;
		}
	}

	protected static final class EquippedItemActionMode implements ActionMode.Callback {

		private WeakReference<ListView> listView;
		private WeakReference<ListableFragment> listFragment;

		public EquippedItemActionMode(ListableFragment fragment, ListView listView) {
			this.listFragment = new WeakReference<ListableFragment>(fragment);
			this.listView = new WeakReference<ListView>(listView);
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, com.actionbarsherlock.view.MenuItem item) {
			final ListView list = listView.get();
			final ListableFragment fragment = listFragment.get();
			if (list == null || fragment == null)
				return false;

			boolean notifyChanged = false;
			boolean refill = false;

			SparseBooleanArray checkedPositions = list.getCheckedItemPositions();
			if (checkedPositions != null) {
				for (int i = checkedPositions.size() - 1; i >= 0; i--) {
					if (checkedPositions.valueAt(i)) {
						Object obj = list.getItemAtPosition(checkedPositions.keyAt(i));

						if (obj instanceof EquippedItem) {

							final EquippedItem equippedItem = (EquippedItem) obj;

							switch (item.getItemId()) {
							case R.id.option_edit:
								ItemsActivity.edit(fragment.getActivity(), getHero(), equippedItem,
										ItemsActivity.ACTION_EDIT);
								break;
							case R.id.option_view:
								ItemsActivity.view(fragment.getActivity(), getHero(), equippedItem);
								break;
							case R.id.option_assign_secondary: {
								final EquippedItem equippedPrimaryWeapon = equippedItem;
								final EquippedItemChooserDialog bkDialog = new EquippedItemChooserDialog(
										fragment.getActivity());

								bkDialog.setEquippedItems(getHero().getEquippedItems(Weapon.class, Shield.class));
								for (Iterator<EquippedItem> iter = bkDialog.getEquippedItems().iterator(); iter
										.hasNext();) {
									EquippedItem eq = iter.next();
									if (eq.getItemSpecification() instanceof Weapon) {
										Weapon weapon = (Weapon) eq.getItemSpecification();
										if (weapon.isTwoHanded())
											iter.remove();
									}
								}
								bkDialog.setSelectedItem(equippedItem.getSecondaryItem());
								// do not select item itself
								bkDialog.getEquippedItems().remove(equippedPrimaryWeapon);
								bkDialog.setOnAcceptListener(new EquippedItemChooserDialog.OnAcceptListener() {

									@Override
									public void onAccept(EquippedItem item, boolean bhKampf) {
										if (item != null) {
											EquippedItem equippedSecondaryWeapon = item;

											equippedPrimaryWeapon.setHand(Hand.rechts);
											equippedSecondaryWeapon.setHand(Hand.links);

											// remove 2way relation if old
											// secondary item existed
											if (equippedSecondaryWeapon.getSecondaryItem() != null
													&& equippedSecondaryWeapon.getSecondaryItem().getSecondaryItem() != null) {
												Debug.verbose("Removing old weapon sec item "
														+ equippedSecondaryWeapon.getSecondaryItem());
												equippedSecondaryWeapon.getSecondaryItem().setSecondaryItem(null);
											}
											if (equippedPrimaryWeapon.getSecondaryItem() != null
													&& equippedPrimaryWeapon.getSecondaryItem().getSecondaryItem() != null) {
												Debug.verbose("Removing old shield sec item "
														+ equippedSecondaryWeapon.getSecondaryItem());
												equippedPrimaryWeapon.getSecondaryItem().setSecondaryItem(null);
											}

											equippedPrimaryWeapon.setSecondaryItem(equippedSecondaryWeapon);
											equippedSecondaryWeapon.setSecondaryItem(equippedPrimaryWeapon);

											equippedPrimaryWeapon.setBeidhändigerKampf(bhKampf);
											equippedSecondaryWeapon.setBeidhändigerKampf(bhKampf);

											fragment.fillListItems(getHero());

										}

									}
								});

								bkDialog.show();
								break;
							}
							case R.id.option_unassign: {
								final EquippedItem equippedPrimaryWeapon = equippedItem;
								EquippedItem equippedSecondaryWeapon = equippedPrimaryWeapon.getSecondaryItem();
								equippedPrimaryWeapon.setSecondaryItem(null);
								if (equippedSecondaryWeapon != null) {
									equippedSecondaryWeapon.setSecondaryItem(null);
								}
								refill = true;
								break;
							}
							case R.id.option_select_version: {
								AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getActivity());
								List<String> specInfo = equippedItem.getItem().getSpecificationNames();
								builder.setItems(specInfo.toArray(new String[0]),
										new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog, int which) {
												equippedItem.setItemSpecification(fragment.getActivity(), equippedItem
														.getItem().getSpecifications().get(which));
												dialog.dismiss();
											}
										});

								builder.setTitle("Wähle eine Variante...");
								builder.show();
								break;
							}
							case R.id.option_select_talent: {

								final List<TalentType> specInfo = new ArrayList<TalentType>();
								final List<String> specName = new ArrayList<String>();
								if (equippedItem.getItemSpecification() instanceof Weapon) {
									Weapon weapon = (Weapon) equippedItem.getItemSpecification();
									for (TalentType type : weapon.getTalentTypes()) {
										specInfo.add(type);
										specName.add(type.xmlName());
									}
								} else if (equippedItem.getItemSpecification() instanceof Shield) {
									Shield shield = (Shield) equippedItem.getItemSpecification();
									for (TalentType type : shield.getTalentTypes()) {
										specInfo.add(type);
										specName.add(type.xmlName());
									}
								}
								if (!specInfo.isEmpty()) {
									AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getActivity());
									builder.setItems(specName.toArray(new String[0]),
											new DialogInterface.OnClickListener() {
												@Override
												public void onClick(DialogInterface dialog, int which) {
													TalentType talentType = specInfo.get(which);
													CombatTalent talent = getHero().getCombatTalent(talentType);
													if (talent != null) {
														equippedItem.setTalent(talent);
														getHero().fireItemChangedEvent(equippedItem);
													}
													dialog.dismiss();
												}
											});

									builder.setTitle("Wähle ein Talent...");
									builder.show();
								}
								break;
							}
							case R.id.option_assign_hunting: {
								getHero().setHuntingWeapon(equippedItem);
								notifyChanged = true;
								break;
							}
							case R.id.option_delete: {
								if (list.getAdapter() instanceof AnimateAdapter) {
									AnimateAdapter<Listable> adapter = (AnimateAdapter<Listable>) list.getAdapter();
									adapter.animateDismiss(checkedPositions.keyAt(i));
								} else {
									getHero().removeEquippedItem(equippedItem);
								}
								break;
							}
							}
						} else if (obj instanceof Talent) {
							Talent talent = (Talent) obj;
							switch (item.getItemId()) {
							case R.id.option_edit:
								EditListener.showEditPopup(fragment.getActivity(), talent);
								mode.finish();
								return true;
							case R.id.option_mark_favorite_talent:
								talent.setFavorite(true);
								notifyChanged = true;
								break;
							case R.id.option_mark_unused_talent:
								talent.setUnused(true);
								notifyChanged = true;
								break;
							case R.id.option_unmark_talent:
								talent.setFavorite(false);
								talent.setUnused(false);
								notifyChanged = true;
								break;
							default:
								return false;
							}
						}
					}
				}
				if (refill) {
					fragment.fillListItems(getHero());
				} else if (notifyChanged) {
					Util.notifyDatasetChanged(list);
				}
			}
			mode.finish();
			return true;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			mode.getMenuInflater().inflate(R.menu.equipped_item_popupmenu, menu);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			ListView list = listView.get();
			BaseListFragment fragment = listFragment.get();
			if (list == null || fragment == null)
				return;

			fragment.mMode = null;
			list.clearChoices();

			Util.notifyDatasetChanged(list);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			ListView list = listView.get();
			BaseListFragment fragment = listFragment.get();
			if (list == null || fragment == null)
				return false;

			SparseBooleanArray checkedPositions = list.getCheckedItemPositions();
			int selected = 0;
			if (checkedPositions != null) {
				for (int i = checkedPositions.size() - 1; i >= 0; i--) {
					if (checkedPositions.valueAt(i)) {
						selected++;
						Object obj = list.getItemAtPosition(checkedPositions.keyAt(i));

						if (obj instanceof EquippedItem) {
							EquippedItem equippedItem = (EquippedItem) obj;

							menu.findItem(R.id.option_assign_secondary).setVisible(false);
							if (equippedItem.getItemSpecification() instanceof Weapon) {
								Weapon weapon = (Weapon) equippedItem.getItemSpecification();
								if (!weapon.isTwoHanded()) {
									menu.findItem(R.id.option_assign_secondary).setVisible(true);
									if (selected == 1) {
										List<EquippedItem> items = getHero().getEquippedItems(Weapon.class,
												Shield.class);
										items.remove(equippedItem);
										menu.findItem(R.id.option_assign_secondary).setEnabled(!items.isEmpty());
									}
								}
							}

							menu.findItem(R.id.option_assign_hunting).setVisible(
									equippedItem.getItemSpecification() instanceof DistanceWeapon);

							menu.findItem(R.id.option_unassign).setVisible(equippedItem.getSecondaryItem() != null);

							menu.findItem(R.id.option_select_version).setVisible(
									equippedItem.getItem().getSpecifications().size() > 1);

							boolean hasMultipleTalentTypes = false;
							if (equippedItem.getItemSpecification() instanceof Weapon) {
								Weapon weapon = (Weapon) equippedItem.getItemSpecification();
								if (weapon.getTalentTypes().size() > 1) {
									hasMultipleTalentTypes = true;
								}
							} else if (equippedItem.getItemSpecification() instanceof Shield) {
								Shield shield = (Shield) equippedItem.getItemSpecification();
								if (shield.getTalentTypes().size() > 1) {
									hasMultipleTalentTypes = true;
								}
							}
							menu.findItem(R.id.option_select_talent).setVisible(hasMultipleTalentTypes);
						}
					}
				}
			}

			menu.findItem(R.id.option_view).setEnabled(selected == 1);
			if (menu.findItem(R.id.option_assign_secondary).isEnabled())
				menu.findItem(R.id.option_assign_secondary).setEnabled(selected == 1);
			menu.findItem(R.id.option_assign_hunting).setEnabled(selected == 1);
			menu.findItem(R.id.option_select_version).setEnabled(selected == 1);
			menu.findItem(R.id.option_select_talent).setEnabled(selected == 1);

			return true;
		}

		protected Hero getHero() {
			return DsaTabApplication.getInstance().getHero();
		}
	}

	protected static final class ArtActionMode implements ActionMode.Callback {

		private WeakReference<ListView> listView;
		private WeakReference<BaseListFragment> listFragment;

		public ArtActionMode(BaseListFragment fragment, ListView listView) {
			this.listFragment = new WeakReference<BaseListFragment>(fragment);
			this.listView = new WeakReference<ListView>(listView);
		}

		/**
		 * @param probe
		 */
		private void showInfo(Art probe, BaseFragment fragment) {
			ArtInfoDialog liturgieInfo = new ArtInfoDialog(fragment.getActivity(), fragment.getHero());
			liturgieInfo.setArt(probe);
			liturgieInfo.show();
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, com.actionbarsherlock.view.MenuItem item) {
			boolean notifyChanged = false;

			ListView list = listView.get();
			BaseListFragment fragment = listFragment.get();
			if (list == null || fragment == null)
				return false;

			SparseBooleanArray checkedPositions = list.getCheckedItemPositions();
			if (checkedPositions != null) {
				for (int i = checkedPositions.size() - 1; i >= 0; i--) {
					if (checkedPositions.valueAt(i)) {
						Object obj = list.getItemAtPosition(checkedPositions.keyAt(i));

						if (obj instanceof Art) {
							Art art = (Art) obj;

							switch (item.getItemId()) {
							case R.id.option_mark_favorite_art:
								art.setFavorite(true);
								notifyChanged = true;
								break;
							case R.id.option_mark_unused_art:
								art.setUnused(true);
								notifyChanged = true;
								break;
							case R.id.option_unmark_art:
								art.setFavorite(false);
								art.setUnused(false);
								notifyChanged = true;
								break;
							case R.id.option_view_art:
								showInfo(art, fragment);
								mode.finish();
								return true;
							default:
								return false;

							}
						}
					}

				}
				if (notifyChanged) {
					Util.notifyDatasetChanged(list);
				}
			}
			mode.finish();
			return true;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			mode.getMenuInflater().inflate(R.menu.art_popupmenu, menu);
			mode.setTitle("Künste");
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			ListView list = listView.get();
			BaseListFragment fragment = listFragment.get();
			if (list == null || fragment == null)
				return;

			fragment.mMode = null;
			list.clearChoices();

			Util.notifyDatasetChanged(list);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.actionbarsherlock.view.ActionMode.Callback#onPrepareActionMode
		 * (com.actionbarsherlock.view.ActionMode, com.actionbarsherlock.view.Menu)
		 */
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			ListView list = listView.get();
			BaseListFragment fragment = listFragment.get();
			if (list == null || fragment == null)
				return false;

			int selected = 0;
			boolean marked = false;
			SparseBooleanArray checkedPositions = list.getCheckedItemPositions();
			if (checkedPositions != null) {
				for (int i = checkedPositions.size() - 1; i >= 0; i--) {
					if (checkedPositions.valueAt(i)) {
						selected++;

						Object obj = list.getItemAtPosition(checkedPositions.keyAt(i));
						if (obj instanceof Art) {
							Art art = (Art) obj;

							marked |= art.isFavorite() || art.isUnused();
						}

					}
				}
			}

			mode.setSubtitle(selected + " ausgewählt");

			boolean changed = false;

			if (selected != 1) {

				if (menu.findItem(R.id.option_view_art).isEnabled()) {
					menu.findItem(R.id.option_view_art).setEnabled(false);
					changed = true;
				}
			} else {

				if (!menu.findItem(R.id.option_view_art).isEnabled()) {
					menu.findItem(R.id.option_view_art).setEnabled(true);
					changed = true;
				}
			}

			if (marked) {
				if (!menu.findItem(R.id.option_unmark_art).isEnabled()) {
					menu.findItem(R.id.option_unmark_art).setEnabled(true);
					changed = true;
				}
			} else {
				if (menu.findItem(R.id.option_unmark_art).isEnabled()) {
					menu.findItem(R.id.option_unmark_art).setEnabled(false);
					changed = true;
				}
			}

			return changed;
		}
	}

	protected static final class SpellActionMode implements ActionMode.Callback {

		private WeakReference<ListView> listView;
		private WeakReference<BaseListFragment> listFragment;

		public SpellActionMode(BaseListFragment fragment, ListView listView) {
			this.listFragment = new WeakReference<BaseListFragment>(fragment);
			this.listView = new WeakReference<ListView>(listView);
		}

		/**
		 * @param probe
		 */
		private void showInfo(Spell probe, Context context) {
			SpellInfoDialog spellInfo = new SpellInfoDialog(context);
			spellInfo.setSpell(probe);
			spellInfo.show();
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, com.actionbarsherlock.view.MenuItem item) {
			boolean notifyChanged = false;

			ListView list = listView.get();
			BaseListFragment fragment = listFragment.get();
			if (list == null || fragment == null)
				return false;

			SparseBooleanArray checkedPositions = list.getCheckedItemPositions();
			if (checkedPositions != null) {
				for (int i = checkedPositions.size() - 1; i >= 0; i--) {
					if (checkedPositions.valueAt(i)) {
						Object obj = list.getItemAtPosition(checkedPositions.keyAt(i));

						if (obj instanceof Spell) {
							Spell spell = (Spell) obj;

							switch (item.getItemId()) {
							case R.id.option_edit_spell:
								EditListener.showEditPopup(fragment.getActivity(), spell);
								mode.finish();
								return true;
							case R.id.option_view_spell:
								showInfo(spell, fragment.getActivity());
								mode.finish();
								return true;
							case R.id.option_mark_favorite_spell:
								spell.setFavorite(true);
								notifyChanged = true;
								break;
							case R.id.option_mark_unused_spell:
								spell.setUnused(true);
								notifyChanged = true;
								break;
							case R.id.option_unmark_spell:
								spell.setFavorite(false);
								spell.setUnused(false);
								notifyChanged = true;
								break;
							default:
								return false;
							}
						}
					}
				}
				if (notifyChanged) {
					Util.notifyDatasetChanged(list);
				}
			}

			mode.finish();
			return true;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			mode.getMenuInflater().inflate(R.menu.spell_popupmenu, menu);
			mode.setTitle("Zauber");
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			ListView list = listView.get();
			BaseListFragment fragment = listFragment.get();
			if (list == null || fragment == null)
				return;

			fragment.mMode = null;
			list.clearChoices();

			Util.notifyDatasetChanged(list);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			ListView list = listView.get();
			BaseListFragment fragment = listFragment.get();
			if (list == null || fragment == null)
				return false;

			int selected = 0;
			boolean marked = false;
			SparseBooleanArray checkedPositions = list.getCheckedItemPositions();
			if (checkedPositions != null) {
				for (int i = checkedPositions.size() - 1; i >= 0; i--) {
					if (checkedPositions.valueAt(i)) {
						selected++;
						Object obj = list.getItemAtPosition(checkedPositions.keyAt(i));
						if (obj instanceof Spell) {
							Spell spell = (Spell) obj;
							marked |= spell.isFavorite() || spell.isUnused();
						}
					}
				}
			}

			mode.setSubtitle(selected + " ausgewählt");

			boolean changed = false;

			if (selected != 1) {
				if (menu.findItem(R.id.option_edit_spell).isEnabled()) {
					menu.findItem(R.id.option_edit_spell).setEnabled(false);
					changed = true;
				}

				if (menu.findItem(R.id.option_view_spell).isEnabled()) {
					menu.findItem(R.id.option_view_spell).setEnabled(false);
					changed = true;
				}
			} else {
				if (!menu.findItem(R.id.option_edit_spell).isEnabled()) {
					menu.findItem(R.id.option_edit_spell).setEnabled(true);
					changed = true;
				}
				if (!menu.findItem(R.id.option_view_spell).isEnabled()) {
					menu.findItem(R.id.option_view_spell).setEnabled(true);
					changed = true;
				}
			}

			if (marked) {
				if (!menu.findItem(R.id.option_unmark_spell).isEnabled()) {
					menu.findItem(R.id.option_unmark_spell).setEnabled(true);
					changed = true;
				}
			} else {
				if (menu.findItem(R.id.option_unmark_spell).isEnabled()) {
					menu.findItem(R.id.option_unmark_spell).setEnabled(false);
					changed = true;
				}
			}

			return changed;
		}
	}

	protected static final class TalentActionMode implements ActionMode.Callback {

		private WeakReference<ListView> listView;
		private WeakReference<BaseListFragment> listFragment;

		public TalentActionMode(BaseListFragment fragment, ListView listView) {
			this.listFragment = new WeakReference<BaseListFragment>(fragment);
			this.listView = new WeakReference<ListView>(listView);
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, com.actionbarsherlock.view.MenuItem item) {
			boolean notifyChanged = false;
			ListView list = listView.get();
			BaseListFragment fragment = listFragment.get();
			if (list == null || fragment == null)
				return false;

			SparseBooleanArray checkedPositions = list.getCheckedItemPositions();
			if (checkedPositions != null) {
				for (int i = checkedPositions.size() - 1; i >= 0; i--) {
					if (checkedPositions.valueAt(i)) {
						Object obj = list.getItemAtPosition(checkedPositions.keyAt(i));
						if (obj instanceof Talent) {
							Talent talent = (Talent) obj;
							switch (item.getItemId()) {
							case R.id.option_edit_talent:
								EditListener.showEditPopup(fragment.getActivity(), talent);
								mode.finish();
								return true;
							case R.id.option_mark_favorite_talent:
								talent.setFavorite(true);
								notifyChanged = true;
								break;
							case R.id.option_mark_unused_talent:
								talent.setUnused(true);
								notifyChanged = true;
								break;
							case R.id.option_unmark_talent:
								talent.setFavorite(false);
								talent.setUnused(false);
								notifyChanged = true;
								break;
							default:
								return false;
							}
						} else {
							return false;
						}
					}

				}
				if (notifyChanged) {
					Util.notifyDatasetChanged(list);
				}
			}
			mode.finish();
			return true;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			mode.getMenuInflater().inflate(R.menu.talent_popupmenu, menu);
			mode.setTitle("Talente");
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			ListView list = listView.get();
			BaseListFragment fragment = listFragment.get();
			if (list == null || fragment == null)
				return;

			fragment.mMode = null;
			list.clearChoices();
			Util.notifyDatasetChanged(list);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			ListView list = listView.get();
			if (list == null)
				return false;

			SparseBooleanArray checkedPositions = list.getCheckedItemPositions();
			int selected = 0;
			boolean metaTalent = false;
			if (checkedPositions != null) {
				for (int i = checkedPositions.size() - 1; i >= 0; i--) {
					if (checkedPositions.valueAt(i)) {
						selected++;
						if (!metaTalent) {
							Object obj = list.getItemAtPosition(checkedPositions.keyAt(i));
							if (obj instanceof MetaTalent) {
								metaTalent = true;
							}
						}
					}
				}
			}

			mode.setSubtitle(selected + " ausgewählt");

			if (metaTalent || selected != 1) {
				if (menu.findItem(R.id.option_edit_talent).isEnabled()) {
					menu.findItem(R.id.option_edit_talent).setEnabled(false);
					return true;
				} else {
					return false;
				}
			} else {
				if (!menu.findItem(R.id.option_edit_talent).isEnabled()) {
					menu.findItem(R.id.option_edit_talent).setEnabled(true);
					return true;
				} else {
					return false;
				}
			}

		}
	}

	protected static final class NoteActionMode implements ActionMode.Callback {

		private WeakReference<ListView> listView;
		private WeakReference<AnimateAdapter<Listable>> animateAdapter;
		private WeakReference<BaseListFragment> listFragment;

		public NoteActionMode(BaseListFragment fragment, ListView listView, AnimateAdapter<Listable> animateAdapter) {
			this.listFragment = new WeakReference<BaseListFragment>(fragment);
			this.listView = new WeakReference<ListView>(listView);
			this.animateAdapter = new WeakReference<AnimateAdapter<Listable>>(animateAdapter);
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, com.actionbarsherlock.view.MenuItem item) {
			boolean notifyNotesChanged = false;

			ListView list = listView.get();
			BaseListFragment fragment = listFragment.get();
			AnimateAdapter<Listable> adapter = animateAdapter.get();
			if (list == null || fragment == null || adapter == null)
				return false;

			SparseBooleanArray checkedPositions = list.getCheckedItemPositions();
			if (checkedPositions != null) {
				for (int i = checkedPositions.size() - 1; i >= 0; i--) {
					if (checkedPositions.valueAt(i)) {
						Object obj = list.getItemAtPosition(checkedPositions.keyAt(i));
						if (obj instanceof Event) {
							Event event = (Event) obj;
							if (item.getItemId() == R.id.option_delete) {
								if (event.isDeletable()) {
									adapter.animateDismiss(checkedPositions.keyAt(i));
									notifyNotesChanged = true;
								}
							} else if (item.getItemId() == R.id.option_edit) {
								NotesEditActivity.edit(event, null, fragment.getActivity());

								mode.finish();
								break;
							}
						} else if (obj instanceof Connection) {
							Connection connection = (Connection) obj;
							if (item.getItemId() == R.id.option_delete) {
								adapter.animateDismiss(checkedPositions.keyAt(i));
								notifyNotesChanged = true;
							} else if (item.getItemId() == R.id.option_edit) {
								NotesEditActivity.edit(connection, fragment.getActivity());

								mode.finish();
								break;
							}
						}
					}
				}
				if (notifyNotesChanged) {
					Util.notifyDatasetChanged(list);
				}

			}
			mode.finish();
			return true;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			mode.getMenuInflater().inflate(R.menu.note_list_popupmenu, menu);
			mode.setTitle("Notizen");
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			ListView list = listView.get();
			BaseListFragment fragment = listFragment.get();
			if (list == null || fragment == null)
				return;

			fragment.mMode = null;
			list.clearChoices();

			Util.notifyDatasetChanged(list);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.actionbarsherlock.view.ActionMode.Callback#onPrepareActionMode
		 * (com.actionbarsherlock.view.ActionMode, com.actionbarsherlock.view.Menu)
		 */
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			ListView list = listView.get();
			BaseListFragment fragment = listFragment.get();
			if (list == null || fragment == null)
				return false;

			SparseBooleanArray checkedPositions = list.getCheckedItemPositions();

			com.actionbarsherlock.view.MenuItem view = menu.findItem(R.id.option_delete);
			int selected = 0;
			boolean allDeletable = true;
			if (checkedPositions != null) {
				for (int i = checkedPositions.size() - 1; i >= 0; i--) {
					if (checkedPositions.valueAt(i)) {
						Object obj = list.getItemAtPosition(checkedPositions.keyAt(i));
						selected++;
						if (obj instanceof Event) {
							Event event = (Event) obj;
							allDeletable &= event.isDeletable();
						}
					}
				}
			}

			if (allDeletable != view.isEnabled()) {
				view.setEnabled(allDeletable);
				return true;
			}

			mode.setSubtitle(selected + " ausgewählt");

			Util.notifyDatasetChanged(list);

			return false;
		}
	}

	private Callback mItemsCallback;
	private Callback mModifiersCallback;
	private Callback mTalentCallback;
	private Callback mSpellCallback;
	private Callback mArtCallback;
	private Callback mNotesCallback;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == DsaTabActivity.ACTION_ADD_MODIFICATOR) {

			if (resultCode == Activity.RESULT_OK) {

				CustomModificator modificator = new CustomModificator(getHero());
				modificator.setModificatorName(data.getStringExtra(ModificatorEditActivity.INTENT_NAME));
				modificator.setRules(data.getStringExtra(ModificatorEditActivity.INTENT_RULES));
				modificator.setComment(data.getStringExtra(ModificatorEditActivity.INTENT_COMMENT));
				modificator.setActive(data.getBooleanExtra(ModificatorEditActivity.INTENT_ACTIVE, true));

				getHero().addModificator(modificator);

			}
		} else if (requestCode == DsaTabActivity.ACTION_EDIT_MODIFICATOR) {
			if (resultCode == Activity.RESULT_OK) {

				UUID id = (UUID) data.getSerializableExtra(ModificatorEditActivity.INTENT_ID);

				for (Modificator modificator : getHero().getUserModificators()) {
					if (modificator instanceof CustomModificator) {
						CustomModificator customModificator = (CustomModificator) modificator;
						if (customModificator.getId().equals(id)) {
							customModificator.setModificatorName(data
									.getStringExtra(ModificatorEditActivity.INTENT_NAME));
							customModificator.setRules(data.getStringExtra(ModificatorEditActivity.INTENT_RULES));
							customModificator.setActive(data.getBooleanExtra(ModificatorEditActivity.INTENT_ACTIVE,
									true));
							customModificator.setComment(data.getStringExtra(ModificatorEditActivity.INTENT_COMMENT));
						}
					}
				}

			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected Callback getActionModeCallback(List<Object> objects) {

		for (Object o : objects) {
			if (o instanceof EquippedItem)
				return mItemsCallback;
			else if (o instanceof CustomModificator)
				return mModifiersCallback;
			else if (o instanceof Talent)
				return mTalentCallback;
			else if (o instanceof Spell)
				return mSpellCallback;
			else if (o instanceof Art)
				return mArtCallback;
			else if (o instanceof NotesItem)
				return mNotesCallback;
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dsatab.fragment.BaseFragment#onFilterChanged(com.dsatab.view. FilterSettings.FilterType,
	 * com.dsatab.view.FilterSettings)
	 */
	@Override
	public void onFilterChanged(FilterType type, ListSettings settings) {
		if (listItemAdapter != null && (type == FilterType.Fight || type == null) && settings instanceof ListSettings) {

			Debug.verbose("fight filter " + settings);

			ListSettings newSettings = (ListSettings) settings;

			listItemAdapter.filter(newSettings);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.actionbarsherlock.app.SherlockFragment#onCreateOptionsMenu(com. actionbarsherlock.view.Menu,
	 * com.actionbarsherlock.view.MenuInflater)
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		ListSettings listSettings = getFilterSettings();
		if (listSettings != null) {
			if (listSettings.hasListItem(ListItemType.EquippedItem) && menu.findItem(R.id.option_set) == null) {
				inflater.inflate(R.menu.menuitem_set, menu);
			}

			if (listSettings.hasListItem(ListItemType.Modificator) && menu.findItem(R.id.option_modifier_add) == null) {
				inflater.inflate(R.menu.modifikator_menu, menu);
			}

			if (listSettings.hasListItem(ListItemType.Document) && menu.findItem(R.id.option_documents_choose) == null) {
				inflater.inflate(R.menu.documents_menu, menu);
			}

			if (listSettings.hasListItem(ListItemType.Notes) && menu.findItem(R.id.option_note_add) == null) {
				inflater.inflate(R.menu.note_list_menu, menu);

				if (menu.findItem(R.id.option_note_filter) != null) {
					SubMenu filterSet = menu.findItem(R.id.option_note_filter).getSubMenu();
					EventCategory[] eventCategory = EventCategory.values();

					for (int i = 0; i < eventCategory.length; i++) {
						MenuItem item = filterSet.add(MENU_FILTER_GROUP, i, Menu.NONE, eventCategory[i].name())
								.setIcon(eventCategory[i].getDrawableId());
						item.setCheckable(true);
						item.setChecked(getFilterSettings().getEventCategories().contains(
								eventCategory[item.getItemId()]));
					}
				}

			}
		}

	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		if (getFilterSettings() != null) {
			if (menu.findItem(R.id.option_set) != null) {
				menu.findItem(R.id.option_set).setVisible(getFilterSettings().hasListItem(ListItemType.EquippedItem));
			}
			if (menu.findItem(R.id.option_modifier_add) != null) {
				menu.findItem(R.id.option_modifier_add).setVisible(
						getFilterSettings().hasListItem(ListItemType.Modificator));
			}
			if (menu.findItem(R.id.option_documents_choose) != null) {
				menu.findItem(R.id.option_documents_choose).setVisible(
						getFilterSettings().hasListItem(ListItemType.Document));
			}

			if (menu.findItem(R.id.option_note_filter) != null) {
				SubMenu filterSet = menu.findItem(R.id.option_note_filter).getSubMenu();
				EventCategory[] eventCategory = EventCategory.values();
				for (int i = 0; i < filterSet.size(); i++) {
					MenuItem item = filterSet.getItem(i);
					item.setChecked(getFilterSettings().getEventCategories().contains(eventCategory[item.getItemId()]));
				}
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.actionbarsherlock.app.SherlockFragment#onOptionsItemSelected(com. actionbarsherlock.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {

		if (item.getGroupId() == MENU_FILTER_GROUP) {

			item.setChecked(!item.isChecked());

			EventCategory category = EventCategory.values()[item.getItemId()];
			if (item.isChecked())
				getFilterSettings().getEventCategories().add(category);
			else
				getFilterSettings().getEventCategories().remove(category);

			listItemAdapter.filter(getFilterSettings());
			return true;
		}

		if (item.getItemId() == R.id.option_modifier_add) {
			getActivity().startActivityForResult(new Intent(getActivity(), ModificatorEditActivity.class),
					DsaTabActivity.ACTION_ADD_MODIFICATOR);
			return true;
		} else if (item.getItemId() == R.id.option_documents_choose) {
			Result resultListener = new Result() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see com.dsatab.view.DirectoryChooserDialogHelper.Result# onChooseDirectory(java.lang.String)
				 */
				@Override
				public void onChooseDirectory(String dir) {

					File directory = new File(dir);
					if (directory.exists()) {
						Editor edit = getPreferences().edit();
						edit.putString(DsaTabPreferenceActivity.KEY_SETUP_SDCARD_DOCUMENTS_PATH, dir);
						edit.commit();

						fillListItems(getHero());
					} else {
						Toast.makeText(getActivity(), "Verzeichnis existiert nicht. Wähle bitte ein anderes aus.",
								Toast.LENGTH_LONG).show();
					}
				}
			};
			File docFile = DsaTabApplication.getDirectory(DsaTabApplication.DIR_PDFS);
			new DirectoryChooserDialogHelper(getActivity(), resultListener, docFile.getAbsolutePath());
			return true;

		} else if (item.getItemId() == R.id.option_note_add) {
			NotesEditActivity.edit(null, null, getActivity());
			return true;
		} else if (item.getItemId() == R.id.option_note_record) {
			recordEvent();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dsatab.fragment.BaseFragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup,
	 * android.os.Bundle)
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = configureContainerView(inflater.inflate(R.layout.sheet_list, container, false));

		itemList = (DynamicListView) root.findViewById(android.R.id.list);
		itemList.setOnItemCheckedListener(this);

		mTalentCallback = new TalentActionMode(this, itemList);
		mSpellCallback = new SpellActionMode(this, itemList);
		mArtCallback = new ArtActionMode(this, itemList);
		mModifiersCallback = new ModifierActionMode(this, itemList);
		mItemsCallback = new EquippedItemActionMode(this, itemList);

		mCallback = new NoteActionMode(this, itemList, animateAdapter);

		return root;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {

		itemList.setOnItemLongClickListener(this);
		itemList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
		itemList.setOnItemClickListener(this);

		super.onActivityCreated(savedInstanceState);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dsatab.activity.BaseMenuActivity#onHeroLoaded(com.dsatab.data.Hero)
	 */
	@Override
	public void onHeroLoaded(Hero hero) {

		listItemAdapter = new ListableItemAdapter(getBaseActivity(), hero, getFilterSettings());
		listItemAdapter.setProbeListener(getProbeListener());
		listItemAdapter.setTargetListener(getTargetListener());
		listItemAdapter.setEditListener(getEditListener());

		fillListItems(hero);

		listItemAdapter.filter(getFilterSettings());

		animateAdapter = new AnimateAdapter<Listable>(listItemAdapter, this);
		animateAdapter.setAbsListView(itemList);

		itemList.setAdapter(animateAdapter);

		mNotesCallback = new NoteActionMode(this, itemList, animateAdapter);

		refreshEmptyView(listItemAdapter);
	}

	@Override
	public void onModifierAdded(Modificator value) {
		if (getFilterSettings().hasListItem(ListItemType.Modificator)) {
			listItemAdapter.add(value);
		}
		// fightItemAdapter.sort(AbstractModificator.NAME_COMPARATOR);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dsatab.view.listener.ModifierChangedListener#onPortraitChanged()
	 */
	@Override
	public void onPortraitChanged() {

	}

	@Override
	public void onModifierRemoved(Modificator value) {
		if (getFilterSettings().hasListItem(ListItemType.Modificator)) {
			listItemAdapter.remove(value);
		}

	}

	@Override
	public void onModifierChanged(Modificator value) {
		listItemAdapter.notifyDataSetChanged();
	}

	@Override
	public void onModifiersChanged(List<Modificator> values) {
		listItemAdapter.notifyDataSetChanged();
	}

	@Override
	public void onValueChanged(Value value) {
		if (value == null) {
			return;
		}

		if (getFilterSettings().isAffected(value)) {
			listItemAdapter.notifyDataSetChanged();
		}

		if (value instanceof Attribute) {
			Attribute attr = (Attribute) value;

			switch (attr.getType()) {
			case Behinderung: {
				listItemAdapter.notifyDataSetChanged();
				break;
			}
			case Körperkraft:
				if (getFilterSettings().hasListItem(ListItemType.EquippedItem)) {
					listItemAdapter.notifyDataSetChanged();
				}
				break;
			default:
				// do nothing
				break;
			}
		}
	}

	private void fillListItems(Hero hero) {
		listItemAdapter.setNotifyOnChange(false);
		listItemAdapter.clear();
		if (getFilterSettings() != null && getFilterSettings().getListItems() != null) {
			for (ListItem listItem : getFilterSettings().getListItems()) {
				switch (listItem.getType()) {
				case Talent:
					if (TextUtils.isEmpty(listItem.getName())) {

						for (TalentGroupType talentGroupType : TalentGroupType.values()) {
							TalentGroup talentGroup = hero.getTalentGroup(talentGroupType);

							if (talentGroup != null && !talentGroup.getTalents().isEmpty()) {
								listItemAdapter.add(new HeaderListItem(talentGroupType.name()));
								listItemAdapter.addAll(talentGroup.getTalents());
							}
						}

					} else {
						try {
							TalentGroupType talentGroupType = TalentGroupType.valueOf(listItem.getName());
							TalentGroup talentGroup = hero.getTalentGroup(talentGroupType);
							if (talentGroup != null && !talentGroup.getTalents().isEmpty()) {
								listItemAdapter.addAll(talentGroup.getTalents());
							}
						} catch (IllegalArgumentException e) {
							// if its no talentgrouptype name try adding talent by name
							Talent talent = hero.getTalent(listItem.getName());
							if (talent != null) {
								listItemAdapter.add(talent);
							}
						}
					}
					break;
				case Spell:
					if (TextUtils.isEmpty(listItem.getName())) {
						listItemAdapter.addAll(hero.getSpells().values());
					} else {
						Spell spell = hero.getSpell(listItem.getName());
						if (spell != null) {
							listItemAdapter.add(spell);
						}
					}
					break;
				case Art:
					if (TextUtils.isEmpty(listItem.getName())) {
						listItemAdapter.addAll(hero.getArts().values());
					} else {
						Art art = hero.getArt(listItem.getName());
						if (art != null) {
							listItemAdapter.add(art);
						}
					}
					break;
				case Attribute:
					if (TextUtils.isEmpty(listItem.getName())) {
						listItemAdapter.addAll(hero.getAttributes().values());
					} else {
						try {
							AttributeType attributeType = AttributeType.valueOf(listItem.getName());
							Attribute attr = hero.getAttribute(attributeType);
							if (attr != null) {

								switch (attr.getType()) {
								case Astralenergie_Aktuell:
								case Astralenergie:
									Integer ae = hero.getAttributeValue(AttributeType.Astralenergie);
									if (ae != null && ae > 0) {
										listItemAdapter.add(attr);
									}
									break;
								case Karmaenergie_Aktuell:
								case Karmaenergie:
									Integer ke = hero.getAttributeValue(AttributeType.Karmaenergie);
									if (ke != null && ke > 0) {
										listItemAdapter.add(attr);
									}
									break;
								default:
									listItemAdapter.add(attr);
								}

							}
						} catch (IllegalArgumentException e) {
							Debug.error(e);
						}
					}
					break;
				case EquippedItem:
					if (TextUtils.isEmpty(listItem.getName())) {
						listItemAdapter.addAll(hero.getEquippedItems());

						addWaffenloseTalente();
					} else {
						EquippedItem item = hero.getEquippedItem(getHero().getActiveSet(), listItem.getName());
						if (item != null) {
							listItemAdapter.add(item);
						}
					}
					break;
				case Modificator:
					if (TextUtils.isEmpty(listItem.getName())) {
						listItemAdapter.addAll(hero.getUserModificators());
					} else {
						listItemAdapter.add(hero.getUserModificators(listItem.getName()));
					}
					break;
				case Header:
					listItemAdapter.add(new HeaderListItem(listItem.getName()));
					break;
				case Document:
					File pdfsDir = DsaTabApplication.getDirectory(DsaTabApplication.DIR_PDFS);
					if (pdfsDir != null && pdfsDir.exists() && pdfsDir.isDirectory()) {
						File[] pdfFiles = pdfsDir.listFiles();
						List<File> documents;
						if (pdfFiles != null) {
							documents = Arrays.asList(pdfFiles);
							Collections.sort(documents, new Util.FileNameComparator());
						} else {
							documents = Collections.emptyList();
							String path = pdfsDir.getAbsolutePath();
							Toast.makeText(getActivity(),
									Util.getText(R.string.message_documents_empty, path).toString(), Toast.LENGTH_SHORT)
									.show();
						}
						for (File file : documents) {
							listItemAdapter.add(new FileListable(file));
						}
					}
					break;
				case Notes:
					if (TextUtils.isEmpty(listItem.getName())) {
						listItemAdapter.addAll(getHero().getEvents());
						listItemAdapter.addAll(getHero().getConnections());
					} else {
						for (NotesItem notesItem : getHero().getEvents()) {
							if (notesItem.getCategory().name().equals(listItem.getName())) {
								listItemAdapter.add(notesItem);
							}
						}
						for (NotesItem notesItem : getHero().getConnections()) {
							if (notesItem.getCategory().name().equals(listItem.getName())) {
								listItemAdapter.add(notesItem);
							}
						}
					}
					break;
				case Purse:
					if (TextUtils.isEmpty(listItem.getName())) {
						PurseListable purseListable = new PurseListable(null);
						listItemAdapter.add(purseListable);
					} else {
						Currency currency = Currency.valueOf(listItem.getName());
						PurseListable purseListable = new PurseListable(currency);
						listItemAdapter.add(purseListable);
					}
					break;
				}

			}
		}
		listItemAdapter.notifyDataSetChanged();
	}

	private void recordEvent() {
		try {

			File recordingsDir = DsaTabApplication.getDirectory(DsaTabApplication.DIR_RECORDINGS);

			final File currentAudio = new File(recordingsDir, "last.3gp");

			final MediaRecorder mediaRecorder = new MediaRecorder();

			mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

			mediaRecorder.setOutputFile(currentAudio.getAbsolutePath());
			mediaRecorder.prepare();
			mediaRecorder.start(); // Recording is now started

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.recording);
			builder.setMessage(R.string.recording_message);
			builder.setPositiveButton(R.string.label_save, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (mediaRecorder != null) {
						try {
							mediaRecorder.stop();
						} catch (IllegalStateException e) {
							Debug.warning("Couldn't stop mediaRecorder something went wrong.", e);
						} finally {
							mediaRecorder.reset();
						}
					}

					File nowAudio = new File(DsaTabApplication.getDirectory(DsaTabApplication.DIR_RECORDINGS), System
							.currentTimeMillis() + ".3gp");
					currentAudio.renameTo(nowAudio);

					NotesEditActivity.edit(null, nowAudio.getAbsolutePath(), getActivity());
				}
			});

			builder.setNegativeButton(R.string.label_cancel, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (mediaRecorder != null) {
						try {
							mediaRecorder.stop();
						} catch (IllegalStateException e) {
							Debug.warning("Couldn't stop mediaRecorder something went wrong.", e);
						} finally {
							mediaRecorder.reset();
						}
					}
					currentAudio.delete();
				}
			});

			AlertDialog dialog = builder.show();

			dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					if (mediaRecorder != null) {
						mediaRecorder.release();
					}
				}
			});

		} catch (IllegalStateException e) {
			Debug.error(e);
		} catch (IOException e) {
			Debug.error(e);
		}
	}

	private void addWaffenloseTalente() {
		Item raufen = DataManager.getItemByName(TalentType.Raufen.xmlName());
		Weapon raufenSpec = (Weapon) raufen.getSpecification(Weapon.class);
		if (raufenSpec != null) {
			EquippedItem raufenEquipped = new EquippedItem(getHero(), getHero().getCombatTalent(
					raufenSpec.getTalentType()), raufen, raufenSpec);
			listItemAdapter.add(raufenEquipped);
		}
		Item ringen = DataManager.getItemByName(TalentType.Ringen.xmlName());
		Weapon ringenSpec = (Weapon) ringen.getSpecification(Weapon.class);
		if (ringenSpec != null) {
			EquippedItem ringenEquipped = new EquippedItem(getHero(), getHero().getCombatTalent(
					ringenSpec.getTalentType()), ringen, ringenSpec);
			listItemAdapter.add(ringenEquipped);
		}

		if (getHero().hasFeature(FeatureType.WaffenloserKampfstilHruruzat)) {
			Item hruruzat = DataManager.getItemByName("Hruruzat");
			Weapon hruruzatSpec = (Weapon) hruruzat.getSpecification(Weapon.class);
			if (hruruzatSpec != null) {
				EquippedItem hruruzatEquipped = new EquippedItem(getHero(), getHero().getCombatTalent(
						hruruzatSpec.getTalentType()), hruruzat, hruruzatSpec);
				listItemAdapter.add(hruruzatEquipped);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget .AdapterView, android.view.View,
	 * int, long)
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		if (parent == itemList) {
			if (mMode == null) {
				Object object = listItemAdapter.getItem(position);

				if (object instanceof AbstractModificator) {
					AbstractModificator modificator = (AbstractModificator) object;
					modificator.setActive(!modificator.isActive());
				} else if (object instanceof Probe) {
					getBaseActivity().checkProbe((Probe) object);
				} else if (object instanceof FileListable) {
					FileListable fileListable = (FileListable) object;
					File file = fileListable.getFile();

					if (file.exists() && file.isFile()) {
						Uri path = Uri.fromFile(file);
						Intent intent = new Intent(Intent.ACTION_VIEW);
						String ext = MimeTypeMap.getFileExtensionFromUrl(path.toString());
						if (ext != null)
							intent.setDataAndType(path, MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext));
						else {
							intent.setData(path);
						}
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

						try {
							startActivity(intent);
						} catch (ActivityNotFoundException e) {
							Toast.makeText(getActivity(),
									"Keine App zum Betrachten von " + file.getName() + " gefunden", Toast.LENGTH_SHORT)
									.show();
						}
					}
				} else if (object instanceof Event) {
					Event event = (Event) object;

					if (event.getAudioPath() != null) {
						try {

							MediaPlayer mediaPlayer = new MediaPlayer();
							mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

							mediaPlayer.setDataSource(event.getAudioPath());
							mediaPlayer.prepare();
							mediaPlayer.start();
							mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

								@Override
								public void onCompletion(MediaPlayer mp) {
									mp.stop();
									mp.reset();
									mp.release();
								}
							});
						} catch (IllegalArgumentException e) {
							Debug.error(e);
						} catch (IllegalStateException e) {
							Debug.error(e);
						} catch (IOException e) {
							Debug.error(e);
						}

					}
				}

				itemList.setItemChecked(position, false);
			} else {
				super.onItemClick(parent, v, position, id);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dsatab.fragment.BaseFragment#onActiveSetChanged(int, int)
	 */
	@Override
	public void onActiveSetChanged(int newSet, int oldSet) {
		fillListItems(getHero());
		if (getActivity() != null) {
			getActivity().supportInvalidateOptionsMenu();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dsatab.view.listener.InventoryChangedListener#onItemAdded(com.dsatab .data.items.Item)
	 */
	@Override
	public void onItemAdded(Item item) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dsatab.view.listener.InventoryChangedListener#onItemRemoved(com.dsatab .data.items.Item)
	 */
	@Override
	public void onItemRemoved(Item item) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dsatab.view.listener.InventoryChangedListener#onItemChanged(com.dsatab .data.items.EquippedItem)
	 */
	@Override
	public void onItemChanged(EquippedItem item) {
		if (item.getSet() == getHero().getActiveSet())
			listItemAdapter.notifyDataSetChanged();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dsatab.view.listener.InventoryChangedListener#onItemEquipped(com. dsatab.data.items.EquippedItem)
	 */
	@Override
	public void onItemEquipped(EquippedItem item) {
		if (item.getSet() == getHero().getActiveSet()) {
			listItemAdapter.add(item);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dsatab.view.listener.InventoryChangedListener#onItemUnequipped(com .dsatab.data.items.EquippedItem)
	 */
	@Override
	public void onItemUnequipped(EquippedItem item) {
		if (item.getSet() == getHero().getActiveSet()) {
			listItemAdapter.remove(item);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dsatab.view.listener.HeroInventoryChangedListener#onItemChanged(com .dsatab.data.items.Item)
	 */
	@Override
	public void onItemChanged(Item item) {
		// TODO Auto-generated method stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dsatab.view.listener.HeroInventoryChangedListener#onItemContainerAdded
	 * (com.dsatab.data.items.ItemContainer)
	 */
	@Override
	public void onItemContainerAdded(ItemContainer itemContainer) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dsatab.view.listener.HeroInventoryChangedListener#onItemContainerRemoved
	 * (com.dsatab.data.items.ItemContainer)
	 */
	@Override
	public void onItemContainerRemoved(ItemContainer itemContainer) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dsatab.view.listener.HeroInventoryChangedListener#onItemContainerChanged
	 * (com.dsatab.data.items.ItemContainer)
	 */
	@Override
	public void onItemContainerChanged(ItemContainer itemContainer) {

	}

	@Override
	public void onDismiss(AbsListView list, int[] positions) {
		for (int pos : positions) {
			Listable item = listItemAdapter.getItem(pos);
			listItemAdapter.remove(item);
			if (item instanceof EquippedItem) {
				getHero().removeEquippedItem((EquippedItem) item);
			} else if (item instanceof Event)
				getHero().removeEvent((Event) item);
			else if (item instanceof Connection)
				getHero().removeConnection((Connection) item);
		}

	}

	@Override
	public void onShow(AbsListView list, int[] positions) {

	}

}
