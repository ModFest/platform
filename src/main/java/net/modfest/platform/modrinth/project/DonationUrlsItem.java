package net.modfest.platform.modrinth.project;

import com.google.gson.annotations.SerializedName;

public class DonationUrlsItem {

	public enum DonationID {
		@SerializedName("patreon") PATREON,
		@SerializedName("bmac") BMAC,
		@SerializedName("paypal") PAYPAL,
		@SerializedName("github") GITHUB,
		@SerializedName("ko-fi") KOFI,
		@SerializedName("other") OTHER
	}

	public enum DonationPlatform {
		@SerializedName("Patreon") PATREON,
		@SerializedName("By Me a Coffee") BMAC,
		@SerializedName("PayPal") PAYPAL,
		@SerializedName("GitHub") GITHUB,
		@SerializedName("Ko-Fi") KOFI,
		@SerializedName("Other") OTHER
	}

	public DonationID id;
	public DonationPlatform platform;
	public String url;

	public DonationID getId() {
		return id;
	}

	public DonationPlatform getPlatform() {
		return platform;
	}

	public String getUrl() {
		return url;
	}
}