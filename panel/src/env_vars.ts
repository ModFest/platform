"use server"

export function getPlatformUrl(): string | undefined {
	if (process.env.PLATFORM_API) {
		return process.env.PLATFORM_API
	}
	if (process.env.NODE_ENV === "development" && process.env.DEV_SERVER_URL) {
		return process.env.DEV_SERVER_URL
	}
}

export function getModrinthApi(): string {
	return process.env.MODRINTH_API!;
}

export function getModrinthSite(): string {
	return process.env.MODRINTH_SITE!;
}

export function getModrinthAppId(): string {
	return process.env.MODRINTH_APP_ID!;
}
