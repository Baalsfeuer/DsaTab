package com.dsatab.data;

public abstract class MarkableElement extends BaseProbe implements Markable {

	private boolean unused, favorite;

	@Override
	public boolean isUnused() {
		return unused;
	}

	@Override
	public boolean isFavorite() {
		return favorite;
	}

	@Override
	public void setFavorite(boolean value) {
		this.favorite = value;
		if (favorite)
			this.unused = false;
	}

	@Override
	public void setUnused(boolean value) {
		this.unused = value;
		if (unused)
			this.favorite = false;
	}

}
